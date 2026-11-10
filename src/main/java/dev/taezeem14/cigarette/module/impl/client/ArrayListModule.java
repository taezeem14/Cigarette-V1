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
    private final NumberSetting rainbowSpeed = new NumberSetting("Rainbow Speed", 1.0, 0.5, 3.0, 0.1);
    private final BooleanSetting background = new BooleanSetting("Background", true);
    private final BooleanSetting suffix = new BooleanSetting("Suffix", true);
    private final ModeSetting font = new ModeSetting("Font", "Custom", "Custom", "Minecraft");
    private final BooleanSetting animation = new BooleanSetting("Animation", true);

    public ArrayListModule() {
        super("ArrayList", "Displays enabled modules on screen", Category.CLIENT);
        addSettings(colorMode, staticColor, rainbowSpeed, background, suffix, font, animation);
        setState(true);
        instance = this;
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

    public boolean isSuffixEnabled() {
        return suffix.isEnabled();
    }

    @Override
    public String getSuffix() {
        return colorMode.getMode();
    }

    public ModeSetting getFont() {
        return font;
    }

    public BooleanSetting getAnimation() {
        return animation;
    }
}
