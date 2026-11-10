package dev.taezeem14.cigarette.module.impl.render;

import dev.taezeem14.cigarette.Cigarette;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import dev.taezeem14.cigarette.ui.clickgui.ClickGUI;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import org.lwjgl.input.Keyboard;

public class ClickGUIModule extends Module {

    private final BooleanSetting blurBackdrop = new BooleanSetting("Blur Backdrop", true);
    private final ModeSetting theme = new ModeSetting("Theme", "Electric Blue",
            "Electric Blue", "Ember Orange", "Amethyst", "Emerald", "Rainbow");

    private String lastSyncedTheme = "Electric Blue";

    public ClickGUIModule() {
        super("ClickGUI", "Opens the Vape V4 replica user interface", Category.RENDER, Keyboard.KEY_RSHIFT);
        addSettings(blurBackdrop, theme);
        ColorUtil.setThemePreset(theme.getMode());
    }

    @Override
    public void onEnable() {
        if (mc.theWorld == null) {
            setState(false);
            return;
        }

        syncThemeFromSetting();

        ClickGUI gui = (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getClickGUI() != null)
                ? Cigarette.INSTANCE.getClickGUI()
                : new ClickGUI();
        mc.displayGuiScreen(gui);
    }

    @Override
    public void onDisable() {
        if (mc.currentScreen instanceof ClickGUI) {
            mc.displayGuiScreen(null);
        }
    }

    public boolean isBlurEnabled() {
        return blurBackdrop.isEnabled();
    }

    public ModeSetting getThemeSetting() {
        return theme;
    }

    public void setThemePreset(String preset) {
        if (preset == null) return;
        theme.setMode(preset);
        lastSyncedTheme = theme.getMode();
        ColorUtil.setThemePreset(lastSyncedTheme);
    }

    public void syncThemeFromSetting() {
        String current = theme.getMode();
        if (current != null && !current.equalsIgnoreCase(lastSyncedTheme)) {
            lastSyncedTheme = current;
            ColorUtil.setThemePreset(current);
        }
    }
}
