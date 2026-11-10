package dev.taezeem14.cigarette.module.impl.blatant;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.TickEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.util.mapping.MappingResolver;

public class Timer extends Module {

    private final NumberSetting speed = new NumberSetting("Speed", 1.2, 0.1, 5.0, 0.05);

    public Timer() {
        super("Timer", "Alters client tick execution rate", Category.BLATANT);
        addSettings(speed);
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
        try {
            Object timerObj = MappingResolver.getFieldValue(mc, mc.getClass(), MappingResolver.MINECRAFT_TIMER);
            if (timerObj != null) {
                MappingResolver.setFieldValue(timerObj, speedVal, timerObj.getClass(), MappingResolver.TIMER_TIMER_SPEED);
            }
        } catch (Exception ignored) {
        }
    }
}
