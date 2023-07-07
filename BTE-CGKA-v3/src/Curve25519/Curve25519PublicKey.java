package Curve25519;

import java.security.PublicKey;

public final class Curve25519PublicKey extends Curve25519Key implements PublicKey {
    public Curve25519PublicKey(byte[] key) {
        super(key);
    }
}