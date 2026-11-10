package dev.taezeem14.cigarette.module.impl.utility;

import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class AntiBot extends Module {

    private static AntiBot instance;

    private final BooleanSetting tabCheck = new BooleanSetting("Tab Check", true);
    private final BooleanSetting pingCheck = new BooleanSetting("Ping Check", false);
    private final NumberSetting minTicksExisted = new NumberSetting("Ticks Existed", 40.0, 10.0, 100.0, 5.0);
    private final BooleanSetting invisibleCheck = new BooleanSetting("Invisible Check", true);
    private final BooleanSetting duplicateUuid = new BooleanSetting("Duplicate UUID", true);

    public AntiBot() {
        super("AntiBot", "Filters out server anticheat bot entities", Category.UTILITY);
        addSettings(tabCheck, pingCheck, minTicksExisted, invisibleCheck, duplicateUuid);
        instance = this;
    }

    @Override
    public String getSuffix() {
        return tabCheck.isEnabled() ? "Tab" : minTicksExisted.getValueInt() + "t";
    }

    public static boolean isBot(Entity entity) {
        if (entity == null) return false;
        if (entity.getId() == -1337) return true;
        if (instance == null || !instance.isEnabled() || !(entity instanceof Player)) {
            return false;
        }

        Player player = (Player) entity;
        if (player == mc.player) return false;

        if (player.tickCount < instance.minTicksExisted.getValueInt()) {
            return true;
        }

        if (instance.invisibleCheck.isEnabled() && player.isInvisible() && mc.player != null) {
            if (player.getY() > mc.player.getY() + 2.0 && mc.player.distanceTo(player) < 6.0F) {
                return true;
            }
        }

        if ((instance.tabCheck.isEnabled() || instance.pingCheck.isEnabled()) && mc.getConnection() != null) {
            PlayerInfo info = mc.getConnection().getPlayerInfo(player.getUUID());
            if (instance.tabCheck.isEnabled() && info == null) {
                return true;
            }
            if (instance.pingCheck.isEnabled() && !mc.hasSingleplayerServer() && info != null && info.getLatency() <= 0) {
                return true;
            }
        }

        if (instance.duplicateUuid.isEnabled() && mc.level != null) {
            UUID uuid = player.getUUID();
            int count = 0;
            for (Player other : mc.level.players()) {
                if (other.getUUID().equals(uuid)) {
                    count++;
                    if (count > 1) return true;
                }
            }
        }
        return false;
    }
}
