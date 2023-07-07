package network;

import key.IdentityKeys;

import java.io.Serializable;

public class WelcomeMsg implements Serializable {
    private static final long serialVersionUID = 1157433524014601914L;
    public String ID;
    public IdentityKeys keys;
    public WelcomeMsg(String id,IdentityKeys k){
        ID = id;
        keys = k;
    }

}
