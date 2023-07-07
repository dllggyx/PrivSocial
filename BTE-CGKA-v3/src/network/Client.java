package network;

import Curve25519.Curve25519KeyPairGenerator;
import key.IdentityKeys;
import key.PublicKeyPair;
import key.SignKeyPair;
import tree_strcture.BinaryTree;
import tree_strcture.Group;
import utils.MyObjectOutputStream;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.*;

public class Client implements Runnable{

    public volatile boolean exitThread = false;
    public String ID;
    private IdentityKeys identityKeys;
    public BinaryTree Btree = null;
    public int serverPort = 8088;

    //mThread: Used to send data to the server
    private Thread mThread = null;
    //receiveThread: Used to receive data from the server
    //private Thread receiveThread = null;
    //handleThread: Used to process message lists $ReceiveByffer$
    //private Thread handleThread = null;
    private ClientHandler cHandler = null;
    private MsgHandler mHandler = null;
    private Socket mSocket = null;
    private OutputStream os = null;
    private MyObjectOutputStream oos = null;
    private Curve25519KeyPairGenerator keyPairGenerator = null;
    public Queue<MyMessage> ReceiveByffer = null;

    public Client(String id){
        ID = id;
        keyPairGenerator = new Curve25519KeyPairGenerator();
        generateKey();
        Btree = new BinaryTree(identityKeys);
        ReceiveByffer = new LinkedList<MyMessage>();
    }


    public void generateKey(){
        byte[] delta = new byte[16];
        Random rand = new Random();
        rand.nextBytes(delta);

        HashMap<String,byte[]> keyPair1 = keyPairGenerator.generateKeyPair(delta);
        PublicKeyPair pkp = new PublicKeyPair(keyPair1.get("publicKey"),keyPair1.get("privateKey"));


        rand.nextBytes(delta);
        HashMap<String,byte[]> keyPair2 = keyPairGenerator.generateKeyPair(delta);
        SignKeyPair skp = new SignKeyPair(keyPair1.get("publicKey"),keyPair1.get("privateKey"));
        identityKeys = new IdentityKeys(pkp,skp);

    }

    public IdentityKeys getPkAndSvk(){
        return new IdentityKeys(new PublicKeyPair(identityKeys.pkp.pk,null),
                new SignKeyPair(identityKeys.skp.svk,null));
    }

    public IdentityKeys getFullIdentityKeys(){
        return identityKeys;
    }

    public void startClient(){
        try{
            mSocket = new Socket(InetAddress.getByName("127.0.0.1"),serverPort);
            os = mSocket.getOutputStream();
            oos = new MyObjectOutputStream(os);
            cHandler = new ClientHandler(mSocket,ReceiveByffer,exitThread);
            Thread receiveThread = new Thread(cHandler);
            receiveThread.start();
            mHandler = new MsgHandler(ReceiveByffer,Btree,exitThread);
            Thread handleThread = new Thread(mHandler);
            handleThread.start();
        }catch(Exception e) {
            e.printStackTrace();
        }
        mThread = new Thread(this,"thread-"+ID);
        mThread.start();
    }

    public void join(){
        try {
            exitThread = true;
            mThread.join();
            cHandler.exitThread = true;
            mHandler.exitThread = true;
            if(mSocket != null) {
                //oos.close();
                oos.writeObject(null);
                oos.flush();
                mSocket.close();
                mSocket = null;
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        mThread = null;
    }

    @Override
    public void run() {
        //System.out.println("ID:"+ID+"=============================");
        //向服务器发送自己的ID
        try {
            oos.writeObject(ID);
            oos.flush();
        }catch (Exception e){
            e.printStackTrace();
        }
        while(!exitThread) {

            if (Btree.infoBuffer.size() > 0) {
                Queue<Information> transmitList = new LinkedList();
                for(int i=0;i<Btree.infoBuffer.size();i++){
                    Information info = Btree.infoBuffer.poll();
                    transmitList.add(info);
                }

                try {
//                    if (mSocket != null){
//                        mSocket.close();
//                        mSocket = null;
//                    }
//                    mSocket = new Socket(InetAddress.getByName("127.0.0.1"),serverPort);
                    oos.writeObject(transmitList);
                    oos.flush();
                    //oos.close();
                    //os.close();
                    //oos = null;
                    //os = null;
                } catch (Exception e) {
                    e.printStackTrace();
                } finally{

                }
            }
        }

    }

    public Socket getSocket(){
        return mSocket;
    }


    public void Create(Group group){
        Btree.startTime = System.currentTimeMillis();
        Btree.create(group,ID);
        identityKeys = Btree.mine.getIdentityKeys();
    }

    public void Update(){
        Btree.startTime = System.currentTimeMillis();
        Btree.update(ID);
        identityKeys = Btree.mine.getIdentityKeys();
    }

    public void Add(String targetID,IdentityKeys targetKey){
        Btree.startTime = System.currentTimeMillis();
        Btree.add(ID,targetID,targetKey);
    }

    public void Remove(String targetID,IdentityKeys targetKey){
        Btree.startTime = System.currentTimeMillis();
        Btree.remove(ID,targetID,targetKey);
        identityKeys = Btree.mine.getIdentityKeys();
    }
}
