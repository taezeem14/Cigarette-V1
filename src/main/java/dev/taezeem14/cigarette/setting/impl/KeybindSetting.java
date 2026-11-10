package dev.taezeem14.cigarette.setting.impl;

import dev.taezeem14.cigarette.setting.Setting;
import org.lwjgl.input.Keyboard;

import java.util.function.Supplier;

public class KeybindSetting extends Setting<Integer> {

    public KeybindSetting(String name, int defaultKeyCode) {
        super(name, defaultKeyCode);
    }

    public int getKeyCode() {
        return getValue();
    }

    public void setKeyCode(int keyCode) {
        setValue(keyCode);
    }

    public String getKeyName() {
        int code = getKeyCode();
        if (code <= 0 || code >= 256) return "NONE";
        String name = Keyboard.getKeyName(code);
        return name != null ? name : "NONE";
    }

    @Override
    public KeybindSetting setVisibility(Supplier<Boolean> visibility) {
        super.setVisibility(visibility);
        return this;
    }
}
