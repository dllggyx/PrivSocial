package key;

import Curve25519.Curve25519KeyAgreement;
import Curve25519.Curve25519PrivateKey;
import Curve25519.Curve25519PublicKey;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static Curve25519.Curve25519.ALGORITHM;

public class EncryptionTools {
    private Cipher cipherEnc;
    private Cipher cipherDec;

    public EncryptionTools(){
        try {
            cipherEnc = Cipher.getInstance("AES/GCM/NoPadding");
            cipherDec = Cipher.getInstance("AES/GCM/NoPadding");
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public byte[] encryptDelta(byte[] mysk, byte[] targetpk, byte []M){
        byte[] C = null;
        try {
            Curve25519KeyAgreement keyAgreement = new Curve25519KeyAgreement(new Curve25519PrivateKey(mysk));
            keyAgreement.doFinal(new Curve25519PublicKey(targetpk));
            SecretKey sharedSecret = keyAgreement.generateSecret(ALGORITHM);
            C = myEncrypt(M,sharedSecret.getEncoded());
        }catch (Exception e){
            e.printStackTrace();
        }
        return C;
    }

    public byte[] decryptDelta(byte[] mysk, byte[] targetpk, byte[] C) {
        byte[] M = null;
        try {
            Curve25519KeyAgreement keyAgreement = new Curve25519KeyAgreement(new Curve25519PrivateKey(mysk));
            keyAgreement.doFinal(new Curve25519PublicKey(targetpk));
            SecretKey sharedSecret = keyAgreement.generateSecret(ALGORITHM);
            M = myDecrypt(C,sharedSecret.getEncoded());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return M;
    }

    public byte[] sign(byte[] svk,Map<byte[], Vector<String>> enc){
        return null;
    }

    public byte[] myToByteArray(BigInteger bi) {
        byte[] array = bi.toByteArray();
        if (array[0] == 0) {
            byte[] tmp = new byte[array.length - 1];
            System.arraycopy(array, 1, tmp, 0, tmp.length);
            array = tmp;
        }
        return array;
    }

    public byte[] myEncrypt(final byte[] plaintext, final byte[] key) {

        final byte[] trueKey = hash("key", key);
        final byte[] iv = new byte[]{'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
        try {
            // From https://proandroiddev.com/security-best-practices-symmetric-encryption-with-aes-in-java-7616beaaade9
            final GCMParameterSpec parameterSpec = new GCMParameterSpec(8 * 16, iv);
            final SecretKeySpec keySpec = new SecretKeySpec(trueKey, "AES");
            cipherEnc.init(Cipher.ENCRYPT_MODE, keySpec, parameterSpec);
            final byte[] c = cipherEnc.doFinal(plaintext);
            return c;
        } catch (GeneralSecurityException exc) {
            throw new RuntimeException(exc);
        }

    }

    public byte[] myDecrypt(final byte[] ciphertext, final byte[] key) {

        final byte[] trueKey = hash("key", key);
        final byte[] iv = new byte[]{'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
        try {
            SecretKeySpec keySpec = new SecretKeySpec(trueKey, "AES");
            final GCMParameterSpec parameterSpec = new GCMParameterSpec(8 * 16, iv);
            cipherDec.init(Cipher.DECRYPT_MODE, keySpec, parameterSpec);
            try {
                return cipherDec.doFinal(ciphertext);
            } catch (AEADBadTagException exc) {
                return null;
            }
        } catch (GeneralSecurityException exc) {
            throw new RuntimeException(exc);
        }
    }


    public static byte[] hash(final String inputString, final byte[]... inputByteArrays) {
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA-256");
            if (inputString != null) {
                md.update(inputString.getBytes(StandardCharsets.UTF_8));
            }
            for (byte[] input : inputByteArrays) md.update(input);
            return md.digest();
        } catch (NoSuchAlgorithmException exc) {
            throw new RuntimeException(exc);
        }
    }
}


