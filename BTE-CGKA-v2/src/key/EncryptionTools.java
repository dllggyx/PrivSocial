package key;

import utils.MyElGamal;
import utils.MyUtils;

import java.math.BigInteger;
import java.util.*;

public class EncryptionTools {

    public static ArrayList<String> encryptDelta(PublicKeyPair pkp, byte []M){
        ArrayList<String> C = new ArrayList<String>();
        try {
            MyElGamal mg = new MyElGamal();
            //C = mg.encryptByPublicKey(M, pk);
            HashMap<String, String> temp = mg.encryElGamal(new BigInteger(1,M),new BigInteger(pkp.g),new BigInteger(pkp.p),new BigInteger(pkp.pk));
            C.add(temp.get("c1"));
            C.add(temp.get("c2"));
        }catch (Exception e){
            e.printStackTrace();
        }
        return C;
    }

    public static byte[] decryptDelta(PublicKeyPair pkp, ArrayList<String> C){

        byte[] M = null;
        try {
            MyElGamal mg = new MyElGamal();
            BigInteger m2 = mg.decryElGamal(new BigInteger(pkp.g),new BigInteger(pkp.p),new BigInteger(pkp.getSecretKey()),C);
            M = myToByteArray(m2);
        }catch (Exception e){
            e.printStackTrace();
        }
        return M;
    }

    public static byte[] sign(SignKeyPair skp,Map<ArrayList<String>, Vector<String>> enc){
        return null;
    }

    public static byte[] myToByteArray(BigInteger bi) {
        byte[] array = bi.toByteArray();
        if (array[0] == 0) {
            byte[] tmp = new byte[array.length - 1];
            System.arraycopy(array, 1, tmp, 0, tmp.length);
            array = tmp;
        }
        return array;
    }

}

