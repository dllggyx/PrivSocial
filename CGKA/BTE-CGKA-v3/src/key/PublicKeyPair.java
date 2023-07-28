package key;

import Curve25519.Curve25519KeyPairGenerator;

import java.io.Serializable;
import java.util.HashMap;

public class PublicKeyPair implements Serializable {
    private static final long serialVersionUID = 2596046337318560346L;
    public byte[] pk;
    private byte[] sk;

    public byte[] getPublicKey() {
        return pk;
    }

    public byte[] getSecretKey() {
        return sk;
    }

    public void setPublicKey(byte[] pk) {
        this.pk = pk;
    }

    public void setSecretKey(byte[] sk) {
        this.sk = sk;
    }

    public PublicKeyPair(byte[] _public, byte[] _secret){
        pk = _public;
        sk = _secret;
    }

    public PublicKeyPair(byte[] delta){
        Curve25519KeyPairGenerator keyPairGenerator = new Curve25519KeyPairGenerator();
        HashMap<String, byte[]> keyPair = keyPairGenerator.generateKeyPair(delta);
        pk = keyPair.get("publicKey");
        sk = keyPair.get("privateKey");
    }

    public PublicKeyPair getPublicMsg(){
        return new PublicKeyPair(pk,null);
    }
}
