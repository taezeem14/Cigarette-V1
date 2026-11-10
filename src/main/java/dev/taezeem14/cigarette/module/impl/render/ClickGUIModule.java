package dev.taezeem14.cigarette.module.impl.render;

import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.ui.clickgui.ClickGUI;
import org.lwjgl.input.Keyboard;

public class ClickGUIModule extends Module {

    private ClickGUI clickGUI;

    public ClickGUIModule() {
        super("ClickGUI", "Opens the Vape V4 replica user interface", Category.RENDER, Keyboard.KEY_RSHIFT);
    }

    @Override
    public void onEnable() {
        if (mc.theWorld == null) {
            setState(false);
            return;
        }

        if (clickGUI == null) {
            clickGUI = new ClickGUI();
        }

        mc.displayGuiScreen(clickGUI);
    }

    @Override
    public void onDisable() {
        if (mc.currentScreen instanceof ClickGUI) {
            mc.displayGuiScreen(null);
        }
    }
}
