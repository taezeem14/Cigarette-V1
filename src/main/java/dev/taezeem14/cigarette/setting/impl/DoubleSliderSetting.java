package dev.taezeem14.cigarette.setting.impl;

import dev.taezeem14.cigarette.setting.Setting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public class DoubleSliderSetting extends Setting<double[]> {

    private final double min;
    private final double max;
    private final double increment;

    public DoubleSliderSetting(String name, double defaultValMin, double defaultValMax, double min, double max, double increment) {
        super(name, new double[]{defaultValMin, defaultValMax});
        this.min = min;
        this.max = max;
        this.increment = increment;
        setValue(new double[]{defaultValMin, defaultValMax});
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

    public double getValueMin() {
        return getValue()[0];
    }

    public double getValueMax() {
        return getValue()[1];
    }

    public void setValueMin(double valMin) {
        double clamped = clamp(round(valMin, increment));
        if (clamped > getValueMax()) {
            clamped = getValueMax();
        }
        getValue()[0] = clamped;
    }

    public void setValueMax(double valMax) {
        double clamped = clamp(round(valMax, increment));
        if (clamped < getValueMin()) {
            clamped = getValueMin();
        }
        getValue()[1] = clamped;
    }

    public double getRandomValue() {
        double low = getValueMin();
        double high = getValueMax();
        if (low >= high) return low;
        return ThreadLocalRandom.current().nextDouble(low, high);
    }

    @Override
    public void setValue(double[] values) {
        if (values == null || values.length < 2) return;
        double low = clamp(round(values[0], increment));
        double high = clamp(round(values[1], increment));
        if (low > high) {
            double temp = low;
            low = high;
            high = temp;
        }
        super.setValue(new double[]{low, high});
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
    public DoubleSliderSetting setVisibility(Supplier<Boolean> visibility) {
        super.setVisibility(visibility);
        return this;
    }
}
