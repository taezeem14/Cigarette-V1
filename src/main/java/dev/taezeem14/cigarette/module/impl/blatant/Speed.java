package dev.taezeem14.cigarette.module.impl.blatant;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.MoveEvent;
import dev.taezeem14.cigarette.event.impl.TickEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.util.player.MoveUtil;

public class Speed extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Strafe", "Strafe", "BHop", "Vanilla");
    private final NumberSetting speedVal = new NumberSetting("Speed", 1.4, 0.5, 5.0, 0.1);

    private double moveSpeed;

    public Speed() {
        super("Speed", "Modifies movement velocity to travel faster", Category.BLATANT);
        addSettings(mode, speedVal);
    }

    @Override
    public String getSuffix() {
        return mode.getMode();
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer != null) {
            moveSpeed = MoveUtil.getBaseMoveSpeed();
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        // Movement handled directly inside onMove for consistent MoveEvent & SafeWalk sync
    }

    @EventTarget
    public void onMove(MoveEvent event) {
        if (mc.thePlayer == null || !MoveUtil.isMoving()) {
            return;
        }

        if (mode.is("BHop")) {
            if (mc.thePlayer.onGround) {
                mc.thePlayer.motionY = 0.32D;
                event.setY(0.32D);
                moveSpeed = MoveUtil.getBaseMoveSpeed() * speedVal.getValue() * 1.15;
            } else {
                moveSpeed = MoveUtil.getBaseMoveSpeed() * speedVal.getValue();
            }
            MoveUtil.strafe(event, moveSpeed);
        } else if (mode.is("Strafe")) {
            if (mc.thePlayer.onGround) {
                mc.thePlayer.motionY = 0.42F;
                event.setY(0.42F);
                moveSpeed = MoveUtil.getBaseMoveSpeed() * speedVal.getValue() * 1.25;
            } else {
                moveSpeed = Math.max(MoveUtil.getBaseMoveSpeed(), moveSpeed * 0.985);
            }
            MoveUtil.strafe(event, moveSpeed);
        } else if (mode.is("Vanilla")) {
            MoveUtil.strafe(event, MoveUtil.getBaseMoveSpeed() * speedVal.getValue());
        }
    }
}
