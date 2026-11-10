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
import dev.taezeem14.cigarette.util.player.InventoryUtil;
import dev.taezeem14.cigarette.util.render.AnimationUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

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
        pendingYawDelta = 0.0F;
        pendingPitchDelta = 0.0F;
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (mc.player == null || mc.level == null || mc.gui.screen() != null) {
            pendingYawDelta = 0.0F;
            pendingPitchDelta = 0.0F;
            return;
        }

        if (clickAim.isEnabled() && !RotationUtil.isPhysicalKeyDown(mc.options.keyAttack)) {
            pendingYawDelta = 0.0F;
            pendingPitchDelta = 0.0F;
            return;
        }

        if (weaponOnly.isEnabled()) {
            ItemStack held = mc.player.getMainHandItem();
            if (!InventoryUtil.isWeapon(held)) {
                pendingYawDelta = 0.0F;
                pendingPitchDelta = 0.0F;
                return;
            }
        }

        if (breakBlocksCheck.isEnabled() && mc.hitResult != null
                && mc.hitResult.getType() == HitResult.Type.BLOCK
                && RotationUtil.isPhysicalKeyDown(mc.options.keyAttack)) {
            pendingYawDelta = 0.0F;
            pendingPitchDelta = 0.0F;
            return;
        }

        Player target = getBestTarget();
        if (target == null) {
            pendingYawDelta = 0.0F;
            pendingPitchDelta = 0.0F;
            return;
        }

        float pt = event.getPartialTicks();
        double ix = target.xo + (target.getX() - target.xo) * pt;
        double iy = target.yo + (target.getY() - target.yo) * pt;
        double iz = target.zo + (target.getZ() - target.zo) * pt;

        Vec3 targetPoint;
        if (multipoint.isEnabled()) {
            AABB interpBox = target.getBoundingBox().move(ix - target.getX(), iy - target.getY(), iz - target.getZ());
            targetPoint = RotationUtil.getClosestPointOnAABB(interpBox, 0.15, 0.85);
        } else {
            targetPoint = new Vec3(ix, iy + target.getEyeHeight() * 0.85, iz);
        }

        float[] targetRotations = RotationUtil.getRotationsToVec(targetPoint);
        float deltaSeconds = AnimationUtil.getDeltaTime();
        float sens = RotationUtil.getMouseSensitivity();

        float curYaw = mc.player.getYRot();
        float curPitch = mc.player.getXRot();

        float yawDiff = RotationUtil.wrapAngleTo180(targetRotations[0] - curYaw);
        float noiseYaw = (float) RandomUtil.nextGaussian(0.0, 0.08 * Math.min(1.0F, Math.abs(yawDiff) / 6.0F));
        float smoothYaw = RotationUtil.smoothSigmoid(curYaw, targetRotations[0] + noiseYaw, horizontalSpeed.getValueFloat(), deltaSeconds);
        float desiredYawStep = (smoothYaw - curYaw) + pendingYawDelta;
        float fixedYaw = RotationUtil.applySensitivityGCD(curYaw, curYaw + desiredYawStep, sens);
        pendingYawDelta = Mth.clamp((curYaw + desiredYawStep) - fixedYaw, -2.0F, 2.0F);
        mc.player.setYRot(fixedYaw);

        if (aimVertically.isEnabled()) {
            float pitchDiff = targetRotations[1] - curPitch;
            float noisePitch = (float) RandomUtil.nextGaussian(0.0, 0.05 * Math.min(1.0F, Math.abs(pitchDiff) / 6.0F));
            float smoothPitch = RotationUtil.smoothSigmoid(curPitch, targetRotations[1] + noisePitch, verticalSpeed.getValueFloat(), deltaSeconds);
            float desiredPitchStep = (smoothPitch - curPitch) + pendingPitchDelta;
            float fixedPitch = RotationUtil.applySensitivityGCD(curPitch, curPitch + desiredPitchStep, sens);
            pendingPitchDelta = Mth.clamp((curPitch + desiredPitchStep) - fixedPitch, -2.0F, 2.0F);
            mc.player.setXRot(Mth.clamp(fixedPitch, -90.0F, 90.0F));
        }
    }

    private Player getBestTarget() {
        Player best = null;
        double bestDist = distance.getValue();
        for (Player player : mc.level.players()) {
            if (player == mc.player || !player.isAlive() || player.isInvisible()) continue;
            if (AntiBot.isBot(player)) continue;
            if (FriendManager.isFriend(player.getName().getString())) continue;
            if (ignoreTeams.isEnabled() && Teams.isTeammate(player)) continue;

            double dist = mc.player.distanceTo(player);
            if (dist > bestDist) continue;
            if (!RotationUtil.isEntityInFov(player, fov.getValueFloat())) continue;

            best = player;
            bestDist = dist;
        }
        return best;
    }
}
