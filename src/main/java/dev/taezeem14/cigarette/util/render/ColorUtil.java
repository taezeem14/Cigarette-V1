package dev.taezeem14.cigarette.util.render;

import java.awt.Color;

public final class ColorUtil {

    // Theme Colors
    public static final int BACKGROUND = 0xFF141416;
    public static final int HEADER = 0xFF1A1A1D;
    public static final int ACCENT = 0xFF3882F6;
    public static final int EMBER_ORANGE = 0xFFFF5722;
    public static final int TEXT_PRIMARY = 0xFFEBEBEF;
    public static final int TEXT_MUTED = 0xFF8E8E93;

    public static final Color COLOR_BACKGROUND = new Color(0x14, 0x14, 0x16);
    public static final Color COLOR_HEADER = new Color(0x1A, 0x1A, 0x1D);
    public static final Color COLOR_ACCENT = new Color(0x38, 0x82, 0xF6);
    public static final Color COLOR_EMBER_ORANGE = new Color(0xFF, 0x57, 0x22);
    public static final Color COLOR_TEXT_PRIMARY = new Color(0xEB, 0xEB, 0xEF);
    public static final Color COLOR_TEXT_MUTED = new Color(0x8E, 0x8E, 0x93);

    private ColorUtil() {
    }

    public static int toRGBA(int r, int g, int b, int a) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public static int toRGBA(int r, int g, int b) {
        return toRGBA(r, g, b, 255);
    }

    public static int getRed(int color) {
        return (color >> 16) & 0xFF;
    }

    public static int getGreen(int color) {
        return (color >> 8) & 0xFF;
    }

    public static int getBlue(int color) {
        return color & 0xFF;
    }

    public static int getAlpha(int color) {
        return (color >> 24) & 0xFF;
    }

    public static int modifyAlpha(int color, int newAlpha) {
        newAlpha = Math.max(0, Math.min(255, newAlpha));
        return (color & 0x00FFFFFF) | (newAlpha << 24);
    }

    public static int hexToRGBA(String hex) {
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (hex.length() == 6) {
            int rgb = Integer.parseInt(hex, 16);
            return 0xFF000000 | rgb;
        } else if (hex.length() == 8) {
            return (int) Long.parseLong(hex, 16);
        }
        return 0xFFFFFFFF;
    }

    public static int interpolateColor(int color1, int color2, float factor) {
        factor = Math.max(0.0f, Math.min(1.0f, factor));
        int a1 = getAlpha(color1);
        int r1 = getRed(color1);
        int g1 = getGreen(color1);
        int b1 = getBlue(color1);

        int a2 = getAlpha(color2);
        int r2 = getRed(color2);
        int g2 = getGreen(color2);
        int b2 = getBlue(color2);

        int a = (int) (a1 + (a2 - a1) * factor);
        int r = (int) (r1 + (r2 - r1) * factor);
        int g = (int) (g1 + (g2 - g1) * factor);
        int b = (int) (b1 + (b2 - b1) * factor);

        return toRGBA(r, g, b, a);
    }

    public static Color interpolateColor(Color c1, Color c2, float factor) {
        return new Color(interpolateColor(c1.getRGB(), c2.getRGB(), factor), true);
    }

    public static int getRainbow(int speed, int offset) {
        float hue = ((System.currentTimeMillis() + offset) % (speed * 1000L)) / (float) (speed * 1000L);
        return Color.HSBtoRGB(hue, 0.8f, 1.0f);
    }

    public static int getRainbow(int speed, int offset, float saturation, float brightness) {
        float hue = ((System.currentTimeMillis() + offset) % (speed * 1000L)) / (float) (speed * 1000L);
        return Color.HSBtoRGB(hue, saturation, brightness);
    }

    public static int getAstolfo(int speed, int offset) {
        float hue = ((System.currentTimeMillis() + offset) % (speed * 1000L)) / (float) (speed * 1000L);
        hue = hue > 0.5f ? 1.0f - hue : hue;
        hue += 0.5f;
        return Color.HSBtoRGB(hue, 0.6f, 1.0f);
    }

    public static int getPulseColor(Color color, int speed, int offset) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        float progress = ((System.currentTimeMillis() + offset) % (speed * 1000L)) / (float) (speed * 1000L);
        float wave = (float) (Math.sin(progress * Math.PI * 2.0) * 0.5 + 0.5);
        float brightness = Math.max(0.3f, Math.min(1.0f, hsb[2] * (0.6f + 0.4f * wave)));
        return Color.HSBtoRGB(hsb[0], hsb[1], brightness);
    }

    public static int getHealthColor(float health, float maxHealth) {
        float ratio = Math.max(0.0f, Math.min(1.0f, health / maxHealth));
        if (ratio > 0.5f) {
            float f = (ratio - 0.5f) * 2.0f;
            return interpolateColor(0xFFFFCC00, 0xFF00FF00, f);
        } else {
            float f = ratio * 2.0f;
            return interpolateColor(0xFFFF0000, 0xFFFFCC00, f);
        }
    }
}
