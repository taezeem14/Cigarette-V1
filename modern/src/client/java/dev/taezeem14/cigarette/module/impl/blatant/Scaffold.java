package dev.taezeem14.cigarette.module.impl.blatant;

import com.mojang.blaze3d.platform.InputConstants;
import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.MotionEvent;
import dev.taezeem14.cigarette.event.impl.Render2DEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.util.math.RotationUtil;
import dev.taezeem14.cigarette.util.player.BlockUtil;
import dev.taezeem14.cigarette.util.player.InventoryUtil;
import dev.taezeem14.cigarette.util.player.MoveUtil;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

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
        if (mc.player != null) {
            originalSlot = mc.player.getInventory().getSelectedSlot();
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
        if (mc.player != null) {
            int current = mc.player.getInventory().getSelectedSlot();
            if (blockPicker.is("Spoof")) {
                if (serverSlot != -1 && serverSlot != current && mc.getConnection() != null) {
                    mc.getConnection().send(new ServerboundSetCarriedItemPacket(current));
                }
            } else if (originalSlot != -1) {
                mc.player.getInventory().setSelectedSlot(originalSlot);
            }
            if (eagleSneaking && mc.options != null) {
                InputConstants.Key sneakKey = KeyMappingHelper.getBoundKeyOf(mc.options.keyShift);
                KeyMapping.set(sneakKey, RotationUtil.isPhysicalKeyDown(mc.options.keyShift));
            }
        }
        originalSlot = -1;
        serverSlot = -1;
        blockFace = null;
        eagleSneaking = false;
    }

    @EventTarget
    public void onMotion(MotionEvent event) {
        if (mc.player == null || mc.level == null) return;

        if (!sprint.isEnabled()) {
            mc.player.setSprinting(false);
        }

        if (event.isPre()) {
            BlockPos targetPos = BlockPos.containing(mc.player.getX(), mc.player.getY() - 1.0, mc.player.getZ());
            boolean airUnder = mc.level.getBlockState(targetPos).isAir();
            blockFace = airUnder ? BlockUtil.getAdjacentBlockFace(targetPos) : null;

            int blockSlot = findBlockSlot();
            if (mode.is("Eagle") && mc.options != null) {
                InputConstants.Key sneakKey = KeyMappingHelper.getBoundKeyOf(mc.options.keyShift);
                if (mc.player.onGround() && airUnder && blockSlot != -1) {
                    KeyMapping.set(sneakKey, true);
                    eagleSneaking = true;
                } else if (eagleSneaking) {
                    KeyMapping.set(sneakKey, RotationUtil.isPhysicalKeyDown(mc.options.keyShift));
                    eagleSneaking = false;
                }
            } else if (eagleSneaking && mc.options != null) {
                KeyMapping.set(KeyMappingHelper.getBoundKeyOf(mc.options.keyShift), RotationUtil.isPhysicalKeyDown(mc.options.keyShift));
                eagleSneaking = false;
            }

            if (mode.is("Telly") && mc.player.onGround() && MoveUtil.isMoving()
                    && !(tower.isEnabled() && mc.options.keyJump.isDown())) {
                mc.player.jumpFromGround();
            }

            if (blockFace == null) {
                int current = mc.player.getInventory().getSelectedSlot();
                if (blockPicker.is("Spoof") && serverSlot != -1 && serverSlot != current && mc.getConnection() != null) {
                    mc.getConnection().send(new ServerboundSetCarriedItemPacket(current));
                    serverSlot = current;
                }
                return;
            }

            Vec3 hitVec = BlockUtil.getHitVec(blockFace.getPos(), blockFace.getFacing());
            float[] rots = RotationUtil.getRotationsToVec(hitVec);
            float targetYaw = rots[0];
            float targetPitch = (rotations.is("Diagonal") || mode.is("GodBridge"))
                    ? (mode.is("GodBridge") ? 79.5F : pitch.getValueFloat())
                    : rots[1];

            float[] fixed = RotationUtil.applySensitivityGCD(
                    new float[]{mc.player.getYRot(), mc.player.getXRot()},
                    new float[]{targetYaw, Mth.clamp(targetPitch, -90.0F, 90.0F)},
                    RotationUtil.getMouseSensitivity()
            );
            event.setYaw(fixed[0]);
            event.setPitch(Mth.clamp(fixed[1], -90.0F, 90.0F));

            if (tower.isEnabled() && blockSlot != -1 && mc.options.keyJump.isDown() && !MoveUtil.isMoving()) {
                mc.player.setDeltaMovement(0.0, 0.41999998688697815, 0.0);
            }

            if (blockSlot != -1) {
                if (blockPicker.is("Switch")) {
                    mc.player.getInventory().setSelectedSlot(blockSlot);
                    serverSlot = blockSlot;
                } else if (blockPicker.is("Spoof") && serverSlot != blockSlot && mc.getConnection() != null) {
                    mc.getConnection().send(new ServerboundSetCarriedItemPacket(blockSlot));
                    serverSlot = blockSlot;
                }
            }
        } else if (event.isPost()) {
            if (blockFace == null || mc.gameMode == null) return;

            int slot = findBlockSlot();
            if (slot == -1) {
                blockFace = null;
                return;
            }

            Vec3 hitVec = BlockUtil.getHitVec(blockFace.getPos(), blockFace.getFacing());
            BlockHitResult hitResult = new BlockHitResult(hitVec, blockFace.getFacing(), blockFace.getPos(), false);
            int savedSlot = mc.player.getInventory().getSelectedSlot();
            if (blockPicker.is("Spoof")) {
                mc.player.getInventory().setSelectedSlot(slot);
            }
            try {
                InteractionResult res = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult);
                if (res.consumesAction()) {
                    mc.player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
                }
            } finally {
                if (blockPicker.is("Spoof")) {
                    mc.player.getInventory().setSelectedSlot(savedSlot);
                }
            }
            blockFace = null;
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!renderBlockCount.isEnabled() || mc.player == null) return;
        int count = getTotalBlockCount();
        String text = count == 1 ? "1 block" : count + " blocks";

        float textW = RenderUtil.getSmallStringWidth(text);
        float textH = RenderUtil.getSmallFontHeight();
        float dotSize = 4.0F;
        float padX = 7.0F;
        float gap = 5.0F;
        float pillW = padX * 2.0F + dotSize + gap + textW;
        float pillH = Math.max(15.0F, textH + 6.0F);
        float pillX = event.getScreenWidth() / 2.0F - pillW / 2.0F;
        float pillY = event.getScreenHeight() / 2.0F + 16.0F;

        int accent = ColorUtil.getAccent();
        int dotCol = count == 0 ? 0xFFFF5555 : (count <= 16 ? 0xFFFFAA00 : accent);
        int outlineCol = count == 0 ? 0xAAFF5555 : ColorUtil.modifyAlpha(accent, 140);

        RenderUtil.drawDropShadow(event.getGraphics(), pillX, pillY, pillW, pillH, 4.0F, 0x66000000);
        RenderUtil.drawRoundedRect(event.getGraphics(), pillX, pillY, pillW, pillH, 4.0F, ColorUtil.modifyAlpha(ColorUtil.BACKGROUND, 225));
        RenderUtil.drawRoundedOutline(event.getGraphics(), pillX, pillY, pillW, pillH, 4.0F, 1.0F, outlineCol);
        RenderUtil.drawRoundedRect(event.getGraphics(), pillX + padX, pillY + (pillH - dotSize) / 2.0F, dotSize, dotSize, dotSize / 2.0F, dotCol);
        RenderUtil.drawSmallText(event.getGraphics(), text, pillX + padX + dotSize + gap, pillY + (pillH - textH) / 2.0F + 0.5F,
                count == 0 ? 0xFFFF7777 : ColorUtil.TEXT_PRIMARY, false);
    }

    private int findBlockSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (InventoryUtil.isValidScaffoldBlock(stack)) return i;
        }
        return -1;
    }

    public int getTotalBlockCount() {
        if (mc.player == null) return 0;
        int total = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (InventoryUtil.isValidScaffoldBlock(stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public boolean isSafeWalk() {
        return isEnabled() && safeWalk.isEnabled();
    }
}
