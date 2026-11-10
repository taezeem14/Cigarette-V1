package dev.taezeem14.cigarette.module.impl.combat;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.TickEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.module.impl.client.FriendManager;
import dev.taezeem14.cigarette.module.impl.utility.Teams;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.util.math.RandomUtil;
import dev.taezeem14.cigarette.util.math.RotationUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

public class AimAssist extends Module {

    private final NumberSetting horizontalSpeed = new NumberSetting("Horizontal Speed", 3.5, 1.0, 10.0, 0.1);
    private final NumberSetting verticalSpeed = new NumberSetting("Vertical Speed", 2.0, 1.0, 10.0, 0.1);
    private final NumberSetting fov = new NumberSetting("FOV", 90.0, 15.0, 360.0, 5.0);
    private final NumberSetting distance = new NumberSetting("Distance", 4.5, 3.0, 6.0, 0.1);
    private final BooleanSetting clickAim = new BooleanSetting("Click Aim", true);
    private final BooleanSetting weaponOnly = new BooleanSetting("Weapon Only", true);
    private final BooleanSetting aimVertically = new BooleanSetting("Aim Vertically", false);
    private final BooleanSetting multipoint = new BooleanSetting("Multipoint Hitbox", true);
    private final BooleanSetting ignoreTeams = new BooleanSetting("Ignore Teams", true);
    private final BooleanSetting breakBlocksCheck = new BooleanSetting("Break Blocks Check", true);

    public AimAssist() {
        super("AimAssist", "Smoothly assists aiming towards opponents", Category.COMBAT);
        addSettings(horizontalSpeed, verticalSpeed, fov, distance, clickAim, weaponOnly, aimVertically, multipoint, ignoreTeams, breakBlocksCheck);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!event.isPre() || mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) {
            return;
        }

        if (clickAim.isEnabled() && !mc.gameSettings.keyBindAttack.isKeyDown()) {
            return;
        }

        if (weaponOnly.isEnabled()) {
            ItemStack held = mc.thePlayer.getHeldItem();
            if (held == null || !(held.getItem() instanceof ItemSword || held.getItem() instanceof ItemAxe)) {
                return;
            }
        }

        if (breakBlocksCheck.isEnabled() && mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            if (mc.gameSettings.keyBindAttack.isKeyDown()) {
                return;
            }
        }

        EntityPlayer target = getBestTarget();
        if (target == null) {
            return;
        }

        Vec3 targetPoint;
        if (multipoint.isEnabled()) {
            targetPoint = RotationUtil.getClosestPointOnAABB(target.getEntityBoundingBox(), 0.15, 0.85);
        } else {
            targetPoint = new Vec3(target.posX, target.posY + target.getEyeHeight() * 0.85, target.posZ);
        }

        float[] targetRotations = RotationUtil.getRotationsToVec(targetPoint);

        // Box-Muller Gaussian micro-jitter to simulate natural wrist acceleration
        float noiseYaw = (float) RandomUtil.nextGaussian(0.0, 0.12);
        float noisePitch = (float) RandomUtil.nextGaussian(0.0, 0.08);

        float smoothYaw = RotationUtil.smoothSigmoid(mc.thePlayer.rotationYaw, targetRotations[0] + noiseYaw, horizontalSpeed.getValueFloat());
        float fixedYaw = RotationUtil.applySensitivityGCD(mc.thePlayer.rotationYaw, smoothYaw, mc.gameSettings.mouseSensitivity);

        mc.thePlayer.rotationYaw = fixedYaw;

        if (aimVertically.isEnabled()) {
            float smoothPitch = RotationUtil.smoothSigmoid(mc.thePlayer.rotationPitch, targetRotations[1] + noisePitch, verticalSpeed.getValueFloat());
            float fixedPitch = RotationUtil.applySensitivityGCD(mc.thePlayer.rotationPitch, smoothPitch, mc.gameSettings.mouseSensitivity);
            mc.thePlayer.rotationPitch = Math.max(-90.0F, Math.min(90.0F, fixedPitch));
        }
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

            if (ignoreTeams.isEnabled() && Teams.isTeammate(player)) {
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
