package dev.taezeem14.cigarette.module.impl.utility;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.TickEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.DoubleSliderSetting;
import dev.taezeem14.cigarette.util.math.TimerUtil;
import dev.taezeem14.cigarette.util.player.InventoryUtil;
import net.minecraft.client.gui.inventory.GuiInventory;

public class AutoArmor extends Module {

    private final DoubleSliderSetting delay = new DoubleSliderSetting("Delay", 60.0, 120.0, 20.0, 300.0, 5.0);
    private final BooleanSetting openInvOnly = new BooleanSetting("Open Inv Only", false);

    private final TimerUtil timer = new TimerUtil();
    private long currentDelay = 80;

    public AutoArmor() {
        super("AutoArmor", "Automatically equips the strongest armor in your inventory", Category.UTILITY);
        addSettings(delay, openInvOnly);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!event.isPre() || mc.thePlayer == null) return;

        if (openInvOnly.isEnabled() && !(mc.currentScreen instanceof GuiInventory)) {
            return;
        }

        if (!timer.hasTimeElapsed(currentDelay)) {
            return;
        }

        for (int type = 0; type < 4; type++) {
            int bestSlot = InventoryUtil.findBestArmor(type);
            if (bestSlot != -1) {
                InventoryUtil.shiftClick(bestSlot);
                currentDelay = (long) delay.getRandomValue();
                timer.reset();
                return;
            }
        }
    }
}
