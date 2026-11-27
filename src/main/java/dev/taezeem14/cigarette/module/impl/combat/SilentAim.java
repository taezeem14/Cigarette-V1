package dev.taezeem14.cigarette.module.impl.combat;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.MotionEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.module.impl.client.FriendManager;
import dev.taezeem14.cigarette.module.impl.utility.AntiBot;
import dev.taezeem14.cigarette.module.impl.utility.Teams;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.util.math.RandomUtil;
import dev.taezeem14.cigarette.util.math.RotationUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;

public class SilentAim extends Module {

    private static SilentAim instance;
    private static float[] activeRotations;

    private final NumberSetting fov = new NumberSetting("FOV", 90.0, 15.0, 180.0, 5.0);
    private final NumberSetting distance = new NumberSetting("Distance", 4.2, 3.0, 6.0, 0.1);
    private final NumberSetting hitChance = new NumberSetting("Hit Chance", 100.0, 0.0, 100.0, 1.0);
    private final BooleanSetting targetMobs = new BooleanSetting("Mobs", true);
    private final ModeSetting movementFix = new ModeSetting("Movement Fix", "Silent", "Silent", "Strict", "None");

    public SilentAim() {
        super("SilentAim", "Silently aims at targets without rotating the client camera", Category.COMBAT);
        addSettings(fov, distance, hitChance, targetMobs, movementFix);
        instance = this;
    }

    @Override
    public String getSuffix() {
        return movementFix.getMode();
    }

    @Override
    public void onEnable() {
        activeRotations = null;
    }

    @Override
    public void onDisable() {
        activeRotations = null;
    }

    @EventTarget
    public void onMotion(MotionEvent event) {
        if (!event.isPre()) {
            return;
        }

        if (mc.thePlayer == null || mc.theWorld == null) {
            activeRotations = null;
            return;
        }

        EntityLivingBase target = getBestTarget();
        if (target == null) {
            activeRotations = null;
            return;
        }

        if (!RandomUtil.chance(hitChance.getValue())) {
            activeRotations = null;
            return;
        }

        float[] targetRotations = RotationUtil.getRotationsToEntity(target);
        float[] base = activeRotations != null
                ? activeRotations
                : new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch};
        float[] fixedRotations = RotationUtil.applySensitivityGCD(base, targetRotations, mc.gameSettings.mouseSensitivity);

        activeRotations = fixedRotations;
        event.setYaw(fixedRotations[0]);
        event.setPitch(fixedRotations[1]);
    }

    public static float[] getActiveRotations() {
        if (instance == null || !instance.isEnabled()) {
            return null;
        }
        return activeRotations;
    }

    public static boolean isMovementFixEnabled() {
        return instance != null && instance.isEnabled() && !instance.movementFix.is("None");
    }

    public static boolean isStrictMovementFix() {
        return instance != null && instance.isEnabled() && instance.movementFix.is("Strict");
    }

    private EntityLivingBase getBestTarget() {
        EntityLivingBase best = null;
        double bestDist = distance.getValue();

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityLivingBase) || entity == mc.thePlayer) {
                continue;
            }

            EntityLivingBase living = (EntityLivingBase) entity;
            if (living.getEntityId() == -1337 || living instanceof EntityArmorStand) {
                continue;
            }

            if (living.isDead || living.getHealth() <= 0.0F || living.isInvisible()) {
                continue;
            }

            if (living instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) living;
                if (AntiBot.isBot(player)) continue;
                if (FriendManager.isFriend(player.getName())) continue;
                if (Teams.isTeammate(player)) continue;
            } else if (!targetMobs.isEnabled()) {
                continue;
            }

            double dist = mc.thePlayer.getDistanceToEntity(living);
            if (dist > bestDist) {
                continue;
            }

            if (!RotationUtil.isEntityInFov(living, fov.getValueFloat())) {
                continue;
            }

            best = living;
            bestDist = dist;
        }

        return best;
    }
}
