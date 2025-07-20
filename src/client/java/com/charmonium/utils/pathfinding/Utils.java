package com.charmonium.utils.pathfinding;

import com.charmonium.utils.blocks.BlockUtils;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class Utils extends Costs {

    public static BlockNodeClass getClassOfStarting(BlockPos startingBlock, BlockPos endBlock) {
        return new BlockNodeClass(
                null,
                startingBlock,
                calculateGCostBlockPos(startingBlock, startingBlock),
                calculateHCostBlockPos(startingBlock, endBlock),
                getFullCost(startingBlock, startingBlock, endBlock),
                null,
                new HashSet<>()
        );
    }

    public static BlockNodeClass getClassOfEnding(BlockPos startingBlock, BlockPos endBlock) {
        return new BlockNodeClass(
                null,
                endBlock,
                calculateGCostBlockPos(startingBlock, endBlock),
                calculateHCostBlockPos(endBlock, endBlock),
                getFullCost(endBlock, startingBlock, endBlock),
                null,
                new HashSet<>()
        );
    }

    public static BlockNodeClass getClassOfBlock(
            BlockPos block,
            BlockNodeClass parent,
            BlockPos starting,
            BlockPos ending,
            HashSet<BlockPos> addBroken
    ) {
        // Copy parent's broken set to avoid mutation issues
        HashSet<BlockPos> brokenCopy = new HashSet<>(parent.getBroken());
        brokenCopy.addAll(addBroken);

        return new BlockNodeClass(
                parent,
                block,
                calculateGCostBlockPos(block, starting),
                calculateHCostBlockPos(block, ending),
                getFullCost(block, starting, ending),
                null,
                brokenCopy
        );
    }

    public static List<BlockNodeClass> getBlocksAround(BlockNodeClass reference, BlockPos start, BlockPos end) {
        List<BlockNodeClass> returnBlocks = new ArrayList<>();
        BlockPos refPos = reference.getBlockPos();

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    BlockPos curBlock = refPos.add(x, y, z);
                    if (!curBlock.equals(refPos)) {
                        returnBlocks.add(getClassOfBlock(
                                curBlock,
                                reference,
                                start,
                                end,
                                new HashSet<>(reference.getBroken())
                        ));
                    }
                }
            }
        }
        return returnBlocks;
    }

    public static List<BlockNodeClass> retracePath(BlockNodeClass startNode, BlockNodeClass endNode) {
        List<BlockNodeClass> nodeClass = new ArrayList<>();
        BlockNodeClass currentNode = endNode;

        while (currentNode != null && currentNode.getParentOfBlock() != null && !currentNode.equals(startNode)) {
            nodeClass.add(currentNode);
            currentNode = currentNode.getParentOfBlock();
        }

        Collections.reverse(nodeClass);
        return nodeClass;
    }

    public static class ReturnClass {
        private final List<BlockPos> blocksToBreak;
        private final ActionTypes actionType;

        public ReturnClass(List<BlockPos> blocksToBreak, ActionTypes actionType) {
            this.blocksToBreak = Collections.unmodifiableList(blocksToBreak);
            this.actionType = actionType;
        }

        public List<BlockPos> getBlocksToBreak() {
            return blocksToBreak;
        }

        public ActionTypes getActionType() {
            return actionType;
        }
    }

    public static ReturnClass isAbleToInteract(BlockNodeClass node) {
        if (canWalkOn(node)) return new ReturnClass(Collections.emptyList(), ActionTypes.WALK);
        if (canJumpOn(node)) return new ReturnClass(Collections.emptyList(), ActionTypes.JUMP);
        if (canFall(node)) return new ReturnClass(Collections.emptyList(), ActionTypes.FALL);
        return null;
    }

    public static boolean canWalkOn(BlockNodeClass node) {
        BlockPos block = node.getBlockPos();
        BlockNodeClass parent = node.getParentOfBlock();
        if (parent == null) return false;

        double yDif = Math.abs(parent.getBlockPos().getY() - block.getY());

        BlockPos blockAbove1 = block.up();
        BlockPos blockBelow1 = block.down();

        if (
                yDif <= 0.001 &&
                        !BlockUtils.isBlockSolid(blockAbove1) &&
                        BlockUtils.isBlockSolid(blockBelow1) &&
                        BlockUtils.isBlockWalkable(block)
        ) {
            if (BlockUtils.distanceFromToXZ(block, parent.getBlockPos()) <= 1) {
                return true;
            }
            return node.isClearOnSides();
        }

        return false;
    }

    private static List<BlockPos> getTheMinList(List<List<BlockPos>> lists) {
        return lists.stream().min(Comparator.comparingInt(List::size)).orElse(null);
    }

    public static boolean canJumpOn(BlockNodeClass node) {
        BlockPos block = node.getBlockPos();
        BlockNodeClass parentBlock = node.getParentOfBlock();
        if (parentBlock == null) return false;

        double yDiff = block.getY() - parentBlock.getBlockPos().getY();

        BlockPos blockAbove1 = block.up();
        BlockPos blockBelow1 = block.down();

        BlockPos blockAboveOneParent = parentBlock.getBlockPos().up();
        BlockPos blockAboveTwoParent = parentBlock.getBlockPos().up(2);

        if (
                yDiff == 1 &&
                        BlockUtils.isBlockSolid(blockBelow1) &&
                        !BlockUtils.isBlockSolid(blockAbove1) &&
                        !BlockUtils.isBlockSolid(blockAboveOneParent) &&
                        !BlockUtils.isBlockSolid(blockAboveTwoParent) &&
                        BlockUtils.isBlockWalkable(block)
        ) {
            if (BlockUtils.distanceFromToXZ(block, parentBlock.getBlockPos()) <= 1) {
                return true;
            }
            return node.isClearOnSides();
        }

        return false;
    }

    public static boolean canFall(BlockNodeClass node) {
        BlockPos block = node.getBlockPos();
        BlockNodeClass parentBlock = node.getParentOfBlock();
        if (parentBlock == null) return false;

        double yDiff = block.getY() - parentBlock.getBlockPos().getY();

        BlockPos blockBelow1 = block.down();
        BlockPos blockAbove1 = block.up();

        if (
                (yDiff < 0 && yDiff > -4 && BlockUtils.isBlockSolid(blockBelow1) && !BlockUtils.isBlockSolid(blockAbove1)) &&
                        BlockUtils.isBlockWalkable(block)
        ) {
            if (BlockUtils.distanceFromToXZ(block, parentBlock.getBlockPos()) <= 1) {
                return true;
            }
            return node.isClearOnSides();
        }

        return false;
    }

    public static boolean isAllClearToY(int y1, int y2, BlockPos block) {
        boolean isGreater = y1 < y2;
        int rem = 0;

        while (y1 != y2) {
            BlockPos curBlock = block.add(0, rem, 0);

            if (!BlockUtils.isBlockSolid(curBlock)) return false;
            y2--;
            rem--;
        }

        return true;
    }

    public static boolean isSameBlock(BlockNodeClass block1, BlockNodeClass block2) {
        return block1.getBlockPos().equals(block2.getBlockPos());
    }
}