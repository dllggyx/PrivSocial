package Curve25519;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

import static Curve25519.Curve25519.KEY_SIZE;

public class Curve25519KeyPairGenerator {
    private final Random mRandom;

    public Curve25519KeyPairGenerator() {this(new Random());}

    //为什么要换成ThreadLocalRandom
    //Random  ->  ThreadLocalRandom

    public Curve25519KeyPairGenerator(Random random) {
        mRandom = random;
    }

    public HashMap<String,byte[]> generateKeyPair(byte [] delta) {
        byte[] privateKey = new byte[KEY_SIZE];
        mRandom.setSeed(Arrays.hashCode(delta));
        mRandom.nextBytes(privateKey);

        byte[] publicKey = new byte[KEY_SIZE];
        byte[] s = new byte[KEY_SIZE];

        Curve25519.keygen(publicKey, s, privateKey);
        HashMap<String,byte[]> keyMap = new HashMap<String,byte[]>();
        keyMap.put("publicKey",publicKey);
        keyMap.put("privateKey",privateKey);
        return keyMap;
    }
}