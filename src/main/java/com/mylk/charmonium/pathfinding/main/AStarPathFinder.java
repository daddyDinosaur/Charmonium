package com.mylk.charmonium.pathfinding.main;

import com.mylk.charmonium.pathfinding.utils.*;
import com.mylk.charmonium.util.BlockUtils;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;

public class AStarPathFinder extends Utils {

  public static HashSet<BlockNodeClass> closedSet = new HashSet<>();

  public BlockPos startBlock = null;
  public BlockPos endBlock = null;

  boolean isStart;
  int ticks;

  int opened = 0;
  int closed = 0;

  public List<BlockNodeClass> run(PathFinderConfig pathFinderConfig) {
    int depth = 0;
    isStart = true;

    PriorityQueue<BlockNodeClass> openSet = new PriorityQueue<>(new BlockNodeCompare());
    closedSet = new HashSet<>();
    HashSet<BlockNodeClass> openList = new HashSet<>();

    BlockNodeClass startPoint = Utils.getClassOfStarting(
            pathFinderConfig.startingBlock,
            pathFinderConfig.destinationBlock
    );

    startBlock = pathFinderConfig.startingBlock;
    endBlock = pathFinderConfig.destinationBlock;

    if (pathFinderConfig.startingBlock.equals(pathFinderConfig.destinationBlock)) {
      List<BlockNodeClass> ls = new ArrayList<>();
      ls.add(startPoint);

      return ls;
    }

    BlockNodeClass endPoint = Utils.getClassOfEnding(pathFinderConfig.startingBlock, pathFinderConfig.destinationBlock);
    openSet.add(startPoint);

    while (depth <= pathFinderConfig.maxIterations && !openSet.isEmpty()) {
      opened = openSet.size();
      closed = closedSet.size();

      BlockNodeClass node = openSet.poll();
      closedSet.add(node);

      if (node.blockPos.equals(endBlock) && node.parentOfBlock != null) {
        endPoint.parentOfBlock = node.parentOfBlock;
        isStart = false;

        return Utils.retracePath(startPoint, endPoint);
      }

      List<BlockNodeClass> children = Utils.getBlocksAround(node, startBlock, endBlock);
      for (BlockNodeClass child : children) {
        if (closedSet.contains(child)) {
          openSet.remove(child);
          openList.remove(child);
          continue;
        }

        ReturnClass typeAction = Utils.isAbleToInteract(child);

        if (typeAction == null) {
          continue;
        }

        child.actionType = typeAction.actionType;
        double newGCost =
                child.parentOfBlock.gCost + BlockUtils.distanceFromTo(child.blockPos, child.parentOfBlock.blockPos);
        if (!openList.contains(child) || newGCost < child.gCost) {
          child.gCost += Costs.calcOtherTotalCost(child);

          child.totalCost = child.hCost + child.gCost;

          openList.add(child);
          openSet.add(child);
        }
      }

      depth++;
    }

    isStart = false;
    return null;
  }

  public List<Vec3> fromClassToVec(List<BlockNodeClass> blockNode) {
    List<Vec3> returnList = new ArrayList<>();

    for (BlockNodeClass block : blockNode) {
      returnList.add(block.getVec());
    }

    return returnList;
  }

  public PathFinderConfig getWalkConfig(BlockPos bp1, BlockPos bp2) {
    return new PathFinderConfig(
            100000,
            bp1,
            bp2
    );
  }

  @SubscribeEvent
  public void onTick(TickEvent.ClientTickEvent event) {
    if (!this.isStart) return;
    if (this.ticks >= 50) {
      this.ticks = 0;
    }

    this.ticks++;
  }
}
