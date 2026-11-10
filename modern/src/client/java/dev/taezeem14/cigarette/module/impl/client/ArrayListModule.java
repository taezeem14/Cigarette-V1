package dev.taezeem14.cigarette.module.impl.client;

import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.ColorSetting;
import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;

public class ArrayListModule extends Module {

    private static ArrayListModule instance;

    private final ModeSetting colorMode = new ModeSetting("Color Mode", "Theme", "Theme", "Rainbow", "Astolfo", "Static");
    private final ColorSetting staticColor = new ColorSetting("Static Color", 0xFF3882F6);
    private final NumberSetting rainbowSpeed = new NumberSetting("Rainbow Speed", 1.0, 0.2, 5.0, 0.1);
    private final BooleanSetting background = new BooleanSetting("Background", true);
    private final BooleanSetting suffix = new BooleanSetting("Suffix", true);
    private final BooleanSetting animation = new BooleanSetting("Slide Animation", true);

    public ArrayListModule() {
        super("ArrayList", "Configures active module list colors and layout", Category.CLIENT);
        addSettings(colorMode, staticColor, rainbowSpeed, background, suffix, animation);
        staticColor.setVisibility(() -> colorMode.is("Static"));
        rainbowSpeed.setVisibility(() -> colorMode.is("Rainbow") || colorMode.is("Astolfo"));
        instance = this;
        setState(true);
    }

    public static ArrayListModule getInstance() {
        return instance;
    }

    public ModeSetting getColorMode() {
        return colorMode;
    }

    public ColorSetting getStaticColor() {
        return staticColor;
    }

    public NumberSetting getRainbowSpeed() {
        return rainbowSpeed;
    }

    public BooleanSetting getBackground() {
        return background;
    }

    public BooleanSetting getSuffixSetting() {
        return suffix;
    }

    public BooleanSetting getAnimation() {
        return animation;
    }
}
