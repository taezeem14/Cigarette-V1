package dev.taezeem14.cigarette.util.math;

import java.util.concurrent.ThreadLocalRandom;

public final class RandomUtil {

    private RandomUtil() {
    }

    public static double nextDouble(double min, double max) {
        if (min >= max) return min;
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    public static float nextFloat(float min, float max) {
        if (min >= max) return min;
        return (float) ThreadLocalRandom.current().nextDouble(min, max);
    }

    public static int nextInt(int min, int max) {
        if (min >= max) return min;
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    public static double nextGaussian(double mean, double stdev) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double u1 = random.nextDouble();
        while (u1 <= 1e-15) {
            u1 = random.nextDouble();
        }
        double u2 = random.nextDouble();

        double z0 = Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * u2);
        return mean + z0 * stdev;
    }

    public static double perlin1D(double x) {
        int x0 = (int) Math.floor(x);
        int x1 = x0 + 1;
        double fx = x - x0;

        // Smoothstep (quintic fade curve: 6t^5 - 15t^4 + 10t^3)
        double fade = fx * fx * fx * (fx * (fx * 6.0 - 15.0) + 10.0);

        double grad0 = pseudoGradient(x0);
        double grad1 = pseudoGradient(x1);

        double v0 = grad0 * fx;
        double v1 = grad1 * (fx - 1.0);

        return v0 + fade * (v1 - v0);
    }

    private static double pseudoGradient(int hash) {
        int h = hash * 374761393 + 668265263;
        h = (h ^ (h >> 13)) * 1274126177;
        int res = h ^ (h >> 16);
        return ((res & 0xFFFF) / 32767.5) - 1.0;
    }

    public static double clampedRandom(double min, double max) {
        return nextDouble(min, max);
    }

    public static boolean chance(double percentage) {
        if (percentage <= 0.0) return false;
        if (percentage >= 100.0) return true;
        return ThreadLocalRandom.current().nextDouble(0.0, 100.0) <= percentage;
    }
}
