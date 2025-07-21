package com.charmonium.utils.pathfinding;

import com.charmonium.utils.blocks.BlockUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Vec3d;
import java.util.*;

public class AStarPathFinder extends Utils {
    private final PriorityQueue<BlockNodeClass> openSet;
    private final LongSet closedSet;
    private final Long2ObjectMap<BlockNodeClass> nodeMap;
    private final BlockPos startBlock;
    private final BlockPos endBlock;
    private final int maxIterations;

    public AStarPathFinder(PathFinderConfig config) {
        openSet = new PriorityQueue<>(256, Comparator.comparingDouble(BlockNodeClass::getTotalCost));
        closedSet = new LongOpenHashSet(256);
        nodeMap = new Long2ObjectOpenHashMap<>(256);
        startBlock = config.getStartingBlock();
        endBlock = config.getDestinationBlock();
        maxIterations = config.getMaxIterations();
    }

    public List<BlockNodeClass> findPath() {
        if (!validateStartEndPositions()) return Collections.emptyList();
        BlockNodeClass startNode = Utils.getClassOfStarting(startBlock, endBlock);
        BlockNodeClass endNode = Utils.getClassOfEnding(startBlock, endBlock);

        openSet.clear();
        closedSet.clear();
        nodeMap.clear();

        openSet.add(startNode);
        nodeMap.put(startBlock.asLong(), startNode);

        int iterations = 0;
        BlockNodeClass bestNode = startNode;
        double bestHeuristic = getImprovedHeuristic(startNode, endNode);

        while (!openSet.isEmpty() && iterations++ < maxIterations) {
            BlockNodeClass currentNode = openSet.poll();
            long curLong = currentNode.getBlockPos().asLong();
            double curG = currentNode.getGCost();

            BlockNodeClass known = nodeMap.get(curLong);
            if (known != null && known.getGCost() < curG) continue;
            if (!closedSet.add(curLong)) continue;

            double currentHeuristic = getImprovedHeuristic(currentNode, endNode);
            if (currentHeuristic < bestHeuristic) {
                bestNode = currentNode;
                bestHeuristic = currentHeuristic;
            }
            if (currentNode.getBlockPos().equals(endBlock)) {
                return smoothPath(reconstructPath(currentNode));
            }

            for (BlockNodeClass neighbor : getNeighbors(currentNode)) {
                long neighborLong = neighbor.getBlockPos().asLong();
                if (closedSet.contains(neighborLong)) continue;

                double tentativeG = curG + getCost(currentNode, neighbor);
                BlockNodeClass pre = nodeMap.get(neighborLong);
                if (pre == null || tentativeG < pre.getGCost()) {
                    BlockNodeClass updated = neighbor.withParent(currentNode, tentativeG, getImprovedHeuristic(neighbor, endNode));
                    nodeMap.put(neighborLong, updated);
                    openSet.add(updated);
                }
            }
        }
        return smoothPath(reconstructPath(bestNode));
    }

    private double getImprovedHeuristic(BlockNodeClass node, BlockNodeClass goal) {
        BlockPos a = node.getBlockPos(), b = goal.getBlockPos();
        int dx = Math.abs(a.getX() - b.getX());
        int dy = Math.abs(a.getY() - b.getY());
        int dz = Math.abs(a.getZ() - b.getZ());
        double h = dx + dz + Math.max(0, dy - 1) * 1.8;
        if (BlockUtils.isStepableUp(a, a.down())) h -= 0.33;
        return h;
    }

    private double getCost(BlockNodeClass from, BlockNodeClass to) {
        double dist = BlockUtils.distanceFromTo(from.getBlockPos(), to.getBlockPos());
        double surround = Costs.calcOtherTotalCost(to);
        double clearance = getClearanceCost(to.getBlockPos());

        ActionTypes actionType = to.getActionType();
        if (actionType == ActionTypes.JUMP) dist += 6.0;
        else if (actionType == ActionTypes.FALL) dist += 2.5;

        BlockPos toBelow = to.getBlockPos().down();
        var state = BlockUtils.getBlockState(toBelow);
        if (actionType == ActionTypes.WALK && state != null && state.getBlock() instanceof SlabBlock && state.get(Properties.SLAB_TYPE) == SlabType.BOTTOM) {
            if (to.getBlockPos().getY() > from.getBlockPos().getY()) dist -= 5.0;
        }
        dist += getAdjacentSolidPenalty(to.getBlockPos()) * 3.5;
        return dist + surround + clearance;
    }

    private double getClearanceCost(BlockPos pos) {
        double penalty = 0;
        for (int x = -2; x <= 2; x++) {
            for (int y = -1; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    if ((x | y | z) == 0) continue;
                    BlockPos check = pos.add(x, y, z);
                    if (BlockUtils.isBlockSolid(check)) {
                        double dist = Math.sqrt(x * x + y * y + z * z);
                        penalty += Math.max(0, 1.2 - dist) * 15;
                    }
                }
            }
        }
        return penalty;
    }

    private static final Direction[] HORIZONTAL_DIRECTIONS = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };

    private int getAdjacentSolidPenalty(BlockPos pos) {
        int penalty = 0;
        for (Direction dir : HORIZONTAL_DIRECTIONS) {
            if (BlockUtils.isBlockSolid(pos.offset(dir))) penalty++;
        }
        return penalty;
    }

    private static final int[][] DIRS = {
            {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1},
            {1, 0, 1}, {-1, 0, -1}, {1, 0, -1}, {-1, 0, 1}
    };

    private List<BlockNodeClass> getNeighbors(BlockNodeClass node) {
        List<BlockNodeClass> neighbors = new ArrayList<>(16);
        BlockPos pos = node.getBlockPos();
        for (int[] dir : DIRS) {
            for (int dy : new int[]{0, -1}) {
                BlockPos curr = pos.add(dir[0], dy, dir[2]);
                BlockNodeClass neighbor = Utils.getClassOfBlock(
                        curr, node, startBlock, endBlock, node.getBroken()
                );
                ReturnClass interact = Utils.isAbleToInteract(neighbor);
                if (interact != null) neighbors.add(neighbor.withActionType(interact.getActionType()));
            }
        }

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if ((dx | dz) == 0) continue;
                BlockPos up = pos.add(dx, 1, dz);
                if (BlockUtils.isStepableUp(pos, up)) {
                    BlockNodeClass neighbor = Utils.getClassOfBlock(up, node, startBlock, endBlock, node.getBroken());
                    ReturnClass interact = Utils.isAbleToInteract(neighbor);
                    if (interact != null && interact.getActionType() == ActionTypes.WALK) {
                        neighbors.add(neighbor.withActionType(ActionTypes.WALK));
                    }
                }
            }
        }
        return neighbors;
    }

    private boolean isWalkablePath(Vec3d start, Vec3d end) {
        Vec3d dir = end.subtract(start);
        double dist = dir.length();
        if (dist < 1e-7) return true;
        dir = dir.normalize();
        int steps = (int) (dist / 0.5);
        double dx = dir.x * 0.5, dy = dir.y * 0.5, dz = dir.z * 0.5;
        double cx = start.x, cy = start.y, cz = start.z;
        for (int i = 0; i <= steps; i++) {
            BlockPos bp = new BlockPos((int) cx, (int) cy, (int) cz);
            if (BlockUtils.isBlockSolid(bp) && !BlockUtils.canWalkThrough(bp)) return false;
            cx += dx; cy += dy; cz += dz;
        }
        return true;
    }

    private List<BlockNodeClass> smoothPath(List<BlockNodeClass> path) {
        int sz = path.size();
        if (sz < 3 || sz > 220) return path;
        List<BlockNodeClass> smoothed = new ArrayList<>(sz);
        int idx = 0;
        while (idx < sz) {
            BlockNodeClass curr = path.get(idx);
            smoothed.add(curr);
            int maxLook = Math.min(idx + 10, sz - 1), furthest = idx;
            ActionTypes act = curr.getActionType();
            for (int j = idx + 1; j <= maxLook; j++) {
                if (path.get(j).getActionType() != act) break;
                if (isWalkablePath(curr.getVec(), path.get(j).getVec())) furthest = j;
            }
            idx = furthest > idx ? furthest : idx + 1;
        }
        if (!smoothed.get(smoothed.size() - 1).equals(path.get(sz - 1)))
            smoothed.add(path.get(sz - 1));
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

    public List<Vec3d> fromClassToVec(List<BlockNodeClass> nodeList) {
        if (nodeList.isEmpty()) return Collections.emptyList();
        List<Vec3d> list = new ArrayList<>(nodeList.size());
        for (BlockNodeClass b : nodeList) list.add(b.getVec());
        return list;
    }

    public static boolean isBlockReachable(BlockPos pos) {
        return !BlockUtils.isBlockSolid(pos) || BlockUtils.canMineBlock(pos);
    }

    private boolean validateStartEndPositions() {
        return isBlockReachable(startBlock) && isBlockReachable(endBlock) && !startBlock.equals(endBlock);
    }
}
