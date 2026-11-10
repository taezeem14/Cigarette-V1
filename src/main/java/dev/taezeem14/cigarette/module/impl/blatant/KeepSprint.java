package dev.taezeem14.cigarette.module.impl.blatant;

import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;

public class KeepSprint extends Module {

    private static KeepSprint instance;

    private final NumberSetting slowdownReduction = new NumberSetting("Slowdown Reduction", 100.0, 0.0, 100.0, 1.0);

    public KeepSprint() {
        super("KeepSprint", "Maintains sprint momentum when attacking entities", Category.BLATANT);
        addSettings(slowdownReduction);
        instance = this;
    }

    public static boolean shouldKeepSprint() {
        return instance != null && instance.isEnabled();
    }

    public static double getSlowdownMultiplier() {
        if (instance == null || !instance.isEnabled()) return 0.6D;
        double reduction = instance.slowdownReduction.getValue() / 100.0;
        return 0.6D + (0.4D * reduction);
    }

    public static KeepSprint getInstance() {
        return instance;
    }
}
