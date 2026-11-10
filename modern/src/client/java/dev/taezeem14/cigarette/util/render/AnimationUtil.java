package dev.taezeem14.cigarette.util.render;

public final class AnimationUtil {

    private static long lastFrameNanos = System.nanoTime();
    private static float deltaTime = 0.016F;

    private AnimationUtil() {
    }

    public static void updateDeltaTime() {
        long now = System.nanoTime();
        long diff = now - lastFrameNanos;
        lastFrameNanos = now;
        if (diff > 0L) {
            deltaTime = Math.max(0.0005F, Math.min(0.1F, diff / 1_000_000_000.0F));
        }
    }

    public static float getDeltaTime() {
        return deltaTime;
    }

    public static float lerp(float current, float target, float speed) {
        float diff = target - current;
        if (Math.abs(diff) < 0.0005F) {
            return target;
        }
        float factor = (float) (1.0 - Math.exp(-Math.max(0.01F, speed) * deltaTime));
        return current + diff * factor;
    }

    public static float easeOutCubic(float t) {
        float c = clamp(t, 0.0F, 1.0F);
        float inv = 1.0F - c;
        return 1.0F - inv * inv * inv;
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
