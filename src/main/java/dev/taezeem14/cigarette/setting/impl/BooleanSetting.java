package dev.taezeem14.cigarette.setting.impl;

import dev.taezeem14.cigarette.setting.Setting;

import java.util.function.Supplier;

public class BooleanSetting extends Setting<Boolean> {

    public BooleanSetting(String name, boolean defaultValue) {
        super(name, defaultValue);
    }

    public boolean isEnabled() {
        return Boolean.TRUE.equals(getValue());
    }

    public void setEnabled(boolean enabled) {
        setValue(enabled);
    }

    public void toggle() {
        setValue(!isEnabled());
    }

    @Override
    public BooleanSetting setVisibility(Supplier<Boolean> visibility) {
        super.setVisibility(visibility);
        return this;
    }
}
