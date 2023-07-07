package network;

import key.IdentityKeys;

import java.io.Serializable;

public class AppendMsg implements Serializable {
    private static final long serialVersionUID = -4548599804146394229L;
    public String tag;
    public String ID;
    public IdentityKeys identityKeys;
    public int pos;
    public boolean expand;
    public AppendMsg(String _tag, String _ID, IdentityKeys keys){
        tag = _tag;
        ID = _ID;
        identityKeys = keys;
        pos = -1;
    }

    public void setPos(int p,boolean ex){
        if(tag.equals("add")) {
            pos = p;
            expand = ex;
        }
    }
}
