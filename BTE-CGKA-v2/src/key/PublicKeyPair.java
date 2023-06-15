package key;

import utils.MyElGamal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class PublicKeyPair implements Serializable {
    private static final long serialVersionUID = 2596046337318560346L;
    public String pk;
    private String sk;

    public String p;
    public String g;

    public String getPublicKey() {
        return pk;
    }

    public String getSecretKey() {
        return sk;
    }

    public void setPublicKey(String pk) {
        this.pk = pk;
    }

    public void setSecretKey(String sk) {
        this.sk = sk;
    }

    public PublicKeyPair(String _g,String _p,String _public, String _secret){
        g = _g;
        p = _p;
        pk = _public;
        sk = _secret;
    }

    public PublicKeyPair(byte[] delta){
        MyElGamal mg = new MyElGamal();
        HashMap<String, String> keyPair = mg.generateKeyPair(delta);
        g = mg.getGStr();
        p = mg.getPStr();
        pk = keyPair.get("publicKey");
        sk = keyPair.get("privateKey");
    }

    public void init(String _g,String _p){
        p = _p;
        g = _g;
    }

    public PublicKeyPair getPublicMsg(){
        return new PublicKeyPair(g,p,pk,null);
    }
}
