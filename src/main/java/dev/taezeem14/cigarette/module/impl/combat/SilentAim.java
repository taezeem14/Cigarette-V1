package dev.taezeem14.cigarette.module.impl.combat;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.MotionEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.module.impl.client.FriendManager;
import dev.taezeem14.cigarette.module.impl.utility.Teams;
import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.util.math.RandomUtil;
import dev.taezeem14.cigarette.util.math.RotationUtil;
import dev.taezeem14.cigarette.util.player.MoveUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public class SilentAim extends Module {

    private final NumberSetting fov = new NumberSetting("FOV", 90.0, 15.0, 180.0, 5.0);
    private final NumberSetting distance = new NumberSetting("Distance", 4.2, 3.0, 6.0, 0.1);
    private final NumberSetting hitChance = new NumberSetting("Hit Chance", 100.0, 0.0, 100.0, 1.0);
    private final ModeSetting movementFix = new ModeSetting("Movement Fix", "Silent", "Silent", "Strict");

    public SilentAim() {
        super("SilentAim", "Silently aims at targets without rotating the client camera", Category.COMBAT);
        addSettings(fov, distance, hitChance, movementFix);
    }

    @EventTarget
    public void onMotion(MotionEvent event) {
        if (!event.isPre() || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        EntityPlayer target = getBestTarget();
        if (target == null) {
            return;
        }

        if (!RandomUtil.chance(hitChance.getValue())) {
            return;
        }

        float[] rotations = RotationUtil.getRotationsToEntity(target);
        event.setYaw(rotations[0]);
        event.setPitch(rotations[1]);

        // Fix movement so server-side spoofed rotations do not cause desync
        MoveUtil.fixMovement(rotations[0]);
    }

    private EntityPlayer getBestTarget() {
        EntityPlayer best = null;
        double bestDist = distance.getValue();

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityPlayer) || entity == mc.thePlayer) {
                continue;
            }

            EntityPlayer player = (EntityPlayer) entity;
            if (player.isDead || player.getHealth() <= 0.0F || player.isInvisible()) {
                continue;
            }

            if (FriendManager.isFriend(player.getName())) {
                continue;
            }

            if (Teams.isTeammate(player)) {
                continue;
            }

            double dist = mc.thePlayer.getDistanceToEntity(player);
            if (dist > bestDist) {
                continue;
            }

            if (!RotationUtil.isEntityInFov(player, fov.getValueFloat())) {
                continue;
            }

            best = player;
            bestDist = dist;
        }

        return best;
    }
}
