package network;

import key.IdentityKeys;
import key.PublicKeyPair;
import key.SignKeyPair;
import tree_strcture.BinaryTree;
import tree_strcture.Group;
import utils.MyElGamal;
import utils.MyObjectOutputStream;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.*;

public class Client implements Runnable{

    public volatile boolean exitThread = false;
    public String role;
    public String ID;
    private IdentityKeys identityKeys;
    public BinaryTree Btree = null;
    public int serverPort = 8088;

    //mThread: Used to send data to the server
    private Thread mThread = null;
    //receiveThread: Used to receive data from the server
    private Thread receiveThread = null;
    //handleThread: Used to process message lists $ReceiveByffer$
    private Thread handleThread = null;
    private Socket mSocket = null;
    private OutputStream os = null;
    private MyObjectOutputStream oos = null;

    public Queue<MyMessage> ReceiveByffer = null;

    public Client(String id){
        ID = id;
        generateKey();
        Btree = new BinaryTree(identityKeys);
        ReceiveByffer = new LinkedList<MyMessage>();
    }

    public void setRole(String clientRole){
        switch (clientRole){
            case "sender":
                role = "sender";
                break;
            case "receiver":
                role = "receiver";
                break;
            default:
                role = null;
                break;
        }
    }

    public void generateKey(){
        byte[] delta = new byte[16];
        Random rand = new Random();
        rand.nextBytes(delta);
        MyElGamal mg1 = new MyElGamal();
        HashMap<String, String> keyPair = mg1.generateKeyPair(delta);
        PublicKeyPair pkp = new PublicKeyPair(mg1.getGStr(),mg1.getPStr(),keyPair.get("publicKey"),keyPair.get("privateKey"));

        rand.nextBytes(delta);
        MyElGamal mg2 = new MyElGamal();
        keyPair = mg2.generateKeyPair(delta);
        SignKeyPair skp = new SignKeyPair(mg2.getGStr(),mg2.getPStr(),keyPair.get("publicKey"),keyPair.get("privateKey"));
        identityKeys = new IdentityKeys(pkp,skp);



    }

    public IdentityKeys getPkAndSvk(){
        String g1 = identityKeys.pkp.g;
        String p1 = identityKeys.pkp.p;
        String g2 = identityKeys.skp.g;
        String p2 = identityKeys.skp.p;
        return new IdentityKeys(new PublicKeyPair(g1,p1,identityKeys.pkp.pk,null),
                new SignKeyPair(g2,p2,identityKeys.skp.svk,null));
    }

    public IdentityKeys getFullIdentityKeys(){
        return identityKeys;
    }

    public void startClient(){
        try{
            mSocket = new Socket(InetAddress.getByName("127.0.0.1"),serverPort);
            os = mSocket.getOutputStream();
            oos = new MyObjectOutputStream(os);
            ClientHandler cHandler = new ClientHandler(mSocket,ReceiveByffer,exitThread);
            receiveThread = new Thread(cHandler);
            receiveThread.start();
            MsgHandler mHandler = new MsgHandler(ReceiveByffer,Btree,exitThread);
            handleThread = new Thread(mHandler);
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
            receiveThread.join();
            handleThread.join();
            if(mSocket != null) {
                oos.close();
                mSocket.close();
                mSocket = null;
            }
        }catch(Exception e){

        }
        mThread = null;
    }

    @Override
    public void run() {
        System.out.println("ID:"+ID+"=============================");
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
        Btree.create(group,ID);
        identityKeys = Btree.mine.getIdentityKeys();
    }

    public void Update(){
        Btree.update(ID);
        identityKeys = Btree.mine.getIdentityKeys();
    }

    public void Add(String targetID,IdentityKeys targetKey){
        Btree.add(ID,targetID,targetKey);
    }

    public void Remove(String targetID,IdentityKeys targetKey){
        Btree.remove(ID,targetID,targetKey);
        identityKeys = Btree.mine.getIdentityKeys();
    }
}
