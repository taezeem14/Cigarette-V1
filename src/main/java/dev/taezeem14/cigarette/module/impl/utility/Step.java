package dev.taezeem14.cigarette.module.impl.utility;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.TickEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.util.player.MoveUtil;

public class Step extends Module {

    private final NumberSetting stepHeight = new NumberSetting("Step Height", 1.0, 1.0, 2.5, 0.5);
    private final ModeSetting mode = new ModeSetting("Mode", "Vanilla", "Vanilla", "Motion");

    public Step() {
        super("Step", "Automatically steps up full blocks without jumping", Category.WORLD);
        addSettings(stepHeight, mode);
    }

    @Override
    public String getSuffix() {
        return String.format("%.1f", stepHeight.getValue());
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer != null) {
            mc.thePlayer.stepHeight = 0.6F;
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!event.isPre() || mc.thePlayer == null) {
            return;
        }

        if (mc.thePlayer.isInWater() || mc.thePlayer.isInLava() || mc.thePlayer.isOnLadder()) {
            mc.thePlayer.stepHeight = 0.6F;
            return;
        }

        if (mode.is("Vanilla")) {
            mc.thePlayer.stepHeight = stepHeight.getValueFloat();
        } else if (mode.is("Motion")) {
            mc.thePlayer.stepHeight = 0.6F;
            if (mc.thePlayer.onGround && mc.thePlayer.isCollidedHorizontally
                    && MoveUtil.isMoving() && !mc.thePlayer.isSneaking()) {
                mc.thePlayer.motionY = 0.42D;
            }
        }
    }
}
