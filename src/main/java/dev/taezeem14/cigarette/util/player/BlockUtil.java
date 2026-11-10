package dev.taezeem14.cigarette.util.player;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;

public final class BlockUtil {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private BlockUtil() {
    }

    public static class BlockFace {
        private final BlockPos pos;
        private final EnumFacing facing;

        public BlockFace(BlockPos pos, EnumFacing facing) {
            this.pos = pos;
            this.facing = facing;
        }

        public BlockPos getPos() {
            return pos;
        }

        public EnumFacing getFacing() {
            return facing;
        }
    }

    public static BlockFace getAdjacentBlockFace(BlockPos pos) {
        if (mc.theWorld == null) return null;

        for (EnumFacing facing : EnumFacing.values()) {
            BlockPos neighbor = pos.offset(facing);
            if (isValidBlock(neighbor)) {
                return new BlockFace(neighbor, facing.getOpposite());
            }
        }

        // Secondary search (diagonal/2-step down)
        for (EnumFacing facing : EnumFacing.HORIZONTALS) {
            BlockPos neighbor = pos.offset(facing).down();
            if (isValidBlock(neighbor)) {
                return new BlockFace(neighbor, EnumFacing.UP);
            }
        }

        return null;
    }

    public static Vec3 getHitVec(BlockPos pos, EnumFacing facing) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;

        if (facing != null) {
            x += facing.getFrontOffsetX() * 0.5D;
            y += facing.getFrontOffsetY() * 0.5D;
            z += facing.getFrontOffsetZ() * 0.5D;
        }

        return new Vec3(x, y, z);
    }

    public static boolean isValidBlock(BlockPos pos) {
        if (mc.theWorld == null || pos == null) return false;

        Block block = mc.theWorld.getBlockState(pos).getBlock();
        if (block == null || block == Blocks.air) return false;

        Material material = block.getMaterial();
        if (material.isLiquid() || !material.isSolid()) return false;

        return block.isCollidable() && block.isFullCube();
    }

    public static Block getBlock(BlockPos pos) {
        if (mc.theWorld == null || pos == null) return Blocks.air;
        return mc.theWorld.getBlockState(pos).getBlock();
    }
}
