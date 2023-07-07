package key;

import java.io.Serializable;

public class SignKeyPair implements Serializable {
    private static final long serialVersionUID = -7668723697993261712L;
    public byte[] svk;
    private byte[] ssk;

    public byte[] getSignPublicKey() {
        return svk;
    }

    public byte[] getSignSecretKey() {
        return ssk;
    }

    public void setSignPublicKey(byte[] ssk) {
        this.ssk = ssk;
    }

    public void setSignSecretKey(byte[] svk) {
        this.svk = svk;
    }

    public SignKeyPair(byte[] _public, byte[] _secret){
        svk = _public;
        ssk = _secret;
    }

    public SignKeyPair getPublicMsg(){
        return new SignKeyPair(svk,null);
    }
}
