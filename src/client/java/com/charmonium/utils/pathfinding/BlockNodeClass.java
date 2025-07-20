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

    public BlockNodeClass(BlockNodeClass parentOfBlock, BlockPos blockPos, double gCost, double hCost, double totalCost, ActionTypes actionType, HashSet<BlockPos> broken) {
        this.parentOfBlock = parentOfBlock;
        this.blockPos = blockPos;
        this.gCost = gCost;
        this.hCost = hCost;
        this.totalCost = totalCost;
        this.actionType = actionType;
        this.broken = broken;
    }

    public BlockPos getBlockPos() { return blockPos; }
    public BlockNodeClass getParentOfBlock() { return parentOfBlock; }
    public double getGCost() { return gCost; }
    public double getHCost() { return hCost; }
    public double getTotalCost() { return totalCost; }
    public ActionTypes getActionType() { return actionType; }
    public HashSet<BlockPos> getBroken() { return broken; }
    public Vec3d getVec() { return BlockUtils.fromBPToVec(blockPos); }
    public boolean isOnSide() { return parentOfBlock != null && BlockUtils.distanceFromTo(blockPos, parentOfBlock.blockPos) >= 1; }
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
        for (BlockPos pos : checkPositions)
            if (BlockUtils.isBlockSolid(pos))
                return false;
        return true;
    }

    public BlockNodeClass withParent(BlockNodeClass parent, double newG, double newH) {
        return new BlockNodeClass(parent, blockPos, newG, newH, newG+newH, actionType, new HashSet<>(broken));
    }

    public BlockNodeClass withActionType(ActionTypes type) {
        return new BlockNodeClass(parentOfBlock, blockPos, gCost, hCost, totalCost, type, broken);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlockNodeClass node)) return false;
        return Objects.equals(blockPos, node.blockPos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blockPos);
    }
}
