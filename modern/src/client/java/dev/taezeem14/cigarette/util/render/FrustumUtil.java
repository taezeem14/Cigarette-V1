package dev.taezeem14.cigarette.util.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public final class FrustumUtil {

    private static final Minecraft mc = Minecraft.getInstance();
    private static final Matrix4f LAST_MVP = new Matrix4f();
    private static Vec3 lastCamPos = Vec3.ZERO;
    private static boolean hasCamera = false;

    private FrustumUtil() {
    }

    public static void updateCamera(CameraRenderState camState) {
        if (camState == null || !camState.initialized
                || camState.projectionMatrix == null || camState.viewRotationMatrix == null || camState.pos == null) {
            return;
        }
        LAST_MVP.set(camState.projectionMatrix).mul(camState.viewRotationMatrix);
        lastCamPos = camState.pos;
        hasCamera = true;
    }

    public static float[] projectToScreen(double worldX, double worldY, double worldZ, int screenW, int screenH) {
        if (!hasCamera || screenW <= 0 || screenH <= 0) {
            return null;
        }
        float rx = (float) (worldX - lastCamPos.x);
        float ry = (float) (worldY - lastCamPos.y);
        float rz = (float) (worldZ - lastCamPos.z);

        Vector4f clip = new Vector4f(rx, ry, rz, 1.0F).mul(LAST_MVP);
        if (clip.w <= 0.0001F) {
            return null;
        }
        float ndcX = clip.x / clip.w;
        float ndcY = clip.y / clip.w;
        float ndcZ = clip.z / clip.w;
        if (ndcZ < -1.0F || ndcZ > 1.0F) {
            return null;
        }
        float sx = (ndcX * 0.5F + 0.5F) * screenW;
        float sy = (0.5F - ndcY * 0.5F) * screenH;
        return new float[]{sx, sy};
    }

    public static float[] getEntityBounds2D(Entity entity, float partialTicks, int screenW, int screenH) {
        if (!hasCamera || entity == null || mc.player == null) {
            return null;
        }
        double ix = entity.xo + (entity.getX() - entity.xo) * partialTicks;
        double iy = entity.yo + (entity.getY() - entity.yo) * partialTicks;
        double iz = entity.zo + (entity.getZ() - entity.zo) * partialTicks;

        AABB box = entity.getBoundingBox().move(ix - entity.getX(), iy - entity.getY(), iz - entity.getZ());
        double[][] corners = {
                {box.minX, box.minY, box.minZ},
                {box.minX, box.minY, box.maxZ},
                {box.minX, box.maxY, box.minZ},
                {box.minX, box.maxY, box.maxZ},
                {box.maxX, box.minY, box.minZ},
                {box.maxX, box.minY, box.maxZ},
                {box.maxX, box.maxY, box.minZ},
                {box.maxX, box.maxY, box.maxZ}
        };

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        boolean anyVisible = false;

        for (double[] c : corners) {
            float[] projected = projectToScreen(c[0], c[1], c[2], screenW, screenH);
            if (projected != null) {
                anyVisible = true;
                minX = Math.min(minX, projected[0]);
                minY = Math.min(minY, projected[1]);
                maxX = Math.max(maxX, projected[0]);
                maxY = Math.max(maxY, projected[1]);
            }
        }

        if (!anyVisible || maxX < 0.0F || minX > screenW || maxY < 0.0F || minY > screenH) {
            return null;
        }
        return new float[]{minX, minY, maxX, maxY};
    }
}
