package tree_strcture;

import key.IdentityKeys;
import key.PublicKeyPair;
import key.SignKeyPair;

import java.util.Random;

public class Node {
    private byte[] delta;
    private IdentityKeys identityKeys;
    public String ID;
    public boolean isLeaf;
    public boolean isBlank;

    //only for leaf
    public int pos;

    public Node leftChild;
    public Node rightChild;

    public Node sibling;
    public Node parent;

    public Node(String ID,IdentityKeys key,int p){

        this.ID = ID;
        identityKeys = key;
        delta = null;
        isLeaf = true;
        isBlank = false;
        leftChild = null;
        rightChild = null;
        sibling = null;
        parent = null;
        pos = p;
    }

    public Node(){
        ID = "";
        delta = null;
        identityKeys = null;
        leftChild = null;
        rightChild = null;
        sibling = null;
        parent = null;
        isLeaf = false;
        isBlank = true;
        pos = -1;
    }

    public Node getParent() {
        return parent;
    }

    public Node getLeftChild() {
        return leftChild;
    }

    public Node getRightChild() {
        return rightChild;
    }

    public byte[] getDelta() {
        return delta;
    }

    public void newSeed(){
        if(delta == null)
            delta = new byte[16];
        Random rand = new Random();
        rand.nextBytes(delta);
    }

    public void blank(){
        ID = "";
        delta = null;
        identityKeys = null;
        isBlank = true;
    }

    public void replaceDelta(byte[] newDelta){
        delta = newDelta;
        generatePublicKeyPair();
    }

    public void generatePublicKeyPair(){
        if(identityKeys == null)
            identityKeys = new IdentityKeys(new PublicKeyPair(delta),null);
        else{
            PublicKeyPair newPkp = new PublicKeyPair(delta);
            identityKeys.pkp = newPkp;
        }
    }

    public IdentityKeys getIdentityKeys() {
        return identityKeys;
    }

    public void setIdentityKeys(IdentityKeys identityKeys) {
        this.identityKeys = identityKeys;
    }

    public void setIdentityKeys(byte[] pk, byte[] svk) {
        if(this.identityKeys == null){
            this.identityKeys = new IdentityKeys(new PublicKeyPair(pk,null),new SignKeyPair(svk,null));
        }
        else {
            this.identityKeys.pkp.pk = pk;
            this.identityKeys.skp.svk = svk;
        }
    }

    public void setPos(int p){
        if(isLeaf)
            pos = p;
    }
}
