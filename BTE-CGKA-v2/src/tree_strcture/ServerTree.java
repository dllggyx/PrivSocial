package tree_strcture;

import key.EncryptionTools;
import key.PublicKeyPair;
import network.AppendMsg;
import network.ServerStorage;
import utils.Hash1;
import key.IdentityKeys;
import network.Information;

import java.net.Socket;
import java.util.*;

/**
 * ServerTree does not maintain group keys and cannot maintain group
 *      keys,only responsible for maintaining group structure
 * "tabServer" in ServerTree are Tab_{server}, can be used to query
 *      members<id, pk, svk, URL, sig_{ssk}(id,pk)>
 * */
public class ServerTree {
    public Node root = null;
    //the local user,the mine of server is null
    public Vector<Node> leaves;

    //Build a queue to create group
    public int size;
    //The current maximum number of support for the Ratchet Tree
    public int scale;
    private Hash1 hash1 = new Hash1();
    private boolean expand = false;

    public void create(Group group, String senderID,IdentityKeys senderKey){
        size = group.getGroupSize();
        scale = (int)Math.pow(2,(int)Math.ceil(Math.log(size) / Math.log(2)));
        leaves = new Vector<Node>();
        int leafCnt = 0;
        for (Map.Entry<String, IdentityKeys> entry : group.memberMap.entrySet()) {
            String key = entry.getKey();
            IdentityKeys value = entry.getValue();
            //If myID is the creator
            if (key.equals(senderID)) {
                Node creator = new Node();
                creator.ID = senderID;
                creator.setIdentityKeys(value);
                creator.isLeaf = true;
                creator.isBlank = false;
                creator.setPos(leafCnt);
                leafCnt++;
                leaves.add(creator);
                synchronized (ServerStorage.tabServer) {
                    if (ServerStorage.tabServer.get(senderID) == null)
                        ServerStorage.tabServer.put(senderID, new TabEntry(value));
                    ServerStorage.tabServer.get(senderID).setKey(value);
                }
                server_sample(creator);
            } else {
                leaves.add(new Node(key, value, leafCnt));
                leafCnt++;
                synchronized (ServerStorage.tabServer) {
                    if (ServerStorage.tabServer.get(key) == null)
                        ServerStorage.tabServer.put(key, new TabEntry(value));
                    ServerStorage.tabServer.get(key).setKey(value);
                }
            }
        }
        for(int i = size;i<scale;i++){
            Node node = new Node();
            node.isLeaf = true;
            node.setPos(size+i);
            leaves.add(node);
        }
        root = buildTree();
        //update the data of senderNode from received information
    }

    public void update(String senderID,IdentityKeys senderKey){
        for(Node leaf: leaves){
            if(leaf.ID.equals(senderID)) {
                server_sample(leaf);
            }
        }
        synchronized (ServerStorage.tabServer) {
            if (ServerStorage.tabServer.get(senderID) == null)
                ServerStorage.tabServer.put(senderID, new TabEntry(senderKey));
            ServerStorage.tabServer.get(senderID).setKey(senderKey);
        }
    }

    public int add(String senderID,String targetID,IdentityKeys targetKey){
        //No sample() required

        size = size + 1;
        int pos = -1;
        //位置由服务器来决定，myID不需要管这个
        if(size > scale){
            copyTree();
            expand = true;
        }

        for(Node leaf: leaves){
            if(leaf.isBlank){
                leaf.isBlank = false;
                leaf.setIdentityKeys(targetKey);
                leaf.ID = targetID;
                pos = leaf.pos;
                break;
            }
        }

        synchronized (ServerStorage.tabServer) {
            if (ServerStorage.tabServer.get(targetID) == null)
                ServerStorage.tabServer.put(targetID, new TabEntry(targetKey));
            ServerStorage.tabServer.get(targetID).setKey(targetKey);
        }
        return pos;
    }

    public void remove(String senderID,IdentityKeys senderKey,String targetID,IdentityKeys targetKey){
        size = size - 1;
        removeClient(targetID);
        for(Node leaf:leaves){
            if(leaf.ID.equals(senderID)) {
                server_sample(leaf);
            }
        }
        synchronized (ServerStorage.tabServer) {
            if (ServerStorage.tabServer.get(senderID) == null)
                ServerStorage.tabServer.put(senderID, new TabEntry(senderKey));
            ServerStorage.tabServer.get(senderID).setKey(senderKey);
        }

    }

    public Set<Node> resolution(Node v){
        Set<Node> res = new HashSet<Node>();
        if(v == null)
            ;
        else if(!v.isBlank)
            res.add(v);
        else if(v.isLeaf == true && v.isBlank == true)
            ;
        else {
            Node left = v.leftChild;
            Node right = v.rightChild;
            res.addAll(resolution(left));
            res.addAll(resolution(right));
        }
        return res;
    }

    public byte[] getKey(){
        //hash(root.getDelta()) => group key
        return root.getDelta();
    }

    public Vector<Node> path(Node node){
        Vector<Node> nodePath = new Vector<Node>();
        Node temp = node;
        while(temp != null) {
            nodePath.add(temp);
            temp = temp.parent;
        }
        return nodePath;
    }

    public void send(Information inform, Socket s){
        //inform.sendMessage(s);
    }

    public void server_sample(Node n){
        Vector<Node> nodePath = path(n);
        for(int i=1;i<nodePath.size();i++){
            nodePath.get(i).isBlank = false;
        }
    }

    public Node buildTree(){
        if(root != null)
            return root;
        //Build a queue to create group
        Queue<Node> queue = new LinkedList();
        for (Node node : leaves) {
            queue.add(node);
        }
        //Completing leaf nodes with empty nodes
//        for(int i = size;i<scale;i++){
//            Node node = new Node();
//            node.isLeaf = true;
//            queue.add(node);
//        }
        while(queue.size() > 1){
            Node left = queue.poll();
            Node right = queue.poll();
            Node parent = new Node();
            parent.leftChild = left;
            parent.rightChild = right;
            left.parent = parent;
            left.sibling = right;
            right.parent = parent;
            right.sibling = left;
            queue.add(parent);
        }

        //return the root
        return queue.poll();
    }

    //This method is used when the tree is full when adding new members
    public void copyTree(){
        //scale->number of leaves in the copy tree
        //在copy的同时，把新的叶子结点加入集合当中
//        for(int i=0;i<scale;i++){
//            Node node = new Node();
//            node.isLeaf = true;
//            leaves.add(node);
//        }
        //buildTree();
        Queue<Node> queue = new LinkedList<Node>();
        for(int i = 0;i<scale;i++){
            Node node = new Node();
            node.isLeaf = true;
            node.setPos(scale+i);
            leaves.add(node);
            queue.add(node);
        }
        while(queue.size() > 1){
            Node left = queue.poll();
            Node right = queue.poll();
            Node parent = new Node();
            parent.leftChild = left;
            parent.rightChild = right;
            left.parent = parent;
            left.sibling = right;
            right.parent = parent;
            right.sibling = left;
            queue.add(parent);
        }
        scale = scale * 2;

        Node rightRoot = queue.poll();
        Node newRoot = new Node();
        newRoot.leftChild = root;
        newRoot.rightChild = rightRoot;
        root = newRoot;
    }


    public boolean removeClient(String targetID){
        Node targetNode = null;
        for(Node leaf: leaves){
            if(leaf.ID.equals(targetID))
                targetNode = leaf;
        }

        if(targetNode == null)
            return false;
        Vector<Node> targetPath = path(targetNode);
        for(Node n: targetPath){
            if(n != root)
                n.blank();
        }
        return true;
    }

    public AppendMsg changeTree(Information info){
        AppendMsg appendMsg = null;
        String targetID = null;
        IdentityKeys targetKey = null;
        switch(info.tag){
            case "init":
                create(info.group,info.senderID,info.senderKey);
                break;
            case "update":
                update(info.senderID,info.senderKey);
                break;
            case "add":
                for(Map.Entry<String,IdentityKeys> entry : info.appendMsg.entrySet()){
                    targetID = entry.getKey();
                    targetKey = entry.getValue();
                }
                int pos = add(info.senderID,targetID,targetKey);
                appendMsg = new AppendMsg("add",targetID,targetKey);
                appendMsg.setPos(pos,expand);
                expand = false;
                break;
            case "remove":
                for(Map.Entry<String,IdentityKeys> entry : info.appendMsg.entrySet()){
                    targetID = entry.getKey();
                    targetKey = entry.getValue();
                }
                remove(info.senderID,info.senderKey,targetID,targetKey);
                appendMsg = new AppendMsg("remove",targetID,targetKey);
                break;
            default:
                break;
        }
        return appendMsg;
    }
}