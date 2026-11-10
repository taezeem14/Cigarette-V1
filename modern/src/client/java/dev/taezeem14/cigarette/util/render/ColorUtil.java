package dev.taezeem14.cigarette.util.render;

import java.awt.Color;

public final class ColorUtil {

    public static final int BACKGROUND = 0xFF141416;
    public static final int HEADER = 0xFF1A1A1D;
    public static final int CONTAINER = 0xFF1E1E22;
    public static final int ELECTRIC_BLUE = 0xFF3882F6;
    public static final int EMBER_ORANGE = 0xFFFF5722;
    public static final int AMETHYST = 0xFF8B5CF6;
    public static final int EMERALD = 0xFF10B981;
    public static final int BORDER = 0xFF27272A;
    public static final int TEXT_PRIMARY = 0xFFEBEBEF;
    public static final int TEXT_MUTED = 0xFF8E8E93;

    public static final String[] THEME_PRESETS = {
            "Electric Blue",
            "Ember Orange",
            "Amethyst",
            "Emerald",
            "Rainbow"
    };

    private static volatile String themePreset = "Electric Blue";
    private static volatile int accentColor = ELECTRIC_BLUE;

    private ColorUtil() {
    }

    public static int getAccent() {
        if ("Rainbow".equalsIgnoreCase(themePreset)) {
            return getRainbow(4, 0);
        }
        return accentColor;
    }

    public static void setAccent(int color) {
        accentColor = 0xFF000000 | (color & 0x00FFFFFF);
    }

    public static String getThemePreset() {
        return themePreset;
    }

    public static void setThemePreset(String preset) {
        if (preset == null || preset.trim().isEmpty()) return;
        for (String p : THEME_PRESETS) {
            if (p.equalsIgnoreCase(preset.trim())) {
                themePreset = p;
                break;
            }
        }
        if ("Electric Blue".equals(themePreset)) {
            accentColor = ELECTRIC_BLUE;
        } else if ("Ember Orange".equals(themePreset)) {
            accentColor = EMBER_ORANGE;
        } else if ("Amethyst".equals(themePreset)) {
            accentColor = AMETHYST;
        } else if ("Emerald".equals(themePreset)) {
            accentColor = EMERALD;
        }
    }

    public static String cycleThemePreset() {
        for (int i = 0; i < THEME_PRESETS.length; i++) {
            if (THEME_PRESETS[i].equalsIgnoreCase(themePreset)) {
                String next = THEME_PRESETS[(i + 1) % THEME_PRESETS.length];
                setThemePreset(next);
                return next;
            }
        }
        setThemePreset(THEME_PRESETS[0]);
        return THEME_PRESETS[0];
    }

    public static int getRainbow(float seconds, int indexOffset) {
        float cycleMs = Math.max(200.0F, seconds * 1000.0F);
        float hue = ((System.currentTimeMillis() + indexOffset) % (long) cycleMs) / cycleMs;
        return 0xFF000000 | (Color.HSBtoRGB(hue, 0.78F, 1.0F) & 0x00FFFFFF);
    }

    public static int interpolateColor(int color1, int color2, float fraction) {
        fraction = Math.max(0.0F, Math.min(1.0F, fraction));
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = (int) (a1 + (a2 - a1) * fraction);
        int r = (int) (r1 + (r2 - r1) * fraction);
        int g = (int) (g1 + (g2 - g1) * fraction);
        int b = (int) (b1 + (b2 - b1) * fraction);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int modifyAlpha(int color, int alpha) {
        alpha = Math.max(0, Math.min(255, alpha));
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    public static int getHealthColor(float health, float maxHealth) {
        if (maxHealth <= 0.0F) return 0xFF00FF00;
        float progress = Math.max(0.0F, Math.min(1.0F, health / maxHealth));
        if (progress > 0.5F) {
            return interpolateColor(0xFFFFFF00, 0xFF00FF66, (progress - 0.5F) * 2.0F);
        }
        return interpolateColor(0xFFFF3333, 0xFFFFFF00, progress * 2.0F);
    }
}
