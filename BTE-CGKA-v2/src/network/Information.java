package network;

import key.EncryptionTools;
import key.IdentityKeys;
import key.PublicKeyPair;
import key.SignKeyPair;
import tree_strcture.Group;
import tree_strcture.Node;

import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.*;

public class Information implements Serializable {

    private static final long serialVersionUID = 5753655624350538724L;
    public String tag;
    public Group group;
    //map<ID,identitykeys>
    public HashMap<String,IdentityKeys> appendMsg;
    public String senderID;
    public IdentityKeys senderKey;

    //<pk,encMsg>
    public Map<ArrayList<String>, Vector<String>> enc;
    public byte []sig;
    //the pk on sender's path;
    public ArrayList<PublicKeyPair> PKs;
    public Information constructMessage(String ID, IdentityKeys ikey,ArrayList<PublicKeyPair>_PKs,HashMap<String,IdentityKeys> _appendMsg){

        switch(tag){
            case "init":
                System.out.println("init");
                senderID = ID;
                senderKey = new IdentityKeys(ikey);
                sig = EncryptionTools.sign(ikey.skp,enc);
                appendMsg = _appendMsg;
                PKs = _PKs;
                break;
            case "update":
                System.out.println("update");
                senderID = ID;
                senderKey = new IdentityKeys(ikey);
                sig = EncryptionTools.sign(ikey.skp,enc);
                appendMsg = _appendMsg;
                PKs = _PKs;
                break;
            case "add":
                System.out.println("add");
                senderID = ID;
                senderKey = new IdentityKeys(ikey);
                sig = EncryptionTools.sign(ikey.skp,enc);
                appendMsg = _appendMsg;
                PKs = null;
                break;
            case "remove":
                System.out.println("remove");
                senderID = ID;
                senderKey = new IdentityKeys(ikey);
                sig = EncryptionTools.sign(ikey.skp,enc);
                appendMsg = _appendMsg;
                PKs = _PKs;
                break;
            default:
                System.out.println("others");
                break;
        }

        return null;
    }
}
