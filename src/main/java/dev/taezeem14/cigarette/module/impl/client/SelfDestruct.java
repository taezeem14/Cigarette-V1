package dev.taezeem14.cigarette.module.impl.client;

import dev.taezeem14.cigarette.Cigarette;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.module.impl.blatant.Timer;
import dev.taezeem14.cigarette.util.mapping.MappingResolver;

public class SelfDestruct extends Module {

    public SelfDestruct() {
        super("SelfDestruct", "Completely unhooks the client and removes all traces from memory", Category.CLIENT);
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer == null) return;

        // 1. Close active screens
        mc.displayGuiScreen(null);

        // 2. Disable all other modules
        if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getModuleManager() != null) {
            for (Module module : Cigarette.INSTANCE.getModuleManager().getModules()) {
                if (module != this && module.isEnabled()) {
                    module.setState(false);
                }
            }
        }

        // 3. Restore all modified Minecraft fields to default
        Timer.setTimerSpeed(1.0F);

        try {
            MappingResolver.setFieldValue(mc, 4, mc.getClass(), MappingResolver.MINECRAFT_RIGHT_CLICK_DELAY_TIMER);
            MappingResolver.setFieldValue(mc, 0, mc.getClass(), MappingResolver.MINECRAFT_LEFT_CLICK_COUNTER);
        } catch (Exception ignored) {
        }

        // 4. Unregister everything from EventBus
        if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getEventBus() != null) {
            Cigarette.INSTANCE.getEventBus().unregister(this);
        }

        // 5. Clear friend and setting caches
        FriendManager.clearFriends();

        // 6. Disable self
        this.setState(false);

        // 7. Request immediate garbage collection
        System.gc();
    }
}
