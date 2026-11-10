package dev.taezeem14.cigarette.setting.impl;

import dev.taezeem14.cigarette.setting.Setting;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class ModeSetting extends Setting<String> {

    private final List<String> modes;
    private int index;

    public ModeSetting(String name, String defaultMode, String... modes) {
        super(name, defaultMode);
        this.modes = Arrays.asList(modes);
        this.index = this.modes.indexOf(defaultMode);
        if (this.index == -1) {
            this.index = 0;
            this.value = modes.length > 0 ? modes[0] : "";
        }
    }

    public List<String> getModes() {
        return modes;
    }

    public String getMode() {
        return getValue();
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        if (index >= 0 && index < modes.size()) {
            this.index = index;
            setValue(modes.get(index));
        }
    }

    public void setMode(String mode) {
        int idx = modes.indexOf(mode);
        if (idx != -1) {
            this.index = idx;
            setValue(mode);
        }
    }

    public void cycle() {
        if (modes.isEmpty()) return;
        index = (index + 1) % modes.size();
        setValue(modes.get(index));
    }

    public void cycleReverse() {
        if (modes.isEmpty()) return;
        index = (index - 1 + modes.size()) % modes.size();
        setValue(modes.get(index));
    }

    public boolean is(String mode) {
        return getValue().equalsIgnoreCase(mode);
    }

    @Override
    public ModeSetting setVisibility(Supplier<Boolean> visibility) {
        super.setVisibility(visibility);
        return this;
    }
}
