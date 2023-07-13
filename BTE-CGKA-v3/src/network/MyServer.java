package network;

import key.IdentityKeys;
import key.PublicKeyPair;
import key.SignKeyPair;
import tree_strcture.Group;
import tree_strcture.Node;
import tree_strcture.ServerTree;
import tree_strcture.TabEntry;
import utils.MyUtils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class MyServer implements Runnable{
    private ServerSocket serverSocket;
    private List<Information> allOut;
    public int size;//群组规模
    public ServerTree sTree = null;

    public Queue<Information> serverInfoBuffer;

    public int port;

    private Thread sThread = null;

    public MyServer(){
        try{
            //serverSocket = new ServerSocket(8088);
            sTree = new ServerTree();
            ServerStorage.tabServer = new LinkedHashMap<String,TabEntry>();
            port = 8088;
            allOut = new ArrayList<Information>();
            ServerStorage.serverInfoBuffer = new LinkedList<Information>();
            sThread = new Thread( this,"server-thread");
            sThread.start();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public Map<String,MyMessage> constructMsg(Information info){
        Map<String,MyMessage> msgMap = new LinkedHashMap<String,MyMessage>();
        switch (info.tag) {
            case "init":
            case "update":
            case "remove":
                info.enc.forEach((data, targetIDSet) -> {
                    for (String targetID : targetIDSet) {
                        byte[] targetpk = ServerStorage.tabServer.get(targetID).pk;
                        byte[] targetsvk = ServerStorage.tabServer.get(targetID).svk;
                        byte[] sig = null;
                        MyMessage myMsg = new MyMessage(info, targetpk, targetsvk, targetID, data, sig);
                        msgMap.put(targetID, myMsg);
                    }
                });
                break;
            case "add":
                //map不允许键重复
                ArrayList<WelcomeMsg> welcome = new ArrayList<WelcomeMsg>();
                for(Node n :sTree.leaves){
                    if(!n.isBlank) {
                        byte[] addPk = ServerStorage.tabServer.get(n.ID).pk;
                        byte[] addSvk = ServerStorage.tabServer.get(n.ID).svk;
                        MyMessage myMsg = new MyMessage(info, addPk, addSvk, n.ID, null, null);
                        msgMap.put(n.ID, myMsg);
                        WelcomeMsg wm = new WelcomeMsg(n.ID, new IdentityKeys(new PublicKeyPair(addPk,null),
                                new SignKeyPair(addSvk,null)));
                        welcome.add(wm);
                    }else{
                        IdentityKeys tempkey= new IdentityKeys(null,null);
                        WelcomeMsg wm = new WelcomeMsg(n.ID,tempkey);
                        welcome.add(wm);
                    }
                }
                info.enc.forEach((data, targetIDSet) -> {
                    for (String targetID : targetIDSet) {
                        byte[] targetpk = ServerStorage.tabServer.get(targetID).pk;
                        byte[] targetsvk = ServerStorage.tabServer.get(targetID).svk;
                        byte[] sig = null;
                        MyMessage myMsg = new MyMessage(info, targetpk, targetsvk, targetID, data, sig);
                        myMsg.setWelcomeMsg(welcome);
                        msgMap.put(targetID, myMsg);
                    }
                });
                break;
            default:
                break;
        }
        return msgMap;
    }



    public void openObjectServer(){
        ServerSocket ss = null;
        try {
            ss = new ServerSocket(port);
            while(true){
                final Socket socket = ss.accept();
                System.out.println("客户端已连接！");
                ServerHandler handler = new ServerHandler(socket);
                Thread mythd = new Thread(handler);
                mythd.start();
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }finally{
            try {
                if(ss != null)
                    ss.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public void sendMsgToClient(MyMessage msg,Socket s){
        //建立socket连接
        //将msg发给用户
        BroadCastHandler handler = new BroadCastHandler(s,msg);
        Thread myThread = new Thread(handler);
        myThread.start();
    }

    @Override
    public void run() {
        while(true){
            if(ServerStorage.getQueueSize() > 0){
                Information info = ServerStorage.popQueue();
                //根据该information更新服务器本地的棘轮树
                AppendMsg appendMsg = null;
                appendMsg = sTree.changeTree(info);
                //<id,MyMessage>
                Map<String,MyMessage> forwardMsgs = constructMsg(info);
                //sendMsgToClient()
                AppendMsg finalAppendMsg = appendMsg;
                forwardMsgs.forEach((clientID,msg)->{
                    //查表获得socket
                    synchronized (ServerStorage.tabServer) {
                        Socket s = ServerStorage.tabServer.get(clientID).getSocket();
                        msg.setAppendMsg(finalAppendMsg);
                        sendMsgToClient(msg, s);
                    }
                });
            }
        }
    }

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        MyServer server = new MyServer();
        synchronized (ServerStorage.tabServer) {
            ServerStorage.tabServer.put("aaaaa", new TabEntry(null, null, "url", new byte[]{'s', 'i', 'g'}));
        }

        server.openObjectServer();

    }


}
