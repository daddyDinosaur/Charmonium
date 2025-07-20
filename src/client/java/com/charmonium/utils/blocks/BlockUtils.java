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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class BlockUtils {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public enum BlockSides {
        up,
        down,
        posX,
        posZ,
        negX,
        negZ,
        NONE
    }

    public static final List<Block> walkables = Arrays.asList(
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
        if (mc.world == null) return null;
        return mc.world.getBlockState(blockPos);
    }

    public static Block getBlockType(BlockPos blockPos) {
        return getBlockState(blockPos).getBlock();
    }

    public static BlockPos fromVecToBP(Vec3d block) {
        return new BlockPos((int) block.x, (int) block.y, (int) block.z);
    }

    public static Vec3d fromBPToVec(BlockPos block) {
        return new Vec3d(block.getX(), block.getY(), block.getZ());
    }

    public static boolean isCarpet(BlockPos pos) {
        Block block = getBlockType(pos);
        return block == Blocks.WHITE_CARPET
                || block == Blocks.ORANGE_CARPET
                || block == Blocks.MAGENTA_CARPET
                || block == Blocks.LIGHT_BLUE_CARPET
                || block == Blocks.YELLOW_CARPET
                || block == Blocks.LIME_CARPET
                || block == Blocks.PINK_CARPET
                || block == Blocks.GRAY_CARPET
                || block == Blocks.LIGHT_GRAY_CARPET
                || block == Blocks.CYAN_CARPET
                || block == Blocks.PURPLE_CARPET
                || block == Blocks.BLUE_CARPET
                || block == Blocks.BROWN_CARPET
                || block == Blocks.GREEN_CARPET
                || block == Blocks.RED_CARPET
                || block == Blocks.BLACK_CARPET;
    }

    public static boolean isBlockWalkable(BlockPos pos) {
        if (isCarpet(pos)) return true;
        Block blockType = getBlockType(pos);
        return blockType == Blocks.AIR ||
                blockType == Blocks.POPPY ||
                blockType == Blocks.SHORT_GRASS ||
                blockType == Blocks.TALL_GRASS ||
                blockType == Blocks.DANDELION ||
                blockType == Blocks.LILAC;
    }

    public static boolean isBlockSolid(BlockPos block) {
        Block blockType = getBlockType(block);
        return blockType != Blocks.WATER &&
                blockType != Blocks.LAVA &&
                blockType != Blocks.AIR &&
                blockType != Blocks.POPPY &&
                blockType != Blocks.SHORT_GRASS &&
                blockType != Blocks.TALL_GRASS &&
                blockType != Blocks.DANDELION &&
                blockType != Blocks.LILAC &&
                blockType != Blocks.BUBBLE_COLUMN;
    }

    public static boolean isStepableUp(BlockPos from, BlockPos to) {
        if (to.getY() - from.getY() != 1) return false;
        BlockPos blockBelow = to.down();
        Block blockBelowType = getBlockType(blockBelow);
        if (blockBelowType instanceof SlabBlock) {
            BlockState state = getBlockState(blockBelow);
            if (state.get(Properties.SLAB_TYPE) == SlabType.BOTTOM) return true;
        }
        if (blockBelowType instanceof StairsBlock) {
            BlockState state = getBlockState(blockBelow);
            Direction stairFacing = state.get(Properties.HORIZONTAL_FACING);
            int dx = to.getX() - from.getX();
            int dz = to.getZ() - from.getZ();
            if ((dx != 0 && stairFacing.getAxis() == Direction.Axis.X && Math.signum(dx) == Math.signum(stairFacing.getVector().getX())) ||
                    (dz != 0 && stairFacing.getAxis() == Direction.Axis.Z && Math.signum(dz) == Math.signum(stairFacing.getVector().getZ()))) {
                return state.get(Properties.BLOCK_HALF) == BlockHalf.BOTTOM;
            }
        }
        return false;
    }

    public static double distanceFromToXZ(BlockPos pos1, BlockPos pos2) {
        final double d1 = pos1.getX() - pos2.getX();
        final double d2 = pos1.getZ() - pos2.getZ();
        return MathHelper.sqrt((float) (d1 * d1 + d2 * d2));
    }

    public static double distanceFromToXZ(Vec3d vec1, Vec3d vec2) {
        final double d1 = vec1.x - vec2.x;
        final double d2 = vec1.z - vec2.z;
        return MathHelper.sqrt((float) (d1 * d1 + d2 * d2));
    }

    public static Vec3d getCenteredVec(Vec3d init) {
        return init.add(0.5, 0, 0.5);
    }

    public static int amountNonAir(Iterable<BlockPos> blocks) {
        AtomicInteger air = new AtomicInteger();
        blocks.forEach(i -> {
            if (!isBlockWalkable(i)) {
                air.getAndIncrement();
            }
        });
        return air.get();
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
        return blocks.stream()
                .min(Comparator.comparingDouble(pos -> distanceFromTo(pos, around)))
                .orElse(null);
    }

    public static Vec3d getClosest(List<Vec3d> blocks, Vec3d around) {
        return blocks.stream()
                .min(Comparator.comparingDouble(pos -> pos.distanceTo(around)))
                .orElse(null);
    }

    public static BlockPos getClosest(List<BlockPos> blocks, Set<BlockPos> broken, BlockPos around) {
        return blocks.stream()
                .filter(pos -> !broken.contains(pos) && RayTracingUtils.isHittable(pos))
                .min(Comparator.comparingDouble(pos -> distanceFromTo(pos, around)))
                .orElse(null);
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
        List<Vec3d> lines = new ArrayList<>();
        int accuracyChecks = 8;
        float accuracy = 1f / accuracyChecks;
        float spaceFromEdge = lowerY ? 0.1f : 8;

        for (float x = pos.getX() + spaceFromEdge; x <= pos.getX() + (1f - spaceFromEdge); x += accuracy) {
            for (float y = pos.getY() + spaceFromEdge; y <= pos.getY() + (1f - spaceFromEdge); y += accuracy) {
                for (float z = pos.getZ() + spaceFromEdge; z <= pos.getZ() + (1f - spaceFromEdge); z += accuracy) {
                    Vec3d target = new Vec3d(x, y, z);
                    if (fromEye.distanceTo(target) > 4f) continue;

                    BlockHitResult hit = mc.world.raycast(new RaycastContext(
                            fromEye,
                            target,
                            RaycastContext.ShapeType.COLLIDER,
                            RaycastContext.FluidHandling.NONE,
                            mc.player
                    ));

                    if (hit != null && hit.getBlockPos().equals(pos)) {
                        lines.add(target);
                    }
                }
            }
        }
        return lines;
    }

    public static Vec3d getNormalVecBetweenVecsRev(Vec3d vec1, Vec3d vec2) {
        Vec3d dir = vec2.subtract(vec1).normalize();
        double cos = Math.cos(Math.PI / 2);
        double sin = Math.sin(Math.PI / 2);

        double x = dir.x * cos - dir.z * sin;
        double z = dir.x * sin + dir.z * cos;
        return new Vec3d(x, dir.y, z);
    }

    public static boolean canWalkThrough(BlockPos blockPos) {
        return canWalkThrough(blockPos, null);
    }

    public static boolean canWalkThrough(BlockPos blockPos, Direction direction) {
        return canWalkThroughBottom(blockPos, direction) &&
                canWalkThroughAbove(blockPos.up(), direction);
    }

    public static Block getBlock(BlockPos blockPos) {
        assert mc.world != null;
        return mc.world.getBlockState(blockPos).getBlock();
    }

    private static boolean canWalkThroughBottom(BlockPos blockPos, Direction direction) {
        if (mc.world == null) return false;
        BlockState state = mc.world.getBlockState(blockPos);
        Block block = state.getBlock();

        // Check for air column below
        if (isAirColumn(blockPos)) return false;

        // Player vertical position check
        Vec3d playerPos = mc.player.getPos();
        if (playerPos.y % 1 >= 0.5 && playerPos.y % 1 <= 0.75) return true;

        // Initial walkable blocks
        if (block instanceof AirBlock ||
                block instanceof CarpetBlock ||
                block instanceof SnowBlock) return true;

        // Door handling
        if (block instanceof DoorBlock && direction != null) {
            return canWalkThroughDoor(blockPos, state, direction);
        }

        // Fence and gate handling
        if (block instanceof FenceBlock) return false;
        if (block instanceof FenceGateBlock) return state.get(Properties.OPEN);

        // Trapdoor handling
        if (block instanceof TrapdoorBlock) {
            if (state.get(Properties.OPEN)) return true;
            state.get(Properties.BLOCK_HALF);
            return false;
        }

        // Slab handling
        if (block instanceof SlabBlock) {
            if (playerPos.y % 1 < 0.5) {
                return state.get(Properties.SLAB_TYPE) == SlabType.BOTTOM;
            }
            return true;
        }

        // Stairs handling
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
        ArrayList<BlockSides> blockSidesNotCovered = new ArrayList<>();

        if (isPassable(blockToSearch.up()))
            blockSidesNotCovered.add(BlockSides.up);
        if (isPassable(blockToSearch.down()))
            blockSidesNotCovered.add(BlockSides.down);
        if (isPassable(blockToSearch.add(1, 0, 0)))
            blockSidesNotCovered.add(BlockSides.posX);
        if (isPassable(blockToSearch.add(-1, 0, 0)))
            blockSidesNotCovered.add(BlockSides.negX);
        if (isPassable(blockToSearch.add(0, 0, 1)))
            blockSidesNotCovered.add(BlockSides.posZ);
        if (isPassable(blockToSearch.add(0, 0, -1)))
            blockSidesNotCovered.add(BlockSides.negZ);

        return blockSidesNotCovered;
    }

    public static boolean isPassable(Block block) {
        return walkables.contains(block);
    }

    public static boolean isPassable(BlockPos block) {
        return isPassable(getBlock(block));
    }


    private static boolean isAirColumn(BlockPos pos) {
        for (int y = pos.getY(); y >= mc.world.getBottomY(); y--) {
            BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
            if (!mc.world.getBlockState(checkPos).isAir()) return false;
        }
        return true;
    }

    private static boolean canWalkThroughDoor(BlockPos pos, BlockState state, Direction direction) {
        Direction doorFacing = state.get(Properties.HORIZONTAL_FACING);
        boolean isOpen = state.get(Properties.OPEN);
        boolean isLowerHalf = state.get(Properties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER;

        // Custom door logic based on direction and player position
        return isOpen && doorFacing.getAxis() == direction.getAxis();
    }

    private static boolean canWalkThroughAbove(BlockPos blockPos, Direction direction) {
        if (mc.world == null) return false;
        BlockState state = mc.world.getBlockState(blockPos);
        Block block = state.getBlock();

        if (block instanceof CarpetBlock) return false;

        if (block instanceof DoorBlock && direction != null) {
            return canWalkThroughDoor(blockPos.down(), state, direction);
        }

        if (block instanceof TrapdoorBlock) {
            Direction playerFacing = Direction.fromHorizontalDegrees(mc.player.getYaw());
            Direction trapdoorFacing = state.get(Properties.HORIZONTAL_FACING);
            boolean standingOn = mc.player.getBlockPos().up().equals(blockPos);

            return state.get(Properties.OPEN) &&
                    trapdoorFacing.getAxis() == playerFacing.getAxis();
        }

        return state.isAir();
    }
}
