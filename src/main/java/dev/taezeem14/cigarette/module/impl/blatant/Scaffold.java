package dev.taezeem14.cigarette.module.impl.blatant;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.MotionEvent;
import dev.taezeem14.cigarette.event.impl.TickEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.util.math.RotationUtil;
import dev.taezeem14.cigarette.util.math.TimerUtil;
import dev.taezeem14.cigarette.util.player.BlockUtil;
import dev.taezeem14.cigarette.util.player.MoveUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.BlockTNT;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;

public class Scaffold extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Normal", "Normal", "Telly", "GodBridge", "Eagle");
    private final ModeSetting rotations = new ModeSetting("Rotations", "Diagonal", "Diagonal", "Strict Raycast");
    private final NumberSetting pitch = new NumberSetting("Pitch", 81.0, 75.0, 85.0, 0.5);
    private final BooleanSetting tower = new BooleanSetting("Tower", true);
    private final BooleanSetting safeWalk = new BooleanSetting("SafeWalk", true);
    private final BooleanSetting sprint = new BooleanSetting("Sprint", false);
    private final ModeSetting blockPicker = new ModeSetting("Block Picker", "Switch", "Switch", "Spoof");
    private final BooleanSetting renderBlockCount = new BooleanSetting("Render Block Count", true);

    private BlockUtil.BlockFace blockFace;
    private float targetYaw, targetPitch;
    private int originalSlot = -1;
    private final TimerUtil placeTimer = new TimerUtil();

    public Scaffold() {
        super("Scaffold", "Automatically places blocks beneath your feet while moving", Category.BLATANT);
        addSettings(mode, rotations, pitch, tower, safeWalk, sprint, blockPicker, renderBlockCount);
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer != null) {
            originalSlot = mc.thePlayer.inventory.currentItem;
        }
        blockFace = null;
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer != null && originalSlot != -1) {
            if (blockPicker.is("Spoof")) {
                mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
            } else {
                mc.thePlayer.inventory.currentItem = originalSlot;
            }
        }
        originalSlot = -1;
        blockFace = null;
    }

    @EventTarget
    public void onMotion(MotionEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (!sprint.isEnabled()) {
            mc.thePlayer.setSprinting(false);
        }

        if (event.isPre()) {
            BlockPos targetPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1.0, mc.thePlayer.posZ);

            if (mc.theWorld.isAirBlock(targetPos)) {
                blockFace = BlockUtil.getAdjacentBlockFace(targetPos);
            } else {
                blockFace = null;
            }

            if (blockFace == null) return;

            // Calculate rotations
            Vec3 hitVec = BlockUtil.getHitVec(blockFace.getPos(), blockFace.getFacing());
            float[] rots = RotationUtil.getRotationsToVec(hitVec);

            if (rotations.is("Diagonal")) {
                targetYaw = rots[0];
                targetPitch = pitch.getValueFloat();
            } else {
                targetYaw = rots[0];
                targetPitch = rots[1];
            }

            event.setYaw(targetYaw);
            event.setPitch(targetPitch);

            // Tower handling
            if (tower.isEnabled() && mc.gameSettings.keyBindJump.isKeyDown() && !MoveUtil.isMoving()) {
                mc.thePlayer.motionX = 0.0;
                mc.thePlayer.motionZ = 0.0;
                mc.thePlayer.motionY = 0.41999998688697815D;
            }

            // Select block
            int blockSlot = findBlockSlot();
            if (blockSlot != -1) {
                if (blockPicker.is("Switch")) {
                    mc.thePlayer.inventory.currentItem = blockSlot;
                } else if (blockPicker.is("Spoof")) {
                    mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(blockSlot));
                }
            }
        } else if (event.isPost()) {
            if (blockFace == null) return;

            int slot = findBlockSlot();
            if (slot == -1) return;

            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
            Vec3 hitVec = BlockUtil.getHitVec(blockFace.getPos(), blockFace.getFacing());

            if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, stack, blockFace.getPos(), blockFace.getFacing(), hitVec)) {
                mc.thePlayer.swingItem();
            }

            blockFace = null;
        }
    }

    private int findBlockSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemBlock && stack.stackSize > 0) {
                Block block = ((ItemBlock) stack.getItem()).getBlock();
                if (block.isFullCube() && !(block instanceof BlockFalling) && !(block instanceof BlockTNT)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public int getTotalBlockCount() {
        if (mc.thePlayer == null) return 0;
        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemBlock) {
                Block block = ((ItemBlock) stack.getItem()).getBlock();
                if (block.isFullCube() && !(block instanceof BlockFalling) && !(block instanceof BlockTNT)) {
                    count += stack.stackSize;
                }
            }
        }
        return count;
    }

    public boolean isSafeWalk() {
        return isEnabled() && safeWalk.isEnabled();
    }
}
