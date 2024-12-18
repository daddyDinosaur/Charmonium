package com.mylk.charmonium.pathfinding.main;

import com.mylk.charmonium.pathfinding.utils.*;
import com.mylk.charmonium.util.BlockUtils;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;

import java.util.*;

public class AStarPathFinderImproved extends Utils {
  private final PriorityQueue<BlockNodeClass> openSet;
  private final Set<BlockPos> closedSet;
  private final Map<BlockPos, BlockNodeClass> nodeMap;
  private final BlockPos startBlock;
  private final BlockPos endBlock;
  private final int maxIterations;
  private final double clearance;

  public AStarPathFinderImproved(PathFinderConfig config) {
    this.openSet = new PriorityQueue<>(new BlockNodeCompare());
    this.closedSet = new HashSet<>();
    this.nodeMap = new HashMap<>();
    this.startBlock = config.startingBlock;
    this.endBlock = config.destinationBlock;
    this.maxIterations = config.maxIterations;
    this.clearance = 1.5; // Adjust this value as needed
  }

  public List<BlockNodeClass> findPath() {
    BlockNodeClass startNode = Utils.getClassOfStarting(startBlock, endBlock);
    BlockNodeClass endNode = Utils.getClassOfEnding(startBlock, endBlock);

    openSet.add(startNode);
    nodeMap.put(startBlock, startNode);

    int iterations = 0;
    while (!openSet.isEmpty() && iterations < maxIterations) {
      BlockNodeClass currentNode = openSet.poll();
      closedSet.add(currentNode.blockPos);

      if (currentNode.blockPos.equals(endBlock)) {
        return smoothPath(reconstructPath(currentNode));
      }

      for (BlockNodeClass neighbor : getNeighbors(currentNode)) {
        if (closedSet.contains(neighbor.blockPos)) {
          continue;
        }

        double tentativeGCost = currentNode.gCost + getCost(currentNode, neighbor);

        if (!openSet.contains(neighbor) || tentativeGCost < neighbor.gCost) {
          neighbor.parentOfBlock = currentNode;
          neighbor.gCost = tentativeGCost;
          neighbor.totalCost = neighbor.gCost + getImprovedHeuristic(neighbor, endNode);

          if (!openSet.contains(neighbor)) {
            openSet.add(neighbor);
          } else {
            openSet.remove(neighbor);
            openSet.add(neighbor);
          }

          nodeMap.put(neighbor.blockPos, neighbor);
        }
      }

      iterations++;
    }

    return null; // Path not found
  }

  private double getImprovedHeuristic(BlockNodeClass node, BlockNodeClass goal) {
    double dx = Math.abs(node.blockPos.getX() - goal.blockPos.getX());
    double dy = Math.abs(node.blockPos.getY() - goal.blockPos.getY());
    double dz = Math.abs(node.blockPos.getZ() - goal.blockPos.getZ());
    return Math.sqrt(dx * dx + dy * dy + dz * dz) * 1.1;
  }

  private double getCost(BlockNodeClass from, BlockNodeClass to) {
    double cost = BlockUtils.distanceFromTo(from.blockPos, to.blockPos);
    cost += Costs.calcOtherTotalCost(to);
    cost += getClearanceCost(to.blockPos);
    return cost;
  }

  private double getClearanceCost(BlockPos pos) {
    double minClearance = Double.MAX_VALUE;
    for (int x = -1; x <= 1; x++) {
      for (int y = -1; y <= 1; y++) {
        for (int z = -1; z <= 1; z++) {
          if (x == 0 && y == 0 && z == 0) continue;
          BlockPos checkPos = pos.add(x, y, z);
          if (BlockUtils.isBlockSolid(checkPos)) {
            double distance = Math.sqrt(x*x + y*y + z*z);
            minClearance = Math.min(minClearance, distance);
          }
        }
      }
    }
    return Math.max(0, clearance - minClearance) * 10;
  }

  private List<BlockNodeClass> getNeighbors(BlockNodeClass node) {
    List<BlockNodeClass> neighbors = new ArrayList<>();
    BlockPos pos = node.blockPos;

    for (int x = -1; x <= 1; x++) {
      for (int y = -1; y <= 1; y++) {
        for (int z = -1; z <= 1; z++) {
          if (x == 0 && y == 0 && z == 0) continue;

          BlockPos neighborPos = pos.add(x, y, z);
          ReturnClass interaction = Utils.isAbleToInteract(Utils.getClassOfBlock(neighborPos, node, startBlock, endBlock, new HashSet<>(node.broken)));

          if (interaction != null) {
            BlockNodeClass neighbor = nodeMap.getOrDefault(neighborPos, Utils.getClassOfBlock(neighborPos, node, startBlock, endBlock, new HashSet<>(node.broken)));
            neighbor.actionType = interaction.actionType;
            neighbors.add(neighbor);
          }
        }
      }
    }

    return neighbors;
  }

  private List<BlockNodeClass> smoothPath(List<BlockNodeClass> path) {
    if (path.size() < 3) return path;

    List<BlockNodeClass> smoothed = new ArrayList<>();
    smoothed.add(path.get(0));

    for (int i = 1; i < path.size() - 1; i++) {
      BlockNodeClass prev = path.get(i - 1);
      BlockNodeClass current = path.get(i);
      BlockNodeClass next = path.get(i + 1);

      if (!isSmooth(prev, current, next)) {
        smoothed.add(current);
      }
    }

    smoothed.add(path.get(path.size() - 1));
    return smoothed;
  }

  private boolean isSmooth(BlockNodeClass a, BlockNodeClass b, BlockNodeClass c) {
    Vec3 va = BlockUtils.fromBPToVec(a.blockPos);
    Vec3 vb = BlockUtils.fromBPToVec(b.blockPos);
    Vec3 vc = BlockUtils.fromBPToVec(c.blockPos);

    Vec3 dir1 = vb.subtract(va).normalize();
    Vec3 dir2 = vc.subtract(vb).normalize();

    double dot = dir1.dotProduct(dir2);
    return dot > 0.9; // Adjust this threshold as needed
  }

  private List<BlockNodeClass> reconstructPath(BlockNodeClass endNode) {
    List<BlockNodeClass> path = new ArrayList<>();
    BlockNodeClass currentNode = endNode;

    while (currentNode != null) {
      path.add(currentNode);
      currentNode = currentNode.parentOfBlock;
    }

    Collections.reverse(path);
    return path;
  }

  public List<Vec3> fromClassToVec(List<BlockNodeClass> blockNode) {
    List<Vec3> returnList = new ArrayList<>();

    for (BlockNodeClass block : blockNode) {
      returnList.add(block.getVec());
    }

    return returnList;
  }
}