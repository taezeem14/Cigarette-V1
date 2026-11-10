package dev.taezeem14.cigarette.util.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayDeque;
import java.util.Deque;

public final class RenderUtil {

    private static final Minecraft mc = Minecraft.getInstance();
    private static final Deque<int[]> SCISSOR_STACK = new ArrayDeque<>();

    private RenderUtil() {
    }

    public static void drawRect(GuiGraphicsExtractor g, float x, float y, float width, float height, int color) {
        if (g == null || width <= 0.0F || height <= 0.0F) return;
        int x1 = Math.round(x);
        int y1 = Math.round(y);
        int x2 = Math.round(x + width);
        int y2 = Math.round(y + height);
        if (x2 > x1 && y2 > y1) {
            g.fill(x1, y1, x2, y2, color);
        }
    }

    public static void drawGradientRect(GuiGraphicsExtractor g, float x, float y, float width, float height, int topColor, int bottomColor) {
        if (g == null || width <= 0.0F || height <= 0.0F) return;
        int x1 = Math.round(x);
        int y1 = Math.round(y);
        int x2 = Math.round(x + width);
        int y2 = Math.round(y + height);
        if (x2 > x1 && y2 > y1) {
            g.fillGradient(x1, y1, x2, y2, topColor, bottomColor);
        }
    }

    public static void drawHorizontalGradient(GuiGraphicsExtractor g, float x, float y, float width, float height, int leftColor, int rightColor) {
        if (g == null || width <= 0.0F || height <= 0.0F) return;
        int steps = Math.max(2, Math.min(32, Math.round(width / 2.0F)));
        float stepW = width / steps;
        for (int i = 0; i < steps; i++) {
            float t = i / (float) (steps - 1);
            int col = ColorUtil.interpolateColor(leftColor, rightColor, t);
            drawRect(g, x + i * stepW, y, stepW + 0.5F, height, col);
        }
    }

    public static void drawOutline(GuiGraphicsExtractor g, float x, float y, float width, float height, float thickness, int color) {
        if (g == null || width <= 0.0F || height <= 0.0F) return;
        float t = Math.max(1.0F, thickness);
        drawRect(g, x, y, width, t, color);
        drawRect(g, x, y + height - t, width, t, color);
        drawRect(g, x, y + t, t, Math.max(0.0F, height - t * 2.0F), color);
        drawRect(g, x + width - t, y + t, t, Math.max(0.0F, height - t * 2.0F), color);
    }

    public static void drawRoundedRect(GuiGraphicsExtractor g, float x, float y, float width, float height, float radius, int color) {
        if (g == null || width <= 0.0F || height <= 0.0F) return;
        int r = Math.min(Math.round(radius), Math.min(Math.round(width) / 2, Math.round(height) / 2));
        if (r <= 1) {
            drawRect(g, x, y, width, height, color);
            return;
        }
        int x1 = Math.round(x);
        int y1 = Math.round(y);
        int x2 = Math.round(x + width);
        int y2 = Math.round(y + height);
        // Core horizontal band & corner scanlines for crisp rounded geometry
        g.fill(x1, y1 + r, x2, y2 - r, color);
        for (int dy = 0; dy < r; dy++) {
            double dist = r - dy - 0.5;
            int inset = r - (int) Math.round(Math.sqrt(Math.max(0.0, r * r - dist * dist)));
            g.fill(x1 + inset, y1 + dy, x2 - inset, y1 + dy + 1, color);
            g.fill(x1 + inset, y2 - dy - 1, x2 - inset, y2 - dy, color);
        }
    }

    public static void drawRoundedRectTop(GuiGraphicsExtractor g, float x, float y, float width, float height, float radius, int color) {
        if (g == null || width <= 0.0F || height <= 0.0F) return;
        int r = Math.min(Math.round(radius), Math.min(Math.round(width) / 2, Math.round(height)));
        if (r <= 1) {
            drawRect(g, x, y, width, height, color);
            return;
        }
        int x1 = Math.round(x);
        int y1 = Math.round(y);
        int x2 = Math.round(x + width);
        int y2 = Math.round(y + height);
        g.fill(x1, y1 + r, x2, y2, color);
        for (int dy = 0; dy < r; dy++) {
            double dist = r - dy - 0.5;
            int inset = r - (int) Math.round(Math.sqrt(Math.max(0.0, r * r - dist * dist)));
            g.fill(x1 + inset, y1 + dy, x2 - inset, y1 + dy + 1, color);
        }
    }

    public static void drawRoundedOutline(GuiGraphicsExtractor g, float x, float y, float width, float height, float radius, float thickness, int color) {
        if (g == null || width <= 0.0F || height <= 0.0F) return;
        int r = Math.min(Math.round(radius), Math.min(Math.round(width) / 2, Math.round(height) / 2));
        if (r <= 1) {
            drawOutline(g, x, y, width, height, thickness, color);
            return;
        }
        int x1 = Math.round(x);
        int y1 = Math.round(y);
        int x2 = Math.round(x + width);
        int y2 = Math.round(y + height);
        g.fill(x1 + r, y1, x2 - r, y1 + 1, color);
        g.fill(x1 + r, y2 - 1, x2 - r, y2, color);
        g.fill(x1, y1 + r, x1 + 1, y2 - r, color);
        g.fill(x2 - 1, y1 + r, x2, y2 - r, color);
        for (int dy = 0; dy < r; dy++) {
            double dist = r - dy - 0.5;
            int inset = r - (int) Math.round(Math.sqrt(Math.max(0.0, r * r - dist * dist)));
            g.fill(x1 + inset, y1 + dy, x1 + inset + 1, y1 + dy + 1, color);
            g.fill(x2 - inset - 1, y1 + dy, x2 - inset, y1 + dy + 1, color);
            g.fill(x1 + inset, y2 - dy - 1, x1 + inset + 1, y2 - dy, color);
            g.fill(x2 - inset - 1, y2 - dy - 1, x2 - inset, y2 - dy, color);
        }
    }

    public static void drawCircle(GuiGraphicsExtractor g, float cx, float cy, float radius, int color) {
        drawRoundedRect(g, cx - radius, cy - radius, radius * 2.0F, radius * 2.0F, radius, color);
    }

    public static void drawDropShadow(GuiGraphicsExtractor g, float x, float y, float width, float height, float radius, int color) {
        int baseAlpha = (color >> 24) & 0xFF;
        if (baseAlpha <= 0) return;
        for (int i = 3; i >= 1; i--) {
            int layerAlpha = Math.max(4, (baseAlpha * (4 - i)) / 10);
            drawRoundedRect(g, x - i, y - i * 0.5F, width + i * 2.0F, height + i * 2.0F, radius + i, ColorUtil.modifyAlpha(0xFF000000, layerAlpha));
        }
    }

    public static void drawText(GuiGraphicsExtractor g, String text, float x, float y, int color, boolean shadow) {
        if (g == null || text == null || mc.font == null) return;
        g.text(mc.font, text, Math.round(x), Math.round(y), color, shadow);
    }

    public static void drawSmallText(GuiGraphicsExtractor g, String text, float x, float y, int color, boolean shadow) {
        if (g == null || text == null || mc.font == null) return;
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(0.82F, 0.82F);
        g.text(mc.font, text, 0, 0, color, shadow);
        g.pose().popMatrix();
    }

    public static void drawCenteredText(GuiGraphicsExtractor g, String text, float cx, float y, int color, boolean shadow) {
        int w = getStringWidth(text);
        drawText(g, text, cx - w / 2.0F, y, color, shadow);
    }

    public static void drawCenteredSmallText(GuiGraphicsExtractor g, String text, float cx, float y, int color, boolean shadow) {
        float w = getSmallStringWidth(text);
        drawSmallText(g, text, cx - w / 2.0F, y, color, shadow);
    }

    public static int getStringWidth(String text) {
        Font font = mc.font;
        return (font != null && text != null) ? font.width(text) : 0;
    }

    public static float getSmallStringWidth(String text) {
        return getStringWidth(text) * 0.82F;
    }

    public static int getFontHeight() {
        return mc.font != null ? mc.font.lineHeight : 9;
    }

    public static float getSmallFontHeight() {
        return getFontHeight() * 0.82F;
    }

    public static void pushScissor(GuiGraphicsExtractor g, float x, float y, float width, float height, float scaleMultiplier) {
        int sx1 = Math.round(x * scaleMultiplier);
        int sy1 = Math.round(y * scaleMultiplier);
        int sx2 = Math.round((x + Math.max(0.0F, width)) * scaleMultiplier);
        int sy2 = Math.round((y + Math.max(0.0F, height)) * scaleMultiplier);

        int[] rect = {sx1, sy1, Math.max(sx1, sx2), Math.max(sy1, sy2)};
        if (!SCISSOR_STACK.isEmpty()) {
            int[] parent = SCISSOR_STACK.peek();
            rect[0] = Math.max(parent[0], rect[0]);
            rect[1] = Math.max(parent[1], rect[1]);
            rect[2] = Math.min(parent[2], rect[2]);
            rect[3] = Math.min(parent[3], rect[3]);
            if (rect[2] < rect[0]) rect[2] = rect[0];
            if (rect[3] < rect[1]) rect[3] = rect[1];
        }
        SCISSOR_STACK.push(rect);
        g.enableScissor(rect[0], rect[1], rect[2], rect[3]);
    }

    public static void popScissor(GuiGraphicsExtractor g) {
        if (!SCISSOR_STACK.isEmpty()) {
            SCISSOR_STACK.pop();
        }
        g.disableScissor();
        if (!SCISSOR_STACK.isEmpty()) {
            int[] parent = SCISSOR_STACK.peek();
            g.enableScissor(parent[0], parent[1], parent[2], parent[3]);
        }
    }

    public static void clearScissorStack() {
        SCISSOR_STACK.clear();
    }
}
