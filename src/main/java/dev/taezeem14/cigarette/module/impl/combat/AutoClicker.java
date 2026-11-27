package dev.taezeem14.cigarette.module.impl.combat;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.Render3DEvent;
import dev.taezeem14.cigarette.event.impl.TickEvent;
import dev.taezeem14.cigarette.forge.ForgeEventBridge;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.DoubleSliderSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.util.mapping.MappingResolver;
import dev.taezeem14.cigarette.util.math.RandomUtil;
import dev.taezeem14.cigarette.util.math.RotationUtil;
import dev.taezeem14.cigarette.util.math.TimerUtil;
import dev.taezeem14.cigarette.util.player.CPSTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class AutoClicker extends Module {

    private final DoubleSliderSetting leftCps = new DoubleSliderSetting("Left CPS", 11.5, 14.5, 1.0, 20.0, 0.1);
    private final DoubleSliderSetting rightCps = new DoubleSliderSetting("Right CPS", 14.0, 18.0, 1.0, 20.0, 0.1);
    private final BooleanSetting cooldown19 = new BooleanSetting("1.9+ Cooldown", false);
    private final BooleanSetting blockHit = new BooleanSetting("BlockHit", true);
    private final NumberSetting blockHitChance = new NumberSetting("BlockHit Chance", 35.0, 0.0, 100.0, 5.0);
    private final NumberSetting jitterStrength = new NumberSetting("Jitter Strength", 0.0, 0.0, 2.0, 0.1);
    private final BooleanSetting weaponOnly = new BooleanSetting("Weapon Only", false);
    private final BooleanSetting breakBlocks = new BooleanSetting("Break Blocks", true);
    private final BooleanSetting inventoryFill = new BooleanSetting("Inventory Fill", false);

    private final TimerUtil leftTimer = new TimerUtil();
    private final TimerUtil rightTimer = new TimerUtil();
    private long nextLeftDelay = 80L;
    private long nextRightDelay = 65L;
    private long cooldownJitterMs = 8L;

    private boolean leftClickHeld = false;
    private long leftReleaseTime = 0L;
    private boolean rightClickHeld = false;
    private long rightReleaseTime = 0L;
    private boolean blockHitHeld = false;
    private long blockHitReleaseTime = 0L;
    private boolean miningBlock = false;

    public AutoClicker() {
        super("AutoClicker", "Automates mouse clicks with humanized Gaussian delays", Category.COMBAT);
        addSettings(leftCps, rightCps, cooldown19, blockHit, blockHitChance, jitterStrength, weaponOnly, breakBlocks, inventoryFill);
    }

    @Override
    public String getSuffix() {
        if (cooldown19.isEnabled()) {
            return "1.9+";
        }
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
        miningBlock = false;
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

        if (!event.isPre() || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        // Inventory fill logic
        if (inventoryFill.isEnabled() && mc.currentScreen instanceof GuiInventory) {
            boolean shiftHeld = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
            if (Mouse.isButtonDown(0) && shiftHeld) {
                if (leftTimer.hasTimeElapsed(nextLeftDelay)) {
                    performInventoryFill((GuiInventory) mc.currentScreen);
                    advanceTimer(leftTimer, nextLeftDelay);
                    calculateNextLeftDelay();
                }
            }
            return;
        }

        if (mc.currentScreen != null) {
            releaseAll();
            return;
        }

        int attackKey = mc.gameSettings.keyBindAttack.getKeyCode();
        int useKey = mc.gameSettings.keyBindUseItem.getKeyCode();

        // Left click handling using physical key state
        if (RotationUtil.isPhysicalKeyDown(attackKey)) {
            boolean allowLeft = true;
            ItemStack held = mc.thePlayer.getHeldItem();

            if (weaponOnly.isEnabled()) {
                if (held == null || !(held.getItem() instanceof ItemSword || held.getItem() instanceof ItemAxe)) {
                    allowLeft = false;
                }
            }

            boolean holdingSword = held != null && held.getItem() instanceof ItemSword;
            if (allowLeft && breakBlocks.isEnabled()
                    && mc.pointedEntity == null
                    && mc.objectMouseOver != null
                    && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                    && !holdingSword) {
                KeyBinding.setKeyBindState(attackKey, true);
                miningBlock = true;
                allowLeft = false;
            } else if (miningBlock) {
                KeyBinding.setKeyBindState(attackKey, false);
                miningBlock = false;
            }

            long effectiveLeftDelay = getEffectiveLeftDelay();
            if (allowLeft && leftTimer.hasTimeElapsed(effectiveLeftDelay)) {
                performLeftClick(now);
                advanceTimer(leftTimer, effectiveLeftDelay);
                calculateNextLeftDelay();

                // Blockhit logic (1.8 PvP only — skipped when 1.9+ Cooldown is active)
                if (!cooldown19.isEnabled() && blockHit.isEnabled() && holdingSword
                        && RandomUtil.chance(blockHitChance.getValue())) {
                    KeyBinding.setKeyBindState(useKey, true);
                    KeyBinding.onTick(useKey);
                    blockHitHeld = true;
                    blockHitReleaseTime = now + RandomUtil.nextInt(15, 35);
                }

                // Jitter logic
                if (jitterStrength.getValue() > 0.0) {
                    float jitterVal = jitterStrength.getValueFloat();
                    float jitterYaw = (float) RandomUtil.nextGaussian(0.0, jitterVal * 0.4);
                    float jitterPitch = (float) RandomUtil.nextGaussian(0.0, jitterVal * 0.3);
                    mc.thePlayer.rotationYaw += jitterYaw;
                    mc.thePlayer.rotationPitch = MathHelper.clamp_float(mc.thePlayer.rotationPitch + jitterPitch, -90.0F, 90.0F);
                }
            }
        } else if (leftClickHeld || miningBlock) {
            KeyBinding.setKeyBindState(attackKey, false);
            leftClickHeld = false;
            miningBlock = false;
        }

        // Right click handling using physical key state
        if (RotationUtil.isPhysicalKeyDown(useKey)) {
            if (!isHoldingContinuousUseItem() && rightTimer.hasTimeElapsed(nextRightDelay)) {
                performRightClick(now);
                advanceTimer(rightTimer, nextRightDelay);
                calculateNextRightDelay();
            }
        } else if (rightClickHeld) {
            KeyBinding.setKeyBindState(useKey, false);
            rightClickHeld = false;
        }
    }

    private boolean isHoldingContinuousUseItem() {
        if (mc.thePlayer == null || !mc.thePlayer.isUsingItem()) {
            return false;
        }
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null) {
            return false;
        }
        if (held.getItem() instanceof ItemFood || held.getItem() instanceof ItemBow || held.getItem() instanceof ItemSword) {
            return true;
        }
        return held.getItem() instanceof ItemPotion && !ItemPotion.isSplash(held.getMetadata());
    }

    private long getEffectiveLeftDelay() {
        if (!cooldown19.isEnabled()) {
            return nextLeftDelay;
        }
        ItemStack held = mc.thePlayer != null ? mc.thePlayer.getHeldItem() : null;
        long base;
        if (held != null && held.getItem() instanceof ItemSword) {
            base = 625L;
        } else if (held != null && held.getItem() instanceof ItemAxe) {
            base = 1000L;
        } else if (held != null && (held.getItem() instanceof ItemPickaxe || held.getItem() instanceof ItemSpade)) {
            base = 850L;
        } else {
            base = 250L;
        }
        return base + cooldownJitterMs;
    }

    private void performLeftClick(long now) {
        try {
            MappingResolver.setFieldValue(mc, 0, Minecraft.class, MappingResolver.MINECRAFT_LEFT_CLICK_COUNTER);
        } catch (Exception ignored) {
        }

        int key = mc.gameSettings.keyBindAttack.getKeyCode();
        KeyBinding.setKeyBindState(key, true);
        leftClickHeld = true;
        leftReleaseTime = now + RandomUtil.nextInt(15, 35);

        if (mc.currentScreen == null) {
            try {
                MappingResolver.getMethod(Minecraft.class, new Class<?>[0], MappingResolver.MINECRAFT_CLICK_MOUSE);
                MappingResolver.invokeMethod(mc, Minecraft.class, new Class<?>[0], new Object[0], MappingResolver.MINECRAFT_CLICK_MOUSE);
                if ((now - ForgeEventBridge.lastMixinTickTime) >= 80L) {
                    CPSTracker.recordLeftClick();
                }
            } catch (Throwable fallback) {
                KeyBinding.onTick(key);
                CPSTracker.recordLeftClick();
            }
        }
    }

    private void performRightClick(long now) {
        try {
            MappingResolver.setFieldValue(mc, 0, Minecraft.class, MappingResolver.MINECRAFT_RIGHT_CLICK_DELAY_TIMER);
        } catch (Exception ignored) {
        }

        int key = mc.gameSettings.keyBindUseItem.getKeyCode();
        KeyBinding.setKeyBindState(key, true);
        rightClickHeld = true;
        rightReleaseTime = now + RandomUtil.nextInt(15, 35);

        if (mc.currentScreen == null) {
            try {
                MappingResolver.getMethod(Minecraft.class, new Class<?>[0], MappingResolver.MINECRAFT_RIGHT_CLICK_MOUSE);
                MappingResolver.invokeMethod(mc, Minecraft.class, new Class<?>[0], new Object[0], MappingResolver.MINECRAFT_RIGHT_CLICK_MOUSE);
                if ((now - ForgeEventBridge.lastMixinTickTime) >= 80L) {
                    CPSTracker.recordRightClick();
                }
            } catch (Throwable fallback) {
                KeyBinding.onTick(key);
                CPSTracker.recordRightClick();
            }
        }
    }

    private void updateScheduledReleases(long now) {
        if (mc.gameSettings == null) return;

        if (leftClickHeld && now >= leftReleaseTime) {
            int attackKey = mc.gameSettings.keyBindAttack.getKeyCode();
            KeyBinding.setKeyBindState(attackKey, RotationUtil.isPhysicalKeyDown(attackKey));
            leftClickHeld = false;
        }

        if (rightClickHeld && now >= rightReleaseTime) {
            int useKey = mc.gameSettings.keyBindUseItem.getKeyCode();
            KeyBinding.setKeyBindState(useKey, RotationUtil.isPhysicalKeyDown(useKey));
            rightClickHeld = false;
        }

        if (blockHitHeld && now >= blockHitReleaseTime) {
            int useKey = mc.gameSettings.keyBindUseItem.getKeyCode();
            KeyBinding.setKeyBindState(useKey, RotationUtil.isPhysicalKeyDown(useKey));
            blockHitHeld = false;
        }
    }

    private void releaseAll() {
        if (mc.gameSettings == null) {
            leftClickHeld = false;
            rightClickHeld = false;
            blockHitHeld = false;
            miningBlock = false;
            return;
        }

        if (leftClickHeld || miningBlock) {
            int attackKey = mc.gameSettings.keyBindAttack.getKeyCode();
            KeyBinding.setKeyBindState(attackKey, RotationUtil.isPhysicalKeyDown(attackKey));
            leftClickHeld = false;
            miningBlock = false;
        }

        if (rightClickHeld || blockHitHeld) {
            int useKey = mc.gameSettings.keyBindUseItem.getKeyCode();
            KeyBinding.setKeyBindState(useKey, RotationUtil.isPhysicalKeyDown(useKey));
            rightClickHeld = false;
            blockHitHeld = false;
        }
    }

    private void performInventoryFill(GuiInventory gui) {
        if (mc.playerController == null || gui.inventorySlots == null) return;

        ScaledResolution sr = new ScaledResolution(mc);
        int mouseX = Mouse.getX() * sr.getScaledWidth() / Math.max(1, mc.displayWidth);
        int mouseY = sr.getScaledHeight() - Mouse.getY() * sr.getScaledHeight() / Math.max(1, mc.displayHeight) - 1;

        Slot hoveredSlot = findHoveredSlot(gui, mouseX, mouseY);
        if (hoveredSlot != null && hoveredSlot.getHasStack() && hoveredSlot.canBeHovered()) {
            mc.playerController.windowClick(gui.inventorySlots.windowId, hoveredSlot.slotNumber, 0, 1, mc.thePlayer);
        }
    }

    private Slot findHoveredSlot(GuiContainer gui, int mouseX, int mouseY) {
        try {
            Slot reflected = MappingResolver.invokeMethod(
                    gui,
                    GuiContainer.class,
                    new Class<?>[]{int.class, int.class},
                    new Object[]{mouseX, mouseY},
                    "getSlotAtPosition",
                    "func_146975_c"
            );
            if (reflected != null) {
                return reflected;
            }
        } catch (Exception ignored) {
        }

        Integer guiLeft = MappingResolver.getFieldValue(gui, GuiContainer.class, "guiLeft", "field_147003_i");
        Integer guiTop = MappingResolver.getFieldValue(gui, GuiContainer.class, "guiTop", "field_147009_r");
        if (guiLeft != null && guiTop != null && gui.inventorySlots != null) {
            int relX = mouseX - guiLeft;
            int relY = mouseY - guiTop;
            for (Slot slot : gui.inventorySlots.inventorySlots) {
                if (relX >= slot.xDisplayPosition - 1 && relX < slot.xDisplayPosition + 17
                        && relY >= slot.yDisplayPosition - 1 && relY < slot.yDisplayPosition + 17) {
                    return slot;
                }
            }
        }

        return gui.getSlotUnderMouse();
    }

    private void advanceTimer(TimerUtil timer, long delayMs) {
        long now = System.currentTimeMillis();
        long nextLast = timer.getLastMS() + delayMs;
        if (now - nextLast > Math.max(100L, delayMs * 2L)) {
            timer.setTime(now);
        } else {
            timer.setTime(nextLast);
        }
    }

    private void calculateNextLeftDelay() {
        cooldownJitterMs = Math.max(0L, Math.min(25L, Math.round(RandomUtil.nextGaussian(8.0, 5.0))));
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
