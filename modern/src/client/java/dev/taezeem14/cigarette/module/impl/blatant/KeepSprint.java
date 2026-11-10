package dev.taezeem14.cigarette.module.impl.blatant;

import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;

public class KeepSprint extends Module {

    private static KeepSprint instance;

    private final NumberSetting slowDown = new NumberSetting("Horizontal Slowdown %", 0.0, 0.0, 40.0, 5.0);

    public KeepSprint() {
        super("KeepSprint", "Prevents sprint cancellation and slowdown when attacking", Category.BLATANT);
        addSettings(slowDown);
        instance = this;
    }

    @Override
    public String getSuffix() {
        return slowDown.getValueInt() + "%";
    }

    public static boolean shouldKeepSprint() {
        return instance != null && instance.isEnabled();
    }

    public static double getVelocityMultiplier() {
        if (instance == null || !instance.isEnabled()) {
            return 0.6;
        }
        return 1.0 - (instance.slowDown.getValue() / 100.0);
    }
}
