package com.mylk.charmonium.pathfinding.utils;

import com.mylk.charmonium.Charmonium;
import com.mylk.charmonium.util.BlockUtils;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;

public class Costs {

  private static final double FALL_COST = 1.5;
  private static final double JUMP_COST = 2.0;
  private static final double SURROUNDINGS_COST_MULTIPLIER = 1.5;

  public static double calculateGCost(BlockNodeClass nodeClass, BlockPos startBlock) {
    return BlockUtils.distanceFromTo(nodeClass.blockPos, startBlock);
  }

  public static double getDistCost(BlockNodeClass node) {
    return node.isOnSide() ? 2 : 0;
  }

  public static double calculateHCost(BlockNodeClass nodeClass, BlockPos finalBlock) {
    double dx = Math.abs(nodeClass.blockPos.getX() - finalBlock.getX());
    double dy = Math.abs(nodeClass.blockPos.getY() - finalBlock.getY());
    double dz = Math.abs(nodeClass.blockPos.getZ() - finalBlock.getZ());
    return Math.max(dx, Math.max(dy, dz)) + (Math.sqrt(3) - 1) * Math.min(dx, Math.min(dy, dz));
  }

  public static double getSlabCost(BlockNodeClass block) {
    return getDistCost(block) == 0 && BlockUtils.getBlockType(block.blockPos.down()).getRegistryName().contains("slab")
            ? -1
            : 0;
  }

  public static double calcOtherTotalCost(BlockNodeClass child) {
    return (
            Utils.calculateSurroundingsDoubleCost(child.blockPos.up()) +
                    getActionCost(child.actionType) +
                    getSlabCost(child) +
                    getDistCost(child)
    );
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
    switch (action) {
      case FALL:
        return FALL_COST;
      case JUMP:
        return JUMP_COST;
    }
    return 1;
  }

  public static double getYawCost(BlockNodeClass node) {
    Vec3 childVec = BlockUtils.getCenteredVec(BlockUtils.fromBPToVec(node.blockPos));
    Vec3 parentVec = BlockUtils.getCenteredVec(BlockUtils.fromBPToVec(node.parentOfBlock.blockPos));

    double yawDifference = Math.atan2(childVec.zCoord - parentVec.zCoord, childVec.xCoord - parentVec.xCoord);

    double yawDegrees = Math.toDegrees(yawDifference);

    if (yawDegrees > 180.0) {
      yawDegrees -= 360.0;
    } else if (yawDegrees < -180.0) {
      yawDegrees += 360.0;
    }

    double yawCost = Math.abs(yawDegrees) / 360;

    return yawCost;
  }

  public static double calculateSurroundingsDoubleCost(BlockPos block) {
    Iterable<BlockPos> blocks = BlockPos.getAllInBox(block.up().up().add(-2, -1, -2), block.add(2, 1, 2));
    return BlockUtils.amountNonAir(blocks) * SURROUNDINGS_COST_MULTIPLIER;
  }

  public static double walkCost() {
    return 1;
  }
}