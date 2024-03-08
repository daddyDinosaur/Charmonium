package com.mylk.charmonium.pathfinding.utils;

import com.mylk.charmonium.util.BlockUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;

import java.util.HashSet;

@Getter
@AllArgsConstructor
public class BlockNodeClass extends BlockUtils {

  public BlockNodeClass parentOfBlock;
  public BlockPos blockPos;

  // Costs
  public double gCost;
  public double hCost;
  public double totalCost;

  // Other
  public ActionTypes actionType;
  public HashSet<BlockPos> broken;

  public BlockPos blockPos() {
    return this.blockPos;
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof BlockNodeClass) {
      return this.blockPos.equals(((BlockNodeClass) other).blockPos);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return this.blockPos.hashCode();
  }

  public Boolean isSame(BlockPos block) {
    return this.blockPos.equals(block);
  }

  public Vec3 getVec() {
    return fromBPToVec(this.blockPos);
  }

  public boolean isOnSide() {
    return BlockUtils.distanceFromTo(this.blockPos, this.parentOfBlock.blockPos) >= 1;
  }

  public boolean isClearOnSides() {
    if (this.parentOfBlock == null) return false;

    BlockPos blockPos = this.parentOfBlock.blockPos;

    Vec3 perpNorm = BlockUtils.getNormalVecBetweenVecsRev(
      BlockUtils.fromBPToVec(this.blockPos),
      BlockUtils.fromBPToVec(blockPos)
    );

    Vec3 centofLine = new Vec3(
      (double) (blockPos.getX() + this.blockPos.getX()) / 2,
      (double) (blockPos.getY() + this.blockPos.getY()) / 2,
      (double) (blockPos.getZ() + this.blockPos.getZ()) / 2
    );

    BlockPos b01 = new BlockPos(
      centofLine.xCoord + perpNorm.xCoord,
      centofLine.yCoord,
      centofLine.zCoord + perpNorm.zCoord
    );
    BlockPos b02 = new BlockPos(
      centofLine.xCoord - perpNorm.xCoord,
      centofLine.yCoord,
      centofLine.zCoord - perpNorm.zCoord
    );

    BlockPos b11 = new BlockPos(
      centofLine.xCoord + perpNorm.xCoord,
      centofLine.yCoord + 1,
      centofLine.zCoord + perpNorm.zCoord
    );
    BlockPos b12 = new BlockPos(
      centofLine.xCoord - (perpNorm.xCoord),
      centofLine.yCoord + 1,
      centofLine.zCoord - (perpNorm.zCoord)
    );

    return (
      !BlockUtils.isBlockSolid(b01) &&
      !BlockUtils.isBlockSolid(b02) &&
      !BlockUtils.isBlockSolid(b11) &&
      !BlockUtils.isBlockSolid(b12)
    );
  }
}
