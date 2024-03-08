package com.mylk.charmonium.pathfinding.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.util.BlockPos;

@Getter
@AllArgsConstructor
public class PathFinderConfig {
  public int maxIterations;

  public BlockPos startingBlock;
  public BlockPos destinationBlock;
}
