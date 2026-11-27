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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;

public class SilentAim extends Module {

    private static SilentAim instance;
    private static float[] activeRotations;

    private final NumberSetting fov = new NumberSetting("FOV", 90.0, 15.0, 180.0, 5.0);
    private final NumberSetting distance = new NumberSetting("Distance", 4.2, 3.0, 6.0, 0.1);
    private final NumberSetting hitChance = new NumberSetting("Hit Chance", 100.0, 0.0, 100.0, 1.0);
    private final BooleanSetting targetMobs = new BooleanSetting("Mobs", true);
    private final ModeSetting movementFix = new ModeSetting("Movement Fix", "Silent", "Silent", "Strict", "None");

    public SilentAim() {
        super("SilentAim", "Silently aims server-side rotations without moving the camera", Category.COMBAT);
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
        if (!event.isPre()) return;
        if (mc.player == null || mc.level == null) {
            activeRotations = null;
            return;
        }

        LivingEntity target = getBestTarget();
        if (target == null || !RandomUtil.chance(hitChance.getValue())) {
            activeRotations = null;
            return;
        }

        float[] targetRots = RotationUtil.getRotationsToEntity(target);
        float[] base = activeRotations != null ? activeRotations : new float[]{mc.player.getYRot(), mc.player.getXRot()};
        float[] fixed = RotationUtil.applySensitivityGCD(base, targetRots, RotationUtil.getMouseSensitivity());
        activeRotations = fixed;
        event.setYaw(fixed[0]);
        event.setPitch(fixed[1]);
    }

    public static float[] getActiveRotations() {
        if (instance == null || !instance.isEnabled()) return null;
        return activeRotations;
    }

    public static boolean isMovementFixEnabled() {
        return instance != null && instance.isEnabled() && !instance.movementFix.is("None");
    }

    public static boolean isStrictMovementFix() {
        return instance != null && instance.isEnabled() && instance.movementFix.is("Strict");
    }

    private LivingEntity getBestTarget() {
        LivingEntity best = null;
        double bestDist = distance.getValue();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || living == mc.player) continue;
            if (living instanceof ArmorStand || living.getId() == -1337) continue;
            if (!living.isAlive() || living.isInvisible()) continue;

            if (living instanceof Player player) {
                if (AntiBot.isBot(player)) continue;
                if (FriendManager.isFriend(player.getName().getString())) continue;
                if (Teams.isTeammate(player)) continue;
            } else if (!targetMobs.isEnabled()) {
                continue;
            }

            double dist = mc.player.distanceTo(living);
            if (dist > bestDist) continue;
            if (!RotationUtil.isEntityInFov(living, fov.getValueFloat())) continue;

            if (best instanceof Player && !(living instanceof Player) && dist >= bestDist * 0.75) {
                continue;
            }

            best = living;
            bestDist = dist;
        }
        return best;
    }
}
