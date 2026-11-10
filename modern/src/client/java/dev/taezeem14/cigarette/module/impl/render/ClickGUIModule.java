package dev.taezeem14.cigarette.module.impl.render;

import com.mojang.blaze3d.platform.InputConstants;
import dev.taezeem14.cigarette.CigaretteModernClient;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.ColorSetting;
import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import dev.taezeem14.cigarette.ui.clickgui.ClickGUI;
import dev.taezeem14.cigarette.util.render.ColorUtil;

public class ClickGUIModule extends Module {

    private final ModeSetting theme = new ModeSetting("Theme", "Electric Blue", ColorUtil.THEME_PRESETS);
    private final ColorSetting accent = new ColorSetting("Custom Accent", ColorUtil.ELECTRIC_BLUE);
    private final BooleanSetting useCustomAccent = new BooleanSetting("Use Custom Accent", false);
    private final BooleanSetting blur = new BooleanSetting("Background Blur", true);

    public ClickGUIModule() {
        super("ClickGUI", "Opens the Vape V4 style configuration interface", Category.RENDER, InputConstants.KEY_RSHIFT);
        addSettings(theme, useCustomAccent, accent, blur);
        accent.setVisibility(useCustomAccent::isEnabled);
    }

    @Override
    public void onEnable() {
        syncThemeFromSetting();
        if (mc.player != null) {
            ClickGUI gui = CigaretteModernClient.getInstance() != null
                    ? CigaretteModernClient.getInstance().getClickGUI()
                    : new ClickGUI();
            mc.gui.setScreen(gui);
        }
    }

    public void syncThemeFromSetting() {
        if (useCustomAccent.isEnabled()) {
            ColorUtil.setThemePreset("Electric Blue");
            ColorUtil.setAccent(accent.getRGB());
        } else {
            ColorUtil.setThemePreset(theme.getMode());
        }
    }

    public void setThemePreset(String preset) {
        useCustomAccent.setValue(false);
        theme.setMode(preset);
        ColorUtil.setThemePreset(preset);
    }

    public boolean isBlurEnabled() {
        return blur.isEnabled();
    }
}
