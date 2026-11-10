package dev.taezeem14.cigarette.setting.impl;

import dev.taezeem14.cigarette.setting.Setting;

import java.awt.Color;

public class ColorSetting extends Setting<Integer> {

    private boolean rainbow = false;
    private float rainbowSpeed = 1.0f;

    public ColorSetting(String name, int defaultColor) {
        super(name, defaultColor);
    }

    public ColorSetting(String name, Color defaultColor) {
        super(name, defaultColor.getRGB());
    }

    public int getRGB() {
        if (rainbow) {
            float hue = (System.currentTimeMillis() % (long) (4000 / Math.max(0.2f, rainbowSpeed))) / (4000.0f / Math.max(0.2f, rainbowSpeed));
            return Color.HSBtoRGB(hue, 0.8f, 1.0f);
        }
        return value;
    }

    public Color getColor() {
        return new Color(getRGB(), true);
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
}
