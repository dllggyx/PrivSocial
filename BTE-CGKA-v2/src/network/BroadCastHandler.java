package network;

import utils.MyObjectOutputStream;

import java.io.*;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

public class BroadCastHandler implements Runnable{
    private Socket socket;
    private MyMessage msg;
    public BroadCastHandler(Socket s,MyMessage mymessage){
        socket = s;
        msg = mymessage;
    }

    @Override
    public void run() {
        try{
            OutputStream os = socket.getOutputStream();
            MyObjectOutputStream oos = new MyObjectOutputStream(os);
            oos.writeObject(msg);
            oos.flush();
            Thread.sleep(200);
            //oos.close();
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }
}
