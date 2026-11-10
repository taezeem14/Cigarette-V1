package dev.taezeem14.cigarette.util.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GLAllocation;
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
    private static final Frustum frustum = new Frustum();

    private static final FloatBuffer MODELVIEW = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer PROJECTION = BufferUtils.createFloatBuffer(16);
    private static final IntBuffer VIEWPORT = BufferUtils.createIntBuffer(16);
    private static final FloatBuffer COORDS = BufferUtils.createFloatBuffer(3);

    private FrustumUtil() {
    }

    public static void updateFrustum() {
        if (mc.getRenderViewEntity() == null) return;

        double x = mc.getRenderViewEntity().posX;
        double y = mc.getRenderViewEntity().posY;
        double z = mc.getRenderViewEntity().posZ;

        frustum.setPosition(x, y, z);

        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MODELVIEW);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, PROJECTION);
        GL11.glGetInteger(GL11.GL_VIEWPORT, VIEWPORT);
    }

    public static boolean isInFrustum(AxisAlignedBB bb) {
        return frustum.isBoundingBoxInFrustum(bb);
    }

    public static boolean isInFrustum(Entity entity) {
        return frustum.isBoundingBoxInFrustum(entity.getEntityBoundingBox());
    }

    public static Vector3f project(double x, double y, double z) {
        COORDS.clear();
        boolean result = GLU.gluProject((float) x, (float) y, (float) z, MODELVIEW, PROJECTION, VIEWPORT, COORDS);
        if (result) {
            float winX = COORDS.get(0);
            float winY = DisplayHeight() - COORDS.get(1);
            float winZ = COORDS.get(2);
            return new Vector3f(winX, winY, winZ);
        }
        return null;
    }

    private static float DisplayHeight() {
        return mc.displayHeight;
    }

    public static Vector4f getEntityPositionsOn2D(Entity entity, float partialTicks) {
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
        int scaleFactor = sr.getScaleFactor();

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
