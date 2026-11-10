package dev.taezeem14.cigarette.util.math;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class RotationUtil {

    private static final Minecraft mc = Minecraft.getInstance();

    private RotationUtil() {
    }

    public static float wrapAngleTo180(float angle) {
        return Mth.wrapDegrees(angle);
    }

    public static float[] getRotationsToVec(Vec3 target) {
        if (mc.player == null || target == null) return new float[]{0.0f, 0.0f};

        Vec3 eyes = mc.player.getEyePosition();
        double diffX = target.x - eyes.x;
        double diffY = target.y - eyes.y;
        double diffZ = target.z - eyes.z;
        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, diffXZ));

        return new float[]{wrapAngleTo180(yaw), wrapAngleTo180(pitch)};
    }

    public static float[] getRotationsToEntity(Entity entity) {
        if (mc.player == null) return new float[]{0.0f, 0.0f};
        if (entity == null) return new float[]{mc.player.getYRot(), mc.player.getXRot()};
        Vec3 bestPoint = getClosestPointOnAABB(entity.getBoundingBox(), 0.15, 0.85);
        return getRotationsToVec(bestPoint);
    }

    public static Vec3 getClosestPointOnAABB(AABB aabb, double minPercent, double maxPercent) {
        if (aabb == null || mc.player == null) return Vec3.ZERO;

        Vec3 eyes = mc.player.getEyePosition();
        double width = aabb.maxX - aabb.minX;
        double height = aabb.maxY - aabb.minY;
        double length = aabb.maxZ - aabb.minZ;

        double targetX = Math.max(aabb.minX + width * minPercent, Math.min(aabb.minX + width * maxPercent, eyes.x));
        double targetY = Math.max(aabb.minY + height * minPercent, Math.min(aabb.minY + height * maxPercent, eyes.y));
        double targetZ = Math.max(aabb.minZ + length * minPercent, Math.min(aabb.minZ + length * maxPercent, eyes.z));

        return new Vec3(targetX, targetY, targetZ);
    }

    public static float linearStep(float current, float target, float maxStep) {
        float diff = wrapAngleTo180(target - current);
        if (maxStep <= 0.0F) return current;
        if (Math.abs(diff) <= maxStep) return current + diff;
        return current + Math.copySign(maxStep, diff);
    }

    public static float smoothSigmoid(float current, float target, float speed) {
        return smoothSigmoid(current, target, speed, 0.05F);
    }

    public static float smoothSigmoid(float current, float target, float speed, float deltaSeconds) {
        float diff = wrapAngleTo180(target - current);
        if (Math.abs(diff) < 0.001F) return current + diff;

        float clampedDelta = Math.max(0.0001F, Math.min(0.25F, deltaSeconds));
        double baseFactor = 2.0 / (1.0 + Math.exp(-speed * 0.25)) - 1.0;
        baseFactor = Math.max(0.0, Math.min(0.9999, baseFactor));
        double frameFactor = 1.0 - Math.pow(1.0 - baseFactor, clampedDelta * 20.0);
        return current + (float) (diff * frameFactor);
    }

    public static float getMouseSensitivity() {
        if (mc.options == null) return 0.5F;
        return mc.options.sensitivity().get().floatValue();
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
        pitch = Mth.clamp(pitch, -90.0F, 90.0F);
        return new float[]{yaw, pitch};
    }

    public static float getAngleDifference(float a, float b) {
        return Math.abs(wrapAngleTo180(a - b));
    }

    public static float getFovToEntity(Entity entity) {
        if (mc.player == null || entity == null) return 360.0F;
        float[] rots = getRotationsToEntity(entity);
        return getAngleDifference(mc.player.getYRot(), rots[0]);
    }

    public static boolean isEntityInFov(Entity entity, float fov) {
        return getFovToEntity(entity) <= fov;
    }

    public static double getDistanceToEntityHitbox(Entity entity) {
        if (mc.player == null || entity == null) return Double.MAX_VALUE;
        AABB box = entity.getBoundingBox();
        if (box == null) return mc.player.distanceTo(entity);
        return mc.player.getEyePosition().distanceTo(getClosestPointOnAABB(box, 0.0, 1.0));
    }

    public static boolean isPhysicalKeyDown(KeyMapping mapping) {
        if (mapping == null) return false;
        InputConstants.Key key = KeyMappingHelper.getBoundKeyOf(mapping);
        if (key == null) return mapping.isDown();
        if (key.getType() == InputConstants.Type.MOUSE) {
            if (mc.mouseHandler == null) return false;
            int btn = key.getValue();
            if (btn == InputConstants.MOUSE_BUTTON_LEFT) return mc.mouseHandler.isLeftPressed();
            if (btn == InputConstants.MOUSE_BUTTON_RIGHT) return mc.mouseHandler.isRightPressed();
            if (btn == InputConstants.MOUSE_BUTTON_MIDDLE) return mc.mouseHandler.isMiddlePressed();
            return mapping.isDown();
        }
        return InputConstants.isKeyDown(key.getValue());
    }

    public static boolean isKeyCodeDown(int keyCode) {
        return keyCode > 0 && InputConstants.isKeyDown(keyCode);
    }
}
