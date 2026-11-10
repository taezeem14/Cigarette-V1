package dev.taezeem14.cigarette.module.impl.combat;

import com.mojang.blaze3d.platform.InputConstants;
import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.Render3DEvent;
import dev.taezeem14.cigarette.event.impl.TickEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.DoubleSliderSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.util.math.RandomUtil;
import dev.taezeem14.cigarette.util.math.RotationUtil;
import dev.taezeem14.cigarette.util.math.TimerUtil;
import dev.taezeem14.cigarette.util.player.InventoryUtil;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.HitResult;

public class AutoClicker extends Module {

    private static AutoClicker instance;

    private final DoubleSliderSetting leftCps = new DoubleSliderSetting("Left CPS", 11.5, 14.5, 1.0, 20.0, 0.1);
    private final DoubleSliderSetting rightCps = new DoubleSliderSetting("Right CPS", 14.0, 18.0, 1.0, 20.0, 0.1);
    private final BooleanSetting blockHit = new BooleanSetting("BlockHit", true);
    private final NumberSetting blockHitChance = new NumberSetting("BlockHit Chance", 35.0, 0.0, 100.0, 5.0);
    private final NumberSetting jitterStrength = new NumberSetting("Jitter Strength", 0.0, 0.0, 2.0, 0.1);
    private final BooleanSetting weaponOnly = new BooleanSetting("Weapon Only", true);
    private final BooleanSetting breakBlocks = new BooleanSetting("Break Blocks", true);

    private final TimerUtil leftTimer = new TimerUtil();
    private final TimerUtil rightTimer = new TimerUtil();
    private long nextLeftDelay = 80L;
    private long nextRightDelay = 65L;

    private boolean leftClickHeld = false;
    private long leftReleaseTime = 0L;
    private boolean rightClickHeld = false;
    private long rightReleaseTime = 0L;
    private boolean blockHitHeld = false;
    private long blockHitReleaseTime = 0L;

    public AutoClicker() {
        super("AutoClicker", "Automates mouse clicks with humanized Gaussian delays", Category.COMBAT);
        addSettings(leftCps, rightCps, blockHit, blockHitChance, jitterStrength, weaponOnly, breakBlocks);
        instance = this;
    }

    public static AutoClicker getInstance() {
        return instance;
    }

    @Override
    public String getSuffix() {
        return String.format("%.1f-%.1f", leftCps.getValueMin(), leftCps.getValueMax());
    }

    @Override
    public void onEnable() {
        leftTimer.reset();
        rightTimer.reset();
        calculateNextLeftDelay();
        calculateNextRightDelay();
        leftClickHeld = false;
        rightClickHeld = false;
        blockHitHeld = false;
    }

    @Override
    public void onDisable() {
        releaseAll();
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        updateScheduledReleases(System.currentTimeMillis());
    }

    @EventTarget
    public void onTick(TickEvent event) {
        long now = System.currentTimeMillis();
        updateScheduledReleases(now);
        if (!event.isPre() || mc.player == null || mc.level == null) return;

        if (mc.gui.screen() != null) {
            releaseAll();
            return;
        }

        if (RotationUtil.isPhysicalKeyDown(mc.options.keyAttack)) {
            boolean allowLeft = true;
            if (weaponOnly.isEnabled() && !InventoryUtil.isWeapon(mc.player.getMainHandItem())) {
                allowLeft = false;
            }
            if (allowLeft && breakBlocks.isEnabled() && mc.hitResult != null
                    && mc.hitResult.getType() == HitResult.Type.BLOCK) {
                InputConstants.Key attackKey = KeyMappingHelper.getBoundKeyOf(mc.options.keyAttack);
                KeyMapping.set(attackKey, true);
                allowLeft = false;
            }
            if (allowLeft && leftTimer.hasTimeElapsed(nextLeftDelay)) {
                InputConstants.Key attackKey = KeyMappingHelper.getBoundKeyOf(mc.options.keyAttack);
                KeyMapping.set(attackKey, true);
                KeyMapping.click(attackKey);
                leftClickHeld = true;
                leftReleaseTime = now + RandomUtil.nextInt(15, 35);
                advanceTimer(leftTimer, nextLeftDelay);
                calculateNextLeftDelay();

                ItemStack offhand = mc.player.getOffhandItem();
                if (blockHit.isEnabled() && offhand.is(Items.SHIELD) && RandomUtil.chance(blockHitChance.getValue())) {
                    InputConstants.Key useKey = KeyMappingHelper.getBoundKeyOf(mc.options.keyUse);
                    KeyMapping.set(useKey, true);
                    KeyMapping.click(useKey);
                    blockHitHeld = true;
                    blockHitReleaseTime = now + RandomUtil.nextInt(15, 35);
                }

                if (jitterStrength.getValue() > 0.0) {
                    float j = jitterStrength.getValueFloat();
                    mc.player.setYRot(mc.player.getYRot() + (float) RandomUtil.nextGaussian(0.0, j * 0.4));
                    mc.player.setXRot(Mth.clamp(mc.player.getXRot() + (float) RandomUtil.nextGaussian(0.0, j * 0.3), -90.0F, 90.0F));
                }
            }
        } else if (leftClickHeld) {
            InputConstants.Key attackKey = KeyMappingHelper.getBoundKeyOf(mc.options.keyAttack);
            KeyMapping.set(attackKey, false);
            leftClickHeld = false;
        }

        if (RotationUtil.isPhysicalKeyDown(mc.options.keyUse)) {
            if (rightTimer.hasTimeElapsed(nextRightDelay)) {
                InputConstants.Key useKey = KeyMappingHelper.getBoundKeyOf(mc.options.keyUse);
                KeyMapping.set(useKey, true);
                KeyMapping.click(useKey);
                rightClickHeld = true;
                rightReleaseTime = now + RandomUtil.nextInt(15, 35);
                advanceTimer(rightTimer, nextRightDelay);
                calculateNextRightDelay();
            }
        } else if (rightClickHeld) {
            InputConstants.Key useKey = KeyMappingHelper.getBoundKeyOf(mc.options.keyUse);
            KeyMapping.set(useKey, false);
            rightClickHeld = false;
        }
    }

    private void updateScheduledReleases(long now) {
        if (mc.options == null) return;
        if (leftClickHeld && now >= leftReleaseTime) {
            KeyMapping.set(KeyMappingHelper.getBoundKeyOf(mc.options.keyAttack), RotationUtil.isPhysicalKeyDown(mc.options.keyAttack));
            leftClickHeld = false;
        }
        if (rightClickHeld && now >= rightReleaseTime) {
            KeyMapping.set(KeyMappingHelper.getBoundKeyOf(mc.options.keyUse), RotationUtil.isPhysicalKeyDown(mc.options.keyUse));
            rightClickHeld = false;
        }
        if (blockHitHeld && now >= blockHitReleaseTime) {
            KeyMapping.set(KeyMappingHelper.getBoundKeyOf(mc.options.keyUse), RotationUtil.isPhysicalKeyDown(mc.options.keyUse));
            blockHitHeld = false;
        }
    }

    private void releaseAll() {
        if (mc.options != null) {
            if (leftClickHeld) KeyMapping.set(KeyMappingHelper.getBoundKeyOf(mc.options.keyAttack), RotationUtil.isPhysicalKeyDown(mc.options.keyAttack));
            if (rightClickHeld || blockHitHeld) KeyMapping.set(KeyMappingHelper.getBoundKeyOf(mc.options.keyUse), RotationUtil.isPhysicalKeyDown(mc.options.keyUse));
        }
        leftClickHeld = false;
        rightClickHeld = false;
        blockHitHeld = false;
    }

    private void advanceTimer(TimerUtil timer, long delayMs) {
        long now = System.currentTimeMillis();
        long nextLast = timer.getLastMS() + delayMs;
        timer.setTime(now - nextLast > Math.max(100L, delayMs * 2L) ? now : nextLast);
    }

    private void calculateNextLeftDelay() {
        double min = leftCps.getValueMin();
        double max = leftCps.getValueMax();
        double cps = Math.max(min, Math.min(max, RandomUtil.nextGaussian((min + max) / 2.0, Math.max(0.2, (max - min) / 4.0))));
        this.nextLeftDelay = Math.max(20L, (long) (1000.0 / cps));
    }

    private void calculateNextRightDelay() {
        double min = rightCps.getValueMin();
        double max = rightCps.getValueMax();
        double cps = Math.max(min, Math.min(max, RandomUtil.nextGaussian((min + max) / 2.0, Math.max(0.2, (max - min) / 4.0))));
        this.nextRightDelay = Math.max(20L, (long) (1000.0 / cps));
    }
}
