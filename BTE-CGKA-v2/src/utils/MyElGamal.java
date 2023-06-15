package utils;


import java.math.BigInteger;
import java.security.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

public class MyElGamal {
    private BigInteger p;
    private BigInteger q;
    private BigInteger g;
    public int keySize = 256;


    public HashMap<String, String> generateKeyPair(byte[] delta) {
        initP(delta);
        Random random = new Random();
        random.setSeed(Arrays.hashCode(delta));
        BigInteger privateKey = new BigInteger(random.nextInt(keySize - 1), random);
        BigInteger publicKey = g.modPow(privateKey, p);
        HashMap<String, String> keyPair = new HashMap<>();
        keyPair.put("privateKey", privateKey.toString());
        keyPair.put("publicKey", publicKey.toString());
        return keyPair;
    }

    public HashMap<String, String> encryElGamal(BigInteger m, BigInteger g, BigInteger p, BigInteger publicKey) {
        SecureRandom secureRandom = new SecureRandom();
        //System.out.println(p.bitLength());
        BigInteger k = new BigInteger(secureRandom.nextInt(p.bitLength() - 1), secureRandom);
        BigInteger K = publicKey.modPow(k, p);
        //System.out.println("K:" + K.toString());
        BigInteger c1 = g.modPow(k, p);
        BigInteger c2 = m.multiply(K).mod(p);
        HashMap<String, String> encMessage = new HashMap<>();
        encMessage.put("c1", c1.toString());
        encMessage.put("c2", c2.toString());
        return encMessage;
    }

    public BigInteger decryElGamal(BigInteger g, BigInteger p, BigInteger privateKey, ArrayList<String> encMessage) {
        BigInteger c1 = new BigInteger(encMessage.get(0));
        BigInteger c2 = new BigInteger(encMessage.get(1));
        BigInteger K = c1.modPow(privateKey, p);
        BigInteger m = c2.multiply(K.modInverse(p)).mod(p);
        //System.out.println("K:" + K.toString());
        //System.out.println("m:" + m);
        return m;
    }

    public void initP(byte[] delta) {
        Random random = new Random();
        random.setSeed(Arrays.hashCode(delta));
        while (true) {
            q = BigInteger.probablePrime(keySize, random);
            if (q.bitLength() != keySize) {
                continue;
            }
            if (q.isProbablePrime(10)) {
                p = q.multiply(new BigInteger("2")).add(BigInteger.ONE);
                if (p.isProbablePrime(10)) {
                    break;
                }
            }
        }
        do {
            g = BigInteger.probablePrime(p.bitLength() - 1, random);
        } while (g.modPow(BigInteger.ONE, p).equals(BigInteger.ONE) || g.modPow(q, p).equals(BigInteger.ONE));
//        System.out.println(p);
//        System.out.println(q);
//        System.out.println(g);
    }

    public BigInteger getG() {
        return g;
    }

    public String getGStr(){
        return g.toString();
    }

    public BigInteger getP() {
        return p;
    }

    public String getPStr(){
        return p.toString();
    }

    public BigInteger getQ() {
        return q;
    }


}

