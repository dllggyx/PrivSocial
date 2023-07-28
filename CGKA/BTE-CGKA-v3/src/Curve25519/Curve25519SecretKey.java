package Curve25519;

import javax.crypto.SecretKey;

public class Curve25519SecretKey extends Curve25519Key implements SecretKey {
    public Curve25519SecretKey(byte[] key) {
        super(key);
    }
}