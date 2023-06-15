package tree_strcture;

import com.sun.xml.internal.bind.v2.model.core.ID;
import key.EncryptionTools;
import key.PublicKeyPair;
import key.SignKeyPair;
import network.MyMessage;
import network.WelcomeMsg;
import utils.Hash1;
import key.IdentityKeys;
import network.Information;
import utils.MyUtils;

import java.net.Socket;
import java.util.*;

public class BinaryTree {
    public Node root = null;
    //the local user,the mine of server is null
    public Vector<Node> leaves = null;
    public Node mine;
    //Build a queue to create group
    public int size;
    //The current maximum number of support for the Ratchet Tree
    public int scale;
    private Hash1 hash1 = new Hash1();
    private Socket s;
    public String treeState;
    public Queue<Information> infoBuffer;
    private IdentityKeys myIdentityKeys = null;
    private int count = 0;

    public BinaryTree(IdentityKeys k){
        s = null;
        treeState = "";
        infoBuffer = new LinkedList();
        myIdentityKeys = k;
    }

    public void create(Group group, String myID){
        count++;
        size = group.getGroupSize();
        scale = (int)Math.pow(2,(int)Math.ceil(Math.log(size) / Math.log(2)));
        leaves = new Vector<Node>();
        int leafCnt = 0;
        for (Map.Entry<String, IdentityKeys> entry : group.memberMap.entrySet()) {
            String key = entry.getKey();
            IdentityKeys value = entry.getValue();
            //If myID is the creator
            if (key.equals(myID)) {
                mine = new Node();
                mine.ID = myID;
                mine.setIdentityKeys(myIdentityKeys);
                mine.isLeaf = true;
                mine.isBlank = false;
                mine.setPos(leafCnt);
                leafCnt++;
                leaves.add(mine);
            } else {
                leaves.add(new Node(key, value, leafCnt));
                leafCnt++;
            }
        }

        for(int i = size;i<scale;i++){
            Node node = new Node();
            node.isLeaf = true;
            node.setPos(size+i);
            leaves.add(node);
        }

        root = buildTree();
        ArrayList<PublicKeyPair> PKs = new ArrayList<PublicKeyPair>();
        sample(PKs);
        Vector<Node> minePath = path(mine);
        //resultMap is used to store encrypted seeds in the form of<pk, (enDelta_a,enDelta_b)>
        Map<ArrayList<String>, Vector<String>>resultMap = new HashMap<ArrayList<String>, Vector<String>>();

        for(Node vertex: minePath){
            Set<Node> res = resolution(vertex.sibling);
            //然后使用集合中结点的公钥分别加密vertex.parent.delta
            for(Node nodeRes: res){
                PublicKeyPair currentPkp = nodeRes.getIdentityKeys().pkp;
                byte[] delta = vertex.parent.getDelta();
                ArrayList<String> enDelta = EncryptionTools.encryptDelta(currentPkp, delta);
                Vector<String> idSet = getSubtreeNodes(nodeRes);
                resultMap.put(enDelta,idSet);
            }
        }
        PublicKeyPair mypkp = mine.getIdentityKeys().pkp;
        SignKeyPair myskp = mine.getIdentityKeys().skp;

        myIdentityKeys = mine.getIdentityKeys();

        group.addMember(myID,new IdentityKeys(mypkp,myskp,0));
        //then construct the information and send to server
        Information inform = new Information();
        inform.tag = "init";
        inform.enc = resultMap;
        inform.group = group;
        inform.constructMessage(myID,myIdentityKeys.getPkAndSvk(),PKs,null);
        send(inform);
    }

    public Node update(String myID){
        count++;
        int encTimes = 0;
        ArrayList<PublicKeyPair> PKs = new ArrayList<PublicKeyPair>();
        sample(PKs);
        Vector<Node> minePath = path(mine);
        //resultMap is used to store encrypted seeds in the form of<enDelta,{IDi,IDk,...}>
        Map<ArrayList<String>,Vector<String>>resultMap = new HashMap<ArrayList<String>,Vector<String>>();
        for(Node vertex: minePath){
            Set<Node> res = resolution(vertex.sibling);
            //然后使用集合中结点的公钥分别加密vertex.parent.delta
            for(Node nodeRes: res){
                PublicKeyPair currentPkp = nodeRes.getIdentityKeys().pkp;

                byte[] delta = vertex.parent.getDelta();
                ArrayList<String> enDelta = EncryptionTools.encryptDelta(currentPkp, delta);

                Vector<String> idSet = getSubtreeNodes(nodeRes);
                resultMap.put(enDelta,idSet);
                encTimes++;
            }
        }
        System.err.println("ID = " + mine.ID + ",encTimes = " + encTimes);
        PublicKeyPair mypkp = mine.getIdentityKeys().pkp;
        SignKeyPair myskp = mine.getIdentityKeys().skp;

        myIdentityKeys = mine.getIdentityKeys();

        //then construct the information and send to server
        Information inform = new Information();
        inform.tag = "update";
        inform.enc = resultMap;
        //null refers to unchanged
        inform.group = null;
        inform.constructMessage(myID,myIdentityKeys.getPkAndSvk(),PKs,null);
        send(inform);
        return null;
    }

    public Node add(String myID,String targetID,IdentityKeys targetKey){
        count++;
        //No sample() required
        //size = size + 1;
        PublicKeyPair targetpkp = targetKey.pkp;
        SignKeyPair targetskp = targetKey.skp;
        Map<ArrayList<String>, Vector<String>>resultMap = new HashMap<ArrayList<String>, Vector<String>>();
        //byte[] enDelta = EncryptionTools.encryptDelta(targetpk, root.getDelta());

        byte[] delta = root.getDelta();

        ArrayList<String> enDelta = EncryptionTools.encryptDelta(targetpkp, delta);

        Vector<String> idSet = new Vector<String>();
        idSet.add(targetID);
        resultMap.put(enDelta,idSet);

        PublicKeyPair mypkp = mine.getIdentityKeys().pkp;
        SignKeyPair myskp = mine.getIdentityKeys().skp;

        Information inform = new Information();
        inform.tag = "add";
        inform.enc = resultMap;
        /**group的值需要修改*/
        inform.group = null;
        HashMap<String,IdentityKeys> appendMsg = new HashMap<String,IdentityKeys>();
        appendMsg.put(targetID,targetKey);
        inform.constructMessage(myID,new IdentityKeys(mypkp,myskp,0),null,appendMsg);
        send(inform);

        return null;
    }

    public Node remove(String myID,String targetID,IdentityKeys targetKey){
        count++;
        PublicKeyPair targetpk = targetKey.pkp;
        SignKeyPair targetsvk = targetKey.skp;
        removeClient(targetID);
        ArrayList<PublicKeyPair> PKs = new ArrayList<PublicKeyPair>();
        sample(PKs);
        Vector<Node> minePath = path(mine);
        //resultMap is used to store encrypted seeds in the form of<pk, enDelta>
        Map<ArrayList<String>, Vector<String>>resultMap = new HashMap<ArrayList<String>, Vector<String>>();
        for(Node vertex: minePath){
            Set<Node> res = resolution(vertex.sibling);
            //然后使用集合中结点的公钥分别加密vertex.parent.delta
            for(Node nodeRes: res){
                PublicKeyPair pkp = nodeRes.getIdentityKeys().pkp;
                //byte []enDelta = EncryptionTools.encryptDelta(pk, vertex.parent.getDelta());

                byte[] delta = vertex.parent.getDelta();

                ArrayList<String> enDelta = EncryptionTools.encryptDelta(pkp, delta);
                Vector<String> idSet = getSubtreeNodes(nodeRes);
                resultMap.put(enDelta,idSet);
            }
        }
        PublicKeyPair mypk = mine.getIdentityKeys().pkp;
        SignKeyPair mysvk = mine.getIdentityKeys().skp;

        myIdentityKeys = mine.getIdentityKeys();

        Information inform = new Information();
        inform.tag = "remove";
        inform.enc = resultMap;
        /**group的值需要修改，或者通过appendMsg修改*/
        inform.group = null;
        HashMap<String,IdentityKeys> appendMsg = new HashMap<String,IdentityKeys>();
        appendMsg.put(targetID,targetKey);
        inform.constructMessage(myID,myIdentityKeys.getPkAndSvk(),PKs,appendMsg);
        send(inform);


        return null;
    }

    public void sample(ArrayList<PublicKeyPair> PKs){
        Vector<Node> nodePath = path(mine);
        mine.newSeed();
        mine.generatePublicKeyPair();
        System.out.println(Arrays.toString(mine.getDelta()));
        PKs.add(mine.getIdentityKeys().getPk());
        for(int i=1;i<nodePath.size();i++){
            nodePath.get(i).replaceDelta(hash1.SHA256(nodePath.get(i-1).getDelta()));
            PKs.add(nodePath.get(i).getIdentityKeys().getPk());
            //nodePath.get(i).generatePublicKeyPair();
            nodePath.get(i).isBlank = false;
            System.out.println(Arrays.toString(nodePath.get(i).getDelta()));
        }
    }

    public Set<Node> resolution(Node v){
        Set<Node> res = new HashSet<Node>();
        if(v == null)
            ;
        else if(!v.isBlank)
            res.add(v);
        else if(v.isLeaf && v.isBlank)
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

    public void send(Information inform){
        infoBuffer.add(inform);
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
//            node.setPos(size+i);
//            leaves.add(node);
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

    public boolean removeClient(String targetID){
        Node targetNode = null;
        for(Node leaf: leaves){
            if(leaf.ID.equals(targetID))
                targetNode = leaf;
        }

        if(targetNode == null)
            return false;

        size = size - 1;
        Vector<Node> targetPath = path(targetNode);
        for(Node n: targetPath){
            if(n != root)
                n.blank();
        }
        return true;
    }

    public void synchronize(Socket socket){
        if(socket != null)
            s = socket;
    }

    public Vector<String> getSubtreeNodes(Node subRoot){
        Queue<Node> subTree = new LinkedList<Node>();
        subTree.add(subRoot);
        while(true){
            Node n = subTree.poll();
            if(n.leftChild == null){
                subTree.add(n);
                break;
            }
            else if(n.rightChild == null){
                subTree.add(n.leftChild);
            }
            else{
                subTree.add(n.leftChild);
                subTree.add(n.rightChild);
            }
        }
        Vector<String> result = new Vector<String>();
        for (Node node : subTree) {
            result.add(node.ID);
        }
        return result;
    }

    /** Receiver call */
    public void update_path(Node n,byte[] newDelta){
        Vector<Node> nodePath = path(n);
        byte[] tempDelta = null;
        for(int i=0;i<nodePath.size();i++){
            nodePath.get(i).isBlank = false;
            if(tempDelta == null)
                tempDelta = newDelta;
            else
                tempDelta = hash1.SHA256(tempDelta);
            nodePath.get(i).replaceDelta(tempDelta);
        }
    }

    public void unblankPath(Node sender,ArrayList<PublicKeyPair> pkps){
        Vector<Node> nodePath = path(sender);
        for(int i=0;i<nodePath.size();i++){
            nodePath.get(i).isBlank = false;
            if(nodePath.get(i).getIdentityKeys() == null)
                nodePath.get(i).setIdentityKeys(new IdentityKeys(pkps.get(i),null));
            else
                nodePath.get(i).getIdentityKeys().pkp = pkps.get(i);
        }
    }

    /** Receiver call */
    //After receiving MyMessage, this method allows the recipient
    // to explicitly bind MyMessage.inc to the specific node
    //返回值为需要更新delta的结点，pkNode为resolution结点，用他的私钥解密
    //<enc node,pkNode>
    public Node[] findMyEnc(Node sender,Node receiver){
        Vector<Node> senderPath = path(sender);
        Set<Node> receiverPath = new HashSet<Node>();
        for(Node n:path(receiver)){
            receiverPath.add(n);
        }
        for(Node n:senderPath){
            for(Node n1: resolution(n.sibling)){
                if(receiverPath.contains(n1)){
                    return new Node[]{n.parent,n1};
                }
            }
        }
        return null;
    }

    public void process(MyMessage msg){
        if(!msg.senderID.equals(msg.receiverID))
            count++;
        switch(msg.tag){
            case "init":
                processInit(msg);
                showTree(msg.tag);
                break;
            case "update":
                processUpdate(msg);
                showTree(msg.tag);
                break;
            case "add":
                processAdd(msg);
                showTree(msg.tag);
                break;
            case "remove":
                processRemove(msg);
                showTree(msg.tag);
                break;
            default:
                break;
        }
    }

    private void processInit(MyMessage msg){
        size = msg.group.getGroupSize();
        scale = (int)Math.pow(2,(int)Math.ceil(Math.log(size) / Math.log(2)));
        if(leaves != null)
            return;
        leaves = new Vector<Node>();
        Node sender = new Node();
        int leafCnt = 0;
        for (Map.Entry<String, IdentityKeys> entry : msg.group.memberMap.entrySet()) {
            String key = entry.getKey();
            IdentityKeys value = entry.getValue();
            //If myID is the creator
            if (key.equals(msg.senderID)) {
                sender.ID = msg.senderID;
                sender.setIdentityKeys(value);
                sender.isLeaf = true;
                sender.isBlank = false;
                sender.setPos(leafCnt);
                leafCnt++;
                leaves.add(sender);
            } else if (key.equals(msg.receiverID)) {
                mine = new Node();
                mine.ID = msg.receiverID;
                mine.setIdentityKeys(myIdentityKeys);
                mine.isLeaf = true;
                mine.isBlank = false;
                mine.setPos(leafCnt);
                leafCnt++;
                leaves.add(mine);
            } else {
                leaves.add(new Node(key, value,leafCnt));
                leafCnt++;
            }
        }
        root = buildTree();
        unblankPath(sender,msg.PKs);
        ArrayList<String> encDelta = msg.enc;
        byte[] deDelta = EncryptionTools.decryptDelta(myIdentityKeys.pkp, encDelta);
        update_path(findMyEnc(sender,mine)[0],deDelta);
    }

    private void processUpdate(MyMessage msg){
        Node sender = null;
        for(Node leaf:leaves){
            if(leaf.ID.equals(msg.senderID)){
                sender = leaf;
                break;
            }
        }
        unblankPath(sender,msg.PKs);
        ArrayList<String> encDelta = msg.enc;

        Node[] nodes = findMyEnc(sender,mine);
        //sk不同，那种子是否相同
        byte[] deDelta = EncryptionTools.decryptDelta(nodes[1].getIdentityKeys().pkp, encDelta);
        update_path(nodes[0],deDelta);

    }

    //所有用户（包括消息发送者）都要执行processAdd
    private void processAdd(MyMessage msg){
        Node target = null;
        int leafCnt = 0;
        size = size + 1;
        if(msg.appendMsg.ID.equals(msg.receiverID)){
            //I'm the new member
            leaves = new Vector<Node>();
            scale = msg.welcomeMsg.size();
            size = 0;
            for (WelcomeMsg wmsg : msg.welcomeMsg) {
                if(wmsg.ID.equals(msg.receiverID)){
                    mine = new Node();
                    mine.ID = msg.receiverID;
                    mine.setIdentityKeys(myIdentityKeys);
                    mine.isLeaf = true;
                    mine.isBlank = false;
                    mine.setPos(leafCnt);
                    leafCnt++;
                    leaves.add(mine);
                    size++;
                }else if(!wmsg.ID.equals("")){
                    leaves.add(new Node(wmsg.ID, wmsg.keys,leafCnt));
                    leafCnt++;
                    size++;
                }else{
                    Node n = new Node();
                    n.isLeaf = true;
                    n.setPos(leafCnt);
                    leafCnt++;
                    leaves.add(n);
                }
            }
            root = buildTree();
            ArrayList<String> encDelta = msg.enc;
            byte[] deDelta = EncryptionTools.decryptDelta(myIdentityKeys.pkp, encDelta);
            if(msg.appendMsg.expand) {
                root.leftChild.replaceDelta(deDelta);
                root.leftChild.isBlank = false;
                root.replaceDelta(hash1.SHA256(root.leftChild.getDelta()));
                root.isBlank = false;
            }else {
                root.replaceDelta(deDelta);
                root.isBlank = false;
            }
        }else{
            if(size > scale){
                copyTree();
                root.replaceDelta(hash1.SHA256(root.leftChild.getDelta()));
                root.isBlank = false;
            }
            for(Node leaf:leaves){
                if(leaf.pos == msg.appendMsg.pos){
                    target = leaf;
                    target.ID = msg.appendMsg.ID;
                    target.setIdentityKeys(msg.appendMsg.identityKeys);
                    target.isBlank = false;
                }
            }

        }

    }

    private void processRemove(MyMessage msg){
        Node sender = null;
        Node target = null;
        for(Node leaf:leaves){
            if(leaf.ID.equals(msg.senderID)){
                sender = leaf;
            }else if(leaf.ID.equals(msg.appendMsg.ID)){
                target = leaf;
            }
        }
        size = size - 1;
        Vector<Node> targetPath = path(target);
        for(Node n: targetPath){
            if(n != root)
                n.blank();
        }
        unblankPath(sender,msg.PKs);
        ArrayList<String> encDelta = msg.enc;
        Node[] nodes = findMyEnc(sender,mine);
        //sk不同，那种子是否相同
        byte[] deDelta = EncryptionTools.decryptDelta(nodes[1].getIdentityKeys().pkp, encDelta);
        update_path(nodes[0],deDelta);
    }

    public void showTree(String tag){
        synchronized(MyUtils.myLock) {
            int temp = MyUtils.myLock;
            System.out.println("====================" + mine.ID + "====================" + "tag: " + tag + ",count: " + count);
            if (tag.equals("add")) {
                depictTree();
            }
            System.out.println(Arrays.toString(root.getDelta()));

            System.out.println("=============================================");
        }
    }

    public void depictTree(){
        Queue<Node> queue = new LinkedList<Node>();
        int countt = 0;
        int i = 0;
        queue.add(root);
        String result = "";
        while(queue.size() > 0){
            Node n = queue.poll();
            String symbol = "o";
            if(!n.isBlank)
                symbol = "*";
            result = result + n.ID + symbol+"   ";
            if(n.leftChild != null)
                queue.add(n.leftChild);
            if(n.rightChild != null)
                queue.add(n.rightChild);
            countt++;
            if(countt == Math.pow(2,i)){
                i++;
                countt = 0;
                System.out.println(result);
                result = "";
            }

        }

    }

    public void copyTree(){
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

}