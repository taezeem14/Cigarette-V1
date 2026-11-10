package dev.taezeem14.cigarette.module.impl.utility;

import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;

public class Teams extends Module {

    private static Teams instance;

    private final BooleanSetting scoreboardTeam = new BooleanSetting("Scoreboard Team", true);
    private final BooleanSetting leatherColor = new BooleanSetting("Leather Armor Color", true);

    public Teams() {
        super("Teams", "Prevents targeting and attacking team members", Category.UTILITY);
        addSettings(scoreboardTeam, leatherColor);
        instance = this;
    }

    public static boolean isTeammate(EntityPlayer player) {
        if (instance == null || !instance.isEnabled() || player == null || mc.thePlayer == null) {
            return false;
        }

        if (player == mc.thePlayer) return true;

        // 1. Scoreboard team check
        if (instance.scoreboardTeam.isEnabled()) {
            if (mc.thePlayer.getTeam() != null && player.getTeam() != null) {
                if (mc.thePlayer.getTeam().isSameTeam(player.getTeam())) {
                    return true;
                }
            }

            // Check prefix color code matching (e.g. §c vs §a)
            String myPrefix = mc.thePlayer.getDisplayName().getFormattedText();
            String otherPrefix = player.getDisplayName().getFormattedText();
            if (myPrefix.length() >= 2 && otherPrefix.length() >= 2) {
                if (myPrefix.startsWith("§") && otherPrefix.startsWith("§")) {
                    if (myPrefix.charAt(1) == otherPrefix.charAt(1)) {
                        return true;
                    }
                }
            }
        }

        // 2. Leather armor color check
        if (instance.leatherColor.isEnabled()) {
            ItemStack myChest = mc.thePlayer.inventory.armorInventory[2];
            ItemStack otherChest = player.inventory.armorInventory[2];

            if (myChest != null && otherChest != null
                    && myChest.getItem() instanceof ItemArmor && otherChest.getItem() instanceof ItemArmor) {
                ItemArmor myArmor = (ItemArmor) myChest.getItem();
                ItemArmor otherArmor = (ItemArmor) otherChest.getItem();

                if (myArmor.hasColor(myChest) && otherArmor.hasColor(otherChest)) {
                    if (myArmor.getColor(myChest) == otherArmor.getColor(otherChest)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static Teams getInstance() {
        return instance;
    }
}
