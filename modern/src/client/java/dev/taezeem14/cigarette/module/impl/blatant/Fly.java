package dev.taezeem14.cigarette.module.impl.blatant;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.MotionEvent;
import dev.taezeem14.cigarette.event.impl.MoveEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.util.player.MoveUtil;
import net.minecraft.world.phys.Vec3;

public class Fly extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Vanilla", "Vanilla", "Motion", "Glide", "Damage Boost");
    private final NumberSetting speedVal = new NumberSetting("Speed", 2.0, 0.5, 5.0, 0.1);

    private boolean boosted = false;
    private int boostTicks = 0;
    private boolean prevAllowFlying = false;
    private boolean prevFlying = false;

    public Fly() {
        super("Fly", "Allows controlled flight in survival mode", Category.BLATANT);
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
        if (mc.player == null) return;

        prevAllowFlying = mc.player.getAbilities().mayfly;
        prevFlying = mc.player.getAbilities().flying;
        if (mode.is("Vanilla")) {
            mc.player.getAbilities().mayfly = true;
            mc.player.getAbilities().flying = true;
        }
    }

    @Override
    public void onDisable() {
        boosted = false;
        boostTicks = 0;
        if (mc.player == null) return;

        if (mode.is("Vanilla") && !mc.player.isCreative() && !mc.player.isSpectator()) {
            mc.player.getAbilities().mayfly = prevAllowFlying;
            mc.player.getAbilities().flying = prevAllowFlying && prevFlying;
        }
        Vec3 delta = mc.player.getDeltaMovement();
        mc.player.setDeltaMovement(0.0, delta.y, 0.0);
    }

    @EventTarget
    public void onMotion(MotionEvent event) {
        if (mc.player == null || !event.isPre()) return;
        if (mode.is("Damage Boost")) {
            if (mc.player.hurtTime > 0 && !boosted) {
                boosted = true;
                boostTicks = 0;
                MoveUtil.strafe(speedVal.getValue() * 2.5);
                Vec3 delta = mc.player.getDeltaMovement();
                mc.player.setDeltaMovement(delta.x, 0.42, delta.z);
            } else if (boosted) {
                boostTicks++;
                if (boostTicks > 2 && mc.player.onGround()) {
                    boosted = false;
                    boostTicks = 0;
                    Vec3 delta = mc.player.getDeltaMovement();
                    mc.player.setDeltaMovement(delta.x * 0.6, delta.y, delta.z * 0.6);
                } else {
                    Vec3 delta = mc.player.getDeltaMovement();
                    mc.player.setDeltaMovement(delta.x, -0.04, delta.z);
                    MoveUtil.strafe(speedVal.getValue());
                }
            }
        }
    }

    @EventTarget
    public void onMove(MoveEvent event) {
        if (mc.player == null) return;

        if (mode.is("Vanilla")) {
            mc.player.getAbilities().mayfly = true;
            mc.player.getAbilities().flying = true;
            MoveUtil.strafe(event, speedVal.getValue());
        } else if (mode.is("Motion")) {
            double ySpeed = 0.0;
            if (mc.options.keyJump.isDown()) {
                ySpeed = speedVal.getValue() * 0.5;
            } else if (mc.options.keyShift.isDown()) {
                ySpeed = -speedVal.getValue() * 0.5;
            }
            Vec3 delta = mc.player.getDeltaMovement();
            mc.player.setDeltaMovement(delta.x, ySpeed, delta.z);
            event.setY(ySpeed);
            if (MoveUtil.isMoving()) {
                MoveUtil.strafe(event, speedVal.getValue());
            } else {
                event.setX(0.0);
                event.setZ(0.0);
            }
        } else if (mode.is("Glide")) {
            Vec3 delta = mc.player.getDeltaMovement();
            mc.player.setDeltaMovement(delta.x, -0.04, delta.z);
            event.setY(-0.04);
            if (MoveUtil.isMoving()) {
                MoveUtil.strafe(event, speedVal.getValue());
            }
        }
    }
}
