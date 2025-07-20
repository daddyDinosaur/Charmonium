package com.charmonium.utils.pathfinding;

import com.charmonium.utils.blocks.BlockUtils;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.HashSet;
import java.util.Objects;

public class BlockNodeClass {
    private final BlockNodeClass parentOfBlock;
    private final BlockPos blockPos;
    private final double gCost;
    private final double hCost;
    private final double totalCost;
    private final ActionTypes actionType;
    private final HashSet<BlockPos> broken;

    public BlockNodeClass(BlockNodeClass parentOfBlock, BlockPos blockPos, double gCost,
                          double hCost, double totalCost, ActionTypes actionType,
                          HashSet<BlockPos> broken) {
        this.parentOfBlock = parentOfBlock;
        this.blockPos = blockPos;
        this.gCost = gCost;
        this.hCost = hCost;
        this.totalCost = totalCost;
        this.actionType = actionType;
        this.broken = broken;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public BlockNodeClass getParentOfBlock() {
        return parentOfBlock;
    }

    public double getGCost() {
        return gCost;
    }

    public double getHCost() {
        return hCost;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public ActionTypes getActionType() {
        return actionType;
    }

    public HashSet<BlockPos> getBroken() {
        return broken;
    }

    BlockNodeClass setParentOfBlock(BlockNodeClass parentOfBlock) {
        return new BlockNodeClass(parentOfBlock, this.blockPos, this.gCost, this.hCost, this.totalCost, this.actionType, this.broken);
    }

    double setGCost(double gCost) {
        return this.gCost + gCost;
    }

    double setHCost(double hCost) {
        return this.hCost + hCost;
    }

    double setTotalCost(double totalCost) {
        return this.totalCost + totalCost;
    }

    BlockPos setBlockPos(BlockPos blockPos) {
        return this.blockPos.add(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    ActionTypes setActionType(ActionTypes actionType) {
        return actionType;
    }

    HashSet<BlockPos> setBroken(HashSet<BlockPos> broken) {
        this.broken.addAll(broken);
        return this.broken;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockNodeClass that = (BlockNodeClass) o;
        return Objects.equals(blockPos, that.blockPos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blockPos);
    }

    public boolean isSame(BlockPos block) {
        return blockPos.equals(block);
    }

    public Vec3d getVec() {
        return BlockUtils.fromBPToVec(blockPos);
    }

    public boolean isOnSide() {
        if (parentOfBlock == null) return false;
        return BlockUtils.distanceFromTo(blockPos, parentOfBlock.blockPos) >= 1;
    }

    public boolean isClearOnSides() {
        if (parentOfBlock == null) return false;

        BlockPos parentPos = parentOfBlock.blockPos;
        Vec3d currentVec = BlockUtils.fromBPToVec(blockPos);
        Vec3d parentVec = BlockUtils.fromBPToVec(parentPos);

        Vec3d perpNorm = BlockUtils.getNormalVecBetweenVecsRev(currentVec, parentVec);
        Vec3d centofLine = new Vec3d(
                (parentPos.getX() + blockPos.getX()) / 2.0,
                (parentPos.getY() + blockPos.getY()) / 2.0,
                (parentPos.getZ() + blockPos.getZ()) / 2.0
        );

        BlockPos[] checkPositions = {
                new BlockPos(
                        (int) (centofLine.x + perpNorm.x),
                        (int) centofLine.y,
                        (int) (centofLine.z + perpNorm.z)
                ),
                new BlockPos(
                        (int) (centofLine.x - perpNorm.x),
                        (int) centofLine.y,
                        (int) (centofLine.z - perpNorm.z)
                ),
                new BlockPos(
                        (int) (centofLine.x + perpNorm.x),
                        (int) (centofLine.y + 1),
                        (int) (centofLine.z + perpNorm.z)
                ),
                new BlockPos(
                        (int) (centofLine.x - perpNorm.x),
                        (int) (centofLine.y + 1),
                        (int) (centofLine.z - perpNorm.z)
                )
        };

        for (BlockPos pos : checkPositions) {
            if (BlockUtils.isBlockSolid(pos)) {
                return false;
            }
        }
        return true;
    }
}