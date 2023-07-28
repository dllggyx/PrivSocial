package key;

import java.io.Serializable;

public class IdentityKeys implements Serializable {
    private static final long serialVersionUID = -7003221622214272459L;
    public PublicKeyPair pkp;
    public SignKeyPair skp;


    public IdentityKeys(PublicKeyPair pkp,SignKeyPair skp,int flag){
        if(flag == 0) {
            this.pkp = new PublicKeyPair(pkp.pk, null);
            this.skp = new SignKeyPair(skp.svk,null);
        }else{
            this.pkp = pkp;
            this.skp = skp;
        }
    }

    public IdentityKeys(PublicKeyPair pkp, SignKeyPair skp){
        this.pkp = pkp;
        this.skp = skp;
    }

    public IdentityKeys getPkAndSvk(){

        return new IdentityKeys(new PublicKeyPair(pkp.pk,null),
                new SignKeyPair(skp.svk,null));
    }

//    public PublicKeyPair getPk(){
//        String g1 = pkp.g;
//        String p1 = pkp.p;
//        return new PublicKeyPair(g1,p1,pkp.pk,null);
//    }

    public IdentityKeys(IdentityKeys ikey){
        pkp = ikey.pkp;
        skp = ikey.skp;
    }
}
