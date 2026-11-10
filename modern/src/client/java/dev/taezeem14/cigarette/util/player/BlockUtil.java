package dev.taezeem14.cigarette.util.player;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class BlockUtil {

    private static final Minecraft mc = Minecraft.getInstance();

    private BlockUtil() {
    }

    public static BlockFace getAdjacentBlockFace(BlockPos targetPos) {
        if (mc.level == null || targetPos == null) return null;

        BlockFace direct = checkOffsets(targetPos);
        if (direct != null) return direct;

        for (Direction offset : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN}) {
            BlockFace candidate = checkOffsets(targetPos.relative(offset));
            if (candidate != null) return candidate;
        }
        return null;
    }

    private static BlockFace checkOffsets(BlockPos pos) {
        for (Direction facing : Direction.values()) {
            BlockPos neighbor = pos.relative(facing);
            BlockState state = mc.level.getBlockState(neighbor);
            if (!state.isAir() && !state.canBeReplaced()) {
                return new BlockFace(neighbor, facing.getOpposite());
            }
        }
        return null;
    }

    public static Vec3 getHitVec(BlockPos pos, Direction facing) {
        double x = pos.getX() + 0.5 + facing.getStepX() * 0.5;
        double y = pos.getY() + 0.5 + facing.getStepY() * 0.5;
        double z = pos.getZ() + 0.5 + facing.getStepZ() * 0.5;
        return new Vec3(x, y, z);
    }

    public static final class BlockFace {
        private final BlockPos pos;
        private final Direction facing;

        public BlockFace(BlockPos pos, Direction facing) {
            this.pos = pos;
            this.facing = facing;
        }

        public BlockPos getPos() {
            return pos;
        }

        public Direction getFacing() {
            return facing;
        }
    }
}
