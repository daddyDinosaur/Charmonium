package com.charmonium.utils.pathfinding;

import net.minecraft.util.math.BlockPos;

public class PathFinderConfig {
    private final int maxIterations;
    private final BlockPos startingBlock;
    private final BlockPos destinationBlock;

    public PathFinderConfig(int maxIterations, BlockPos startingBlock, BlockPos destinationBlock) {
        this.maxIterations = maxIterations;
        this.startingBlock = startingBlock;
        this.destinationBlock = destinationBlock;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public BlockPos getStartingBlock() {
        return startingBlock;
    }

    public BlockPos getDestinationBlock() {
        return destinationBlock;
    }
}
