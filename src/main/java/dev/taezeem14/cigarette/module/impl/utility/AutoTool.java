package dev.taezeem14.cigarette.module.impl.utility;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.TickEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;

public class AutoTool extends Module {

    private final BooleanSetting switchBack = new BooleanSetting("Switch Back", true);

    private int originalSlot = -1;
    private boolean mining = false;

    public AutoTool() {
        super("AutoTool", "Automatically switches to the optimal tool when mining", Category.UTILITY);
        addSettings(switchBack);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!event.isPre() || mc.thePlayer == null || mc.theWorld == null) return;

        if (mc.gameSettings.keyBindAttack.isKeyDown() && mc.objectMouseOver != null
                && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            BlockPos pos = mc.objectMouseOver.getBlockPos();
            IBlockState state = mc.theWorld.getBlockState(pos);

            int bestSlot = findBestTool(state);
            if (bestSlot != -1) {
                if (!mining) {
                    originalSlot = mc.thePlayer.inventory.currentItem;
                    mining = true;
                }
                mc.thePlayer.inventory.currentItem = bestSlot;
            }
        } else {
            if (mining) {
                if (switchBack.isEnabled() && originalSlot != -1) {
                    mc.thePlayer.inventory.currentItem = originalSlot;
                }
                originalSlot = -1;
                mining = false;
            }
        }
    }

    private int findBestTool(IBlockState state) {
        int bestSlot = -1;
        float bestSpeed = 1.0F;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null) {
                float speed = stack.getStrVsBlock(state.getBlock());
                if (speed > bestSpeed) {
                    bestSpeed = speed;
                    bestSlot = i;
                }
            }
        }

        return bestSlot;
    }
}
