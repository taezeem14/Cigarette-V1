package dev.taezeem14.cigarette.util.player;

import com.google.common.base.Predicates;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import java.util.List;

public final class RaycastUtil {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private RaycastUtil() {
    }

    public static Entity raycastEntity(double reach, float yaw, float pitch, double expand) {
        if (mc.thePlayer == null || mc.theWorld == null) return null;

        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0F);
        Vec3 look = getVectorForRotation(pitch, yaw);
        Vec3 reachVec = eyes.addVector(look.xCoord * reach, look.yCoord * reach, look.zCoord * reach);

        Entity pointedEntity = null;
        List<Entity> entities = mc.theWorld.getEntitiesInAABBexcluding(
                mc.thePlayer,
                mc.thePlayer.getEntityBoundingBox().addCoord(look.xCoord * reach, look.yCoord * reach, look.zCoord * reach).expand(1.0D, 1.0D, 1.0D),
                Predicates.and(EntitySelectors.NOT_SPECTATING, Entity::canBeCollidedWith)
        );

        double minDistance = reach;

        for (Entity entity : entities) {
            float collisionBorderSize = entity.getCollisionBorderSize() + (float) expand;
            AxisAlignedBB aabb = entity.getEntityBoundingBox().expand(collisionBorderSize, collisionBorderSize, collisionBorderSize);
            MovingObjectPosition intercept = aabb.calculateIntercept(eyes, reachVec);

            if (aabb.isVecInside(eyes)) {
                if (minDistance >= 0.0D) {
                    pointedEntity = entity;
                    minDistance = 0.0D;
                }
            } else if (intercept != null) {
                double distanceTo = eyes.distanceTo(intercept.hitVec);
                if (distanceTo < minDistance || minDistance == 0.0D) {
                    pointedEntity = entity;
                    minDistance = distanceTo;
                }
            }
        }

        return pointedEntity;
    }

    public static Entity raycastEntity(double reach, double expand) {
        if (mc.thePlayer == null) return null;
        return raycastEntity(reach, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, expand);
    }

    public static MovingObjectPosition raycast(double reach, float yaw, float pitch) {
        if (mc.thePlayer == null || mc.theWorld == null) return null;

        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0F);
        Vec3 look = getVectorForRotation(pitch, yaw);
        Vec3 reachVec = eyes.addVector(look.xCoord * reach, look.yCoord * reach, look.zCoord * reach);

        return mc.theWorld.rayTraceBlocks(eyes, reachVec, false, false, true);
    }

    public static Vec3 getVectorForRotation(float pitch, float yaw) {
        float f = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
        float f1 = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
        float f2 = -MathHelper.cos(-pitch * 0.017453292F);
        float f3 = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3(f1 * f2, f3, f * f2);
    }
}
