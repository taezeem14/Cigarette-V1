package dev.taezeem14.cigarette.module.impl.utility;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.TickEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.util.math.RotationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class AutoTool extends Module {

    private final BooleanSetting switchBack = new BooleanSetting("Switch Back", true);

    private int originalSlot = -1;
    private boolean mining = false;

    public AutoTool() {
        super("AutoTool", "Automatically switches to the optimal tool when mining", Category.UTILITY);
        addSettings(switchBack);
    }

    @Override
    public String getSuffix() {
        return switchBack.isEnabled() ? "Switch" : "Stay";
    }

    @Override
    public void onDisable() {
        if (mining && switchBack.isEnabled() && originalSlot != -1 && mc.player != null) {
            mc.player.getInventory().setSelectedSlot(originalSlot);
        }
        originalSlot = -1;
        mining = false;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!event.isPre() || mc.player == null || mc.level == null) return;

        if (mc.gui.screen() == null && RotationUtil.isPhysicalKeyDown(mc.options.keyAttack)
                && mc.hitResult instanceof BlockHitResult bhr
                && mc.hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = bhr.getBlockPos();
            BlockState state = mc.level.getBlockState(pos);
            int bestSlot = findBestTool(state);
            if (bestSlot != -1) {
                if (!mining) {
                    originalSlot = mc.player.getInventory().getSelectedSlot();
                    mining = true;
                }
                mc.player.getInventory().setSelectedSlot(bestSlot);
            }
        } else if (mining) {
            if (switchBack.isEnabled() && originalSlot != -1) {
                mc.player.getInventory().setSelectedSlot(originalSlot);
            }
            originalSlot = -1;
            mining = false;
        }
    }

    private int findBestTool(BlockState state) {
        int bestSlot = -1;
        float bestSpeed = 1.0F;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                float speed = stack.getDestroySpeed(state);
                if (speed > bestSpeed) {
                    bestSpeed = speed;
                    bestSlot = i;
                }
            }
        }
        return bestSlot;
    }
}
