package dev.taezeem14.cigarette.module.impl.utility;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.TickEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;

import java.lang.reflect.Field;

public class SpeedMine extends Module {

    private final NumberSetting speedMultiplier = new NumberSetting("Speed Multiplier", 1.35, 1.0, 2.5, 0.05);
    private final BooleanSetting noBreakDelay = new BooleanSetting("No Break Delay", true);

    private static Field destroyDelayField;
    private static Field destroyProgressField;
    private static boolean fieldsResolved = false;

    private float lastProgress = 0.0F;

    public SpeedMine() {
        super("SpeedMine", "Accelerates block breaking and removes mining cooldown", Category.WORLD);
        addSettings(speedMultiplier, noBreakDelay);
    }

    @Override
    public String getSuffix() {
        return String.format("%.2fx", speedMultiplier.getValue());
    }

    @Override
    public void onDisable() {
        lastProgress = 0.0F;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!event.isPre() || mc.player == null || mc.level == null || mc.gameMode == null) return;

        resolveFields();

        try {
            if (noBreakDelay.isEnabled() && destroyDelayField != null) {
                if (destroyDelayField.getInt(mc.gameMode) > 0) {
                    destroyDelayField.setInt(mc.gameMode, 0);
                }
            }

            if (mc.gameMode.isDestroying() && speedMultiplier.getValue() > 1.0 && destroyProgressField != null) {
                float cur = destroyProgressField.getFloat(mc.gameMode);
                if (cur <= 0.0F) {
                    lastProgress = 0.0F;
                } else if (cur < 1.0F) {
                    float delta = cur >= lastProgress ? (cur - lastProgress) : cur;
                    if (delta > 0.0F) {
                        float boosted = Math.min(1.0F, cur + delta * (speedMultiplier.getValueFloat() - 1.0F));
                        destroyProgressField.setFloat(mc.gameMode, boosted);
                        lastProgress = boosted;
                    }
                }
            } else {
                lastProgress = 0.0F;
            }
        } catch (Throwable ignored) {
        }
    }

    private static void resolveFields() {
        if (fieldsResolved) return;
        fieldsResolved = true;
        destroyDelayField = findField("destroyDelay", "field_3716");
        destroyProgressField = findField("destroyProgress", "field_3715");
    }

    private static Field findField(String... names) {
        for (String name : names) {
            try {
                Field f = MultiPlayerGameMode.class.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }
}
