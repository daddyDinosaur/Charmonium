package com.charmonium.utils.blocks;

import com.charmonium.utils.rotation.RayTracingUtils;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import net.minecraft.block.*;
import net.minecraft.state.property.Properties;
import java.util.*;

public class BlockUtils {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public enum BlockSides {
        up, down, posX, posZ, negX, negZ, NONE
    }

    public static final Set<Block> walkables = Set.of(
            Blocks.AIR,
            Blocks.OAK_WALL_SIGN,
            Blocks.SUGAR_CANE,
            Blocks.TALL_GRASS,
            Blocks.SUNFLOWER,
            Blocks.DEAD_BUSH,
            Blocks.WILDFLOWERS,
            Blocks.STONE_SLAB,
            Blocks.OAK_SLAB,
            Blocks.RAIL,
            Blocks.ACTIVATOR_RAIL,
            Blocks.DETECTOR_RAIL,
            Blocks.POWERED_RAIL,
            Blocks.WHITE_CARPET
    );

    public static BlockState getBlockState(BlockPos blockPos) {
        var w = mc.world;
        return w == null ? null : w.getBlockState(blockPos);
    }

    public static Block getBlockType(BlockPos blockPos) {
        var state = getBlockState(blockPos);
        return state == null ? Blocks.AIR : state.getBlock();
    }

    public static BlockPos fromVecToBP(Vec3d block) {
        return new BlockPos((int) block.x, (int) block.y, (int) block.z);
    }

    public static Vec3d fromBPToVec(BlockPos block) {
        return new Vec3d(block.getX(), block.getY(), block.getZ());
    }

    private static final Set<Block> carpetBlocks = Set.of(
            Blocks.WHITE_CARPET, Blocks.ORANGE_CARPET, Blocks.MAGENTA_CARPET, Blocks.LIGHT_BLUE_CARPET,
            Blocks.YELLOW_CARPET, Blocks.LIME_CARPET, Blocks.PINK_CARPET, Blocks.GRAY_CARPET,
            Blocks.LIGHT_GRAY_CARPET, Blocks.CYAN_CARPET, Blocks.PURPLE_CARPET, Blocks.BLUE_CARPET,
            Blocks.BROWN_CARPET, Blocks.GREEN_CARPET, Blocks.RED_CARPET, Blocks.BLACK_CARPET
    );

    public static boolean isCarpet(BlockPos pos) {
        return carpetBlocks.contains(getBlockType(pos));
    }

    public static boolean isBlockWalkable(BlockPos pos) {
        var blockType = getBlockType(pos);
        if (carpetBlocks.contains(blockType)) return true;
        return blockType == Blocks.AIR ||
                blockType == Blocks.POPPY ||
                blockType == Blocks.SHORT_GRASS ||
                blockType == Blocks.TALL_GRASS ||
                blockType == Blocks.DANDELION ||
                blockType == Blocks.LILAC;
    }

    private static final Set<Block> clearlyNotSolid = Set.of(
            Blocks.WATER, Blocks.LAVA, Blocks.AIR, Blocks.POPPY, Blocks.SHORT_GRASS, Blocks.TALL_GRASS, Blocks.DANDELION, Blocks.LILAC, Blocks.BUBBLE_COLUMN
    );

    public static boolean isBlockSolid(BlockPos block) {
        return !clearlyNotSolid.contains(getBlockType(block));
    }

    public static boolean isStepableUp(BlockPos from, BlockPos to) {
        if (to.getY() - from.getY() != 1) return false;
        BlockPos blockBelow = to.down();
        Block blockBelowType = getBlockType(blockBelow);
        BlockState state = getBlockState(blockBelow);
        if (blockBelowType instanceof SlabBlock && state.get(Properties.SLAB_TYPE) == SlabType.BOTTOM) return true;
        if (blockBelowType instanceof StairsBlock) {
            Direction stairFacing = state.get(Properties.HORIZONTAL_FACING);
            int dx = to.getX() - from.getX();
            int dz = to.getZ() - from.getZ();
            if (((dx != 0 && stairFacing.getAxis() == Direction.Axis.X && Math.signum(dx) == Math.signum(stairFacing.getVector().getX())) ||
                    (dz != 0 && stairFacing.getAxis() == Direction.Axis.Z && Math.signum(dz) == Math.signum(stairFacing.getVector().getZ()))) &&
                    state.get(Properties.BLOCK_HALF) == BlockHalf.BOTTOM) return true;
        }
        return false;
    }

    public static double distanceFromToXZ(BlockPos pos1, BlockPos pos2) {
        double d1 = pos1.getX() - pos2.getX();
        double d2 = pos1.getZ() - pos2.getZ();
        return Math.sqrt(d1 * d1 + d2 * d2);
    }

    public static double distanceFromToXZ(Vec3d vec1, Vec3d vec2) {
        double d1 = vec1.x - vec2.x, d2 = vec1.z - vec2.z;
        return Math.sqrt(d1 * d1 + d2 * d2);
    }

    public static Vec3d getCenteredVec(Vec3d init) {
        return new Vec3d(init.x + 0.5, init.y, init.z + 0.5);
    }

    public static int amountNonAir(Iterable<BlockPos> blocks) {
        int count = 0;
        for (BlockPos i : blocks)
            if (!isBlockWalkable(i)) count++;
        return count;
    }

    public static double distanceFromTo(BlockPos pos1, BlockPos pos2) {
        double d1 = pos1.getX() - pos2.getX();
        double d2 = pos1.getY() - pos2.getY();
        double d3 = pos1.getZ() - pos2.getZ();
        return Math.sqrt(d1 * d1 + d2 * d2 + d3 * d3);
    }

    public static double distanceFromTo(Vec3d pos1, Vec3d pos2) {
        return pos1.distanceTo(pos2);
    }

    public static BlockPos getClosest(List<BlockPos> blocks, BlockPos around) {
        double minDist = Double.MAX_VALUE;
        BlockPos closest = null;
        for (BlockPos pos : blocks) {
            double d = distanceFromTo(pos, around);
            if (d < minDist) { minDist = d; closest = pos; }
        }
        return closest;
    }

    public static Vec3d getClosest(List<Vec3d> blocks, Vec3d around) {
        double minDist = Double.MAX_VALUE;
        Vec3d closest = null;
        for (Vec3d pos : blocks) {
            double d = pos.distanceTo(around);
            if (d < minDist) { minDist = d; closest = pos; }
        }
        return closest;
    }

    public static BlockPos getClosest(List<BlockPos> blocks, Set<BlockPos> broken, BlockPos around) {
        double minDist = Double.MAX_VALUE;
        BlockPos closest = null;
        for (BlockPos pos : blocks) {
            if (!broken.contains(pos) && RayTracingUtils.isHittable(pos)) {
                double d = distanceFromTo(pos, around);
                if (d < minDist) { minDist = d; closest = pos; }
            }
        }
        return closest;
    }

    public static boolean canMineBlock(BlockPos b) {
        if (mc.player == null) return false;
        Vec3d eyePos = mc.player.getEyePos();
        return !getAllVisibilityLines(b, eyePos).isEmpty();
    }

    public static List<Vec3d> getAllVisibilityLines(BlockPos pos, Vec3d fromEye) {
        return getAllVisibilityLines(pos, fromEye, true);
    }

    public static List<Vec3d> getAllVisibilityLines(BlockPos pos, Vec3d fromEye, boolean lowerY) {
        List<Vec3d> lines = new ArrayList<>(4);
        int accuracyChecks = 8;
        float accuracy = 1f / accuracyChecks;
        float spaceFromEdge = lowerY ? 0.1f : 8;
        double baseX = pos.getX(), baseY = pos.getY(), baseZ = pos.getZ();

        for (float x = (float)(baseX + spaceFromEdge); x <= baseX + (1f - spaceFromEdge); x += accuracy)
            for (float y = (float)(baseY + spaceFromEdge); y <= baseY + (1f - spaceFromEdge); y += accuracy)
                for (float z = (float)(baseZ + spaceFromEdge); z <= baseZ + (1f - spaceFromEdge); z += accuracy) {
                    Vec3d target = new Vec3d(x, y, z);
                    if (fromEye.distanceTo(target) > 4f) continue;
                    BlockHitResult hit = mc.world.raycast(new RaycastContext(
                            fromEye, target,
                            RaycastContext.ShapeType.COLLIDER,
                            RaycastContext.FluidHandling.NONE,
                            mc.player
                    ));
                    if (hit != null && hit.getBlockPos().equals(pos)) lines.add(target);
                }
        return lines;
    }

    public static Vec3d getNormalVecBetweenVecsRev(Vec3d vec1, Vec3d vec2) {
        Vec3d dir = vec2.subtract(vec1).normalize();
        double x = dir.x * 0 - dir.z * 1;
        double z = dir.x * 1 + dir.z * 0;
        return new Vec3d(x, dir.y, z);
    }

    public static boolean canWalkThrough(BlockPos blockPos) {
        return canWalkThrough(blockPos, null);
    }

    public static boolean canWalkThrough(BlockPos blockPos, Direction direction) {
        return canWalkThroughBottom(blockPos, direction) && canWalkThroughAbove(blockPos.up(), direction);
    }

    public static Block getBlock(BlockPos blockPos) {
        var w = mc.world;
        return w == null ? Blocks.AIR : w.getBlockState(blockPos).getBlock();
    }

    private static boolean canWalkThroughBottom(BlockPos blockPos, Direction direction) {
        var w = mc.world;
        if (w == null) return false;
        BlockState state = w.getBlockState(blockPos);
        Block block = state.getBlock();
        if (isAirColumn(blockPos)) return false;
        Vec3d playerPos = mc.player.getPos();
        double remY = playerPos.y % 1;
        if (remY >= 0.5 && remY <= 0.75) return true;
        if (block instanceof AirBlock || block instanceof CarpetBlock || block instanceof SnowBlock) return true;
        if (block instanceof DoorBlock && direction != null) return canWalkThroughDoor(blockPos, state, direction);
        if (block instanceof FenceBlock) return false;
        if (block instanceof FenceGateBlock) return state.get(Properties.OPEN);
        if (block instanceof TrapdoorBlock) {
            if (state.get(Properties.OPEN)) return true;
            state.get(Properties.BLOCK_HALF);
            return false;
        }
        if (block instanceof SlabBlock) {
            if (remY < 0.5) return state.get(Properties.SLAB_TYPE) == SlabType.BOTTOM;
            return true;
        }
        if (block instanceof StairsBlock) {
            Direction facing = state.get(Properties.HORIZONTAL_FACING);
            BlockPos playerBlock = mc.player.getBlockPos();
            BlockPos diff = blockPos.subtract(playerBlock);
            if (state.get(Properties.BLOCK_HALF) == BlockHalf.TOP) return false;
            return switch (facing) {
                case NORTH -> diff.getZ() < 0;
                case SOUTH -> diff.getZ() > 0;
                case WEST -> diff.getX() < 0;
                case EAST -> diff.getX() > 0;
                default -> false;
            };
        }
        return state.isAir();
    }

    public static ArrayList<BlockSides> getAdjBlocksNotCovered(BlockPos blockToSearch) {
        ArrayList<BlockSides> blockSidesNotCovered = new ArrayList<>(6);
        if (isPassable(blockToSearch.up())) blockSidesNotCovered.add(BlockSides.up);
        if (isPassable(blockToSearch.down())) blockSidesNotCovered.add(BlockSides.down);
        if (isPassable(blockToSearch.add(1, 0, 0))) blockSidesNotCovered.add(BlockSides.posX);
        if (isPassable(blockToSearch.add(-1, 0, 0))) blockSidesNotCovered.add(BlockSides.negX);
        if (isPassable(blockToSearch.add(0, 0, 1))) blockSidesNotCovered.add(BlockSides.posZ);
        if (isPassable(blockToSearch.add(0, 0, -1))) blockSidesNotCovered.add(BlockSides.negZ);
        return blockSidesNotCovered;
    }

    public static boolean isPassable(Block block) {
        return walkables.contains(block);
    }

    public static boolean isPassable(BlockPos block) {
        return isPassable(getBlock(block));
    }

    private static boolean isAirColumn(BlockPos pos) {
        var w = mc.world;
        int y = pos.getY(), minY = w == null ? 0 : w.getBottomY();
        int x = pos.getX(), z = pos.getZ();
        for (; y >= minY; y--) if (!w.getBlockState(new BlockPos(x, y, z)).isAir()) return false;
        return true;
    }

    private static boolean canWalkThroughDoor(BlockPos pos, BlockState state, Direction direction) {
        Direction doorFacing = state.get(Properties.HORIZONTAL_FACING);
        boolean isOpen = state.get(Properties.OPEN);
        return isOpen && doorFacing.getAxis() == direction.getAxis();
    }

    private static boolean canWalkThroughAbove(BlockPos blockPos, Direction direction) {
        var w = mc.world;
        if (w == null) return false;
        BlockState state = w.getBlockState(blockPos);
        Block block = state.getBlock();
        if (block instanceof CarpetBlock) return false;
        if (block instanceof DoorBlock && direction != null) return canWalkThroughDoor(blockPos.down(), state, direction);
        if (block instanceof TrapdoorBlock) {
            Direction playerFacing = Direction.fromHorizontalDegrees(mc.player.getYaw());
            Direction trapdoorFacing = state.get(Properties.HORIZONTAL_FACING);
            return state.get(Properties.OPEN) && trapdoorFacing.getAxis() == playerFacing.getAxis();
        }
        return state.isAir();
    }

    public static boolean isStairOrBottomSlab(BlockPos pos) {
        BlockState state = getBlockState(pos.down());
        if (state == null) return false;
        Block block = state.getBlock();
        if (block instanceof SlabBlock) return state.get(Properties.SLAB_TYPE) == SlabType.BOTTOM;
        if (block instanceof StairsBlock) return state.get(Properties.BLOCK_HALF) == BlockHalf.BOTTOM;
        return false;
    }
}
