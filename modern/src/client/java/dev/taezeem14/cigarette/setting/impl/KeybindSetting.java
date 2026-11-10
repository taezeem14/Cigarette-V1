package dev.taezeem14.cigarette.setting.impl;

import com.mojang.blaze3d.platform.InputConstants;
import dev.taezeem14.cigarette.setting.Setting;

import java.util.Locale;

public class KeybindSetting extends Setting<Integer> {

    public KeybindSetting(String name, int defaultKeyCode) {
        super(name, defaultKeyCode);
    }

    public int getKeyCode() {
        return value;
    }

    public void setKeyCode(int keyCode) {
        setValue(keyCode);
    }

    public String getKeyName() {
        if (value == null || value <= 0) {
            return "None";
        }
        return formatKeyCode(value);
    }

    public static String formatKeyCode(int keyCode) {
        if (keyCode <= 0) {
            return "None";
        }
        try {
            String raw = InputConstants.Type.KEYBOARD.getOrCreate(keyCode).getDisplayName().getString();
            if (raw != null && !raw.isEmpty()) {
                return raw.toUpperCase(Locale.ROOT);
            }
        } catch (Throwable ignored) {
        }
        return "KEY_" + keyCode;
    }
}
