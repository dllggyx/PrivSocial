package key;

import java.io.Serializable;

public class SignKeyPair implements Serializable {
    private static final long serialVersionUID = -7668723697993261712L;
    public String svk;
    private String ssk;
    public String p;
    public String g;

    public String getSignPublicKey() {
        return svk;
    }

    public String getSignSecretKey() {
        return ssk;
    }

    public void setSignPublicKey(String ssk) {
        this.ssk = ssk;
    }

    public void setSignSecretKey(String svk) {
        this.svk = svk;
    }

    public SignKeyPair(String _g, String _p, String _public, String _secret){
        g = _g;
        p = _p;
        svk = _public;
        ssk = _secret;
    }

    public void init(String _g, String _p){
        g = _g;
        p = _p;
    }

    public SignKeyPair getPublicMsg(){
        return new SignKeyPair(g,p,svk,null);
    }
}
