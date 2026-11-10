package dev.taezeem14.cigarette.module.impl.client;

import dev.taezeem14.cigarette.Cigarette;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.module.impl.blatant.Timer;
import dev.taezeem14.cigarette.util.mapping.MappingResolver;
import dev.taezeem14.cigarette.util.player.CPSTracker;

public class SelfDestruct extends Module {

    public SelfDestruct() {
        super("SelfDestruct", "Completely unhooks the client and removes all traces from memory", Category.CLIENT);
    }

    @Override
    public void onEnable() {
        // 1. Close active client GUI screens
        if (mc != null) {
            mc.displayGuiScreen(null);
        }

        // 2. Disable all other modules (restores module-managed state such as Fullbright, Timer, keys)
        if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getModuleManager() != null) {
            for (Module module : Cigarette.INSTANCE.getModuleManager().getModules()) {
                if (module != this && module.isEnabled()) {
                    module.setState(false);
                }
            }
        }

        // 3. Explicitly restore Fullbright gamma, Timer speed, and Minecraft click fields to vanilla defaults
        if (mc != null && mc.gameSettings != null && mc.gameSettings.gammaSetting > 10.0F) {
            mc.gameSettings.gammaSetting = 1.0F;
        }
        Timer.setTimerSpeed(1.0F);

        if (mc != null) {
            try {
                MappingResolver.setFieldValue(mc, 4, mc.getClass(), MappingResolver.MINECRAFT_RIGHT_CLICK_DELAY_TIMER);
                MappingResolver.setFieldValue(mc, 0, mc.getClass(), MappingResolver.MINECRAFT_LEFT_CLICK_COUNTER);
            } catch (Exception ignored) {
            }
        }

        // 4. Clear runtime caches and disable self
        FriendManager.clearFriends();
        CPSTracker.clear();
        this.setState(false);

        // 5. Unhook client instance and release references
        if (Cigarette.INSTANCE != null) {
            Cigarette.INSTANCE.destruct();
        }

        // 6. Request immediate garbage collection
        System.gc();
    }
}
