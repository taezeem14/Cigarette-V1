package dev.taezeem14.cigarette.module.impl.combat;

import dev.taezeem14.cigarette.Cigarette;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.DoubleSliderSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.util.math.RandomUtil;

public class Reach extends Module {

    private static Reach instance;

    private final DoubleSliderSetting range = new DoubleSliderSetting("Range", 3.00, 3.35, 3.00, 6.00, 0.01);
    private final NumberSetting chance = new NumberSetting("Chance", 100.0, 0.0, 100.0, 1.0);
    private final BooleanSetting sprintOnly = new BooleanSetting("Sprint Only", false);
    private final BooleanSetting waterCheck = new BooleanSetting("Water Check", false);
    private final BooleanSetting hitThroughWalls = new BooleanSetting("Hit Through Walls", false);

    public Reach() {
        super("Reach", "Extends attack reach distance", Category.COMBAT);
        addSettings(range, chance, sprintOnly, waterCheck, hitThroughWalls);
        instance = this;
    }

    public static double getReachDistance() {
        if (instance == null || !instance.isEnabled() || mc.thePlayer == null) {
            return 3.0D;
        }

        if (mc.thePlayer.capabilities.isCreativeMode) {
            return 5.0D;
        }

        if (instance.sprintOnly.isEnabled() && !mc.thePlayer.isSprinting()) {
            return 3.0D;
        }

        if (instance.waterCheck.isEnabled() && mc.thePlayer.isInWater()) {
            return 3.0D;
        }

        if (!RandomUtil.chance(instance.chance.getValue())) {
            return 3.0D;
        }

        return instance.range.getRandomValue();
    }

    public boolean canHitThroughWalls() {
        return hitThroughWalls.isEnabled();
    }

    public static Reach getInstance() {
        return instance;
    }
}
