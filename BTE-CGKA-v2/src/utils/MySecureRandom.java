package utils;

import java.security.SecureRandom;
import java.util.Random;

public class MySecureRandom extends SecureRandom {
    private final Random random;

    public MySecureRandom(Random random) {
        this.random = random;
    }

    @Override
    public void nextBytes(byte[] bytes) {
        random.nextBytes(bytes);
    }

    @Override
    public int nextInt() {
        return random.nextInt();
    }

//    public void setSeed(long seed){
//        this.random.setSeed(seed);
//    }
}

