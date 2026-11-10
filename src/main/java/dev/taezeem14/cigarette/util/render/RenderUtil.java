package dev.taezeem14.cigarette.util.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.util.ArrayDeque;
import java.util.Deque;

public final class RenderUtil {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Deque<int[]> SCISSOR_STACK = new ArrayDeque<>();
    private static final Deque<Integer> STATE_STACK = new ArrayDeque<>();

    private static final int FLAG_DEPTH = 1;
    private static final int FLAG_BLEND = 2;
    private static final int FLAG_TEXTURE = 4;
    private static final int FLAG_ALPHA = 8;

    private RenderUtil() {
    }

    public static void enableGL2D() {
        int flags = 0;
        if (GL11.glIsEnabled(GL11.GL_DEPTH_TEST)) flags |= FLAG_DEPTH;
        if (GL11.glIsEnabled(GL11.GL_BLEND)) flags |= FLAG_BLEND;
        if (GL11.glIsEnabled(GL11.GL_TEXTURE_2D)) flags |= FLAG_TEXTURE;
        if (GL11.glIsEnabled(GL11.GL_ALPHA_TEST)) flags |= FLAG_ALPHA;
        STATE_STACK.push(flags);

        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.disableTexture2D();
        GlStateManager.disableAlpha();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
    }

    public static void disableGL2D() {
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_DONT_CARE);

        int flags = STATE_STACK.isEmpty() ? (FLAG_TEXTURE | FLAG_DEPTH | FLAG_ALPHA) : STATE_STACK.pop();
        if ((flags & FLAG_TEXTURE) != 0) {
            GlStateManager.enableTexture2D();
        } else {
            GlStateManager.disableTexture2D();
        }
        if ((flags & FLAG_BLEND) != 0) {
            GlStateManager.enableBlend();
        } else {
            GlStateManager.disableBlend();
        }
        if ((flags & FLAG_ALPHA) != 0) {
            GlStateManager.enableAlpha();
        } else {
            GlStateManager.disableAlpha();
        }
        if ((flags & FLAG_DEPTH) != 0) {
            GlStateManager.enableDepth();
        } else {
            GlStateManager.disableDepth();
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void enableGL3D() {
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.disableTexture2D();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GlStateManager.disableDepth();
        GlStateManager.disableLighting();
        GlStateManager.disableAlpha();
        GlStateManager.depthMask(false);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
    }

    public static void disableGL3D() {
        GlStateManager.depthMask(true);
        GlStateManager.enableAlpha();
        GlStateManager.enableDepth();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_DONT_CARE);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    public static void color(int color) {
        float alpha = (color >> 24 & 0xFF) / 255.0F;
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        GlStateManager.color(red, green, blue, alpha);
    }

    public static void drawRect(float x, float y, float width, float height, int color) {
        if (width <= 0.0f || height <= 0.0f) return;
        enableGL2D();
        color(color);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x, y + height);
        GL11.glVertex2f(x + width, y + height);
        GL11.glVertex2f(x + width, y);
        GL11.glVertex2f(x, y);
        GL11.glEnd();
        disableGL2D();
    }

    public static void drawBorderedRect(float x, float y, float width, float height, float lineWidth, int innerColor, int borderColor) {
        drawRect(x, y, width, height, innerColor);
        drawOutline(x, y, width, height, lineWidth, borderColor);
    }

    public static void drawOutline(float x, float y, float width, float height, float lineWidth, int color) {
        if (width <= 0.0f || height <= 0.0f) return;
        enableGL2D();
        color(color);
        GL11.glLineWidth(lineWidth);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + width, y);
        GL11.glVertex2f(x + width, y + height);
        GL11.glVertex2f(x, y + height);
        GL11.glEnd();
        GL11.glLineWidth(1.0F);
        disableGL2D();
    }

    public static void drawRoundedRect(float x, float y, float width, float height, float radius, int color) {
        if (width <= 0.0f || height <= 0.0f) return;
        radius = Math.max(0.0f, Math.min(radius, Math.min(width, height) / 2.0f));
        if (radius <= 0.05f) {
            drawRect(x, y, width, height, color);
            return;
        }

        float x1 = x;
        float y1 = y;
        float x2 = x + width;
        float y2 = y + height;

        enableGL2D();
        color(color);

        GL11.glBegin(GL11.GL_POLYGON);

        for (int i = 0; i <= 90; i += 3) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(x1 + radius - Math.sin(rad) * radius, y1 + radius - Math.cos(rad) * radius);
        }
        for (int i = 90; i <= 180; i += 3) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(x1 + radius - Math.sin(rad) * radius, y2 - radius - Math.cos(rad) * radius);
        }
        for (int i = 180; i <= 270; i += 3) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(x2 - radius - Math.sin(rad) * radius, y2 - radius - Math.cos(rad) * radius);
        }
        for (int i = 270; i <= 360; i += 3) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(x2 - radius - Math.sin(rad) * radius, y1 + radius - Math.cos(rad) * radius);
        }

        GL11.glEnd();
        disableGL2D();
    }

    public static void drawRoundedRectTop(float x, float y, float width, float height, float radius, int color) {
        if (width <= 0.0f || height <= 0.0f) return;
        radius = Math.max(0.0f, Math.min(radius, Math.min(width / 2.0f, height)));
        if (radius <= 0.05f) {
            drawRect(x, y, width, height, color);
            return;
        }

        float x1 = x;
        float y1 = y;
        float x2 = x + width;
        float y2 = y + height;

        enableGL2D();
        color(color);

        GL11.glBegin(GL11.GL_POLYGON);
        for (int i = 0; i <= 90; i += 3) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(x1 + radius - Math.sin(rad) * radius, y1 + radius - Math.cos(rad) * radius);
        }
        GL11.glVertex2d(x1, y2);
        GL11.glVertex2d(x2, y2);
        for (int i = 270; i <= 360; i += 3) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(x2 - radius - Math.sin(rad) * radius, y1 + radius - Math.cos(rad) * radius);
        }
        GL11.glEnd();
        disableGL2D();
    }

    public static void drawRoundedRectBottom(float x, float y, float width, float height, float radius, int color) {
        if (width <= 0.0f || height <= 0.0f) return;
        radius = Math.max(0.0f, Math.min(radius, Math.min(width / 2.0f, height)));
        if (radius <= 0.05f) {
            drawRect(x, y, width, height, color);
            return;
        }

        float x1 = x;
        float y1 = y;
        float x2 = x + width;
        float y2 = y + height;

        enableGL2D();
        color(color);

        GL11.glBegin(GL11.GL_POLYGON);
        GL11.glVertex2d(x1, y1);
        for (int i = 90; i <= 180; i += 3) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(x1 + radius - Math.sin(rad) * radius, y2 - radius - Math.cos(rad) * radius);
        }
        for (int i = 180; i <= 270; i += 3) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(x2 - radius - Math.sin(rad) * radius, y2 - radius - Math.cos(rad) * radius);
        }
        GL11.glVertex2d(x2, y1);
        GL11.glEnd();
        disableGL2D();
    }

    public static void drawRoundedOutline(float x, float y, float width, float height, float radius, float lineWidth, int color) {
        if (width <= 0.0f || height <= 0.0f) return;
        radius = Math.max(0.0f, Math.min(radius, Math.min(width, height) / 2.0f));
        if (radius <= 0.05f) {
            drawOutline(x, y, width, height, lineWidth, color);
            return;
        }

        float x1 = x;
        float y1 = y;
        float x2 = x + width;
        float y2 = y + height;

        enableGL2D();
        color(color);
        GL11.glLineWidth(lineWidth);

        GL11.glBegin(GL11.GL_LINE_LOOP);

        for (int i = 0; i <= 90; i += 3) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(x1 + radius - Math.sin(rad) * radius, y1 + radius - Math.cos(rad) * radius);
        }
        for (int i = 90; i <= 180; i += 3) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(x1 + radius - Math.sin(rad) * radius, y2 - radius - Math.cos(rad) * radius);
        }
        for (int i = 180; i <= 270; i += 3) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(x2 - radius - Math.sin(rad) * radius, y2 - radius - Math.cos(rad) * radius);
        }
        for (int i = 270; i <= 360; i += 3) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(x2 - radius - Math.sin(rad) * radius, y1 + radius - Math.cos(rad) * radius);
        }

        GL11.glEnd();
        GL11.glLineWidth(1.0F);
        disableGL2D();
    }

    public static void drawCircle(float cx, float cy, float radius, int color) {
        if (radius <= 0.0f) return;
        enableGL2D();
        color(color);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(cx, cy);
        for (int i = 0; i <= 360; i += 6) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(cx + Math.sin(rad) * radius, cy + Math.cos(rad) * radius);
        }
        GL11.glEnd();
        disableGL2D();
    }

    public static void drawCircleOutline(float cx, float cy, float radius, float lineWidth, int color) {
        if (radius <= 0.0f) return;
        enableGL2D();
        color(color);
        GL11.glLineWidth(lineWidth);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i < 360; i += 6) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(cx + Math.sin(rad) * radius, cy + Math.cos(rad) * radius);
        }
        GL11.glEnd();
        GL11.glLineWidth(1.0F);
        disableGL2D();
    }

    public static void drawGradientRect(float x, float y, float width, float height, int startColor, int endColor, boolean horizontal) {
        if (width <= 0.0f || height <= 0.0f) return;
        enableGL2D();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GL11.glBegin(GL11.GL_QUADS);

        if (horizontal) {
            color(startColor);
            GL11.glVertex2f(x, y);
            GL11.glVertex2f(x, y + height);
            color(endColor);
            GL11.glVertex2f(x + width, y + height);
            GL11.glVertex2f(x + width, y);
        } else {
            color(startColor);
            GL11.glVertex2f(x, y);
            color(endColor);
            GL11.glVertex2f(x, y + height);
            GL11.glVertex2f(x + width, y + height);
            color(startColor);
            GL11.glVertex2f(x + width, y);
        }

        GL11.glEnd();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        disableGL2D();
    }

    public static void drawDropShadow(float x, float y, float width, float height, float radius, int shadowColor) {
        if (width <= 0.0f || height <= 0.0f) return;
        int shadowAlpha = ColorUtil.getAlpha(shadowColor);
        if (shadowAlpha <= 0) return;

        int steps = Math.max(1, Math.min(8, (int) Math.ceil(radius)));
        for (int i = 0; i < steps; i++) {
            float spread = (float) i + 0.5f;
            float alphaFactor = 1.0f - (float) i / (float) steps;
            int alpha = (int) (shadowAlpha * (alphaFactor * alphaFactor * 0.30f));
            if (alpha <= 0) continue;
            int col = ColorUtil.modifyAlpha(shadowColor, alpha);
            drawRoundedOutline(x - spread, y - spread, width + spread * 2.0f, height + spread * 2.0f, radius + spread, 1.2f, col);
        }
    }

    public static void pushScissor(float x, float y, float width, float height, float scaleMultiplier) {
        ScaledResolution sr = new ScaledResolution(mc);
        double totalScale = sr.getScaleFactor() * (double) Math.max(0.01f, scaleMultiplier);

        int left = (int) Math.floor(x * totalScale);
        int right = (int) Math.ceil((x + Math.max(0.0f, width)) * totalScale);
        int topFromTop = (int) Math.floor(y * totalScale);
        int bottomFromTop = (int) Math.ceil((y + Math.max(0.0f, height)) * totalScale);

        int bottom = mc.displayHeight - bottomFromTop;
        int top = mc.displayHeight - topFromTop;

        left = Math.max(0, Math.min(mc.displayWidth, left));
        right = Math.max(left, Math.min(mc.displayWidth, right));
        bottom = Math.max(0, Math.min(mc.displayHeight, bottom));
        top = Math.max(bottom, Math.min(mc.displayHeight, top));

        int[] parent = SCISSOR_STACK.peek();
        if (parent != null) {
            left = Math.max(left, parent[0]);
            bottom = Math.max(bottom, parent[1]);
            right = Math.max(left, Math.min(right, parent[2]));
            top = Math.max(bottom, Math.min(top, parent[3]));
        }

        int[] box = new int[]{left, bottom, right, top};
        SCISSOR_STACK.push(box);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(left, bottom, Math.max(0, right - left), Math.max(0, top - bottom));
    }

    public static void pushScissor(float x, float y, float width, float height) {
        pushScissor(x, y, width, height, 1.0f);
    }

    public static void popScissor() {
        if (!SCISSOR_STACK.isEmpty()) {
            SCISSOR_STACK.pop();
        }

        if (SCISSOR_STACK.isEmpty()) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        } else {
            int[] parent = SCISSOR_STACK.peek();
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(parent[0], parent[1], Math.max(0, parent[2] - parent[0]), Math.max(0, parent[3] - parent[1]));
        }
    }

    public static void clearScissorStack() {
        SCISSOR_STACK.clear();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    public static void startScissor(float x, float y, float width, float height) {
        pushScissor(x, y, width, height);
    }

    public static void stopScissor() {
        popScissor();
    }

    public static void draw3DBox(AxisAlignedBB bb, int color, boolean fill, boolean outline, float lineWidth) {
        RenderManager rm = mc.getRenderManager();
        double renderX = rm.viewerPosX;
        double renderY = rm.viewerPosY;
        double renderZ = rm.viewerPosZ;

        AxisAlignedBB box = new AxisAlignedBB(
                bb.minX - renderX, bb.minY - renderY, bb.minZ - renderZ,
                bb.maxX - renderX, bb.maxY - renderY, bb.maxZ - renderZ
        );

        enableGL3D();

        if (fill) {
            color(ColorUtil.modifyAlpha(color, Math.min(60, ColorUtil.getAlpha(color))));
            drawSolidBox(box);
        }

        if (outline) {
            color(ColorUtil.modifyAlpha(color, 255));
            GL11.glLineWidth(lineWidth);
            drawOutlinedBox(box);
            GL11.glLineWidth(1.0F);
        }

        disableGL3D();
    }

    public static void drawSolidBox(AxisAlignedBB bb) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();

        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldRenderer.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        worldRenderer.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        worldRenderer.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        worldRenderer.pos(bb.minX, bb.minY, bb.maxZ).endVertex();

        worldRenderer.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        worldRenderer.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        worldRenderer.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        worldRenderer.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();

        worldRenderer.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        worldRenderer.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        worldRenderer.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        worldRenderer.pos(bb.maxX, bb.minY, bb.minZ).endVertex();

        worldRenderer.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        worldRenderer.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        worldRenderer.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        worldRenderer.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();

        worldRenderer.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        worldRenderer.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        worldRenderer.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        worldRenderer.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();

        worldRenderer.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        worldRenderer.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        worldRenderer.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        worldRenderer.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        tessellator.draw();
    }

    public static void drawOutlinedBox(AxisAlignedBB bb) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();

        worldRenderer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        worldRenderer.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        worldRenderer.pos(bb.maxX, bb.minY, bb.minZ).endVertex();

        worldRenderer.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        worldRenderer.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();

        worldRenderer.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        worldRenderer.pos(bb.minX, bb.minY, bb.maxZ).endVertex();

        worldRenderer.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        worldRenderer.pos(bb.minX, bb.minY, bb.minZ).endVertex();

        worldRenderer.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        worldRenderer.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();

        worldRenderer.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        worldRenderer.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();

        worldRenderer.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        worldRenderer.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();

        worldRenderer.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        worldRenderer.pos(bb.minX, bb.maxY, bb.minZ).endVertex();

        worldRenderer.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        worldRenderer.pos(bb.minX, bb.maxY, bb.minZ).endVertex();

        worldRenderer.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        worldRenderer.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();

        worldRenderer.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        worldRenderer.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();

        worldRenderer.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        worldRenderer.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        tessellator.draw();
    }

    public static void draw3DLine(Vec3 start, Vec3 end, int color, float lineWidth) {
        RenderManager rm = mc.getRenderManager();
        double renderX = rm.viewerPosX;
        double renderY = rm.viewerPosY;
        double renderZ = rm.viewerPosZ;

        enableGL3D();
        color(color);
        GL11.glLineWidth(lineWidth);

        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(start.xCoord - renderX, start.yCoord - renderY, start.zCoord - renderZ);
        GL11.glVertex3d(end.xCoord - renderX, end.yCoord - renderY, end.zCoord - renderZ);
        GL11.glEnd();

        GL11.glLineWidth(1.0F);
        disableGL3D();
    }

    public static void drawFaIcon(String iconName, float x, float y, float size, int color) {
        if (iconName == null) return;
        String key = iconName.toLowerCase(java.util.Locale.ROOT);
        float s = size / 12.0f;
        enableGL2D();
        color(color);
        GL11.glLineWidth(1.35F);

        if ("combat".equals(key)) {
            drawGlCircleOutline(x + 6.0f * s, y + 6.0f * s, 4.0f * s);
            drawGlLine(x + 6.0f * s, y + 0.8f * s, x + 6.0f * s, y + 3.2f * s);
            drawGlLine(x + 6.0f * s, y + 8.8f * s, x + 6.0f * s, y + 11.2f * s);
            drawGlLine(x + 0.8f * s, y + 6.0f * s, x + 3.2f * s, y + 6.0f * s);
            drawGlLine(x + 8.8f * s, y + 6.0f * s, x + 11.2f * s, y + 6.0f * s);
        } else if ("blatant".equals(key)) {
            GL11.glBegin(GL11.GL_LINE_STRIP);
            GL11.glVertex2f(x + 7.2f * s, y + 1.0f * s);
            GL11.glVertex2f(x + 3.0f * s, y + 6.5f * s);
            GL11.glVertex2f(x + 6.2f * s, y + 6.5f * s);
            GL11.glVertex2f(x + 4.8f * s, y + 11.0f * s);
            GL11.glVertex2f(x + 9.2f * s, y + 5.2f * s);
            GL11.glVertex2f(x + 6.0f * s, y + 5.2f * s);
            GL11.glVertex2f(x + 7.2f * s, y + 1.0f * s);
            GL11.glEnd();
        } else if ("render".equals(key)) {
            GL11.glBegin(GL11.GL_LINE_STRIP);
            GL11.glVertex2f(x + 1.0f * s, y + 6.0f * s);
            GL11.glVertex2f(x + 3.8f * s, y + 3.2f * s);
            GL11.glVertex2f(x + 8.2f * s, y + 3.2f * s);
            GL11.glVertex2f(x + 11.0f * s, y + 6.0f * s);
            GL11.glVertex2f(x + 8.2f * s, y + 8.8f * s);
            GL11.glVertex2f(x + 3.8f * s, y + 8.8f * s);
            GL11.glVertex2f(x + 1.0f * s, y + 6.0f * s);
            GL11.glEnd();
            drawGlCircleOutline(x + 6.0f * s, y + 6.0f * s, 1.6f * s);
        } else if ("utility".equals(key) || "world".equals(key)) {
            drawGlCircleOutline(x + 8.3f * s, y + 3.7f * s, 2.3f * s);
            GL11.glLineWidth(1.9F);
            drawGlLine(x + 6.8f * s, y + 5.2f * s, x + 2.2f * s, y + 9.8f * s);
        } else if ("client".equals(key)) {
            GL11.glBegin(GL11.GL_LINE_LOOP);
            GL11.glVertex2f(x + 6.0f * s, y + 1.2f * s);
            GL11.glVertex2f(x + 10.2f * s, y + 3.0f * s);
            GL11.glVertex2f(x + 9.8f * s, y + 7.0f * s);
            GL11.glVertex2f(x + 6.0f * s, y + 10.8f * s);
            GL11.glVertex2f(x + 2.2f * s, y + 7.0f * s);
            GL11.glVertex2f(x + 1.8f * s, y + 3.0f * s);
            GL11.glEnd();
        } else if ("search".equals(key)) {
            drawGlCircleOutline(x + 4.8f * s, y + 4.8f * s, 3.2f * s);
            drawGlLine(x + 7.2f * s, y + 7.2f * s, x + 10.6f * s, y + 10.6f * s);
        } else if ("profiles".equals(key)) {
            GL11.glBegin(GL11.GL_LINE_LOOP);
            GL11.glVertex2f(x + 6.0f * s, y + 1.5f * s);
            GL11.glVertex2f(x + 10.5f * s, y + 3.8f * s);
            GL11.glVertex2f(x + 6.0f * s, y + 6.1f * s);
            GL11.glVertex2f(x + 1.5f * s, y + 3.8f * s);
            GL11.glEnd();
            GL11.glBegin(GL11.GL_LINE_STRIP);
            GL11.glVertex2f(x + 1.5f * s, y + 7.0f * s);
            GL11.glVertex2f(x + 6.0f * s, y + 9.3f * s);
            GL11.glVertex2f(x + 10.5f * s, y + 7.0f * s);
            GL11.glEnd();
        } else if ("hud".equals(key)) {
            drawGlLine(x + 1.5f * s, y + 3.0f * s, x + 10.5f * s, y + 3.0f * s);
            drawGlLine(x + 1.5f * s, y + 6.0f * s, x + 10.5f * s, y + 6.0f * s);
            drawGlLine(x + 1.5f * s, y + 9.0f * s, x + 10.5f * s, y + 9.0f * s);
            drawGlCircleOutline(x + 4.2f * s, y + 3.0f * s, 1.3f * s);
            drawGlCircleOutline(x + 7.8f * s, y + 6.0f * s, 1.3f * s);
            drawGlCircleOutline(x + 5.0f * s, y + 9.0f * s, 1.3f * s);
        }

        GL11.glLineWidth(1.0F);
        disableGL2D();
    }

    private static void drawGlLine(float x1, float y1, float x2, float y2) {
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(x1, y1);
        GL11.glVertex2f(x2, y2);
        GL11.glEnd();
    }

    private static void drawGlCircleOutline(float cx, float cy, float r) {
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i < 16; i++) {
            double a = i * Math.PI * 2.0 / 16.0;
            GL11.glVertex2f(cx + (float) Math.cos(a) * r, cy + (float) Math.sin(a) * r);
        }
        GL11.glEnd();
    }
}
