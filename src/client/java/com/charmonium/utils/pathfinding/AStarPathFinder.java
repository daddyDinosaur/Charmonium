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
        this.openSet = new PriorityQueue<>(Comparator.comparingDouble(BlockNodeClass::getTotalCost));
        this.closedSet = new LongOpenHashSet(256);
        this.nodeMap = new Long2ObjectOpenHashMap<>(256);
        this.startBlock = config.getStartingBlock();
        this.endBlock = config.getDestinationBlock();
        this.maxIterations = config.getMaxIterations();
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
        double bestHeuristic = Double.MAX_VALUE;

        while (!openSet.isEmpty() && iterations < maxIterations) {
            BlockNodeClass currentNode = openSet.poll();
            long curLong = currentNode.getBlockPos().asLong();
            double curG = currentNode.getGCost();

            BlockNodeClass known = nodeMap.get(curLong);
            if (known != null && known.getGCost() < curG) continue;
            if (closedSet.contains(curLong)) continue;
            closedSet.add(curLong);

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
            iterations++;
        }
        return smoothPath(reconstructPath(bestNode));
    }

    private double getImprovedHeuristic(BlockNodeClass node, BlockNodeClass goal) {
        BlockPos a = node.getBlockPos(), b = goal.getBlockPos();
        int dx = Math.abs(a.getX() - b.getX()), dy = Math.abs(a.getY() - b.getY()), dz = Math.abs(a.getZ() - b.getZ());
        double linear = dx + dz + Math.max(0, dy - 1) * 1.8;
        if (BlockUtils.isStepableUp(a, a.down()))
            linear -= 0.2;
        return linear;
    }

    private double getCost(BlockNodeClass from, BlockNodeClass to) {
        double dist = BlockUtils.distanceFromTo(from.getBlockPos(), to.getBlockPos());
        double surround = Costs.calcOtherTotalCost(to);
        double clearance = getClearanceCost(to.getBlockPos());
        if (to.getActionType() == ActionTypes.JUMP) dist += 5.0;
        if (to.getActionType() == ActionTypes.FALL) dist += 1.5;

        BlockPos toBelow = to.getBlockPos().down();
        boolean isSlab = BlockUtils.getBlockState(toBelow) != null &&
                BlockUtils.getBlockState(toBelow).getBlock() instanceof SlabBlock &&
                BlockUtils.getBlockState(toBelow).get(Properties.SLAB_TYPE) == SlabType.BOTTOM;
        if (to.getActionType() == ActionTypes.WALK && isSlab) {
            if (to.getBlockPos().getY() > from.getBlockPos().getY()) {
                dist -= 2.0;
            }
        }
        dist += getAdjacentSolidPenalty(to.getBlockPos()) * 3.5;
        return dist + surround + clearance;
    }


    private double getClearanceCost(BlockPos pos) {
        double penalty = 0;
        for (int x = -2; x <= 2; x++) for (int y = -1; y <= 2; y++) for (int z = -2; z <= 2; z++) {
            if (x == 0 && y == 0 && z == 0) continue;
            BlockPos check = pos.add(x, y, z);
            if (BlockUtils.isBlockSolid(check)) {
                double dist = Math.sqrt(x * x + y * y + z * z);
                penalty += Math.max(0, 1.2 - dist) * 15;
            }
        }
        return penalty;
    }

    private int getAdjacentSolidPenalty(BlockPos pos) {
        int penalty = 0;
        Direction[] dirs = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (Direction dir : dirs) {
            BlockPos neighbor = pos.offset(dir);
            if (BlockUtils.isBlockSolid(neighbor)) penalty++;
        }
        return penalty;
    }

    private List<BlockNodeClass> getNeighbors(BlockNodeClass node) {
        List<BlockNodeClass> walkNeighbors = new ArrayList<>(8);
        List<BlockNodeClass> jumpNeighbors = new ArrayList<>(4);
        List<BlockNodeClass> fallNeighbors = new ArrayList<>(4);
        BlockPos pos = node.getBlockPos();
        int[][] dirs = {
                {1,0,0},{-1,0,0},{0,0,1},{0,0,-1},
                {1,0,1},{-1,0,-1},{1,0,-1},{-1,0,1}
        };

        for (int[] dir : dirs) {
            for (int dy : new int[]{0, 1, -1}) {
                BlockPos curr = pos.add(dir[0], dy, dir[2]);
                if (Math.abs(curr.getY() - pos.getY()) > 1) continue;

                BlockNodeClass neighbor = Utils.getClassOfBlock(
                        curr, node, startBlock, endBlock, node.getBroken()
                );
                ReturnClass interact = Utils.isAbleToInteract(neighbor);
                if (interact == null) continue;
                ActionTypes action = interact.getActionType();
                neighbor = neighbor.withActionType(action);

                if (action == ActionTypes.WALK) {
                    walkNeighbors.add(neighbor);
                } else if (action == ActionTypes.JUMP) {
                    jumpNeighbors.add(neighbor);
                } else if (action == ActionTypes.FALL) {
                    fallNeighbors.add(neighbor);
                }
            }
        }
        List<BlockNodeClass> results = new ArrayList<>(walkNeighbors);

        for (BlockNodeClass jumpNeighbor : jumpNeighbors) {
            BlockPos jumpPos = jumpNeighbor.getBlockPos();
            boolean hasWalkAround = false;
            for (BlockNodeClass walkNeighbor : walkNeighbors) {
                if (walkNeighbor.getBlockPos().getY() == pos.getY()
                        && BlockUtils.distanceFromToXZ(walkNeighbor.getBlockPos(), jumpPos) <= 1.5) {
                    hasWalkAround = true;
                    break;
                }
            }
            if (!hasWalkAround) results.add(jumpNeighbor);
        }
        results.addAll(fallNeighbors);
        return results;
    }

    private boolean isValidNeighbor(BlockNodeClass node, BlockPos tgt, int[] dir, int dy) {
        BlockPos from = node.getBlockPos();
        if (!BlockUtils.isBlockWalkable(tgt) || !BlockUtils.isBlockWalkable(tgt.up())) return false;
        if (BlockUtils.isBlockSolid(tgt) || BlockUtils.isBlockSolid(tgt.up())) return false;
        if (Math.abs(tgt.getY() - from.getY()) > 1) return false;

        BlockPos tgtBelow = tgt.down();
        BlockPos tgtAbove = tgt.up();
        boolean ground = BlockUtils.isBlockSolid(tgtBelow) || BlockUtils.isBlockSolid(tgt);
        boolean airAbove = BlockUtils.isBlockWalkable(tgtAbove);
        boolean onSlab = BlockUtils.getBlockType(tgtBelow) instanceof SlabBlock &&
                BlockUtils.getBlockState(tgtBelow).get(Properties.SLAB_TYPE) == SlabType.BOTTOM;

        if (dy == 1) {
            if (onSlab && ground && airAbove) return true;
            if (BlockUtils.isStepableUp(from, tgt) && ground && airAbove) return true;
        } else if (dy == 0) {
            if (ground && airAbove) return true;
        } else if (dy == -1) {
            if (BlockUtils.isBlockSolid(tgtBelow) && BlockUtils.isBlockWalkable(tgt)) return true;
        }

        if (dir[0] != 0 && dir[2] != 0) {
            BlockPos adj1 = from.add(dir[0], 0, 0);
            BlockPos adj2 = from.add(0, 0, dir[2]);
            if (!(isValidNeighbor(node, adj1, new int[]{dir[0], 0, 0}, 0) &&
                    isValidNeighbor(node, adj2, new int[]{0, 0, dir[2]}, 0)))
                return false;
        }
        return true;
    }

    private void addNeighbor(List<BlockNodeClass> neighbors, BlockNodeClass node, BlockPos neighborPos) {
        BlockNodeClass neighbor = Utils.getClassOfBlock(
                neighborPos, node, startBlock, endBlock, node.getBroken()
        );
        ReturnClass interaction = Utils.isAbleToInteract(neighbor);
        if (interaction != null)
            neighbors.add(neighbor.withActionType(interaction.getActionType()));
    }

    private boolean isWalkablePath(Vec3d start, Vec3d end) {
        Vec3d dir = end.subtract(start).normalize();
        double dist = start.distanceTo(end);
        for (double d = 0; d < dist; d += 0.5) {
            Vec3d check = start.add(dir.x * d, dir.y * d, dir.z * d);
            BlockPos bp = new BlockPos((int) check.x, (int) check.y, (int) check.z);
            if (BlockUtils.isBlockSolid(bp) && !BlockUtils.canWalkThrough(bp)) return false;
        }
        return true;
    }

    private List<BlockNodeClass> smoothPath(List<BlockNodeClass> path) {
        if (path.size() < 3 || path.size() > 220) return path;
        List<BlockNodeClass> smoothed = new ArrayList<>(path.size());
        int idx = 0;
        while (idx < path.size()) {
            BlockNodeClass curr = path.get(idx);
            smoothed.add(curr);
            int maxLook = Math.min(idx + 10, path.size() - 1), furthest = idx;
            for (int j = idx + 1; j <= maxLook; j++)
                if (isWalkablePath(curr.getVec(), path.get(j).getVec()))
                    furthest = j;
            idx = furthest > idx ? furthest : idx + 1;
        }
        if (!smoothed.get(smoothed.size() - 1).equals(path.get(path.size() - 1)))
            smoothed.add(path.get(path.size() - 1));
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
        return nodeList.stream().map(BlockNodeClass::getVec).toList();
    }

    public static boolean isBlockReachable(BlockPos pos) {
        return !BlockUtils.isBlockSolid(pos) || BlockUtils.canMineBlock(pos);
    }

    private boolean validateStartEndPositions() {
        return isBlockReachable(startBlock) && isBlockReachable(endBlock) && !startBlock.equals(endBlock);
    }
}
