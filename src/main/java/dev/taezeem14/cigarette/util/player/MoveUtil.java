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
        if (mc.thePlayer == null || mc.thePlayer.movementInput == null) return;

        float forward = mc.thePlayer.movementInput.moveForward;
        float strafe = mc.thePlayer.movementInput.moveStrafe;

        float diff = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - targetYaw);
        double rad = Math.toRadians(diff);

        float fixedForward = (float) (forward * Math.cos(rad) - strafe * Math.sin(rad));
        float fixedStrafe = (float) (forward * Math.sin(rad) + strafe * Math.cos(rad));

        mc.thePlayer.movementInput.moveForward = Math.round(fixedForward);
        mc.thePlayer.movementInput.moveStrafe = Math.round(fixedStrafe);
    }

    public static void jump() {
        if (mc.thePlayer != null) {
            mc.thePlayer.jump();
        }
    }
}
