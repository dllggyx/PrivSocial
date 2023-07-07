package tree_strcture;

import key.IdentityKeys;
import key.PublicKeyPair;
import key.SignKeyPair;

import java.net.Socket;

public class TabEntry {
    // pkp.sk == null
    public byte[] pk;
    // skp.ssk == null
    public byte[] svk;
    public String url;
    public byte[] sig;
    public Socket socket;

    public TabEntry(byte[] _pk,byte[] _svk,String _url,byte[] _sig){
        pk = _pk;
        svk = _svk;
        url = _url;
        sig = _sig;
        socket = null;
    }

    public TabEntry(byte[] _pk,byte[] _svk){
        pk = _pk;
        svk = _svk;
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

    public void setKey(byte[] _pk,byte[] _svk){
        pk = _pk;
        svk = _svk;
    }

}
