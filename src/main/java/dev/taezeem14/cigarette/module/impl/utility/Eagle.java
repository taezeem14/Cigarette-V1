package dev.taezeem14.cigarette.module.impl.utility;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.TickEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.DoubleSliderSetting;
import dev.taezeem14.cigarette.util.math.RotationUtil;
import dev.taezeem14.cigarette.util.math.TimerUtil;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;

public class Eagle extends Module {

    private final DoubleSliderSetting delay = new DoubleSliderSetting("Delay", 15.0, 40.0, 0.0, 100.0, 1.0);
    private final BooleanSetting blocksOnly = new BooleanSetting("Blocks Only", true);
    private final BooleanSetting pitchCheck = new BooleanSetting("Pitch Check (>65°)", true);

    private boolean sneaking = false;
    private final TimerUtil timer = new TimerUtil();
    private long currentDelay = 25;

    public Eagle() {
        super("Eagle", "Automatically sneaks at block edges for legit fastbridging", Category.UTILITY);
        addSettings(delay, blocksOnly, pitchCheck);
    }

    @Override
    public String getSuffix() {
        return String.format("%.0f-%.0f", delay.getValueMin(), delay.getValueMax());
    }

    @Override
    public void onDisable() {
        if (sneaking && mc.thePlayer != null && mc.gameSettings != null) {
            int sneakKey = mc.gameSettings.keyBindSneak.getKeyCode();
            KeyBinding.setKeyBindState(sneakKey, RotationUtil.isPhysicalKeyDown(sneakKey));
            sneaking = false;
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!event.isPre() || mc.thePlayer == null || mc.theWorld == null) return;

        if (blocksOnly.isEnabled()) {
            ItemStack held = mc.thePlayer.getHeldItem();
            if (held == null || !(held.getItem() instanceof ItemBlock)) {
                if (sneaking) {
                    resetSneak();
                }
                return;
            }
        }

        if (pitchCheck.isEnabled() && mc.thePlayer.rotationPitch < 65.0F) {
            if (sneaking) {
                resetSneak();
            }
            return;
        }

        BlockPos under = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1.0, mc.thePlayer.posZ);
        boolean isAir = mc.theWorld.isAirBlock(under);

        if (mc.thePlayer.onGround && isAir && mc.thePlayer.movementInput.moveForward < 0.0F) {
            if (!sneaking && timer.hasTimeElapsed(currentDelay)) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), true);
                sneaking = true;
                currentDelay = (long) delay.getRandomValue();
                timer.reset();
            }
        } else if (sneaking && timer.hasTimeElapsed(currentDelay)) {
            resetSneak();
            currentDelay = (long) delay.getRandomValue();
            timer.reset();
        }
    }

    private void resetSneak() {
        if (mc.gameSettings != null) {
            int sneakKey = mc.gameSettings.keyBindSneak.getKeyCode();
            KeyBinding.setKeyBindState(sneakKey, RotationUtil.isPhysicalKeyDown(sneakKey));
        }
        sneaking = false;
    }
}
