package dev.taezeem14.cigarette.module.impl.blatant;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.MotionEvent;
import dev.taezeem14.cigarette.event.impl.MoveEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.util.player.MoveUtil;

public class Fly extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Vanilla", "Vanilla", "Motion", "Glide", "Damage Boost");
    private final NumberSetting speedVal = new NumberSetting("Speed", 2.0, 0.5, 5.0, 0.1);

    private boolean boosted = false;
    private int boostTicks = 0;

    public Fly() {
        super("Fly", "Allows the player to fly in survival mode", Category.BLATANT);
        addSettings(mode, speedVal);
    }

    @Override
    public String getSuffix() {
        return mode.getMode();
    }

    @Override
    public void onEnable() {
        boosted = false;
        boostTicks = 0;
        if (mc.thePlayer == null) return;

        if (mode.is("Vanilla")) {
            mc.thePlayer.capabilities.isFlying = true;
        }
    }

    @Override
    public void onDisable() {
        boosted = false;
        boostTicks = 0;
        if (mc.thePlayer == null) return;

        if (mode.is("Vanilla") && !mc.thePlayer.capabilities.isCreativeMode) {
            mc.thePlayer.capabilities.isFlying = false;
        }

        mc.thePlayer.motionX = 0.0;
        mc.thePlayer.motionZ = 0.0;
    }

    @EventTarget
    public void onMotion(MotionEvent event) {
        if (mc.thePlayer == null || !event.isPre()) return;

        if (mode.is("Damage Boost")) {
            if (mc.thePlayer.hurtTime > 0 && !boosted) {
                boosted = true;
                boostTicks = 0;
                MoveUtil.strafe(speedVal.getValue() * 2.5);
                mc.thePlayer.motionY = 0.42;
            } else if (boosted) {
                boostTicks++;
                if (boostTicks > 2 && mc.thePlayer.onGround) {
                    boosted = false;
                    boostTicks = 0;
                    mc.thePlayer.motionX *= 0.6;
                    mc.thePlayer.motionZ *= 0.6;
                } else {
                    mc.thePlayer.motionY = -0.04;
                    MoveUtil.strafe(speedVal.getValue());
                }
            }
        }
    }

    @EventTarget
    public void onMove(MoveEvent event) {
        if (mc.thePlayer == null) return;

        if (mode.is("Vanilla")) {
            mc.thePlayer.capabilities.isFlying = true;
            MoveUtil.strafe(event, speedVal.getValue());
        } else if (mode.is("Motion")) {
            double ySpeed = 0.0;
            if (mc.gameSettings.keyBindJump.isKeyDown()) {
                ySpeed = speedVal.getValue() * 0.5;
            } else if (mc.gameSettings.keyBindSneak.isKeyDown()) {
                ySpeed = -speedVal.getValue() * 0.5;
            }

            mc.thePlayer.motionY = ySpeed;
            event.setY(ySpeed);

            if (MoveUtil.isMoving()) {
                MoveUtil.strafe(event, speedVal.getValue());
            } else {
                event.setX(0.0);
                event.setZ(0.0);
            }
        } else if (mode.is("Glide")) {
            mc.thePlayer.motionY = -0.04;
            event.setY(-0.04);
            if (MoveUtil.isMoving()) {
                MoveUtil.strafe(event, speedVal.getValue());
            }
        }
    }
}
