package dev.taezeem14.cigarette.util.player;

import dev.taezeem14.cigarette.event.impl.MoveEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public final class MoveUtil {

    private static final Minecraft mc = Minecraft.getInstance();

    private MoveUtil() {
    }

    public static boolean isMoving() {
        if (mc.player == null || mc.player.input == null) return false;
        Vec2 vec = mc.player.input.getMoveVector();
        return vec.y != 0.0F || vec.x != 0.0F;
    }

    public static double getSpeed() {
        if (mc.player == null) return 0.0;
        Vec3 delta = mc.player.getDeltaMovement();
        return Math.hypot(delta.x, delta.z);
    }

    public static double getBaseMoveSpeed() {
        double baseSpeed = 0.2873;
        if (mc.player != null) {
            if (mc.player.hasEffect(MobEffects.SPEED)) {
                int amp = mc.player.getEffect(MobEffects.SPEED).getAmplifier();
                baseSpeed *= 1.0 + 0.2 * (amp + 1);
            }
            if (mc.player.hasEffect(MobEffects.SLOWNESS)) {
                int amp = mc.player.getEffect(MobEffects.SLOWNESS).getAmplifier();
                baseSpeed *= 1.0 - 0.15 * (amp + 1);
            }
        }
        return baseSpeed;
    }

    public static void strafe(double speed) {
        if (mc.player == null) return;
        if (!isMoving()) {
            Vec3 delta = mc.player.getDeltaMovement();
            mc.player.setDeltaMovement(0.0, delta.y, 0.0);
            return;
        }
        double dir = getDirection();
        Vec3 delta = mc.player.getDeltaMovement();
        mc.player.setDeltaMovement(-Math.sin(dir) * speed, delta.y, Math.cos(dir) * speed);
    }

    public static void strafe(MoveEvent event, double speed) {
        if (!isMoving()) {
            event.setX(0.0);
            event.setZ(0.0);
            return;
        }
        double dir = getDirection();
        event.setX(-Math.sin(dir) * speed);
        event.setZ(Math.cos(dir) * speed);
    }

    public static double getDirection() {
        float yaw = mc.player.getYRot();
        Vec2 move = mc.player.input.getMoveVector();
        float moveForward = move.y;
        float moveStrafe = move.x;

        if (moveForward < 0.0F) {
            yaw += 180.0F;
        }
        float forward = moveForward < 0.0F ? -0.5F : (moveForward > 0.0F ? 0.5F : 1.0F);
        if (moveStrafe > 0.0F) {
            yaw -= 90.0F * forward;
        } else if (moveStrafe < 0.0F) {
            yaw += 90.0F * forward;
        }
        return Math.toRadians(yaw);
    }

    public static Vec2 fixMovementVector(Vec2 rawMove, float playerYaw, float targetYaw, boolean strict) {
        if (rawMove == null || (rawMove.x == 0.0F && rawMove.y == 0.0F)) {
            return rawMove;
        }
        float forward = rawMove.y;
        float strafe = rawMove.x;
        float inputMax = Math.max(Math.abs(forward), Math.abs(strafe));
        float normForward = forward / inputMax;
        float normStrafe = strafe / inputMax;

        if (strict) {
            double desired = calcMoveAngle(playerYaw, normForward, normStrafe);
            float bestF = 0.0F;
            float bestS = 0.0F;
            double bestErr = Double.MAX_VALUE;
            for (int f = -1; f <= 1; f++) {
                for (int s = -1; s <= 1; s++) {
                    if (f == 0 && s == 0) continue;
                    double cand = calcMoveAngle(targetYaw, f, s);
                    double err = Math.abs(wrapAngleRadians(desired - cand));
                    if (err < bestErr) {
                        bestErr = err;
                        bestF = (float) f;
                        bestS = (float) s;
                    }
                }
            }
            return new Vec2(bestS * inputMax, bestF * inputMax);
        }

        float diff = Mth.wrapDegrees(playerYaw - targetYaw);
        double rad = Math.toRadians(diff);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);

        double fixedF = normForward * cos + normStrafe * sin;
        double fixedS = normStrafe * cos - normForward * sin;

        int df = Math.max(-1, Math.min(1, (int) Math.round(fixedF)));
        int ds = Math.max(-1, Math.min(1, (int) Math.round(fixedS)));
        if (df == 0 && ds == 0) {
            if (Math.abs(fixedF) >= Math.abs(fixedS)) {
                df = fixedF >= 0.0 ? 1 : -1;
            } else {
                ds = fixedS >= 0.0 ? 1 : -1;
            }
        }
        return new Vec2(ds * inputMax, df * inputMax);
    }

    private static double calcMoveAngle(float yaw, float forward, float strafe) {
        double rad = Math.toRadians(yaw);
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);
        double mx = strafe * cos - forward * sin;
        double mz = forward * cos + strafe * sin;
        return Math.atan2(mx, mz);
    }

    private static double wrapAngleRadians(double angle) {
        while (angle > Math.PI) angle -= Math.PI * 2.0;
        while (angle <= -Math.PI) angle += Math.PI * 2.0;
        return angle;
    }
}
