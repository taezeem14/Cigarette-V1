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
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class BedESP extends Module {

    private final NumberSetting scanRadius = new NumberSetting("Scan Radius", 32.0, 8.0, 48.0, 2.0);
    private final ColorSetting bedColor = new ColorSetting("Bed Color", 0xFFFF5722);
    private final BooleanSetting fill = new BooleanSetting("Fill", true);
    private final BooleanSetting outline = new BooleanSetting("Outline", true);
    private final NumberSetting lineWidth = new NumberSetting("Line Width", 1.5, 0.5, 3.0, 0.1);
    private final BooleanSetting defenseOverlay = new BooleanSetting("Defense Overlay", true);

    private final List<AxisAlignedBB> bedBoxes = new CopyOnWriteArrayList<>();
    private final List<DefenseBlock> defenseBlocks = new CopyOnWriteArrayList<>();
    private final AtomicBoolean scanInFlight = new AtomicBoolean(false);
    private ScheduledExecutorService scannerExecutor;

    public BedESP() {
        super("BedESP", "Highlights beds and defense layers in Bedwars / Eggwars", Category.RENDER);
        addSettings(scanRadius, bedColor, fill, outline, lineWidth, defenseOverlay);
    }

    @Override
    public void onEnable() {
        if (scannerExecutor == null || scannerExecutor.isShutdown()) {
            scannerExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "BedESP-Scanner");
                t.setDaemon(true);
                return t;
            });
            scannerExecutor.scheduleWithFixedDelay(this::scanWorldSafe, 0L, 1000L, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void onDisable() {
        if (scannerExecutor != null && !scannerExecutor.isShutdown()) {
            scannerExecutor.shutdownNow();
            scannerExecutor = null;
        }
        scanInFlight.set(false);
        bedBoxes.clear();
        defenseBlocks.clear();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        // Scanning runs safely on the single daemon ScheduledExecutorService
    }

    private void scanWorldSafe() {
        if (!isEnabled() || !scanInFlight.compareAndSet(false, true)) {
            return;
        }

        try {
            WorldClient world = mc.theWorld;
            if (world == null || mc.thePlayer == null) {
                bedBoxes.clear();
                defenseBlocks.clear();
                return;
            }

            int radius = scanRadius.getValueInt();
            int playerX = (int) Math.floor(mc.thePlayer.posX);
            int playerY = (int) Math.floor(mc.thePlayer.posY);
            int playerZ = (int) Math.floor(mc.thePlayer.posZ);

            List<BlockPos> rawBeds = new ArrayList<>();
            for (int x = -radius; x <= radius; x++) {
                for (int y = -16; y <= 16; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        BlockPos pos = new BlockPos(playerX + x, playerY + y, playerZ + z);
                        Block block = world.getBlockState(pos).getBlock();
                        if (block == Blocks.bed) {
                            rawBeds.add(pos);
                        }
                    }
                }
            }

            Set<BlockPos> bedSet = new HashSet<>(rawBeds);
            Set<BlockPos> visitedBeds = new HashSet<>();
            Set<BlockPos> visitedDefense = new HashSet<>();
            List<AxisAlignedBB> mergedBeds = new ArrayList<>();
            List<DefenseBlock> foundDefense = new ArrayList<>();
            boolean scanDef = defenseOverlay.isEnabled();

            for (BlockPos pos : rawBeds) {
                if (!visitedBeds.add(pos)) {
                    continue;
                }

                // Combine adjacent bed half into one unified bounding box
                BlockPos pairPos = findAdjacentBed(pos, bedSet, visitedBeds);
                double minX = pos.getX();
                double minY = pos.getY();
                double minZ = pos.getZ();
                double maxX = pos.getX() + 1.0;
                double maxY = pos.getY() + 0.5625;
                double maxZ = pos.getZ() + 1.0;

                if (pairPos != null) {
                    visitedBeds.add(pairPos);
                    minX = Math.min(minX, pairPos.getX());
                    minZ = Math.min(minZ, pairPos.getZ());
                    maxX = Math.max(maxX, pairPos.getX() + 1.0);
                    maxZ = Math.max(maxZ, pairPos.getZ() + 1.0);
                }

                mergedBeds.add(new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ));

                if (scanDef) {
                    scanDefense(world, pos, visitedDefense, foundDefense);
                    if (pairPos != null) {
                        scanDefense(world, pairPos, visitedDefense, foundDefense);
                    }
                }
            }

            bedBoxes.clear();
            bedBoxes.addAll(mergedBeds);

            defenseBlocks.clear();
            defenseBlocks.addAll(foundDefense);
        } catch (Throwable ignored) {
        } finally {
            scanInFlight.set(false);
        }
    }

    private BlockPos findAdjacentBed(BlockPos pos, Set<BlockPos> bedSet, Set<BlockPos> visited) {
        BlockPos[] horizontal = {pos.north(), pos.south(), pos.east(), pos.west()};
        for (BlockPos neighbor : horizontal) {
            if (bedSet.contains(neighbor) && !visited.contains(neighbor)) {
                return neighbor;
            }
        }
        return null;
    }

    private void scanDefense(WorldClient world, BlockPos bedPos, Set<BlockPos> visitedDefense, List<DefenseBlock> output) {
        BlockPos[] neighbors = {
                bedPos.up(), bedPos.down(),
                bedPos.north(), bedPos.south(),
                bedPos.east(), bedPos.west()
        };

        for (BlockPos neighbor : neighbors) {
            if (!visitedDefense.add(neighbor)) {
                continue;
            }
            Block block = world.getBlockState(neighbor).getBlock();
            if (block == Blocks.obsidian) {
                output.add(new DefenseBlock(neighbor, 0xFF4A148C));
            } else if (block == Blocks.end_stone) {
                output.add(new DefenseBlock(neighbor, 0xFFFFF59D));
            } else if (block == Blocks.planks) {
                output.add(new DefenseBlock(neighbor, 0xFF8D6E63));
            } else if (block == Blocks.stained_hardened_clay) {
                output.add(new DefenseBlock(neighbor, 0xFFFF7043));
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;

        for (AxisAlignedBB bb : bedBoxes) {
            RenderUtil.draw3DBox(bb, bedColor.getRGB(), fill.isEnabled(), outline.isEnabled(), lineWidth.getValueFloat());
        }

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
