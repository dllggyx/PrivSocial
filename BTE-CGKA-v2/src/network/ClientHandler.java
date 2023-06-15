package network;

import tree_strcture.TabEntry;
import utils.MyObjectInputStream;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.Queue;

public class ClientHandler implements Runnable{
    Socket socket;
    Queue<MyMessage> receiveMsgList;
    boolean exitThread;
    public ClientHandler(Socket s, Queue<MyMessage> msgList, boolean exit){
        socket = s;
        receiveMsgList = msgList;
        exitThread = exit;
    }

    @Override
    public void run() {
        //receive the message from server
        try {
            InputStream is = socket.getInputStream();
            MyObjectInputStream ois = new MyObjectInputStream(is);

            while(!exitThread) {
                try {
                    Object object = ois.readObject();
                    MyMessage myMessage = (MyMessage) object;
                    //打印对象
                    receiveMsgList.add(myMessage);
                }catch (EOFException e){
                    ois.close();
                    ois = new MyObjectInputStream(is);
                }catch (Exception e){
                    e.printStackTrace();
                    System.out.println("object read error");
                    break;
                }
            }
            //关闭socket
            //socket.close();
        } catch (Exception e) {
            System.out.println("socket has been closed");
            e.printStackTrace();
        }
//        finally {
//            if (socket != null)
//                try {
//                    socket.close();
//                } catch (IOException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//        }
    }

}
