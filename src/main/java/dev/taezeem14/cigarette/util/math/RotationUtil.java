package dev.taezeem14.cigarette.util.math;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public final class RotationUtil {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private RotationUtil() {
    }

    public static float wrapAngleTo180(float angle) {
        angle = angle % 360.0F;
        if (angle >= 180.0F) {
            angle -= 360.0F;
        }
        if (angle < -180.0F) {
            angle += 360.0F;
        }
        return angle;
    }

    public static double wrapAngleTo180(double angle) {
        angle = angle % 360.0;
        if (angle >= 180.0) {
            angle -= 360.0;
        }
        if (angle < -180.0) {
            angle += 360.0;
        }
        return angle;
    }

    public static float[] getRotationsToVec(Vec3 target) {
        if (mc.thePlayer == null || target == null) return new float[]{0.0f, 0.0f};

        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0f);
        double diffX = target.xCoord - eyes.xCoord;
        double diffY = target.yCoord - eyes.yCoord;
        double diffZ = target.zCoord - eyes.zCoord;
        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, diffXZ));

        return new float[]{wrapAngleTo180(yaw), wrapAngleTo180(pitch)};
    }

    public static float[] getRotationsToEntity(Entity entity) {
        if (entity == null || mc.thePlayer == null) return new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch};
        Vec3 bestPoint = getClosestPointOnAABB(entity.getEntityBoundingBox(), 0.15, 0.85);
        return getRotationsToVec(bestPoint);
    }

    public static Vec3 getClosestPointOnAABB(AxisAlignedBB aabb, double minPercent, double maxPercent) {
        if (aabb == null || mc.thePlayer == null) return new Vec3(0, 0, 0);

        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0f);
        double width = aabb.maxX - aabb.minX;
        double height = aabb.maxY - aabb.minY;
        double length = aabb.maxZ - aabb.minZ;

        double clampedMinX = aabb.minX + width * minPercent;
        double clampedMaxX = aabb.minX + width * maxPercent;

        double clampedMinY = aabb.minY + height * minPercent;
        double clampedMaxY = aabb.minY + height * maxPercent;

        double clampedMinZ = aabb.minZ + length * minPercent;
        double clampedMaxZ = aabb.minZ + length * maxPercent;

        double targetX = Math.max(clampedMinX, Math.min(clampedMaxX, eyes.xCoord));
        double targetY = Math.max(clampedMinY, Math.min(clampedMaxY, eyes.yCoord));
        double targetZ = Math.max(clampedMinZ, Math.min(clampedMaxZ, eyes.zCoord));

        return new Vec3(targetX, targetY, targetZ);
    }

    public static float smoothSigmoid(float current, float target, float speed) {
        float diff = wrapAngleTo180(target - current);
        if (Math.abs(diff) < 0.001F) return target;

        // Sigmoid mapping: smooth acceleration & deceleration
        double factor = 2.0 / (1.0 + Math.exp(-speed * 0.25)) - 1.0;
        float step = (float) (diff * factor);
        return current + step;
    }

    public static float applySensitivityGCD(float currentAngle, float targetAngle, float sensitivity) {
        float delta = wrapAngleTo180(targetAngle - currentAngle);
        float f = sensitivity * 0.6F + 0.2F;
        float gcd = f * f * f * 8.0F * 0.15F;
        if (gcd <= 0.0F) return targetAngle;

        float fixedDelta = Math.round(delta / gcd) * gcd;
        return currentAngle + fixedDelta;
    }

    public static float[] applySensitivityGCD(float[] currentRotations, float[] targetRotations, float sensitivity) {
        float yaw = applySensitivityGCD(currentRotations[0], targetRotations[0], sensitivity);
        float pitch = applySensitivityGCD(currentRotations[1], targetRotations[1], sensitivity);
        pitch = MathHelper.clamp_float(pitch, -90.0F, 90.0F);
        return new float[]{yaw, pitch};
    }

    public static float getAngleDifference(float a, float b) {
        return Math.abs(wrapAngleTo180(a - b));
    }

    public static float getRotationDifference(float[] current, float[] target) {
        float yawDiff = getAngleDifference(current[0], target[0]);
        float pitchDiff = Math.abs(current[1] - target[1]);
        return (float) Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
    }

    public static float getFovToEntity(Entity entity) {
        if (mc.thePlayer == null || entity == null) return 360.0F;
        float[] rotations = getRotationsToEntity(entity);
        return getAngleDifference(mc.thePlayer.rotationYaw, rotations[0]);
    }

    public static boolean isEntityInFov(Entity entity, float fov) {
        return getFovToEntity(entity) <= fov;
    }
}
