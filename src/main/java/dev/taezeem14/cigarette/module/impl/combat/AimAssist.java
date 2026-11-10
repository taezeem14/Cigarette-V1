package dev.taezeem14.cigarette.module.impl.combat;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.Render3DEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.module.impl.client.FriendManager;
import dev.taezeem14.cigarette.module.impl.utility.AntiBot;
import dev.taezeem14.cigarette.module.impl.utility.Teams;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.util.math.RandomUtil;
import dev.taezeem14.cigarette.util.math.RotationUtil;
import dev.taezeem14.cigarette.util.render.AnimationUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
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

    private float pendingYawDelta = 0.0F;
    private float pendingPitchDelta = 0.0F;

    public AimAssist() {
        super("AimAssist", "Smoothly assists aiming towards opponents", Category.COMBAT);
        addSettings(horizontalSpeed, verticalSpeed, fov, distance, clickAim, weaponOnly, aimVertically, multipoint, ignoreTeams, breakBlocksCheck);
    }

    @Override
    public String getSuffix() {
        return String.format("%.1f", horizontalSpeed.getValue());
    }

    @Override
    public void onDisable() {
        resetPendingDeltas();
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) {
            resetPendingDeltas();
            return;
        }

        int attackKey = mc.gameSettings.keyBindAttack.getKeyCode();
        if (clickAim.isEnabled() && !RotationUtil.isPhysicalKeyDown(attackKey)) {
            resetPendingDeltas();
            return;
        }

        if (weaponOnly.isEnabled()) {
            ItemStack held = mc.thePlayer.getHeldItem();
            if (held == null || !(held.getItem() instanceof ItemSword || held.getItem() instanceof ItemAxe)) {
                resetPendingDeltas();
                return;
            }
        }

        if (breakBlocksCheck.isEnabled() && mc.objectMouseOver != null
                && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                && RotationUtil.isPhysicalKeyDown(attackKey)) {
            resetPendingDeltas();
            return;
        }

        EntityPlayer target = getBestTarget();
        if (target == null) {
            resetPendingDeltas();
            return;
        }

        float pt = event.getPartialTicks();
        double interpX = target.lastTickPosX + (target.posX - target.lastTickPosX) * pt;
        double interpY = target.lastTickPosY + (target.posY - target.lastTickPosY) * pt;
        double interpZ = target.lastTickPosZ + (target.posZ - target.lastTickPosZ) * pt;

        Vec3 targetPoint;
        if (multipoint.isEnabled()) {
            AxisAlignedBB interpBox = target.getEntityBoundingBox().offset(
                    interpX - target.posX,
                    interpY - target.posY,
                    interpZ - target.posZ
            );
            targetPoint = RotationUtil.getClosestPointOnAABB(interpBox, 0.15, 0.85);
        } else {
            targetPoint = new Vec3(interpX, interpY + target.getEyeHeight() * 0.85, interpZ);
        }

        float[] targetRotations = RotationUtil.getRotationsToVec(targetPoint);
        float deltaSeconds = AnimationUtil.getDeltaTime();

        float yawDiff = RotationUtil.wrapAngleTo180(targetRotations[0] - mc.thePlayer.rotationYaw);
        float jitterScale = Math.min(1.0F, Math.abs(yawDiff) / 6.0F);
        float noiseYaw = (float) RandomUtil.nextGaussian(0.0, 0.08 * jitterScale);

        float smoothYaw = RotationUtil.smoothSigmoid(
                mc.thePlayer.rotationYaw,
                targetRotations[0] + noiseYaw,
                horizontalSpeed.getValueFloat(),
                deltaSeconds
        );
        float desiredYawStep = (smoothYaw - mc.thePlayer.rotationYaw) + pendingYawDelta;
        float fixedYaw = RotationUtil.applySensitivityGCD(
                mc.thePlayer.rotationYaw,
                mc.thePlayer.rotationYaw + desiredYawStep,
                mc.gameSettings.mouseSensitivity
        );
        pendingYawDelta = MathHelper.clamp_float((mc.thePlayer.rotationYaw + desiredYawStep) - fixedYaw, -2.0F, 2.0F);
        mc.thePlayer.rotationYaw = fixedYaw;

        if (aimVertically.isEnabled()) {
            float pitchDiff = targetRotations[1] - mc.thePlayer.rotationPitch;
            float pitchJitterScale = Math.min(1.0F, Math.abs(pitchDiff) / 6.0F);
            float noisePitch = (float) RandomUtil.nextGaussian(0.0, 0.05 * pitchJitterScale);

            float smoothPitch = RotationUtil.smoothSigmoid(
                    mc.thePlayer.rotationPitch,
                    targetRotations[1] + noisePitch,
                    verticalSpeed.getValueFloat(),
                    deltaSeconds
            );
            float desiredPitchStep = (smoothPitch - mc.thePlayer.rotationPitch) + pendingPitchDelta;
            float fixedPitch = RotationUtil.applySensitivityGCD(
                    mc.thePlayer.rotationPitch,
                    mc.thePlayer.rotationPitch + desiredPitchStep,
                    mc.gameSettings.mouseSensitivity
            );
            pendingPitchDelta = MathHelper.clamp_float((mc.thePlayer.rotationPitch + desiredPitchStep) - fixedPitch, -2.0F, 2.0F);
            mc.thePlayer.rotationPitch = MathHelper.clamp_float(fixedPitch, -90.0F, 90.0F);
        }
    }

    private void resetPendingDeltas() {
        pendingYawDelta = 0.0F;
        pendingPitchDelta = 0.0F;
    }

    private EntityPlayer getBestTarget() {
        EntityPlayer best = null;
        double bestDist = distance.getValue();

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityPlayer) || entity == mc.thePlayer) {
                continue;
            }

            EntityPlayer player = (EntityPlayer) entity;
            if (player.getEntityId() == -1337 || AntiBot.isBot(player)) {
                continue;
            }

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
