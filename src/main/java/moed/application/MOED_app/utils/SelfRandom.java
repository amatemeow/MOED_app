package moed.application.MOED_app.utils;

import java.time.LocalTime;
import java.util.Random;

public class SelfRandom extends Random {
    private double seed = getNewSeed();
    private long cnt = 0;
    private final double M = Math.pow(2, 32);
    private Long A = 1664525L;
    private Long C = 1013904223L;

    @Override
    public double nextDouble() {
        seed = (A * seed + C) % M;
        return seed / M;
    }

    private double getNewSeed() {
        return (double) Math.round((double) LocalTime.now().toNanoOfDay());
    }
}
