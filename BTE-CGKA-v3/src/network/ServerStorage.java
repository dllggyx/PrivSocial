package network;

import tree_strcture.TabEntry;

import java.util.Map;
import java.util.Queue;

public class ServerStorage {
    public static Queue<Information> serverInfoBuffer;
    public static Map<String, TabEntry> tabServer;

    public static void addQueue(Information info){
        serverInfoBuffer.add(info);
    }

    public static Information popQueue(){
        return serverInfoBuffer.poll();
    }

    public static int getQueueSize(){
        return serverInfoBuffer.size();
    }
}
