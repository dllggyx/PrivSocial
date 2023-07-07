package network;

import tree_strcture.BinaryTree;

import java.util.Queue;

public class MsgHandler implements Runnable{
    BinaryTree bTree;
    Queue<MyMessage> msgQueue;
    public volatile boolean exitThread;
    public MsgHandler(Queue<MyMessage> queue, BinaryTree bt,boolean exit){
        bTree = bt;
        msgQueue = queue;
        exitThread = exit;
    }
    @Override
    public void run() {
        while(!exitThread){
            synchronized (msgQueue) {
                if (msgQueue.size() > 0) {
                    MyMessage msg = msgQueue.poll();
                    synchronized (bTree) {
                        bTree.process(msg);
                    }
                }
            }
        }
    }
}
