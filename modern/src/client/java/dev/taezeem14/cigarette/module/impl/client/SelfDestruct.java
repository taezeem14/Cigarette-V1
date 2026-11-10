package dev.taezeem14.cigarette.module.impl.client;

import dev.taezeem14.cigarette.CigaretteModernClient;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.module.impl.blatant.Timer;
import dev.taezeem14.cigarette.module.impl.render.Fullbright;
import dev.taezeem14.cigarette.util.player.CPSTracker;

public class SelfDestruct extends Module {

    public SelfDestruct() {
        super("SelfDestruct", "Cleanly disables modules, resets modified client state, and clears overlays", Category.CLIENT);
    }

    @Override
    public void onEnable() {
        if (mc.gui != null && mc.gui.screen() != null) {
            mc.gui.setScreen(null);
        }

        Timer.setTimerSpeed(1.0F);
        if (mc.options != null) {
            Fullbright.restoreGamma();
            mc.options.keyAttack.setDown(false);
            mc.options.keyUse.setDown(false);
            mc.options.keyJump.setDown(false);
            mc.options.keyShift.setDown(false);
            mc.options.keyUp.setDown(false);
        }

        if (CigaretteModernClient.getInstance() != null && CigaretteModernClient.getInstance().getModuleManager() != null) {
            for (Module mod : CigaretteModernClient.getInstance().getModuleManager().getModules()) {
                if (mod != this && mod.isEnabled()) {
                    mod.setState(false);
                }
                mod.setKeyCode(0);
            }
        }

        CPSTracker.clear();
        if (CigaretteModernClient.getInstance() != null) {
            CigaretteModernClient.getInstance().destruct();
        }
        setState(false);
    }
}
