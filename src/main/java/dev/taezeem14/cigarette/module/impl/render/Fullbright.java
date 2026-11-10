package dev.taezeem14.cigarette.module.impl.render;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.TickEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;

public class Fullbright extends Module {

    private float previousGamma = 1.0f;

    public Fullbright() {
        super("Fullbright", "Boosts world brightness to maximum gamma", Category.RENDER);
    }

    @Override
    public void onEnable() {
        if (mc.gameSettings != null) {
            previousGamma = mc.gameSettings.gammaSetting;
            mc.gameSettings.gammaSetting = 1000.0f;
        }
    }

    @Override
    public void onDisable() {
        if (mc.gameSettings != null) {
            mc.gameSettings.gammaSetting = previousGamma;
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (mc.gameSettings != null && mc.gameSettings.gammaSetting < 100.0f) {
            mc.gameSettings.gammaSetting = 1000.0f;
        }
    }
}
