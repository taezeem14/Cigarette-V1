package dev.taezeem14.cigarette.module.impl.render;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.Render3DEvent;
import dev.taezeem14.cigarette.event.impl.TickEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.ColorSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.util.render.RenderUtil;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BedESP extends Module {

    private final NumberSetting scanRadius = new NumberSetting("Scan Radius", 32.0, 8.0, 48.0, 2.0);
    private final ColorSetting bedColor = new ColorSetting("Bed Color", 0xFFFF5722);
    private final BooleanSetting fill = new BooleanSetting("Fill", true);
    private final BooleanSetting outline = new BooleanSetting("Outline", true);
    private final NumberSetting lineWidth = new NumberSetting("Line Width", 1.5, 0.5, 3.0, 0.1);
    private final BooleanSetting defenseOverlay = new BooleanSetting("Defense Overlay", true);

    private final List<BlockPos> bedPositions = new CopyOnWriteArrayList<>();
    private final List<DefenseBlock> defenseBlocks = new CopyOnWriteArrayList<>();
    private int tickCounter = 0;

    public BedESP() {
        super("BedESP", "Highlights beds and defense layers in Bedwars / Eggwars", Category.RENDER);
        addSettings(scanRadius, bedColor, fill, outline, lineWidth, defenseOverlay);
    }

    @Override
    public void onDisable() {
        bedPositions.clear();
        defenseBlocks.clear();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;

        if (tickCounter++ % 20 == 0) {
            scanWorld();
        }
    }

    private void scanWorld() {
        new Thread(() -> {
            if (mc.theWorld == null || mc.thePlayer == null) return;

            int radius = scanRadius.getValueInt();
            int playerX = (int) mc.thePlayer.posX;
            int playerY = (int) mc.thePlayer.posY;
            int playerZ = (int) mc.thePlayer.posZ;

            List<BlockPos> foundBeds = new ArrayList<>();
            List<DefenseBlock> foundDefense = new ArrayList<>();

            for (int x = -radius; x <= radius; x++) {
                for (int y = -16; y <= 16; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        BlockPos pos = new BlockPos(playerX + x, playerY + y, playerZ + z);
                        Block block = mc.theWorld.getBlockState(pos).getBlock();

                        if (block == Blocks.bed) {
                            foundBeds.add(pos);

                            if (defenseOverlay.isEnabled()) {
                                scanDefense(pos, foundDefense);
                            }
                        }
                    }
                }
            }

            bedPositions.clear();
            bedPositions.addAll(foundBeds);

            defenseBlocks.clear();
            defenseBlocks.addAll(foundDefense);
        }, "BedESP-Scanner").start();
    }

    private void scanDefense(BlockPos bedPos, List<DefenseBlock> output) {
        BlockPos[] neighbors = {
                bedPos.up(), bedPos.down(),
                bedPos.north(), bedPos.south(),
                bedPos.east(), bedPos.west()
        };

        for (BlockPos neighbor : neighbors) {
            Block block = mc.theWorld.getBlockState(neighbor).getBlock();
            if (block == Blocks.obsidian) {
                output.add(new DefenseBlock(neighbor, 0xFF4A148C)); // Dark Purple
            } else if (block == Blocks.end_stone) {
                output.add(new DefenseBlock(neighbor, 0xFFFFF59D)); // Pale Yellow
            } else if (block == Blocks.planks) {
                output.add(new DefenseBlock(neighbor, 0xFF8D6E63)); // Brown Wood
            } else if (block == Blocks.stained_hardened_clay) {
                output.add(new DefenseBlock(neighbor, 0xFFFF7043)); // Clay
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;

        // Render Beds
        for (BlockPos pos : bedPositions) {
            AxisAlignedBB bb = new AxisAlignedBB(
                    pos.getX(), pos.getY(), pos.getZ(),
                    pos.getX() + 1.0, pos.getY() + 0.5625, pos.getZ() + 1.0
            );

            RenderUtil.draw3DBox(bb, bedColor.getRGB(), fill.isEnabled(), outline.isEnabled(), lineWidth.getValueFloat());
        }

        // Render Defenses
        if (defenseOverlay.isEnabled()) {
            for (DefenseBlock defense : defenseBlocks) {
                BlockPos pos = defense.pos;
                AxisAlignedBB bb = new AxisAlignedBB(
                        pos.getX(), pos.getY(), pos.getZ(),
                        pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0
                );

                RenderUtil.draw3DBox(bb, defense.color, fill.isEnabled(), outline.isEnabled(), 1.0f);
            }
        }
    }

    private static class DefenseBlock {
        final BlockPos pos;
        final int color;

        DefenseBlock(BlockPos pos, int color) {
            this.pos = pos;
            this.color = color;
        }
    }
}
