package network;


import key.IdentityKeys;
import key.PublicKeyPair;
import key.SignKeyPair;
import tree_strcture.Group;
import tree_strcture.Node;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**The server breaks down information into messages and distributes them to users*/
public class MyMessage implements Serializable {
    private static final long serialVersionUID = 757827392691148261L;
    public IdentityKeys sender;
    public byte[] receiverPk;
    public byte[] receiverSvk;
    public String senderID;
    public String receiverID;
    public byte[] sig;
    public byte[] enc;
    public Group group;
    public String tag;
    public AppendMsg appendMsg;
    public ArrayList<WelcomeMsg> welcomeMsg;
    public ArrayList<byte[]> PKs;

    public MyMessage(IdentityKeys key){
        this.sender = key;
    }

    public MyMessage(byte []enc){

    }

    public MyMessage(Information info, byte[] pk, byte[] svk, String id, byte[] data, byte[] enc_sig){
        tag = info.tag;
        group = info.group;
        senderID = info.senderID;
        sender = new IdentityKeys(new PublicKeyPair(info.senderPk,null),new SignKeyPair(info.senderSvk,null));
        receiverID = id;
        receiverPk = pk;
        receiverSvk = svk;
        sig = enc_sig;
        enc = data;
        appendMsg = null;
        PKs = info.PKs;
    }

    public MyMessage(){

    }

    public void setAppendMsg(AppendMsg appendMsg) {
        this.appendMsg = appendMsg;
    }

    //used in add operation
    public void setWelcomeMsg(ArrayList<WelcomeMsg> wmsg){
        welcomeMsg = wmsg;
    }
}
