package dev.taezeem14.cigarette.module.impl.combat;

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

    public Reach() {
        super("Reach", "Extends attack interaction range", Category.COMBAT);
        addSettings(range, chance, sprintOnly, waterCheck);
        instance = this;
    }

    @Override
    public String getSuffix() {
        return String.format("%.2f-%.2f", range.getValueMin(), range.getValueMax());
    }

    public static double getReachDistance(double fallback) {
        if (instance == null || !instance.isEnabled() || mc.player == null) {
            return fallback;
        }
        if (mc.player.isCreative()) {
            return Math.max(fallback, 5.0);
        }
        if (instance.sprintOnly.isEnabled() && !mc.player.isSprinting()) {
            return fallback;
        }
        if (instance.waterCheck.isEnabled() && mc.player.isInWater()) {
            return fallback;
        }
        if (!RandomUtil.chance(instance.chance.getValue())) {
            return fallback;
        }
        return Math.max(fallback, instance.range.getRandomValue());
    }
}
