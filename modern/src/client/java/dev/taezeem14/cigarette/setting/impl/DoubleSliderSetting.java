package dev.taezeem14.cigarette.setting.impl;

import dev.taezeem14.cigarette.setting.Setting;

import java.util.concurrent.ThreadLocalRandom;

public class DoubleSliderSetting extends Setting<double[]> {

    private final double minLimit;
    private final double maxLimit;
    private final double increment;

    public DoubleSliderSetting(String name, double defaultMin, double defaultMax, double minLimit, double maxLimit, double increment) {
        super(name, new double[]{defaultMin, defaultMax});
        this.minLimit = minLimit;
        this.maxLimit = maxLimit;
        this.increment = increment;
        setValueMin(defaultMin);
        setValueMax(defaultMax);
    }

    public double getValueMin() {
        return value[0];
    }

    public double getValueMax() {
        return value[1];
    }

    public void setValueMin(double val) {
        double precision = 1.0 / increment;
        double clamped = Math.max(minLimit, Math.min(value[1], val));
        value[0] = Math.round(clamped * precision) / precision;
    }

    public void setValueMax(double val) {
        double precision = 1.0 / increment;
        double clamped = Math.max(value[0], Math.min(maxLimit, val));
        value[1] = Math.round(clamped * precision) / precision;
    }

    public float getMinFloat() {
        return (float) value[0];
    }

    public float getMaxFloat() {
        return (float) value[1];
    }

    public double getMinLimit() {
        return minLimit;
    }

    public double getMaxLimit() {
        return maxLimit;
    }

    public double getIncrement() {
        return increment;
    }

    public double getRandomValue() {
        if (value[0] >= value[1]) return value[0];
        return ThreadLocalRandom.current().nextDouble(value[0], value[1]);
    }
}
