package tree_strcture;

import key.IdentityKeys;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public class Group implements Serializable {
    private static final long serialVersionUID = -3157277115960539999L;
    //(id,[pk,svk])
    public Map<String, IdentityKeys> memberMap;

    public Group(){
        memberMap = new LinkedHashMap<>();
    }

    public void addMember(String ID,IdentityKeys key){
        memberMap.put(ID,key);
    }

    public void removeMember(String ID){
        memberMap.remove(ID);
    }

    public int getGroupSize(){
        return memberMap.size();
    }

    public IdentityKeys getKey(String ID){
        return memberMap.get(ID);
    }

}
