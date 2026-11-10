package dev.taezeem14.cigarette.module.impl.render;

import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;

public class Fullbright extends Module {

    private static Fullbright instance;
    private static double previousGamma = 0.5;
    private static boolean savedGamma = false;

    public Fullbright() {
        super("Fullbright", "Eliminates darkness in caves and night environments", Category.RENDER);
        instance = this;
    }

    @Override
    public void onEnable() {
        if (mc.options != null && !savedGamma) {
            previousGamma = mc.options.gamma().get();
            savedGamma = true;
            mc.options.gamma().set(1.0);
        }
    }

    @Override
    public void onDisable() {
        restoreGamma();
    }

    public static void restoreGamma() {
        if (mc.options != null && savedGamma) {
            mc.options.gamma().set(Math.max(0.0, Math.min(1.0, previousGamma)));
            savedGamma = false;
        }
    }

    public static boolean shouldApplyFullbright() {
        return instance != null && instance.isEnabled();
    }
}
