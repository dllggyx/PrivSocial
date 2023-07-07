package network;

import tree_strcture.TabEntry;
import utils.MyObjectInputStream;

import java.io.*;
import java.net.Socket;
import java.util.Queue;

public class ServerHandler implements Runnable{
    private Socket socket;


    public ServerHandler(Socket socket){

        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            InputStream is = socket.getInputStream();

            MyObjectInputStream ois = new MyObjectInputStream(is);
            //先接收ID
            String clientID = (String)ois.readObject();
            synchronized (ServerStorage.tabServer) {
                if (ServerStorage.tabServer.get(clientID) == null)
                    ServerStorage.tabServer.put(clientID, new TabEntry(null, null, null, null));
                ServerStorage.tabServer.get(clientID).setSocket(socket);
            }
            System.out.println("socket connected with: " + clientID);
            while(true) {
                try {
                    Object object = ois.readObject();
                    if(object == null)
                        break;
                    Queue<Information> infoList = (Queue<Information>) object;
                    //打印对象
                    for (int i = 0; i < infoList.size(); i++) {
                        Information info = infoList.poll();
                        ServerStorage.addQueue(info);
                        System.out.println("ID:" + info.senderID + ", tag:" + info.tag);
                    }


                }catch (EOFException e){
                    ois.close();
                    ois = new MyObjectInputStream(is);
                }catch (Exception e){
                    e.printStackTrace();
                    socket.close();
                    System.out.println("object read error");
                    break;
                }
            }

            //关闭socket
            socket.close();
            synchronized (ServerStorage.tabServer) {
                if (ServerStorage.tabServer.get(clientID) == null)
                    ServerStorage.tabServer.put(clientID, new TabEntry(null, null, null, null));
                ServerStorage.tabServer.get(clientID).setSocket(null);
            }
            System.out.println("socket has been closed");
        } catch (Exception e) {
            System.out.println("socket has been closed");
            //e.printStackTrace();


        } finally {
            if (socket != null)
                try {
                    socket.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
        }
    }

}
