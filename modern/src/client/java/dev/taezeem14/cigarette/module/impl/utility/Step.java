package dev.taezeem14.cigarette.module.impl.utility;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.TickEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.util.player.MoveUtil;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Step extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Vanilla", "Vanilla", "Motion");
    private final NumberSetting stepHeight = new NumberSetting("Step Height", 1.0, 1.0, 2.5, 0.5);

    public Step() {
        super("Step", "Allows stepping up full blocks smoothly without jumping", Category.WORLD);
        addSettings(mode, stepHeight);
    }

    @Override
    public String getSuffix() {
        return String.format("%s %.1f", mode.getMode(), stepHeight.getValue());
    }

    @Override
    public void onDisable() {
        setPlayerStepHeight(0.6);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!event.isPre() || mc.player == null || mc.level == null) return;

        if (mode.is("Vanilla")) {
            setPlayerStepHeight(stepHeight.getValue());
        } else if (mode.is("Motion")) {
            setPlayerStepHeight(0.6);
            if (mc.player.onGround() && mc.player.horizontalCollision && MoveUtil.isMoving()
                    && !mc.player.isShiftKeyDown() && !mc.player.isInWater()) {
                double dir = MoveUtil.getDirection();
                double dx = -Math.sin(dir) * 0.2;
                double dz = Math.cos(dir) * 0.2;
                AABB box = mc.player.getBoundingBox();

                for (double h = 1.0; h <= stepHeight.getValue() + 0.01; h += 0.5) {
                    if (mc.level.noCollision(mc.player, box.move(0.0, h, 0.0))
                            && mc.level.noCollision(mc.player, box.move(dx, h, dz))) {
                        Vec3 delta = mc.player.getDeltaMovement();
                        double boostY = Math.min(0.6, 0.42 + (h - 1.0) * 0.12);
                        mc.player.setDeltaMovement(delta.x, boostY, delta.z);
                        break;
                    }
                }
            }
        }
    }

    private void setPlayerStepHeight(double value) {
        if (mc.player == null) return;
        AttributeInstance attr = mc.player.getAttribute(Attributes.STEP_HEIGHT);
        if (attr != null) {
            attr.setBaseValue(value);
        }
    }
}
