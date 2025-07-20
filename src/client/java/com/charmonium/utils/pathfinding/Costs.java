package com.charmonium.utils.pathfinding;

import com.charmonium.utils.blocks.BlockUtils;
import net.minecraft.block.Block;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.stream.StreamSupport;

public class Costs {
    private static final double FALL_COST = 1.5;
    private static final double JUMP_COST = 4.0;
    private static final double WALK_COST = 1.0;
    private static final double SURROUNDINGS_COST_MULTIPLIER = 1.5;

    public static double calculateGCost(BlockNodeClass nodeClass, BlockPos startBlock) {
        return BlockUtils.distanceFromTo(nodeClass.getBlockPos(), startBlock);
    }

    public static double getDistCost(BlockNodeClass node) {
        return node.isOnSide() ? 2 : 0;
    }

    public static double calculateHCost(BlockNodeClass nodeClass, BlockPos finalBlock) {
        BlockPos pos = nodeClass.getBlockPos();
        double dx = Math.abs(pos.getX() - finalBlock.getX());
        double dy = Math.abs(pos.getY() - finalBlock.getY());
        double dz = Math.abs(pos.getZ() - finalBlock.getZ());
        return Math.max(dx, Math.max(dy, dz)) + (Math.sqrt(3) - 1) * Math.min(dx, Math.min(dy, dz));
    }

    public static double getSlabCost(BlockNodeClass block) {
        if (getDistCost(block) != 0) return 0;
        Block belowBlock = BlockUtils.getBlockType(block.getBlockPos().down());
        return belowBlock.getDefaultState().isIn(BlockTags.SLABS) ? -1 : 0;
    }

    public static double calcOtherTotalCost(BlockNodeClass child) {
        return calculateSurroundingsDoubleCost(child.getBlockPos().up()) +
                getActionCost(child.getActionType()) +
                getSlabCost(child) +
                getDistCost(child);
    }

    public static double calculateFullCostDistance(BlockNodeClass nodeClass, BlockPos start, BlockPos end) {
        return calculateGCost(nodeClass, start) + calculateHCost(nodeClass, end);
    }

    public static double calculateGCostBlockPos(BlockPos pos1, BlockPos startBlock) {
        return BlockUtils.distanceFromTo(pos1, startBlock);
    }

    public static double calculateHCostBlockPos(BlockPos pos1, BlockPos finalBlock) {
        return BlockUtils.distanceFromTo(pos1, finalBlock);
    }

    public static double calculateFullCostDistance(BlockPos pos1, BlockPos startBlock, BlockPos finalBlock) {
        return calculateGCostBlockPos(pos1, startBlock) + calculateHCostBlockPos(pos1, finalBlock);
    }

    public static double getFullCost(BlockPos pos1, BlockPos startBlock, BlockPos finalBlock) {
        return calculateFullCostDistance(pos1, startBlock, finalBlock);
    }

    public static double getActionCost(ActionTypes action) {
        if (action == null) return 1;
        switch (action) {
            case WALK: return WALK_COST;
            case FALL: return FALL_COST;
            case JUMP: return JUMP_COST;
            default: return 1;
        }
    }

    public static double getYawCost(BlockNodeClass node) {
        if (node.getParentOfBlock() == null) return 0;

        Vec3d childVec = BlockUtils.getCenteredVec(BlockUtils.fromBPToVec(node.getBlockPos()));
        Vec3d parentVec = BlockUtils.getCenteredVec(BlockUtils.fromBPToVec(node.getParentOfBlock().getBlockPos()));

        double yawDifference = Math.atan2(childVec.z - parentVec.z, childVec.x - parentVec.x);
        double yawDegrees = Math.toDegrees(yawDifference);

        yawDegrees = (yawDegrees + 360) % 360; // Normalize to 0-360
        if (yawDegrees > 180) yawDegrees -= 360; // Convert to -180 to 180

        return Math.abs(yawDegrees) / 360;
    }

    public static double calculateSurroundingsDoubleCost(BlockPos block) {
        Iterable<BlockPos> blocks = BlockPos.iterate(
                block.up(2).add(-2, -1, -2),
                block.add(2, 1, 2)
        );
        long nonAirCount = StreamSupport.stream(blocks.spliterator(), false)
                .filter(pos -> !BlockUtils.isBlockWalkable(pos))
                .count();
        return nonAirCount * SURROUNDINGS_COST_MULTIPLIER;
    }
}
