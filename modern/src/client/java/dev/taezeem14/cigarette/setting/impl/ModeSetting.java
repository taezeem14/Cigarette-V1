package dev.taezeem14.cigarette.setting.impl;

import dev.taezeem14.cigarette.setting.Setting;

import java.util.Arrays;
import java.util.List;

public class ModeSetting extends Setting<String> {

    private final List<String> modes;
    private int index;

    public ModeSetting(String name, String defaultMode, String... modes) {
        super(name, defaultMode);
        this.modes = Arrays.asList(modes);
        int idx = this.modes.indexOf(defaultMode);
        this.index = Math.max(0, idx);
    }

    public String getMode() {
        return value;
    }

    public void setMode(String mode) {
        int idx = modes.indexOf(mode);
        if (idx != -1) {
            this.index = idx;
            setValue(mode);
        }
    }

    public boolean is(String mode) {
        return value.equalsIgnoreCase(mode);
    }

    public void cycle() {
        if (index < modes.size() - 1) {
            index++;
        } else {
            index = 0;
        }
        setValue(modes.get(index));
    }

    public List<String> getModes() {
        return modes;
    }

    public int getIndex() {
        return index;
    }
}
