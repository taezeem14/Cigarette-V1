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
            Font baseRegular = getFont("font.ttf", 18);
            Font baseBold = getFont("font_bold.ttf", 18);
            Font baseSmall = getFont("font.ttf", 14);
            Font baseTitle = getFont("font_bold.ttf", 22);

            regular = new CFontRenderer(baseRegular, true, true);
            bold = new CFontRenderer(baseBold, true, true);
            small = new CFontRenderer(baseSmall, true, true);
            title = new CFontRenderer(baseTitle, true, true);

            initialized = true;
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback to standard system font
            Font fallback = new Font("Arial", Font.PLAIN, 18);
            regular = new CFontRenderer(fallback, true, true);
            bold = new CFontRenderer(fallback.deriveFont(Font.BOLD), true, true);
            small = new CFontRenderer(new Font("Arial", Font.PLAIN, 14), true, true);
            title = new CFontRenderer(new Font("Arial", Font.BOLD, 22), true, true);
            initialized = true;
        }
    }

    private static Font getFont(String resourcePath, int size) {
        try {
            InputStream is = FontUtil.class.getResourceAsStream("/assets/minecraft/cigarette/fonts/" + resourcePath);
            if (is == null) {
                is = FontUtil.class.getResourceAsStream("/assets/minecraft/cigarette/" + resourcePath);
            }
            if (is != null) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, is);
                return font.deriveFont(Font.PLAIN, (float) size);
            }
        } catch (Exception ignored) {
        }

        // Clean fallback to SF Pro, Product Sans, or Arial
        String[] fontNames = {"SF Pro Display", "Product Sans", "Segoe UI", "Arial"};
        for (String name : fontNames) {
            Font font = new Font(name, Font.PLAIN, size);
            if (font.getFamily().equalsIgnoreCase(name)) {
                return font;
            }
        }
        return new Font("Arial", Font.PLAIN, size);
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
