package dev.taezeem14.cigarette.module.impl.utility;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.TickEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.util.mapping.MappingResolver;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemEgg;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemExpBottle;
import net.minecraft.item.ItemSnowball;
import net.minecraft.item.ItemStack;

public class FastPlace extends Module {

    private final NumberSetting delay = new NumberSetting("Delay", 0.0, 0.0, 4.0, 1.0);
    private final BooleanSetting blocksOnly = new BooleanSetting("Blocks Only", true);
    private final BooleanSetting projectilesOnly = new BooleanSetting("Projectiles Only", false);

    public FastPlace() {
        super("FastPlace", "Removes right-click block placement and throwing delay", Category.UTILITY);
        addSettings(delay, blocksOnly, projectilesOnly);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!event.isPre() || mc.thePlayer == null) return;

        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null) return;

        if (blocksOnly.isEnabled() && !(held.getItem() instanceof ItemBlock)) {
            return;
        }

        if (projectilesOnly.isEnabled()) {
            boolean isProj = held.getItem() instanceof ItemEgg || held.getItem() instanceof ItemSnowball
                    || held.getItem() instanceof ItemEnderPearl || held.getItem() instanceof ItemExpBottle;
            if (!isProj) return;
        }

        try {
            int currentDelay = MappingResolver.getFieldValue(mc, mc.getClass(), MappingResolver.MINECRAFT_RIGHT_CLICK_DELAY_TIMER);
            if (currentDelay > delay.getValueInt()) {
                MappingResolver.setFieldValue(mc, delay.getValueInt(), mc.getClass(), MappingResolver.MINECRAFT_RIGHT_CLICK_DELAY_TIMER);
            }
        } catch (Exception ignored) {
        }
    }
}
