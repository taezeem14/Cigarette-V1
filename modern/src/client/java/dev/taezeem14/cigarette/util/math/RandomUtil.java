package dev.taezeem14.cigarette.util.math;

import java.util.concurrent.ThreadLocalRandom;

public final class RandomUtil {

    private static double nextGaussianValue;
    private static boolean hasNextGaussian = false;

    private RandomUtil() {
    }

    public static double nextGaussian(double mean, double stdDev) {
        if (hasNextGaussian) {
            hasNextGaussian = false;
            return mean + nextGaussianValue * stdDev;
        }

        double u1, u2, s;
        do {
            u1 = 2.0 * ThreadLocalRandom.current().nextDouble() - 1.0;
            u2 = 2.0 * ThreadLocalRandom.current().nextDouble() - 1.0;
            s = u1 * u1 + u2 * u2;
        } while (s >= 1.0 || s == 0.0);

        double multiplier = Math.sqrt(-2.0 * Math.log(s) / s);
        nextGaussianValue = u2 * multiplier;
        hasNextGaussian = true;

        return mean + (u1 * multiplier) * stdDev;
    }

    public static double nextDouble(double min, double max) {
        if (min >= max) return min;
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    public static float nextFloat(float min, float max) {
        if (min >= max) return min;
        return min + ThreadLocalRandom.current().nextFloat() * (max - min);
    }

    public static int nextInt(int min, int max) {
        if (min >= max) return min;
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    public static boolean chance(double percentage) {
        if (percentage <= 0) return false;
        if (percentage >= 100) return true;
        return ThreadLocalRandom.current().nextDouble(0.0, 100.0) <= percentage;
    }
}
