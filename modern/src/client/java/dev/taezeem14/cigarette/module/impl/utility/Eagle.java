package dev.taezeem14.cigarette.module.impl.utility;

import com.mojang.blaze3d.platform.InputConstants;
import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.TickEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.DoubleSliderSetting;
import dev.taezeem14.cigarette.util.math.RotationUtil;
import dev.taezeem14.cigarette.util.math.TimerUtil;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public class Eagle extends Module {

    private final DoubleSliderSetting delay = new DoubleSliderSetting("Delay", 15.0, 40.0, 0.0, 100.0, 1.0);
    private final BooleanSetting blocksOnly = new BooleanSetting("Blocks Only", true);
    private final BooleanSetting pitchCheck = new BooleanSetting("Pitch Check (>65)", true);

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
        if (sneaking) resetSneak();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!event.isPre() || mc.player == null || mc.level == null) return;

        if (blocksOnly.isEnabled()) {
            ItemStack held = mc.player.getMainHandItem();
            if (held.isEmpty() || !(held.getItem() instanceof BlockItem)) {
                if (sneaking) resetSneak();
                return;
            }
        }
        if (pitchCheck.isEnabled() && mc.player.getXRot() < 65.0F) {
            if (sneaking) resetSneak();
            return;
        }

        BlockPos under = BlockPos.containing(mc.player.getX(), mc.player.getY() - 1.0, mc.player.getZ());
        boolean isAir = mc.level.getBlockState(under).isAir();
        boolean movingBack = mc.player.input != null && mc.player.input.getMoveVector().y < 0.0F;

        if (mc.player.onGround() && isAir && movingBack) {
            if (!sneaking && timer.hasTimeElapsed(currentDelay)) {
                KeyMapping.set(KeyMappingHelper.getBoundKeyOf(mc.options.keyShift), true);
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
        if (mc.options != null) {
            InputConstants.Key sneakKey = KeyMappingHelper.getBoundKeyOf(mc.options.keyShift);
            KeyMapping.set(sneakKey, RotationUtil.isPhysicalKeyDown(mc.options.keyShift));
        }
        sneaking = false;
    }
}
