package dev.taezeem14.cigarette.module.impl.render;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.Render3DEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.ColorSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

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

    private final List<AABB> bedBoxes = new CopyOnWriteArrayList<>();
    private final List<DefenseBlock> defenseBlocks = new CopyOnWriteArrayList<>();
    private final AtomicBoolean scanInFlight = new AtomicBoolean(false);
    private ScheduledExecutorService scanner;

    public BedESP() {
        super("BedESP", "Highlights beds and defense layers in Bedwars", Category.RENDER);
        addSettings(scanRadius, bedColor, fill, outline, lineWidth, defenseOverlay);
    }

    @Override
    public void onEnable() {
        if (scanner == null || scanner.isShutdown()) {
            scanner = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "BedESP-Scanner");
                t.setDaemon(true);
                return t;
            });
            scanner.scheduleWithFixedDelay(this::scanWorldSafe, 0L, 1000L, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void onDisable() {
        if (scanner != null && !scanner.isShutdown()) {
            scanner.shutdownNow();
            scanner = null;
        }
        scanInFlight.set(false);
        bedBoxes.clear();
        defenseBlocks.clear();
    }

    private void scanWorldSafe() {
        if (!isEnabled() || !scanInFlight.compareAndSet(false, true)) return;
        try {
            ClientLevel level = mc.level;
            if (level == null || mc.player == null) {
                bedBoxes.clear();
                defenseBlocks.clear();
                return;
            }
            int radius = scanRadius.getValueInt();
            int px = (int) Math.floor(mc.player.getX());
            int py = (int) Math.floor(mc.player.getY());
            int pz = (int) Math.floor(mc.player.getZ());

            List<BlockPos> rawBeds = new ArrayList<>();
            for (int x = -radius; x <= radius; x++) {
                for (int y = -16; y <= 16; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        BlockPos pos = new BlockPos(px + x, py + y, pz + z);
                        if (level.getBlockState(pos).getBlock() instanceof BedBlock) {
                            rawBeds.add(pos);
                        }
                    }
                }
            }

            Set<BlockPos> bedSet = new HashSet<>(rawBeds);
            Set<BlockPos> visited = new HashSet<>();
            Set<BlockPos> visitedDef = new HashSet<>();
            List<AABB> mergedBeds = new ArrayList<>();
            List<DefenseBlock> foundDef = new ArrayList<>();
            boolean scanDef = defenseOverlay.isEnabled();

            for (BlockPos pos : rawBeds) {
                if (!visited.add(pos)) continue;
                BlockPos pair = findAdjacentBed(pos, bedSet, visited);
                double minX = pos.getX();
                double minY = pos.getY();
                double minZ = pos.getZ();
                double maxX = pos.getX() + 1.0;
                double maxY = pos.getY() + 0.5625;
                double maxZ = pos.getZ() + 1.0;
                if (pair != null) {
                    visited.add(pair);
                    minX = Math.min(minX, pair.getX());
                    minZ = Math.min(minZ, pair.getZ());
                    maxX = Math.max(maxX, pair.getX() + 1.0);
                    maxZ = Math.max(maxZ, pair.getZ() + 1.0);
                }
                mergedBeds.add(new AABB(minX, minY, minZ, maxX, maxY, maxZ));
                if (scanDef) {
                    scanDefense(level, pos, visitedDef, foundDef);
                    if (pair != null) scanDefense(level, pair, visitedDef, foundDef);
                }
            }

            bedBoxes.clear();
            bedBoxes.addAll(mergedBeds);
            defenseBlocks.clear();
            defenseBlocks.addAll(foundDef);
        } catch (Throwable ignored) {
        } finally {
            scanInFlight.set(false);
        }
    }

    private BlockPos findAdjacentBed(BlockPos pos, Set<BlockPos> bedSet, Set<BlockPos> visited) {
        for (Direction f : Direction.Plane.HORIZONTAL) {
            BlockPos adj = pos.relative(f);
            if (bedSet.contains(adj) && !visited.contains(adj)) {
                return adj;
            }
        }
        return null;
    }

    private void scanDefense(ClientLevel level, BlockPos bedPos, Set<BlockPos> visited, List<DefenseBlock> out) {
        for (Direction f : Direction.values()) {
            if (f == Direction.DOWN) continue;
            BlockPos check = bedPos.relative(f);
            if (!visited.add(check)) continue;
            Block block = level.getBlockState(check).getBlock();
            if (block == Blocks.OBSIDIAN) {
                out.add(new DefenseBlock(check, 0xFF9C27B0));
            } else if (block == Blocks.END_STONE) {
                out.add(new DefenseBlock(check, 0xFFFFEB3B));
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (mc.level == null || mc.player == null) return;
        int col = bedColor.getRGB();
        float lw = lineWidth.getValueFloat();

        for (AABB box : bedBoxes) {
            GizmoStyle style = buildStyle(col, lw, fill.isEnabled() ? 55 : 0, outline.isEnabled());
            if (style != null) {
                Gizmos.cuboid(box, style);
            }
        }

        if (defenseOverlay.isEnabled()) {
            for (DefenseBlock db : defenseBlocks) {
                Gizmos.cuboid(db.pos, GizmoStyle.strokeAndFill(db.color, 1.0F, ColorUtil.modifyAlpha(db.color, 38)));
            }
        }
    }

    private GizmoStyle buildStyle(int col, float lw, int fillAlpha, boolean doOutline) {
        int fillCol = fillAlpha > 0 ? ColorUtil.modifyAlpha(col, fillAlpha) : 0;
        if (doOutline && fillAlpha > 0) return GizmoStyle.strokeAndFill(col, lw, fillCol);
        if (doOutline) return GizmoStyle.stroke(col, lw);
        if (fillAlpha > 0) return GizmoStyle.fill(fillCol);
        return null;
    }

    private static final class DefenseBlock {
        final BlockPos pos;
        final int color;

        DefenseBlock(BlockPos pos, int color) {
            this.pos = pos;
            this.color = color;
        }
    }
}
