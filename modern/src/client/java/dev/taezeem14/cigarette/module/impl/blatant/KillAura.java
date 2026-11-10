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
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class KillAura extends Module {

    private static KillAura instance;
    private static float[] activeRotations;

    private final ModeSetting targetMode = new ModeSetting("Target Mode", "Single", "Single", "Switch", "Multi");
    private final ModeSetting sortMode = new ModeSetting("Sort Mode", "Distance", "Distance", "Health", "HurtTime", "FOV");
    private final BooleanSetting respectCooldown = new BooleanSetting("1.9+ Cooldown", true);
    private final DoubleSliderSetting aps = new DoubleSliderSetting("APS", 10.0, 14.0, 1.0, 20.0, 0.5);
    private final NumberSetting attackRange = new NumberSetting("Attack Range", 3.6, 2.5, 6.0, 0.1);
    private final NumberSetting swingRange = new NumberSetting("Swing Range", 4.0, 2.5, 6.0, 0.1);
    private final ModeSetting rotations = new ModeSetting("Rotations", "Smooth", "Smooth", "Linear", "Instant");
    private final BooleanSetting keepSprint = new BooleanSetting("KeepSprint", true);
    private final BooleanSetting targetPlayers = new BooleanSetting("Players", true);
    private final BooleanSetting targetMobs = new BooleanSetting("Mobs", false);
    private final BooleanSetting targetInvisibles = new BooleanSetting("Invisibles", true);

    private final TimerUtil attackTimer = new TimerUtil();
    private final TimerUtil switchTimer = new TimerUtil();
    private LivingEntity currentTarget;
    private long nextAttackDelay = 100L;

    public KillAura() {
        super("KillAura", "Automatically attacks targets with modern cooldown or spam timing", Category.BLATANT);
        addSettings(targetMode, sortMode, respectCooldown, aps, attackRange, swingRange, rotations, keepSprint,
                targetPlayers, targetMobs, targetInvisibles);
        aps.setVisibility(() -> !respectCooldown.isEnabled());
        instance = this;
    }

    @Override
    public String getSuffix() {
        return respectCooldown.isEnabled() ? targetMode.getMode() + " 1.9+" : targetMode.getMode();
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
    }

    @EventTarget
    public void onMotion(MotionEvent event) {
        if (mc.player == null || mc.level == null) {
            activeRotations = null;
            return;
        }
        if (!event.isPre()) return;

        List<LivingEntity> targets = getTargets();
        if (targets.isEmpty()) {
            currentTarget = null;
            activeRotations = null;
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
        float curYaw = activeRotations != null ? activeRotations[0] : mc.player.getYRot();
        float curPitch = activeRotations != null ? activeRotations[1] : mc.player.getXRot();
        float sens = RotationUtil.getMouseSensitivity();

        float[] rawTarget;
        if (rotations.is("Smooth")) {
            rawTarget = new float[]{
                    RotationUtil.smoothSigmoid(curYaw, targetRotations[0], 6.0F),
                    RotationUtil.smoothSigmoid(curPitch, targetRotations[1], 5.0F)
            };
        } else if (rotations.is("Linear")) {
            rawTarget = new float[]{
                    RotationUtil.linearStep(curYaw, targetRotations[0], 25.0F),
                    RotationUtil.linearStep(curPitch, targetRotations[1], 18.0F)
            };
        } else {
            rawTarget = targetRotations;
        }

        float[] fixed = RotationUtil.applySensitivityGCD(new float[]{curYaw, curPitch}, rawTarget, sens);
        fixed[1] = Mth.clamp(fixed[1], -90.0F, 90.0F);
        activeRotations = fixed;
        event.setYaw(fixed[0]);
        event.setPitch(fixed[1]);

        boolean readyToHit = respectCooldown.isEnabled()
                ? mc.player.getAttackStrengthScale(0.5F) >= 0.92F
                : attackTimer.hasTimeElapsed(nextAttackDelay);

        if (readyToHit) {
            if (targetMode.is("Multi")) {
                for (LivingEntity target : targets) {
                    attack(target);
                }
            } else {
                attack(currentTarget);
            }
            if (!respectCooldown.isEnabled()) {
                advanceAttackTimer();
                calculateNextDelay();
            }
        }
    }

    private void attack(LivingEntity target) {
        if (target == null || mc.player == null || mc.gameMode == null) return;
        double dist = Math.min(mc.player.distanceTo(target), RotationUtil.getDistanceToEntityHitbox(target));
        if (dist <= attackRange.getValue()) {
            mc.gameMode.attack(mc.player, target);
        }
        if (dist <= swingRange.getValue()) {
            mc.player.swing(InteractionHand.MAIN_HAND, mc.player.getMainHandItem().getAttackAnimation(), false);
        }
    }

    private List<LivingEntity> getTargets() {
        List<LivingEntity> list = new ArrayList<>();
        double maxDist = Math.max(attackRange.getValue(), swingRange.getValue());

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || entity == mc.player) continue;
            if (!living.isAlive()) continue;
            if (living.isInvisible() && !targetInvisibles.isEnabled()) continue;

            if (living instanceof Player player) {
                if (!targetPlayers.isEnabled()) continue;
                if (AntiBot.isBot(player) || FriendManager.isFriend(player.getName().getString()) || Teams.isTeammate(player)) {
                    continue;
                }
            } else if (living instanceof Monster || living instanceof Slime || living instanceof Animal) {
                if (!targetMobs.isEnabled()) continue;
            } else {
                continue;
            }

            double dist = Math.min(mc.player.distanceTo(living), RotationUtil.getDistanceToEntityHitbox(living));
            if (dist <= maxDist) {
                list.add(living);
            }
        }

        switch (sortMode.getMode()) {
            case "Health" -> list.sort(Comparator.comparingDouble(LivingEntity::getHealth));
            case "HurtTime" -> list.sort(Comparator.comparingInt(e -> e.hurtTime));
            case "FOV" -> list.sort(Comparator.comparingDouble(RotationUtil::getFovToEntity));
            default -> list.sort(Comparator.comparingDouble(RotationUtil::getDistanceToEntityHitbox));
        }
        return list;
    }

    private void advanceAttackTimer() {
        long now = System.currentTimeMillis();
        long nextLast = attackTimer.getLastMS() + nextAttackDelay;
        attackTimer.setTime(now - nextLast > Math.max(100L, nextAttackDelay * 2L) ? now : nextLast);
    }

    private void calculateNextDelay() {
        double min = aps.getValueMin();
        double max = aps.getValueMax();
        double targetAPS = Math.max(min, Math.min(max, RandomUtil.nextGaussian((min + max) / 2.0, Math.max(0.2, (max - min) / 4.0))));
        this.nextAttackDelay = Math.max(25L, (long) (1000.0 / targetAPS));
    }

    public static float[] getActiveRotations() {
        if (instance == null || !instance.isEnabled()) return null;
        return activeRotations;
    }

    public static boolean shouldKeepSprint() {
        return instance != null && instance.isEnabled() && instance.keepSprint.isEnabled();
    }

    public LivingEntity getCurrentTarget() {
        return currentTarget;
    }
}
