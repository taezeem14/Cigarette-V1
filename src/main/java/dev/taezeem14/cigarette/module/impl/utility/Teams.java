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

    @Override
    public String getSuffix() {
        return scoreboardTeam.isEnabled() ? "Scoreboard" : "Armor";
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

            // Check color code immediately formatting the player's name (skipping §r reset and style codes)
            char myColor = extractNameColorCode(mc.thePlayer.getDisplayName().getFormattedText(), mc.thePlayer.getName());
            char otherColor = extractNameColorCode(player.getDisplayName().getFormattedText(), player.getName());
            if (myColor != '\0' && myColor == otherColor) {
                return true;
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

    private static char extractNameColorCode(String formatted, String rawName) {
        if (formatted == null || rawName == null) return '\0';
        int nameIdx = formatted.indexOf(rawName);
        int scanEnd = nameIdx >= 0 ? nameIdx : formatted.length();
        char activeColor = '\0';
        for (int i = 0; i < scanEnd - 1; i++) {
            if (formatted.charAt(i) == '\u00a7') {
                char code = Character.toLowerCase(formatted.charAt(i + 1));
                if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) {
                    activeColor = code;
                } else if (code == 'r') {
                    activeColor = '\0';
                }
                i++;
            }
        }
        return activeColor;
    }

    public static Teams getInstance() {
        return instance;
    }
}
