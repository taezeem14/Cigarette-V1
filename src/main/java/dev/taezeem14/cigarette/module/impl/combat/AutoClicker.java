package dev.taezeem14.cigarette.module.impl.combat;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.Render2DEvent;
import dev.taezeem14.cigarette.event.impl.TickEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.DoubleSliderSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.util.mapping.MappingResolver;
import dev.taezeem14.cigarette.util.math.RandomUtil;
import dev.taezeem14.cigarette.util.math.TimerUtil;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class AutoClicker extends Module {

    private final DoubleSliderSetting leftCps = new DoubleSliderSetting("Left CPS", 11.5, 14.5, 1.0, 20.0, 0.1);
    private final DoubleSliderSetting rightCps = new DoubleSliderSetting("Right CPS", 14.0, 18.0, 1.0, 20.0, 0.1);
    private final BooleanSetting blockHit = new BooleanSetting("BlockHit", true);
    private final NumberSetting blockHitChance = new NumberSetting("BlockHit Chance", 35.0, 0.0, 100.0, 5.0);
    private final NumberSetting jitterStrength = new NumberSetting("Jitter Strength", 0.0, 0.0, 2.0, 0.1);
    private final BooleanSetting weaponOnly = new BooleanSetting("Weapon Only", true);
    private final BooleanSetting breakBlocks = new BooleanSetting("Break Blocks", true);
    private final BooleanSetting inventoryFill = new BooleanSetting("Inventory Fill", false);

    private final TimerUtil leftTimer = new TimerUtil();
    private final TimerUtil rightTimer = new TimerUtil();
    private long nextLeftDelay = 80;
    private long nextRightDelay = 65;

    public AutoClicker() {
        super("AutoClicker", "Automates mouse clicks with humanized Gaussian delays", Category.COMBAT);
        addSettings(leftCps, rightCps, blockHit, blockHitChance, jitterStrength, weaponOnly, breakBlocks, inventoryFill);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!event.isPre() || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        // Inventory fill logic
        if (inventoryFill.isEnabled() && mc.currentScreen instanceof GuiInventory) {
            if (Mouse.isButtonDown(0) && (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))) {
                if (leftTimer.hasTimeElapsed(nextLeftDelay)) {
                    calculateNextLeftDelay();
                    leftTimer.reset();
                }
            }
            return;
        }

        if (mc.currentScreen != null) {
            return;
        }

        // Left click handling
        if (mc.gameSettings.keyBindAttack.isKeyDown()) {
            if (weaponOnly.isEnabled()) {
                ItemStack held = mc.thePlayer.getHeldItem();
                if (held == null || !(held.getItem() instanceof ItemSword || held.getItem() instanceof ItemAxe)) {
                    return;
                }
            }

            if (breakBlocks.isEnabled() && mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                return;
            }

            if (leftTimer.hasTimeElapsed(nextLeftDelay)) {
                performLeftClick();
                calculateNextLeftDelay();
                leftTimer.reset();

                // Blockhit logic
                if (blockHit.isEnabled() && mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword) {
                    if (RandomUtil.chance(blockHitChance.getValue())) {
                        int rmbKey = mc.gameSettings.keyBindUseItem.getKeyCode();
                        KeyBinding.setKeyBindState(rmbKey, true);
                        KeyBinding.onTick(rmbKey);
                        KeyBinding.setKeyBindState(rmbKey, false);
                    }
                }

                // Jitter logic
                if (jitterStrength.getValue() > 0.0) {
                    float jitterVal = jitterStrength.getValueFloat();
                    float jitterYaw = (float) RandomUtil.nextGaussian(0.0, jitterVal * 0.4);
                    float jitterPitch = (float) RandomUtil.nextGaussian(0.0, jitterVal * 0.3);
                    mc.thePlayer.rotationYaw += jitterYaw;
                    mc.thePlayer.rotationPitch += jitterPitch;
                }
            }
        }

        // Right click handling
        if (mc.gameSettings.keyBindUseItem.isKeyDown()) {
            if (rightTimer.hasTimeElapsed(nextRightDelay)) {
                performRightClick();
                calculateNextRightDelay();
                rightTimer.reset();
            }
        }
    }

    private void performLeftClick() {
        int key = mc.gameSettings.keyBindAttack.getKeyCode();
        KeyBinding.setKeyBindState(key, true);
        KeyBinding.onTick(key);
        KeyBinding.setKeyBindState(key, false);

        // Reset click counter via reflection
        try {
            MappingResolver.setFieldValue(mc, 0, mc.getClass(), MappingResolver.MINECRAFT_LEFT_CLICK_COUNTER);
        } catch (Exception ignored) {
        }
    }

    private void performRightClick() {
        int key = mc.gameSettings.keyBindUseItem.getKeyCode();
        KeyBinding.setKeyBindState(key, true);
        KeyBinding.onTick(key);
        KeyBinding.setKeyBindState(key, false);

        // Reset delay timer via reflection
        try {
            MappingResolver.setFieldValue(mc, 0, mc.getClass(), MappingResolver.MINECRAFT_RIGHT_CLICK_DELAY_TIMER);
        } catch (Exception ignored) {
        }
    }

    private void calculateNextLeftDelay() {
        double min = leftCps.getValueMin();
        double max = leftCps.getValueMax();
        double mean = (min + max) / 2.0;
        double stdev = Math.max(0.2, (max - min) / 4.0);
        double targetCPS = RandomUtil.nextGaussian(mean, stdev);
        targetCPS = Math.max(min, Math.min(max, targetCPS));
        this.nextLeftDelay = Math.max(20L, (long) (1000.0 / targetCPS));
    }

    private void calculateNextRightDelay() {
        double min = rightCps.getValueMin();
        double max = rightCps.getValueMax();
        double mean = (min + max) / 2.0;
        double stdev = Math.max(0.2, (max - min) / 4.0);
        double targetCPS = RandomUtil.nextGaussian(mean, stdev);
        targetCPS = Math.max(min, Math.min(max, targetCPS));
        this.nextRightDelay = Math.max(20L, (long) (1000.0 / targetCPS));
    }
}
