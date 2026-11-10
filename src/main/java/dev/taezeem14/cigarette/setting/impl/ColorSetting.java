package dev.taezeem14.cigarette.setting.impl;

import dev.taezeem14.cigarette.setting.Setting;

import java.awt.Color;
import java.util.function.Supplier;

public class ColorSetting extends Setting<Integer> {

    private boolean rainbow;
    private float rainbowSpeed = 1.0f;

    public ColorSetting(String name, int defaultRgb) {
        super(name, defaultRgb);
        this.rainbow = false;
    }

    public ColorSetting(String name, int defaultRgb, boolean rainbow) {
        super(name, defaultRgb);
        this.rainbow = rainbow;
    }

    public int getRGB() {
        if (rainbow) {
            float hue = ((System.currentTimeMillis() * (int) (rainbowSpeed * 2.0f)) % 10000L) / 10000.0f;
            int rgb = Color.HSBtoRGB(hue, 0.8f, 1.0f);
            int alpha = (getValue() >> 24) & 0xFF;
            return (alpha << 24) | (rgb & 0x00FFFFFF);
        }
        return getValue();
    }

    public Color getColor() {
        return new Color(getRGB(), true);
    }

    public int getRed() {
        return (getRGB() >> 16) & 0xFF;
    }

    public int getGreen() {
        return (getRGB() >> 8) & 0xFF;
    }

    public int getBlue() {
        return getRGB() & 0xFF;
    }

    public int getAlpha() {
        return (getValue() >> 24) & 0xFF;
    }

    public boolean isRainbow() {
        return rainbow;
    }

    public void setRainbow(boolean rainbow) {
        this.rainbow = rainbow;
    }

    public float getRainbowSpeed() {
        return rainbowSpeed;
    }

    public void setRainbowSpeed(float rainbowSpeed) {
        this.rainbowSpeed = rainbowSpeed;
    }

    @Override
    public ColorSetting setVisibility(Supplier<Boolean> visibility) {
        super.setVisibility(visibility);
        return this;
    }
}
