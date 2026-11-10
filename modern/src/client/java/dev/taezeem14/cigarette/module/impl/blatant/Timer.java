package dev.taezeem14.cigarette.module.impl.blatant;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.TickEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;

public class Timer extends Module {

    private static Timer instance;
    private static volatile float activeTimerSpeed = 1.0F;

    private final NumberSetting speed = new NumberSetting("Speed", 1.2, 0.1, 5.0, 0.05);

    public Timer() {
        super("Timer", "Alters client tick execution speed", Category.BLATANT);
        addSettings(speed);
        instance = this;
    }

    @Override
    public String getSuffix() {
        return String.format("%.2f", speed.getValue());
    }

    @Override
    public void onDisable() {
        setTimerSpeed(1.0F);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!event.isPre()) return;
        setTimerSpeed(speed.getValueFloat());
    }

    public static void setTimerSpeed(float speedVal) {
        activeTimerSpeed = Math.max(0.05F, speedVal);
    }

    public static float getActiveTimerSpeed() {
        if (instance == null || !instance.isEnabled()) {
            return 1.0F;
        }
        return activeTimerSpeed;
    }
}
