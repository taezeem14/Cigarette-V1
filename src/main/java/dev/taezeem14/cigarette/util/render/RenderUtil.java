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

import java.awt.Color;

public final class RenderUtil {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private RenderUtil() {
    }

    public static void enableGL2D() {
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_NICEST);
    }

    public static void disableGL2D() {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_DONT_CARE);
        GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_DONT_CARE);
    }

    public static void enableGL3D() {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDepthMask(false);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
    }

    public static void disableGL3D() {
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glPopMatrix();
    }

    public static void color(int color) {
        float alpha = (color >> 24 & 0xFF) / 255.0F;
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        GL11.glColor4f(red, green, blue, alpha);
    }

    public static void drawRect(float x, float y, float width, float height, int color) {
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
        enableGL2D();
        color(color);
        GL11.glLineWidth(lineWidth);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + width, y);
        GL11.glVertex2f(x + width, y + height);
        GL11.glVertex2f(x, y + height);
        GL11.glEnd();
        disableGL2D();
    }

    public static void drawRoundedRect(float x, float y, float width, float height, float radius, int color) {
        float x1 = x;
        float y1 = y;
        float x2 = x + width;
        float y2 = y + height;

        enableGL2D();
        color(color);

        GL11.glBegin(GL11.GL_POLYGON);

        // Top-left corner
        for (int i = 0; i <= 90; i += 3) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(x1 + radius - Math.sin(rad) * radius, y1 + radius - Math.cos(rad) * radius);
        }

        // Bottom-left corner
        for (int i = 90; i <= 180; i += 3) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(x1 + radius - Math.sin(rad) * radius, y2 - radius - Math.cos(rad) * radius);
        }

        // Bottom-right corner
        for (int i = 180; i <= 270; i += 3) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(x2 - radius - Math.sin(rad) * radius, y2 - radius - Math.cos(rad) * radius);
        }

        // Top-right corner
        for (int i = 270; i <= 360; i += 3) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(x2 - radius - Math.sin(rad) * radius, y1 + radius - Math.cos(rad) * radius);
        }

        GL11.glEnd();
        disableGL2D();
    }

    public static void drawRoundedOutline(float x, float y, float width, float height, float radius, float lineWidth, int color) {
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
        disableGL2D();
    }

    public static void drawGradientRect(float x, float y, float width, float height, int startColor, int endColor, boolean horizontal) {
        enableGL2D();
        GL11.glShadeModel(GL11.GL_SMOOTH);
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
        GL11.glShadeModel(GL11.GL_FLAT);
        disableGL2D();
    }

    public static void drawDropShadow(float x, float y, float width, float height, float radius, int shadowColor) {
        int shadowAlpha = ColorUtil.getAlpha(shadowColor);
        int steps = (int) Math.max(1, radius);
        for (int i = 0; i < steps; i++) {
            float spread = (float) i;
            float alphaFactor = (1.0f - (float) i / (float) steps);
            int alpha = (int) (shadowAlpha * (alphaFactor * alphaFactor * 0.35f));
            int col = ColorUtil.modifyAlpha(shadowColor, alpha);
            drawRoundedOutline(x - spread, y - spread, width + spread * 2, height + spread * 2, radius + spread, 1.0f, col);
        }
    }

    public static void startScissor(float x, float y, float width, float height) {
        ScaledResolution sr = new ScaledResolution(mc);
        int scale = sr.getScaleFactor();

        int scissorX = (int) (x * scale);
        int scissorY = (int) ((sr.getScaledHeight() - (y + height)) * scale);
        int scissorWidth = (int) (width * scale);
        int scissorHeight = (int) (height * scale);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(
                Math.max(0, scissorX),
                Math.max(0, scissorY),
                Math.max(0, scissorWidth),
                Math.max(0, scissorHeight)
        );
    }

    public static void stopScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
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

        disableGL3D();
    }
}
