package dev.taezeem14.cigarette.module.impl.utility;

import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class FastPlace extends Module {

    private static FastPlace instance;

    private final NumberSetting delay = new NumberSetting("Delay", 0.0, 0.0, 4.0, 1.0);
    private final BooleanSetting blocksOnly = new BooleanSetting("Blocks Only", true);
    private final BooleanSetting projectilesOnly = new BooleanSetting("Projectiles Only", false);

    public FastPlace() {
        super("FastPlace", "Removes right-click block placement and throwing delay", Category.UTILITY);
        addSettings(delay, blocksOnly, projectilesOnly);
        instance = this;
    }

    @Override
    public String getSuffix() {
        return String.valueOf(delay.getValueInt());
    }

    public static int modifyRightClickDelay(int vanillaDelay) {
        if (instance == null || !instance.isEnabled() || mc.player == null) {
            return vanillaDelay;
        }
        ItemStack held = mc.player.getMainHandItem();
        if (held.isEmpty()) return vanillaDelay;

        boolean wantBlocks = instance.blocksOnly.isEnabled();
        boolean wantProj = instance.projectilesOnly.isEnabled();
        boolean isBlock = held.getItem() instanceof BlockItem;
        boolean isProj = held.is(Items.EGG) || held.is(Items.SNOWBALL)
                || held.is(Items.ENDER_PEARL) || held.is(Items.EXPERIENCE_BOTTLE);

        if (wantBlocks && wantProj) {
            if (!isBlock && !isProj) return vanillaDelay;
        } else if (wantBlocks && !isBlock) {
            return vanillaDelay;
        } else if (wantProj && !isProj) {
            return vanillaDelay;
        }
        return Math.min(vanillaDelay, instance.delay.getValueInt());
    }
}
