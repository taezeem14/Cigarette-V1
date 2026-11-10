package dev.taezeem14.cigarette.setting.impl;

import dev.taezeem14.cigarette.setting.Setting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Supplier;

public class NumberSetting extends Setting<Double> {

    private final double min;
    private final double max;
    private final double increment;

    public NumberSetting(String name, double defaultValue, double min, double max, double increment) {
        super(name, defaultValue);
        this.min = min;
        this.max = max;
        this.increment = increment;
        setValue(clamp(defaultValue));
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getIncrement() {
        return increment;
    }

    public float getValueFloat() {
        return getValue().floatValue();
    }

    public int getValueInt() {
        return getValue().intValue();
    }

    public long getValueLong() {
        return getValue().longValue();
    }

    @Override
    public void setValue(Double value) {
        if (value == null) return;
        super.setValue(clamp(round(value, increment)));
    }

    private double clamp(double val) {
        return Math.max(min, Math.min(max, val));
    }

    private static double round(double value, double increment) {
        if (increment <= 0.0) return value;
        BigDecimal bd = BigDecimal.valueOf(Math.round(value / increment) * increment);
        return bd.setScale(4, RoundingMode.HALF_UP).doubleValue();
    }

    @Override
    public NumberSetting setVisibility(Supplier<Boolean> visibility) {
        super.setVisibility(visibility);
        return this;
    }
}
