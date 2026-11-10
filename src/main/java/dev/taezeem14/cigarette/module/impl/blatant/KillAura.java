package dev.taezeem14.cigarette.module.impl.blatant;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.MotionEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.module.impl.client.FriendManager;
import dev.taezeem14.cigarette.module.impl.utility.AntiBot;
import dev.taezeem14.cigarette.module.impl.utility.Teams;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.DoubleSliderSetting;
import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.util.math.RandomUtil;
import dev.taezeem14.cigarette.util.math.RotationUtil;
import dev.taezeem14.cigarette.util.math.TimerUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class KillAura extends Module {

    private static KillAura instance;
    private static float[] activeRotations;

    private final ModeSetting targetMode = new ModeSetting("Target Mode", "Single", "Single", "Switch", "Multi");
    private final ModeSetting sortMode = new ModeSetting("Sort Mode", "Distance", "Distance", "Health", "HurtTime", "FOV");
    private final DoubleSliderSetting aps = new DoubleSliderSetting("APS", 10.0, 14.0, 1.0, 20.0, 0.5);
    private final NumberSetting attackRange = new NumberSetting("Attack Range", 4.2, 3.0, 6.0, 0.1);
    private final NumberSetting swingRange = new NumberSetting("Swing Range", 4.5, 3.0, 6.0, 0.1);
    private final ModeSetting autoBlockMode = new ModeSetting("AutoBlock Mode", "Fake", "None", "Vanilla", "Fake", "Interact");
    private final ModeSetting rotations = new ModeSetting("Rotations", "Smooth", "Smooth", "Linear", "Instant");
    private final BooleanSetting keepSprint = new BooleanSetting("KeepSprint", true);

    private final TimerUtil attackTimer = new TimerUtil();
    private final TimerUtil switchTimer = new TimerUtil();
    private EntityPlayer currentTarget;
    private long nextAttackDelay = 100L;
    private boolean blocking = false;

    public KillAura() {
        super("KillAura", "Automatically attacks hostile entities within range", Category.BLATANT);
        addSettings(targetMode, sortMode, aps, attackRange, swingRange, autoBlockMode, rotations, keepSprint);
        instance = this;
    }

    @Override
    public String getSuffix() {
        return targetMode.getMode();
    }

    @Override
    public void onEnable() {
        activeRotations = null;
        currentTarget = null;
        attackTimer.reset();
        switchTimer.reset();
        calculateNextDelay();
    }

    @Override
    public void onDisable() {
        currentTarget = null;
        activeRotations = null;
        if (blocking && mc.thePlayer != null) {
            stopBlocking();
        }
        blocking = false;
    }

    @EventTarget
    public void onMotion(MotionEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            activeRotations = null;
            return;
        }

        List<EntityPlayer> targets = getTargets();

        if (targets.isEmpty()) {
            currentTarget = null;
            activeRotations = null;
            if (blocking) {
                stopBlocking();
            }
            return;
        }

        if (targetMode.is("Switch")) {
            if (currentTarget == null || !targets.contains(currentTarget) || switchTimer.hasTimeElapsed(500L)) {
                currentTarget = targets.get(0);
                switchTimer.reset();
            }
        } else {
            currentTarget = targets.get(0);
        }

        if (currentTarget == null) {
            activeRotations = null;
            return;
        }

        float[] targetRotations = RotationUtil.getRotationsToEntity(currentTarget);

        if (event.isPre()) {
            float curYaw = activeRotations != null ? activeRotations[0] : mc.thePlayer.rotationYaw;
            float curPitch = activeRotations != null ? activeRotations[1] : mc.thePlayer.rotationPitch;

            float newYaw;
            float newPitch;

            if (rotations.is("Smooth")) {
                float smoothYaw = RotationUtil.smoothSigmoid(curYaw, targetRotations[0], 6.0F);
                float smoothPitch = RotationUtil.smoothSigmoid(curPitch, targetRotations[1], 5.0F);
                float[] fixed = RotationUtil.applySensitivityGCD(
                        new float[]{curYaw, curPitch},
                        new float[]{smoothYaw, smoothPitch},
                        mc.gameSettings.mouseSensitivity
                );
                newYaw = fixed[0];
                newPitch = fixed[1];
            } else if (rotations.is("Linear")) {
                float stepYaw = RotationUtil.linearStep(curYaw, targetRotations[0], 25.0F);
                float stepPitch = RotationUtil.linearStep(curPitch, targetRotations[1], 18.0F);
                float[] fixed = RotationUtil.applySensitivityGCD(
                        new float[]{curYaw, curPitch},
                        new float[]{stepYaw, stepPitch},
                        mc.gameSettings.mouseSensitivity
                );
                newYaw = fixed[0];
                newPitch = fixed[1];
            } else {
                float[] fixed = RotationUtil.applySensitivityGCD(
                        new float[]{curYaw, curPitch},
                        targetRotations,
                        mc.gameSettings.mouseSensitivity
                );
                newYaw = fixed[0];
                newPitch = fixed[1];
            }

            newPitch = MathHelper.clamp_float(newPitch, -90.0F, 90.0F);
            activeRotations = new float[]{newYaw, newPitch};
            event.setYaw(newYaw);
            event.setPitch(newPitch);

            // AutoBlock handling (Unblock before hit if Interact)
            if (canAutoBlock() && autoBlockMode.is("Interact") && blocking) {
                stopBlocking();
            }

            // Attack handling (AttackEvent is dispatched by MixinPlayerControllerMP.attackEntity)
            if (attackTimer.hasTimeElapsed(nextAttackDelay)) {
                if (targetMode.is("Multi")) {
                    for (EntityPlayer target : targets) {
                        attack(target);
                    }
                } else {
                    attack(currentTarget);
                }
                advanceAttackTimer();
                calculateNextDelay();
            }
        } else if (event.isPost()) {
            // Re-block after hit
            if (canAutoBlock()) {
                if (autoBlockMode.is("Interact") || autoBlockMode.is("Vanilla")) {
                    startBlocking();
                }
            }
        }
    }

    private void attack(EntityPlayer target) {
        if (target == null || mc.thePlayer == null) return;
        double dist = mc.thePlayer.getDistanceToEntity(target);

        if (dist <= swingRange.getValue()) {
            mc.thePlayer.swingItem();
        }

        if (dist <= attackRange.getValue() && mc.playerController != null) {
            mc.playerController.attackEntity(mc.thePlayer, target);
        }
    }

    private void startBlocking() {
        if (!blocking && mc.getNetHandler() != null && mc.thePlayer != null
                && mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword) {
            mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
            blocking = true;
        }
    }

    private void stopBlocking() {
        if (blocking && mc.getNetHandler() != null) {
            mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(
                    C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                    BlockPos.ORIGIN,
                    EnumFacing.DOWN
            ));
            blocking = false;
        }
    }

    private boolean canAutoBlock() {
        if (mc.thePlayer == null) return false;
        ItemStack held = mc.thePlayer.getHeldItem();
        return held != null && held.getItem() instanceof ItemSword && !autoBlockMode.is("None");
    }

    private List<EntityPlayer> getTargets() {
        List<EntityPlayer> list = new ArrayList<>();
        double maxDist = Math.max(attackRange.getValue(), swingRange.getValue());

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityPlayer) || entity == mc.thePlayer) continue;
            if (entity.getEntityId() == -1337) continue;

            EntityPlayer player = (EntityPlayer) entity;
            if (AntiBot.isBot(player)) continue;
            if (player.isDead || player.getHealth() <= 0.0F) continue;
            if (FriendManager.isFriend(player.getName())) continue;
            if (Teams.isTeammate(player)) continue;

            double dist = mc.thePlayer.getDistanceToEntity(player);
            if (dist <= maxDist) {
                list.add(player);
            }
        }

        switch (sortMode.getMode()) {
            case "Distance":
                list.sort(Comparator.comparingDouble(e -> mc.thePlayer.getDistanceToEntity(e)));
                break;
            case "Health":
                list.sort(Comparator.comparingDouble(EntityPlayer::getHealth));
                break;
            case "HurtTime":
                list.sort(Comparator.comparingInt(e -> e.hurtTime));
                break;
            case "FOV":
                list.sort(Comparator.comparingDouble(RotationUtil::getFovToEntity));
                break;
        }

        return list;
    }

    private void advanceAttackTimer() {
        long now = System.currentTimeMillis();
        long nextLast = attackTimer.getLastMS() + nextAttackDelay;
        if (now - nextLast > Math.max(100L, nextAttackDelay * 2L)) {
            attackTimer.setTime(now);
        } else {
            attackTimer.setTime(nextLast);
        }
    }

    private void calculateNextDelay() {
        double min = aps.getValueMin();
        double max = aps.getValueMax();
        double mean = (min + max) / 2.0;
        double stdev = Math.max(0.2, (max - min) / 4.0);
        double targetAPS = RandomUtil.nextGaussian(mean, stdev);
        targetAPS = Math.max(min, Math.min(max, targetAPS));
        this.nextAttackDelay = Math.max(25L, (long) (1000.0 / targetAPS));
    }

    public static float[] getActiveRotations() {
        if (instance == null || !instance.isEnabled()) {
            return null;
        }
        return activeRotations;
    }

    public static boolean shouldKeepSprint() {
        return instance != null && instance.isEnabled() && instance.keepSprint.isEnabled();
    }

    public EntityPlayer getCurrentTarget() {
        return currentTarget;
    }

    public boolean isAutoBlocking() {
        return isEnabled() && canAutoBlock();
    }
}
