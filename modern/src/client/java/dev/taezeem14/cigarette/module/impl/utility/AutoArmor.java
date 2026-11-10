package dev.taezeem14.cigarette.module.impl.utility;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.TickEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.DoubleSliderSetting;
import dev.taezeem14.cigarette.util.math.TimerUtil;
import dev.taezeem14.cigarette.util.player.InventoryUtil;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.EquipmentSlot;

public class AutoArmor extends Module {

    private final DoubleSliderSetting delay = new DoubleSliderSetting("Delay", 60.0, 120.0, 20.0, 300.0, 5.0);
    private final BooleanSetting openInvOnly = new BooleanSetting("Open Inv Only", false);

    private final TimerUtil timer = new TimerUtil();
    private long currentDelay = 80;

    public AutoArmor() {
        super("AutoArmor", "Automatically equips the strongest armor in your inventory", Category.UTILITY);
        addSettings(delay, openInvOnly);
    }

    @Override
    public String getSuffix() {
        return String.format("%.0f-%.0f", delay.getValueMin(), delay.getValueMax());
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!event.isPre() || mc.player == null) return;
        if (openInvOnly.isEnabled() && !(mc.gui.screen() instanceof InventoryScreen)) return;
        if (!timer.hasTimeElapsed(currentDelay)) return;

        for (EquipmentSlot slot : InventoryUtil.ARMOR_SLOTS) {
            int bestSlot = InventoryUtil.findBestArmorSlot(slot);
            if (bestSlot != -1) {
                InventoryUtil.shiftClick(bestSlot);
                currentDelay = (long) delay.getRandomValue();
                timer.reset();
                return;
            }
        }
    }
}
