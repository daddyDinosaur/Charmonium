package com.charmonium.utils.pathfinding;

import com.charmonium.utils.blocks.BlockUtils;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.*;

public class AStarPathFinder extends Utils {
    private final PriorityQueue<BlockNodeClass> openSet;
    private final Set<BlockPos> closedSet;
    private final Map<BlockPos, BlockNodeClass> nodeMap;
    private final BlockPos startBlock;
    private final BlockPos endBlock;
    private final int maxIterations;

    public AStarPathFinder(PathFinderConfig config) {
        this.openSet = new PriorityQueue<>(new BlockNodeCompare());
        this.closedSet = new HashSet<>();
        this.nodeMap = new HashMap<>();
        this.startBlock = config.getStartingBlock();
        this.endBlock = config.getDestinationBlock();
        this.maxIterations = config.getMaxIterations();
    }

    public List<BlockNodeClass> findPath() {
        if (!validateStartEndPositions()) {
            return Collections.emptyList();
        }

        BlockNodeClass startNode = Utils.getClassOfStarting(startBlock, endBlock);
        BlockNodeClass endNode = Utils.getClassOfEnding(startBlock, endBlock);

        openSet.add(startNode);
        nodeMap.put(startBlock, startNode);

        int iterations = 0;
        BlockNodeClass bestNode = startNode;
        double bestHeuristic = Double.MAX_VALUE;

        while (!openSet.isEmpty() && iterations < maxIterations) {
            BlockNodeClass currentNode = openSet.poll();
            closedSet.add(currentNode.getBlockPos());

            double currentHeuristic = getImprovedHeuristic(currentNode, endNode);
            if (currentHeuristic < bestHeuristic) {
                bestNode = currentNode;
                bestHeuristic = currentHeuristic;
            }

            if (currentNode.getBlockPos().equals(endBlock)) {
                return smoothPath(reconstructPath(currentNode));
            }

            for (BlockNodeClass neighbor : getNeighbors(currentNode)) {
                if (closedSet.contains(neighbor.getBlockPos())) continue;

                double tentativeGCost = currentNode.getGCost() + getCost(currentNode, neighbor);

                if (!openSet.contains(neighbor) || tentativeGCost < neighbor.getGCost()) {
                    neighbor.setParentOfBlock(currentNode);
                    neighbor.setGCost(tentativeGCost);
                    neighbor.setTotalCost(tentativeGCost + getImprovedHeuristic(neighbor, endNode));

                    if (!openSet.contains(neighbor)) {
                        openSet.add(neighbor);
                    } else {
                        openSet.remove(neighbor);
                        openSet.add(neighbor);
                    }
                    nodeMap.put(neighbor.getBlockPos(), neighbor);
                }
            }
            iterations++;
        }
        return smoothPath(reconstructPath(bestNode));
    }

    private double getImprovedHeuristic(BlockNodeClass node, BlockNodeClass goal) {
        BlockPos nodePos = node.getBlockPos();
        BlockPos goalPos = goal.getBlockPos();
        double dx = Math.abs(nodePos.getX() - goalPos.getX());
        double dy = Math.abs(nodePos.getY() - goalPos.getY());
        double dz = Math.abs(nodePos.getZ() - goalPos.getZ());
        return Math.sqrt(dx * dx + dy * dy + dz * dz) * 1.1;
    }

    private double getCost(BlockNodeClass from, BlockNodeClass to) {
        double cost = BlockUtils.distanceFromTo(from.getBlockPos(), to.getBlockPos());
        cost += Costs.calcOtherTotalCost(to);
        cost += getClearanceCost(to.getBlockPos());
        return cost;
    }

    private double getClearanceCost(BlockPos pos) {
        double penalty = 0;
        for(int x = -2; x <= 2; x++) {
            for(int y = -1; y <= 2; y++) {
                for(int z = -2; z <= 2; z++) {
                    if(x == 0 && y == 0 && z == 0) continue;
                    BlockPos checkPos = pos.add(x, y, z);
                    if(BlockUtils.isBlockSolid(checkPos)) {
                        double distance = Math.sqrt(x*x + y*y + z*z);
                        penalty += Math.max(0, 1.2 - distance) * 15;
                    }
                }
            }
        }
        return penalty;
    }

    private List<BlockNodeClass> getNeighbors(BlockNodeClass node) {
        List<BlockNodeClass> neighbors = new ArrayList<>();
        BlockPos pos = node.getBlockPos();
        int[][] directions = {
                {1,0,0}, {-1,0,0}, {0,0,1}, {0,0,-1},
                {1,0,1}, {-1,0,-1}, {1,0,-1}, {-1,0,1}
        };

        for (int[] dir : directions) {
            processDirection(neighbors, node, pos, dir);
        }
        return neighbors;
    }

    private void processDirection(List<BlockNodeClass> neighbors, BlockNodeClass node, BlockPos pos, int[] dir) {
        BlockPos base = pos.add(dir[0], 0, dir[2]);
        checkVerticalMovement(neighbors, node, base, 0);  // Flat
        checkVerticalMovement(neighbors, node, base.up(), 1);  // Step up
        checkVerticalMovement(neighbors, node, base.down(), -1);  // Step down
    }

    private void checkVerticalMovement(List<BlockNodeClass> neighbors, BlockNodeClass node, BlockPos target, int yDiff) {
        if (canStepTo(node.getBlockPos(), target, yDiff)) {
            addNeighbor(neighbors, node, target);
        }
    }

    private boolean canStepTo(BlockPos from, BlockPos to, int yDiff) {
        if (Math.abs(to.getY() - from.getY()) > 1) return false;

        boolean ground = BlockUtils.isBlockSolid(to.down()) || BlockUtils.isBlockSolid(to);
        boolean space = BlockUtils.isBlockWalkable(to) &&
                BlockUtils.isBlockWalkable(to.up());

        return switch (yDiff) {
            case 1 -> ground && space && BlockUtils.isBlockWalkable(to.up());
            case -1 -> BlockUtils.isBlockWalkable(to) && BlockUtils.isBlockSolid(to.down());
            default -> ground && space;
        };
    }

    private void addNeighbor(List<BlockNodeClass> neighbors, BlockNodeClass node, BlockPos neighborPos) {
        BlockNodeClass neighbor = nodeMap.computeIfAbsent(neighborPos,
                k -> Utils.getClassOfBlock(neighborPos, node, startBlock, endBlock, new HashSet<>(node.getBroken())));

        ReturnClass interaction = Utils.isAbleToInteract(neighbor);
        if (interaction != null) {
            neighbor.setActionType(interaction.getActionType());
            neighbors.add(neighbor);
        }
    }

    private boolean isWalkablePath(Vec3d start, Vec3d end) {
        Vec3d direction = end.subtract(start).normalize();
        double distance = start.distanceTo(end);

        for(double d = 0; d < distance; d += 0.5) {
            Vec3d checkPos = start.add(direction.x * d, direction.y * d, direction.z * d);
            BlockPos bp = new BlockPos((int) checkPos.x, (int) checkPos.y, (int) checkPos.z);
            if(BlockUtils.isBlockSolid(bp) && !BlockUtils.canWalkThrough(bp)) {
                return false;
            }
        }
        return true;
    }

    private List<BlockNodeClass> smoothPath(List<BlockNodeClass> path) {
        if(path.size() < 3) return path;

        List<BlockNodeClass> smoothed = new ArrayList<>();
        int currentIndex = 0;

        while(currentIndex < path.size()) {
            BlockNodeClass current = path.get(currentIndex);
            smoothed.add(current);

            int furthestVisible = currentIndex;
            int lookaheadLimit = Math.min(currentIndex + 5, path.size() - 1);

            for(int j = currentIndex + 1; j <= lookaheadLimit; j++) {
                if(isWalkablePath(current.getVec(), path.get(j).getVec())) {
                    furthestVisible = j;
                }
            }

            currentIndex = (furthestVisible > currentIndex) ? furthestVisible : currentIndex + 1;
        }

        if(!smoothed.get(smoothed.size()-1).equals(path.get(path.size()-1))) {
            smoothed.add(path.get(path.size()-1));
        }

        return smoothed;
    }

    private List<BlockNodeClass> reconstructPath(BlockNodeClass node) {
        LinkedList<BlockNodeClass> path = new LinkedList<>();
        while (node != null) {
            path.addFirst(node);
            node = node.getParentOfBlock();
        }
        return path;
    }

    public List<Vec3d> fromClassToVec(List<BlockNodeClass> blockNode) {
        return blockNode.stream()
                .map(BlockNodeClass::getVec)
                .toList();
    }

    public static boolean isBlockReachable(BlockPos pos) {
        return !BlockUtils.isBlockSolid(pos) || BlockUtils.canMineBlock(pos);
    }

    private boolean validateStartEndPositions() {
        return isBlockReachable(startBlock) &&
                isBlockReachable(endBlock) &&
                !startBlock.equals(endBlock);
    }
}
