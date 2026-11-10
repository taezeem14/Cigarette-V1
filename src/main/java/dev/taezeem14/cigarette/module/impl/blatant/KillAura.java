package dev.taezeem14.cigarette.module.impl.blatant;

import dev.taezeem14.cigarette.Cigarette;
import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.AttackEvent;
import dev.taezeem14.cigarette.event.impl.MotionEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.module.impl.client.FriendManager;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class KillAura extends Module {

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
    private long nextAttackDelay = 100;
    private boolean blocking = false;

    public KillAura() {
        super("KillAura", "Automatically attacks hostile entities within range", Category.BLATANT);
        addSettings(targetMode, sortMode, aps, attackRange, swingRange, autoBlockMode, rotations, keepSprint);
    }

    @Override
    public void onDisable() {
        currentTarget = null;
        if (blocking && mc.thePlayer != null) {
            stopBlocking();
        }
    }

    @EventTarget
    public void onMotion(MotionEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        List<EntityPlayer> targets = getTargets();

        if (targets.isEmpty()) {
            currentTarget = null;
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

        if (currentTarget == null) return;

        float[] targetRotations = RotationUtil.getRotationsToEntity(currentTarget);

        if (event.isPre()) {
            // Apply rotations
            if (rotations.is("Smooth")) {
                float smoothYaw = RotationUtil.smoothSigmoid(mc.thePlayer.rotationYaw, targetRotations[0], 6.0f);
                float smoothPitch = RotationUtil.smoothSigmoid(mc.thePlayer.rotationPitch, targetRotations[1], 5.0f);
                float[] fixed = RotationUtil.applySensitivityGCD(new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch}, new float[]{smoothYaw, smoothPitch}, mc.gameSettings.mouseSensitivity);
                event.setYaw(fixed[0]);
                event.setPitch(fixed[1]);
            } else if (rotations.is("Linear")) {
                float stepYaw = targetRotations[0];
                float stepPitch = targetRotations[1];
                event.setYaw(stepYaw);
                event.setPitch(stepPitch);
            } else if (rotations.is("Instant")) {
                event.setYaw(targetRotations[0]);
                event.setPitch(targetRotations[1]);
            }

            // AutoBlock handling (Unblock before hit if Interact)
            if (canAutoBlock() && autoBlockMode.is("Interact") && blocking) {
                stopBlocking();
            }

            // Attack handling
            if (attackTimer.hasTimeElapsed(nextAttackDelay)) {
                if (targetMode.is("Multi")) {
                    for (EntityPlayer target : targets) {
                        attack(target);
                    }
                } else {
                    attack(currentTarget);
                }
                calculateNextDelay();
                attackTimer.reset();
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
        double dist = mc.thePlayer.getDistanceToEntity(target);

        if (dist <= swingRange.getValue()) {
            mc.thePlayer.swingItem();
        }

        if (dist <= attackRange.getValue()) {
            if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getEventBus() != null) {
                Cigarette.INSTANCE.getEventBus().post(new AttackEvent(target));
            }
            mc.playerController.attackEntity(mc.thePlayer, target);
        }
    }

    private void startBlocking() {
        if (!blocking && mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword) {
            mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
            blocking = true;
        }
    }

    private void stopBlocking() {
        if (blocking && mc.thePlayer != null) {
            mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(
                    C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                    BlockPos.ORIGIN,
                    EnumFacing.DOWN
            ));
            blocking = false;
        }
    }

    private boolean canAutoBlock() {
        ItemStack held = mc.thePlayer.getHeldItem();
        return held != null && held.getItem() instanceof ItemSword && !autoBlockMode.is("None");
    }

    private List<EntityPlayer> getTargets() {
        List<EntityPlayer> list = new ArrayList<>();
        double maxDist = Math.max(attackRange.getValue(), swingRange.getValue());

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityPlayer) || entity == mc.thePlayer) continue;

            EntityPlayer player = (EntityPlayer) entity;
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

    private void calculateNextDelay() {
        double min = aps.getValueMin();
        double max = aps.getValueMax();
        double mean = (min + max) / 2.0;
        double stdev = Math.max(0.2, (max - min) / 4.0);
        double targetAPS = RandomUtil.nextGaussian(mean, stdev);
        targetAPS = Math.max(min, Math.min(max, targetAPS));
        this.nextAttackDelay = Math.max(25L, (long) (1000.0 / targetAPS));
    }

    public EntityPlayer getCurrentTarget() {
        return currentTarget;
    }

    public boolean isAutoBlocking() {
        return isEnabled() && canAutoBlock();
    }
}
