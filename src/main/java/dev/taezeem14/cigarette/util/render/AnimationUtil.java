package dev.taezeem14.cigarette.util.render;

import org.lwjgl.Sys;

/**
 * Frame-rate-independent animation timing and easing utilities backed by LWJGL's high-resolution timer.
 */
public final class AnimationUtil {

    private static long lastTime = 0L;
    private static float deltaTime = 0.016f;

    private AnimationUtil() {
    }

    /**
     * Updates the cached frame delta time in seconds, clamped to [0.001, 0.05].
     * Duplicate calls within the same timer tick are safely ignored.
     */
    public static void updateDeltaTime() {
        long now = Sys.getTime();
        long resolution = Sys.getTimerResolution();
        if (resolution <= 0L) {
            resolution = 1000L;
        }

        if (lastTime == 0L) {
            lastTime = now;
            deltaTime = 0.016f;
            return;
        }

        if (now <= lastTime) {
            if (now < lastTime) {
                lastTime = now;
            }
            return;
        }

        float dt = (float) (now - lastTime) / (float) resolution;
        lastTime = now;
        deltaTime = clamp(dt, 0.001f, 0.05f);
    }

    public static float getDeltaTime() {
        return deltaTime;
    }

    /**
     * Exponentially interpolates {@code current} toward {@code target} independently of frame rate.
     */
    public static float lerp(float current, float target, float speed) {
        if (Math.abs(target - current) < 0.0005f) {
            return target;
        }
        if (speed <= 0.0f) {
            return current;
        }
        float factor = 1.0f - (float) Math.exp(-speed * deltaTime);
        factor = clamp(factor, 0.0f, 1.0f);
        return current + (target - current) * factor;
    }

    public static float easeOutCubic(float t) {
        float x = clamp(t, 0.0f, 1.0f);
        float inv = 1.0f - x;
        return 1.0f - inv * inv * inv;
    }

    public static float easeOutExpo(float t) {
        float x = clamp(t, 0.0f, 1.0f);
        return x >= 1.0f ? 1.0f : 1.0f - (float) Math.pow(2.0, -10.0 * x);
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
