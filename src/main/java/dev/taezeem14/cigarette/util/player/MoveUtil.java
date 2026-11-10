package dev.taezeem14.cigarette.util.player;

import dev.taezeem14.cigarette.event.impl.MoveEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;

public final class MoveUtil {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private MoveUtil() {
    }

    public static boolean isMoving() {
        return mc.thePlayer != null && (mc.thePlayer.movementInput.moveForward != 0.0F || mc.thePlayer.movementInput.moveStrafe != 0.0F);
    }

    public static double getSpeed() {
        if (mc.thePlayer == null) return 0.0;
        return Math.hypot(mc.thePlayer.motionX, mc.thePlayer.motionZ);
    }

    public static double getBaseMoveSpeed() {
        double baseSpeed = 0.2873;
        if (mc.thePlayer != null) {
            if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
                int amplifier = mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier();
                baseSpeed *= 1.0 + 0.2 * (amplifier + 1);
            }
            if (mc.thePlayer.isPotionActive(Potion.moveSlowdown)) {
                int amplifier = mc.thePlayer.getActivePotionEffect(Potion.moveSlowdown).getAmplifier();
                baseSpeed *= 1.0 - 0.15 * (amplifier + 1);
            }
        }
        return baseSpeed;
    }

    public static void setSpeed(double speed) {
        strafe(speed);
    }

    public static void strafe() {
        strafe(getSpeed());
    }

    public static void strafe(double speed) {
        if (!isMoving()) {
            mc.thePlayer.motionX = 0.0;
            mc.thePlayer.motionZ = 0.0;
            return;
        }

        double direction = getDirection();
        mc.thePlayer.motionX = -Math.sin(direction) * speed;
        mc.thePlayer.motionZ = Math.cos(direction) * speed;
    }

    public static void strafe(MoveEvent event, double speed) {
        if (!isMoving()) {
            event.setX(0.0);
            event.setZ(0.0);
            return;
        }

        double direction = getDirection();
        event.setX(-Math.sin(direction) * speed);
        event.setZ(Math.cos(direction) * speed);
    }

    public static double getDirection() {
        float rotationYaw = mc.thePlayer.rotationYaw;

        if (mc.thePlayer.moveForward < 0.0F) {
            rotationYaw += 180.0F;
        }

        float forward = 1.0F;
        if (mc.thePlayer.moveForward < 0.0F) {
            forward = -0.5F;
        } else if (mc.thePlayer.moveForward > 0.0F) {
            forward = 0.5F;
        }

        if (mc.thePlayer.moveStrafing > 0.0F) {
            rotationYaw -= 90.0F * forward;
        }
        if (mc.thePlayer.moveStrafing < 0.0F) {
            rotationYaw += 90.0F * forward;
        }

        return Math.toRadians(rotationYaw);
    }

    public static void fixMovement(float targetYaw) {
        fixMovement(targetYaw, false);
    }

    public static void fixMovement(float targetYaw, boolean strict) {
        if (mc.thePlayer == null || mc.thePlayer.movementInput == null) return;

        float forward = mc.thePlayer.movementInput.moveForward;
        float strafe = mc.thePlayer.movementInput.moveStrafe;
        if (forward == 0.0F && strafe == 0.0F) return;

        float inputScale = mc.thePlayer.movementInput.sneak ? 0.3F : 1.0F;
        float inputMax = Math.max(Math.abs(forward), Math.abs(strafe));
        float normForward = forward / inputMax;
        float normStrafe = strafe / inputMax;

        if (strict) {
            double desiredAngle = calcMoveAngle(mc.thePlayer.rotationYaw, normForward, normStrafe);
            float bestForward = 0.0F;
            float bestStrafe = 0.0F;
            double bestError = Double.MAX_VALUE;

            for (int f = -1; f <= 1; f++) {
                for (int s = -1; s <= 1; s++) {
                    if (f == 0 && s == 0) continue;
                    double candidateAngle = calcMoveAngle(targetYaw, f, s);
                    double error = Math.abs(wrapAngleRadians(desiredAngle - candidateAngle));
                    if (error < bestError) {
                        bestError = error;
                        bestForward = (float) f;
                        bestStrafe = (float) s;
                    }
                }
            }

            mc.thePlayer.movementInput.moveForward = bestForward * inputScale;
            mc.thePlayer.movementInput.moveStrafe = bestStrafe * inputScale;
            return;
        }

        float diff = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - targetYaw);
        double rad = Math.toRadians(diff);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);

        double fixedForward = normForward * cos + normStrafe * sin;
        double fixedStrafe = normStrafe * cos - normForward * sin;

        int discreteForward = Math.max(-1, Math.min(1, (int) Math.round(fixedForward)));
        int discreteStrafe = Math.max(-1, Math.min(1, (int) Math.round(fixedStrafe)));

        if (discreteForward == 0 && discreteStrafe == 0) {
            if (Math.abs(fixedForward) >= Math.abs(fixedStrafe)) {
                discreteForward = fixedForward >= 0.0 ? 1 : -1;
            } else {
                discreteStrafe = fixedStrafe >= 0.0 ? 1 : -1;
            }
        }

        mc.thePlayer.movementInput.moveForward = discreteForward * inputScale;
        mc.thePlayer.movementInput.moveStrafe = discreteStrafe * inputScale;
    }

    private static double calcMoveAngle(float yaw, float forward, float strafe) {
        double rad = Math.toRadians(yaw);
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);
        double motionX = strafe * cos - forward * sin;
        double motionZ = forward * cos + strafe * sin;
        return Math.atan2(motionX, motionZ);
    }

    private static double wrapAngleRadians(double angle) {
        while (angle > Math.PI) angle -= Math.PI * 2.0;
        while (angle <= -Math.PI) angle += Math.PI * 2.0;
        return angle;
    }

    public static void jump() {
        if (mc.thePlayer != null) {
            mc.thePlayer.jump();
        }
    }
}
