package dev.taezeem14.cigarette.module.impl.utility;

import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

public class Teams extends Module {

    private static Teams instance;

    private final BooleanSetting scoreboardTeam = new BooleanSetting("Scoreboard Team", true);
    private final BooleanSetting leatherColor = new BooleanSetting("Leather Armor Color", true);

    public Teams() {
        super("Teams", "Prevents targeting and attacking team members", Category.UTILITY);
        addSettings(scoreboardTeam, leatherColor);
        instance = this;
    }

    @Override
    public String getSuffix() {
        return scoreboardTeam.isEnabled() ? "Scoreboard" : "Armor";
    }

    public static boolean isTeammate(Player player) {
        if (instance == null || !instance.isEnabled() || player == null || mc.player == null) {
            return false;
        }
        if (player == mc.player) return true;

        if (instance.scoreboardTeam.isEnabled()) {
            if (mc.player.isAlliedTo(player)) {
                return true;
            }
            Integer myCol = mc.player.getDisplayName() != null && mc.player.getDisplayName().getStyle().getColor() != null
                    ? mc.player.getDisplayName().getStyle().getColor().getValue() : null;
            Integer otherCol = player.getDisplayName() != null && player.getDisplayName().getStyle().getColor() != null
                    ? player.getDisplayName().getStyle().getColor().getValue() : null;
            if (myCol != null && myCol.equals(otherCol)) {
                return true;
            }
        }

        if (instance.leatherColor.isEnabled()) {
            ItemStack myChest = mc.player.getItemBySlot(EquipmentSlot.CHEST);
            ItemStack otherChest = player.getItemBySlot(EquipmentSlot.CHEST);
            if (!myChest.isEmpty() && !otherChest.isEmpty()
                    && myChest.has(DataComponents.DYED_COLOR) && otherChest.has(DataComponents.DYED_COLOR)) {
                int c1 = DyedItemColor.getOrDefault(myChest, -1);
                int c2 = DyedItemColor.getOrDefault(otherChest, -2);
                if (c1 != -1 && c1 == c2) {
                    return true;
                }
            }
        }
        return false;
    }
}
