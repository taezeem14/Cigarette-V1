package dev.taezeem14.cigarette.module.impl.blatant;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.MotionEvent;
import dev.taezeem14.cigarette.event.impl.Render2DEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.ui.font.FontUtil;
import dev.taezeem14.cigarette.util.math.RotationUtil;
import dev.taezeem14.cigarette.util.player.BlockUtil;
import dev.taezeem14.cigarette.util.player.MoveUtil;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.BlockTNT;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public class Scaffold extends Module {

    private static Scaffold instance;

    private final ModeSetting mode = new ModeSetting("Mode", "Normal", "Normal", "Telly", "GodBridge", "Eagle");
    private final ModeSetting rotations = new ModeSetting("Rotations", "Diagonal", "Diagonal", "Strict Raycast");
    private final NumberSetting pitch = new NumberSetting("Pitch", 81.0, 75.0, 85.0, 0.5);
    private final BooleanSetting tower = new BooleanSetting("Tower", true);
    private final BooleanSetting safeWalk = new BooleanSetting("SafeWalk", true);
    private final BooleanSetting sprint = new BooleanSetting("Sprint", false);
    private final ModeSetting blockPicker = new ModeSetting("Block Picker", "Switch", "Switch", "Spoof");
    private final BooleanSetting renderBlockCount = new BooleanSetting("Render Block Count", true);

    private BlockUtil.BlockFace blockFace;
    private float targetYaw;
    private float targetPitch;
    private int originalSlot = -1;
    private int serverSlot = -1;
    private boolean eagleSneaking = false;

    public Scaffold() {
        super("Scaffold", "Automatically places blocks beneath your feet while moving", Category.BLATANT);
        addSettings(mode, rotations, pitch, tower, safeWalk, sprint, blockPicker, renderBlockCount);
        instance = this;
    }

    public static Scaffold getInstance() {
        return instance;
    }

    @Override
    public String getSuffix() {
        return mode.getMode();
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer != null) {
            originalSlot = mc.thePlayer.inventory.currentItem;
            serverSlot = originalSlot;
        } else {
            originalSlot = -1;
            serverSlot = -1;
        }
        blockFace = null;
        eagleSneaking = false;
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer != null) {
            if (blockPicker.is("Spoof")) {
                if (serverSlot != -1 && serverSlot != mc.thePlayer.inventory.currentItem && mc.getNetHandler() != null) {
                    mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
                }
            } else if (originalSlot != -1) {
                mc.thePlayer.inventory.currentItem = originalSlot;
            }

            if (eagleSneaking && mc.gameSettings != null) {
                int sneakKey = mc.gameSettings.keyBindSneak.getKeyCode();
                KeyBinding.setKeyBindState(sneakKey, RotationUtil.isPhysicalKeyDown(sneakKey));
            }
        }
        originalSlot = -1;
        serverSlot = -1;
        blockFace = null;
        eagleSneaking = false;
    }

    @EventTarget
    public void onMotion(MotionEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (!sprint.isEnabled()) {
            mc.thePlayer.setSprinting(false);
        }

        if (event.isPre()) {
            BlockPos targetPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1.0, mc.thePlayer.posZ);
            boolean airUnder = mc.theWorld.isAirBlock(targetPos);

            if (airUnder) {
                blockFace = BlockUtil.getAdjacentBlockFace(targetPos);
            } else {
                blockFace = null;
            }

            int blockSlot = findBlockSlot();

            // Eagle mode: auto-sneak over air when placing blocks
            if (mode.is("Eagle") && mc.gameSettings != null) {
                int sneakKey = mc.gameSettings.keyBindSneak.getKeyCode();
                if (mc.thePlayer.onGround && airUnder && blockSlot != -1) {
                    KeyBinding.setKeyBindState(sneakKey, true);
                    eagleSneaking = true;
                } else if (eagleSneaking) {
                    KeyBinding.setKeyBindState(sneakKey, RotationUtil.isPhysicalKeyDown(sneakKey));
                    eagleSneaking = false;
                }
            } else if (eagleSneaking && mc.gameSettings != null) {
                int sneakKey = mc.gameSettings.keyBindSneak.getKeyCode();
                KeyBinding.setKeyBindState(sneakKey, RotationUtil.isPhysicalKeyDown(sneakKey));
                eagleSneaking = false;
            }

            // Telly mode: auto-jump on ground while moving
            if (mode.is("Telly") && mc.thePlayer.onGround && MoveUtil.isMoving()
                    && !(tower.isEnabled() && mc.gameSettings.keyBindJump.isKeyDown())) {
                mc.thePlayer.jump();
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

            float[] fixed = RotationUtil.applySensitivityGCD(
                    new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch},
                    new float[]{targetYaw, MathHelper.clamp_float(targetPitch, -90.0F, 90.0F)},
                    mc.gameSettings.mouseSensitivity
            );
            event.setYaw(fixed[0]);
            event.setPitch(MathHelper.clamp_float(fixed[1], -90.0F, 90.0F));

            // Tower handling
            if (tower.isEnabled() && blockSlot != -1 && mc.gameSettings.keyBindJump.isKeyDown() && !MoveUtil.isMoving()) {
                mc.thePlayer.motionX = 0.0;
                mc.thePlayer.motionZ = 0.0;
                mc.thePlayer.motionY = 0.41999998688697815D;
            }

            // Select block (send C09 only when serverSlot changes)
            if (blockSlot != -1) {
                if (blockPicker.is("Switch")) {
                    mc.thePlayer.inventory.currentItem = blockSlot;
                    serverSlot = blockSlot;
                } else if (blockPicker.is("Spoof")) {
                    if (serverSlot != blockSlot && mc.getNetHandler() != null) {
                        mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(blockSlot));
                        serverSlot = blockSlot;
                    }
                }
            }
        } else if (event.isPost()) {
            if (blockFace == null) return;

            int slot = findBlockSlot();
            if (slot == -1 || mc.playerController == null) {
                blockFace = null;
                return;
            }

            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
            if (stack == null) {
                blockFace = null;
                return;
            }

            Vec3 hitVec = BlockUtil.getHitVec(blockFace.getPos(), blockFace.getFacing());
            int savedClientSlot = mc.thePlayer.inventory.currentItem;

            if (blockPicker.is("Spoof")) {
                mc.thePlayer.inventory.currentItem = slot;
            }
            try {
                if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, stack, blockFace.getPos(), blockFace.getFacing(), hitVec)) {
                    mc.thePlayer.swingItem();
                }
            } finally {
                if (blockPicker.is("Spoof")) {
                    mc.thePlayer.inventory.currentItem = savedClientSlot;
                }
            }

            blockFace = null;
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!renderBlockCount.isEnabled() || mc.thePlayer == null || mc.gameSettings.showDebugInfo) {
            return;
        }

        ScaledResolution sr = event.getScaledResolution() != null ? event.getScaledResolution() : new ScaledResolution(mc);
        int count = getTotalBlockCount();
        String text = count == 1 ? "1 block" : count + " blocks";

        FontUtil.ensureInitialized();
        float textW = FontUtil.small.getStringWidth(text);
        float textH = FontUtil.small.getHeight();

        float dotSize = 4.0f;
        float paddingX = 7.0f;
        float gap = 5.0f;
        float pillW = paddingX * 2.0f + dotSize + gap + textW;
        float pillH = Math.max(15.0f, textH + 6.0f);
        float pillX = sr.getScaledWidth() / 2.0f - pillW / 2.0f;
        float pillY = sr.getScaledHeight() / 2.0f + 16.0f;

        int accent = ColorUtil.getAccent();
        int indicatorColor = count == 0 ? 0xFFFF5555 : (count <= 16 ? 0xFFFFAA00 : accent);
        int outlineColor = count == 0 ? 0xAAFF5555 : ColorUtil.modifyAlpha(accent, 140);

        RenderUtil.drawDropShadow(pillX, pillY, pillW, pillH, 4.0f, 0x66000000);
        RenderUtil.drawRoundedRect(pillX, pillY, pillW, pillH, 4.0f, ColorUtil.modifyAlpha(ColorUtil.BACKGROUND, 225));
        RenderUtil.drawRoundedOutline(pillX, pillY, pillW, pillH, 4.0f, 1.0f, outlineColor);
        RenderUtil.drawRoundedRect(pillX + paddingX, pillY + (pillH - dotSize) / 2.0f, dotSize, dotSize, dotSize / 2.0f, indicatorColor);
        FontUtil.small.drawString(text, pillX + paddingX + dotSize + gap, pillY + (pillH - textH) / 2.0f + 0.5f, count == 0 ? 0xFFFF7777 : ColorUtil.TEXT_PRIMARY);
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
