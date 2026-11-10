package dev.taezeem14.cigarette.util.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.culling.ClippingHelperImpl;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public final class FrustumUtil {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final FloatBuffer MODELVIEW = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer PROJECTION = BufferUtils.createFloatBuffer(16);
    private static final IntBuffer VIEWPORT = BufferUtils.createIntBuffer(16);
    private static final FloatBuffer COORDS = BufferUtils.createFloatBuffer(3);

    private static Frustum frustum;
    private static boolean frustumUpdated = false;

    private FrustumUtil() {
    }

    /**
     * Captures the active 3D modelview, projection, and viewport matrices.
     * Must be invoked during 3D world rendering ({@code Render3DEvent}).
     */
    public static void updateFrustum() {
        if (mc.getRenderViewEntity() == null || mc.getRenderManager() == null) {
            return;
        }

        if (frustum == null) {
            frustum = new Frustum();
        }

        ClippingHelperImpl.getInstance();
        frustum.setPosition(
                mc.getRenderManager().viewerPosX,
                mc.getRenderManager().viewerPosY,
                mc.getRenderManager().viewerPosZ
        );

        MODELVIEW.clear();
        PROJECTION.clear();
        VIEWPORT.clear();

        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MODELVIEW);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, PROJECTION);
        GL11.glGetInteger(GL11.GL_VIEWPORT, VIEWPORT);

        frustumUpdated = true;
    }

    public static boolean isInFrustum(AxisAlignedBB bb) {
        if (frustum == null || bb == null) {
            return true;
        }
        return frustum.isBoundingBoxInFrustum(bb);
    }

    public static boolean isInFrustum(Entity entity) {
        if (entity == null) {
            return false;
        }
        return isInFrustum(entity.getEntityBoundingBox());
    }

    public static Vector3f project(double x, double y, double z) {
        if (!frustumUpdated) {
            return null;
        }

        MODELVIEW.rewind();
        PROJECTION.rewind();
        VIEWPORT.rewind();
        COORDS.clear();

        boolean result = GLU.gluProject((float) x, (float) y, (float) z, MODELVIEW, PROJECTION, VIEWPORT, COORDS);
        if (result) {
            float winX = COORDS.get(0);
            float winY = mc.displayHeight - COORDS.get(1);
            float winZ = COORDS.get(2);
            return new Vector3f(winX, winY, winZ);
        }
        return null;
    }

    public static Vector4f getEntityPositionsOn2D(Entity entity, float partialTicks) {
        if (!frustumUpdated || entity == null || mc.getRenderManager() == null) {
            return null;
        }

        AxisAlignedBB entityBB = entity.getEntityBoundingBox();
        if (entityBB != null && !isInFrustum(entityBB.expand(0.3, 0.3, 0.3))) {
            return null;
        }

        double renderPosX = mc.getRenderManager().viewerPosX;
        double renderPosY = mc.getRenderManager().viewerPosY;
        double renderPosZ = mc.getRenderManager().viewerPosZ;

        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - renderPosX;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - renderPosY;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - renderPosZ;

        double width = entity.width / 2.0D;
        double height = entity.height;

        AxisAlignedBB bb = new AxisAlignedBB(
                x - width, y, z - width,
                x + width, y + height, z + width
        );

        double[][] vectors = new double[][]{
                {bb.minX, bb.minY, bb.minZ},
                {bb.minX, bb.maxY, bb.minZ},
                {bb.maxX, bb.minY, bb.minZ},
                {bb.maxX, bb.maxY, bb.minZ},
                {bb.minX, bb.minY, bb.maxZ},
                {bb.minX, bb.maxY, bb.maxZ},
                {bb.maxX, bb.minY, bb.maxZ},
                {bb.maxX, bb.maxY, bb.maxZ}
        };

        ScaledResolution sr = new ScaledResolution(mc);
        int scaleFactor = Math.max(1, sr.getScaleFactor());

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        boolean onScreen = false;

        for (double[] vector : vectors) {
            Vector3f projection = project(vector[0], vector[1], vector[2]);
            if (projection != null && projection.z >= 0.0f && projection.z < 1.0f) {
                float screenX = projection.x / scaleFactor;
                float screenY = projection.y / scaleFactor;

                minX = Math.min(minX, screenX);
                minY = Math.min(minY, screenY);
                maxX = Math.max(maxX, screenX);
                maxY = Math.max(maxY, screenY);
                onScreen = true;
            }
        }

        if (!onScreen) {
            return null;
        }

        return new Vector4f(minX, minY, maxX, maxY);
    }
}
