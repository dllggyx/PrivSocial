package tree_strcture;

import key.IdentityKeys;
import key.PublicKeyPair;
import key.SignKeyPair;

import java.net.Socket;

public class TabEntry {
    // pkp.sk == null
    public PublicKeyPair pkp;
    // skp.ssk == null
    public SignKeyPair skp;
    public String url;
    public byte[] sig;
    public Socket socket;

    public TabEntry(PublicKeyPair _pkp,SignKeyPair _skp,String _url,byte[] _sig){
        pkp = _pkp;
        skp = _skp;
        url = _url;
        sig = _sig;
        socket = null;
    }

    public TabEntry(IdentityKeys keys){
        pkp = keys.pkp;
        skp = keys.skp;
        socket = null;
    }

    public void setSocket(Socket s){
        socket = s;
    }

    public void rmSocket(){
        if(socket != null){
            try {
                socket.close();
                socket = null;
            }catch (Exception e){

            }
        }
    }

    public Socket getSocket(){
        return socket;
    }

    public void setKey(IdentityKeys keys){
        pkp = keys.pkp;
        skp = keys.skp;
    }

}
