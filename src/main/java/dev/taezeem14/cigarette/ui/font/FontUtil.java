package dev.taezeem14.cigarette.ui.font;

import java.awt.Font;
import java.io.InputStream;

public final class FontUtil {

    public static CFontRenderer regular;
    public static CFontRenderer bold;
    public static CFontRenderer small;
    public static CFontRenderer title;

    private static boolean initialized = false;

    private FontUtil() {
    }

    public static void init() {
        if (initialized) return;

        try {
            Font baseRegular = getFont("SF-UI-Display-Regular.otf", Font.PLAIN, 17);
            Font baseSmall = getFont("SF-UI-Display-Regular.otf", Font.PLAIN, 13);
            Font baseBold = getFont("SF-UI-Display-Regular.otf", Font.BOLD, 17);
            Font baseTitle = getFont("Comfortaa-Regular.ttf", Font.BOLD, 21);

            regular = new CFontRenderer(baseRegular, true, true);
            small = new CFontRenderer(baseSmall, true, true);
            bold = new CFontRenderer(baseBold, true, true);
            title = new CFontRenderer(baseTitle, true, true);

            initialized = true;
        } catch (Exception e) {
            e.printStackTrace();
            Font fallback = new Font("Arial", Font.PLAIN, 17);
            regular = new CFontRenderer(fallback, true, true);
            small = new CFontRenderer(new Font("Arial", Font.PLAIN, 13), true, true);
            bold = new CFontRenderer(fallback.deriveFont(Font.BOLD), true, true);
            title = new CFontRenderer(new Font("Arial", Font.BOLD, 21), true, true);
            initialized = true;
        }
    }

    private static Font getFont(String resourceName, int style, int size) {
        String[] candidatePaths = {
                "/assets/cigarette/fonts/" + resourceName,
                "/assets/minecraft/cigarette/fonts/" + resourceName,
                "/assets/cigarette/" + resourceName
        };

        for (String path : candidatePaths) {
            try (InputStream is = FontUtil.class.getResourceAsStream(path)) {
                if (is != null) {
                    Font font = Font.createFont(Font.TRUETYPE_FONT, is);
                    return font.deriveFont(style, (float) size);
                }
            } catch (Exception ignored) {
            }
        }

        String[] fontNames = {"SF Pro Display", "Segoe UI", "Arial"};
        for (String name : fontNames) {
            Font font = new Font(name, style, size);
            if (font.getFamily().equalsIgnoreCase(name)) {
                return font;
            }
        }
        return new Font("Arial", style, size);
    }

    public static void ensureInitialized() {
        if (!initialized || regular == null) {
            init();
        }
    }

    public static float drawString(String text, float x, float y, int color) {
        ensureInitialized();
        return regular.drawString(text, x, y, color);
    }

    public static float drawStringWithShadow(String text, float x, float y, int color) {
        ensureInitialized();
        return regular.drawStringWithShadow(text, x, y, color);
    }

    public static float drawCenteredString(String text, float x, float y, int color) {
        ensureInitialized();
        return regular.drawCenteredString(text, x, y, color);
    }

    public static float drawCenteredStringWithShadow(String text, float x, float y, int color) {
        ensureInitialized();
        return regular.drawCenteredStringWithShadow(text, x, y, color);
    }

    public static int getStringWidth(String text) {
        ensureInitialized();
        return regular.getStringWidth(text);
    }

    public static int getHeight() {
        ensureInitialized();
        return regular.getHeight();
    }
}
