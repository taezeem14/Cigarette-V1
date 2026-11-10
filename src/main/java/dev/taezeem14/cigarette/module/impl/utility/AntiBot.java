package dev.taezeem14.cigarette.module.impl.utility;

import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import java.util.UUID;

public class AntiBot extends Module {

    private static AntiBot instance;

    private final BooleanSetting tabCheck = new BooleanSetting("Tab Check", true);
    private final BooleanSetting pingCheck = new BooleanSetting("Ping Check", false);
    private final NumberSetting minTicksExisted = new NumberSetting("Ticks Existed", 40.0, 10.0, 100.0, 5.0);
    private final BooleanSetting invisibleCheck = new BooleanSetting("Invisible Check", true);
    private final BooleanSetting duplicateUuid = new BooleanSetting("Duplicate UUID", true);

    public AntiBot() {
        super("AntiBot", "Filters out Watchdog and anticheat bot entities", Category.UTILITY);
        addSettings(tabCheck, pingCheck, minTicksExisted, invisibleCheck, duplicateUuid);
        instance = this;
    }

    @Override
    public String getSuffix() {
        return tabCheck.isEnabled() ? "Tab" : minTicksExisted.getValueInt() + "t";
    }

    public static boolean isBot(Entity entity) {
        if (entity == null) {
            return false;
        }

        // Always treat Blink's local clone (-1337) as an ignored/bot entity
        if (entity.getEntityId() == -1337) {
            return true;
        }

        if (instance == null || !instance.isEnabled() || !(entity instanceof EntityPlayer)) {
            return false;
        }

        EntityPlayer player = (EntityPlayer) entity;

        if (player == mc.thePlayer) return false;

        // Min ticks check
        if (player.ticksExisted < instance.minTicksExisted.getValueInt()) {
            return true;
        }

        // Invisible hovering bot check
        if (instance.invisibleCheck.isEnabled() && player.isInvisible() && mc.thePlayer != null) {
            if (player.posY > mc.thePlayer.posY + 2.0D && mc.thePlayer.getDistanceToEntity(player) < 6.0D) {
                return true;
            }
        }

        // Tab list and optional ping check
        if ((instance.tabCheck.isEnabled() || instance.pingCheck.isEnabled()) && mc.getNetHandler() != null) {
            NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(player.getUniqueID());
            if (instance.tabCheck.isEnabled() && info == null) {
                return true;
            }
            if (instance.pingCheck.isEnabled() && !mc.isSingleplayer() && info != null && info.getResponseTime() <= 0) {
                return true;
            }
        }

        // Duplicate UUID check
        if (instance.duplicateUuid.isEnabled() && mc.theWorld != null) {
            UUID uuid = player.getUniqueID();
            int count = 0;
            for (Entity e : mc.theWorld.loadedEntityList) {
                if (e instanceof EntityPlayer && e.getUniqueID().equals(uuid)) {
                    count++;
                    if (count > 1) return true;
                }
            }
        }

        return false;
    }

    public static AntiBot getInstance() {
        return instance;
    }
}
