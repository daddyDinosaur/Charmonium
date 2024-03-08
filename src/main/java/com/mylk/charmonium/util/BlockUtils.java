package com.mylk.charmonium.util;

import com.mylk.charmonium.handler.RotationHandler;
import com.mylk.charmonium.mixin.block.RenderGlobalAccessor;
import com.mylk.charmonium.util.charHelpers.VectorUtils;
import com.mylk.charmonium.util.helper.Rotation;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.DestroyBlockProgress;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;


public class BlockUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Block[] initialWalkables = {Blocks.air, Blocks.water, Blocks.flowing_water, Blocks.waterlily, Blocks.wall_sign, Blocks.reeds, Blocks.pumpkin_stem, Blocks.melon_stem};

    public static float getUnitX() {
        return getUnitX((mc.thePlayer.rotationYaw % 360 + 360) % 360);
    }

    public static float getUnitZ() {
        return getUnitZ((mc.thePlayer.rotationYaw % 360 + 360) % 360);
    }

    public enum BlockSides {
        up,
        down,
        posX,
        posZ,
        negX,
        negZ,
        NONE
    }

    public static final List<Block> walkables = Arrays.asList(
            Blocks.air,
            Blocks.wall_sign,
            Blocks.reeds,
            Blocks.tallgrass,
            Blocks.yellow_flower,
            Blocks.deadbush,
            Blocks.red_flower,
            Blocks.stone_slab,
            Blocks.wooden_slab,
            Blocks.rail,
            Blocks.activator_rail,
            Blocks.detector_rail,
            Blocks.golden_rail,
            Blocks.carpet
    );

    public static float getUnitX(float modYaw) {
        float yaw = AngleUtils.get360RotationYaw(modYaw);
        if (yaw < 30) {
            return 0;
        } else if (yaw < 150) {
            return -1f;
        } else if (yaw < 210) {
            return 0;
        } else if (yaw < 330) {
            return 1f;
        } else {
            return 0;
        }
    }

    public static float getUnitZ(float modYaw) {
        float yaw = AngleUtils.get360RotationYaw(modYaw);
        if (yaw < 60) {
            return 1f;
        } else if (yaw < 120) {
            return 0;
        } else if (yaw < 240) {
            return -1f;
        } else if (yaw < 300) {
            return 0;
        } else {
            return 1;
        }
    }

    public static Vec3 getBlockPosCenter(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    public static double getHorizontalDistance(Vec3 a, Vec3 b) {
        double dx = a.xCoord - b.xCoord;
        double dz = a.zCoord - b.zCoord;
        return dx * dx + dz * dz;
    }

    public static Block getBlock(BlockPos blockPos) {
        return mc.theWorld.getBlockState(blockPos).getBlock();
    }

    public static Block getRelativeBlock(float x, float y, float z) {
        return mc.theWorld.getBlockState(
                new BlockPos(
                        mc.thePlayer.posX + getUnitX() * z + getUnitZ() * -1 * x,
                        (mc.thePlayer.posY % 1 > 0.7 ? Math.ceil(mc.thePlayer.posY) : mc.thePlayer.posY) + y,
                        mc.thePlayer.posZ + getUnitZ() * z + getUnitX() * x
                )).getBlock();
    }

    public static BlockPos getRelativeBlockPos(float x, float y, float z) {
        return new BlockPos(
                mc.thePlayer.posX + getUnitX() * z + getUnitZ() * -1 * x,
                (mc.thePlayer.posY % 1 > 0.7 ? Math.ceil(mc.thePlayer.posY) : mc.thePlayer.posY) + y,
                mc.thePlayer.posZ + getUnitZ() * z + getUnitX() * x
        );
    }


    public static Block getRelativeFullBlock(float x, float y, float z) {
        return mc.theWorld.getBlockState(
                new BlockPos(
                        mc.thePlayer.posX + getUnitX() * z + getUnitZ() * -1 * x,
                        mc.thePlayer.posY + y,
                        mc.thePlayer.posZ + getUnitZ() * z + getUnitX() * x
                )).getBlock();
    }

    public static BlockPos getRelativeFullBlockPos(float x, float y, float z) {
        return new BlockPos(
                mc.thePlayer.posX + getUnitX() * z + getUnitZ() * -1 * x,
                mc.thePlayer.posY + y,
                mc.thePlayer.posZ + getUnitZ() * z + getUnitX() * x
        );
    }

    public static ArrayList<BlockSides> getAdjBlocksNotCovered(BlockPos blockToSearch) {
        ArrayList<BlockSides> blockSidesNotCovered = new ArrayList<>();

        if (isPassable(blockToSearch.up()))
            blockSidesNotCovered.add(BlockSides.up);
        if (isPassable(blockToSearch.down()))
            blockSidesNotCovered.add(BlockSides.down);
        if (isPassable(blockToSearch.add(1, 0, 0)))
            blockSidesNotCovered.add(BlockSides.posX);
        if (isPassable(blockToSearch.add(-1, 0, 0)))
            blockSidesNotCovered.add(BlockSides.negX);
        if (isPassable(blockToSearch.add(0, 0, 1)))
            blockSidesNotCovered.add(BlockSides.posZ);
        if (isPassable(blockToSearch.add(0, 0, -1)))
            blockSidesNotCovered.add(BlockSides.negZ);

        return blockSidesNotCovered;
    }

    public static boolean isPassable(Block block) {
        return walkables.contains(block);
    }

    public static boolean isPassable(BlockPos block) {
        return isPassable(getBlock(block));
    }

    public static Block getRelativeBlock(float x, float y, float z, float yaw) {
        return mc.theWorld.getBlockState(
                new BlockPos(
                        mc.thePlayer.posX + getUnitX(yaw) * z + getUnitZ(yaw) * -1 * x,
                        (mc.thePlayer.posY % 1 > 0.7 ? Math.ceil(mc.thePlayer.posY) : mc.thePlayer.posY) + y,
                        mc.thePlayer.posZ + getUnitZ(yaw) * z + getUnitX(yaw) * x
                )).getBlock();
    }

    public static BlockPos getRelativeBlockPos(float x, float y, float z, float yaw) {
        return new BlockPos(
                mc.thePlayer.posX + getUnitX(yaw) * z + getUnitZ(yaw) * -1 * x,
                (mc.thePlayer.posY % 1 > 0.7 ? Math.ceil(mc.thePlayer.posY) : mc.thePlayer.posY) + y,
                mc.thePlayer.posZ + getUnitZ(yaw) * z + getUnitX(yaw) * x
        );
    }

    public static Vec3 getRelativeVec(float x, float y, float z, float yaw) {
        return new Vec3(
                mc.thePlayer.posX + getUnitX(yaw) * z + getUnitZ(yaw) * -1 * x,
                (mc.thePlayer.posY % 1 > 0.7 ? Math.ceil(mc.thePlayer.posY) : mc.thePlayer.posY) + y,
                mc.thePlayer.posZ + getUnitZ(yaw) * z + getUnitX(yaw) * x
        );
    }

    public static int bedrockCount() {
        int count = 0;
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (getBlock(mc.thePlayer.getPosition().add(i, 1, j)).equals(Blocks.bedrock))
                    count++;
            }
        }
        return count;
    }

    public boolean canFlyThrough() {
        return BlockUtils.getRelativeFullBlock(0, 0, 1).isPassable(mc.theWorld, BlockUtils.getRelativeBlockPos(0, 0, 1))
                && BlockUtils.getRelativeFullBlock(0, 1, 1).isPassable(mc.theWorld, BlockUtils.getRelativeBlockPos(0, 1, 1));
    }

    public static boolean canWalkThrough(BlockPos blockPos) {
        return canWalkThrough(blockPos, null);
    }

    public static boolean canWalkThrough(BlockPos blockPos, Direction direction) {
        return canWalkThroughBottom(blockPos, direction) && canWalkThroughAbove(blockPos.add(0, 1, 0), direction);
    }

    private static boolean canWalkThroughBottom(BlockPos blockPos, Direction direction) {
        IBlockState state = mc.theWorld.getBlockState(blockPos);
        Block block = state.getBlock();

        // if no blocks down to 65, then return false
        boolean allAir = true;
        for (int y = blockPos.getY(); y >= 65; y--) {
            if (mc.theWorld.getBlockState(new BlockPos(blockPos.getX(), y, blockPos.getZ())).getBlock() != Blocks.air) {
                allAir = false;
                break;
            }
        }

        if (allAir) return false;

        if (mc.thePlayer.posY % 1 >= 0.5 && mc.thePlayer.posY % 1 <= 0.75)
            return true;

        if (Arrays.asList(initialWalkables).contains(block))
            return true;

        if (block instanceof BlockDoor && direction != null) {
            return canWalkThroughDoor(blockPos, direction);
        }

        if (block instanceof BlockFence)
            return false;

        if (block instanceof BlockFenceGate)
            return state.getValue(BlockFenceGate.OPEN);

        if (block instanceof BlockTrapDoor) {
            return state.getValue(BlockTrapDoor.OPEN) || state.getValue(BlockTrapDoor.HALF) == BlockTrapDoor.DoorHalf.BOTTOM;
        }

        if (block instanceof BlockSnow)
            return state.getValue(BlockSnow.LAYERS) <= 5;

        if (block instanceof BlockSlab) {
            // if the player is on the bottom half of the slab, all slabs are walkable (top, bottom and double)
            if (mc.thePlayer.posY % 1 < 0.5) {
                if (((BlockSlab) block).isDouble())
                    return false;
                return state.getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.BOTTOM;
            }
        }

        if (block instanceof BlockCarpet)
            return true;

        if (block instanceof BlockStairs) {
            // check if the stairs are rotated in the direction the player is coming from
            EnumFacing facing = state.getValue(BlockStairs.FACING);
            BlockPos posDiff = blockPos.subtract(mc.thePlayer.getPosition());
            if (state.getValue(BlockStairs.HALF) == BlockStairs.EnumHalf.TOP)
                return false;
            if (facing == EnumFacing.NORTH && posDiff.getZ() < 0)
                return true;
            if (facing == EnumFacing.SOUTH && posDiff.getZ() > 0)
                return true;
            if (facing == EnumFacing.WEST && posDiff.getX() < 0)
                return true;
            return facing == EnumFacing.EAST && posDiff.getX() > 0;
        }

        return block.isPassable(mc.theWorld, blockPos);
    }

    private static boolean canWalkThroughAbove(BlockPos blockPos, Direction direction) {
        IBlockState state = mc.theWorld.getBlockState(blockPos);
        Block block = state.getBlock();

        if (block instanceof BlockCarpet)
            return false;

        if (block instanceof BlockDoor && direction != null) {
            return canWalkThroughDoor(blockPos.subtract(new Vec3i(0, 1, 0)), direction);
        }

        if (block instanceof BlockFence)
            return false;

        if (block instanceof BlockFenceGate)
            return state.getValue(BlockFenceGate.OPEN);

        if (block instanceof BlockTrapDoor) {
            EnumFacing playerFacing = EnumFacing.fromAngle(mc.thePlayer.rotationYaw);
            EnumFacing doorFacing = mc.theWorld.getBlockState(blockPos).getValue(BlockTrapDoor.FACING);
            boolean standingOnDoor = getRelativeBlockPos(0, 1, 0).equals(blockPos);

            if (state.getValue(BlockTrapDoor.OPEN) && direction != null) {
                return canWalkThroughDoorWithDirection(direction, playerFacing, doorFacing, standingOnDoor);
            } else {
                return state.getValue(BlockTrapDoor.HALF) == BlockTrapDoor.DoorHalf.TOP;
            }
        }

        return block.isPassable(mc.theWorld, blockPos);
    }

    private static boolean canWalkThroughDoorWithDirection(Direction direction, EnumFacing playerFacing, EnumFacing doorFacing, boolean standingOnDoor) {
        switch (direction) {
            case FORWARD:
                if (doorFacing.equals(playerFacing.getOpposite()) && standingOnDoor) {
                    return false;
                }
                if (!standingOnDoor && doorFacing.equals(playerFacing)) {
                    return false;
                }
                break;
            case BACKWARD:
                if (doorFacing.equals(playerFacing) && standingOnDoor) {
                    return false;
                }
                if (!standingOnDoor && doorFacing.equals(playerFacing.getOpposite())) {
                    return false;
                }
                break;
            case LEFT:
                if (doorFacing.equals(playerFacing.rotateY()) && standingOnDoor) {
                    return false;
                }
                if (!standingOnDoor && doorFacing.equals(playerFacing.rotateYCCW())) {
                    return false;
                }
                break;
            case RIGHT:
                if (doorFacing.equals(playerFacing.rotateYCCW()) && standingOnDoor) {
                    return false;
                }
                if (!standingOnDoor && doorFacing.equals(playerFacing.rotateY())) {
                    return false;
                }
                break;
        }
        return true;
    }

    public static List<BlockPos> findBlock(Box box, ArrayList<BlockPos> forbiddenBlockPos, int minY, int maxY, BlockData<?>... requiredBlocks) {
        return findBlock(box, forbiddenBlockPos, minY, maxY, new ArrayList<>(Arrays.asList(requiredBlocks)));
    }

    public static List<BlockPos> findBlock(Box searchBox, ArrayList<BlockPos> forbiddenBlockPos, int minY, int maxY, ArrayList<BlockData<?>> requiredBlock) {

        List<BlockPos> foundBlocks = new ArrayList<>();
        if(forbiddenBlockPos != null && !forbiddenBlockPos.isEmpty())
            forbiddenBlockPos.forEach(System.out::println);

        BlockPos currentBlock;

        for (int i = 0; i <= Math.abs(searchBox.dx_bound2 - searchBox.dx_bound1); i++) {
            for (int j = 0; j <= Math.abs(searchBox.dy_bound2 - searchBox.dy_bound1); j++) {
                for (int k = 0; k <= Math.abs(searchBox.dz_bound2 - searchBox.dz_bound1); k++) {

                    //rectangular scan
                    currentBlock = (getPlayerLoc().add(i + Math.min(searchBox.dx_bound2, searchBox.dx_bound1),  j + Math.min(searchBox.dy_bound2, searchBox.dy_bound1),  k + Math.min(searchBox.dz_bound2, searchBox.dz_bound1)));
                    BlockPos finalCurrentBlock = currentBlock;
                    if(requiredBlock.stream().anyMatch(blockData -> {
                        Block block = mc.theWorld.getBlockState(finalCurrentBlock).getBlock();
                        if (!blockData.block.equals(block)) return false;
                        if (blockData.requiredBlockStateValue == null) return true;

                        if(blockData.requiredBlockStateValue instanceof EnumDyeColor)
                            return block.getMetaFromState(mc.theWorld.getBlockState(finalCurrentBlock)) == ((EnumDyeColor) blockData.requiredBlockStateValue).getMetadata();
                        if(blockData.requiredBlockStateValue instanceof BlockStone.EnumType)
                            return block.getMetaFromState(mc.theWorld.getBlockState(finalCurrentBlock)) == ((BlockStone.EnumType) blockData.requiredBlockStateValue).getMetadata();

                        return false;
                        //Add more "data types" here when necessary
                    })) {
                        if (forbiddenBlockPos != null && !forbiddenBlockPos.isEmpty() && forbiddenBlockPos.contains(currentBlock))
                            continue;
                        if (currentBlock.getY() > maxY || currentBlock.getY() < minY)
                            continue;

                        foundBlocks.add(currentBlock);
                    }
                }
            }
        }
        foundBlocks.sort(Comparator.comparingDouble(b -> getDistanceBetweenTwoBlock(b, BlockUtils.getPlayerLoc().add(0, 1.62d, 0))));
        return foundBlocks;
    }

    public static BlockPos getPlayerLoc() {
        return getRelativeBlockPos(0, 0, 0);
    }

    public static double getDistanceBetweenTwoBlock(BlockPos b1, BlockPos b2){
        return Math.sqrt((b1.getX() - b2.getX()) * (b1.getX() - b2.getX())
                + (b1.getY() - b2.getY()) * (b1.getY() - b2.getY())
                + (b1.getZ() - b2.getZ()) * (b1.getZ() - b2.getZ()));
    }

    public static boolean isAboveHeadClear() {
        BlockPos blockPosStart = getRelativeBlockPos(0, 1, 0);
        for (int y = blockPosStart.getY(); y < 100; y++) {
            BlockPos blockPos = new BlockPos(blockPosStart.getX(), y, blockPosStart.getZ());
            if (blockHasCollision(blockPos)) {
                return false;
            }
        }
        return true;
    }

    private static boolean blockHasCollision(BlockPos blockPos) {
        AxisAlignedBB axisAlignedBB = mc.theWorld.getBlockState(blockPos).getBlock().getCollisionBoundingBox(mc.theWorld, blockPos, mc.theWorld.getBlockState(blockPos));
        return !mc.theWorld.getBlockState(blockPos).getBlock().isPassable(mc.theWorld, blockPos) || axisAlignedBB != null;
    }

    public static Vec3 getBlockCenter(BlockPos blockPos) {
        return new Vec3(blockPos.add(0.5d, 0.5d, 0.5d));
    }

    public static Block getFrontBlock() {
        return getRelativeBlock(0, 0, 1);
    }

    public static boolean canWalkThroughDoor(Direction direction) {
        return canWalkThroughDoor(getRelativeBlockPos(0, 0, 0), direction);
    }

    public static boolean canWalkThroughDoor(BlockPos blockPos, Direction direction) {
        Block block = mc.theWorld.getBlockState(blockPos).getBlock();
        if (!(block instanceof BlockDoor)) return true;

        EnumFacing playerFacing = EnumFacing.fromAngle(mc.thePlayer.rotationYaw);
        EnumFacing doorFacing = mc.theWorld.getBlockState(blockPos).getValue(BlockDoor.FACING);
        boolean standingOnDoor = getRelativeBlockPos(0, 0, 0).equals(blockPos);

        return canWalkThroughDoorWithDirection(direction, playerFacing, doorFacing, standingOnDoor);
    }

    public static boolean canFlyHigher(int distance) {
        AxisAlignedBB playerAABB = mc.thePlayer.getEntityBoundingBox();
        Vec3[] corners = {
                new Vec3(playerAABB.minX + 0.0001, playerAABB.minY + mc.thePlayer.height, playerAABB.minZ + 0.0001),
                new Vec3(playerAABB.minX + 0.0001, playerAABB.minY + mc.thePlayer.height, playerAABB.maxZ - 0.0001),
                new Vec3(playerAABB.maxX - 0.0001, playerAABB.minY + mc.thePlayer.height, playerAABB.minZ + 0.0001),
                new Vec3(playerAABB.maxX - 0.0001, playerAABB.minY + mc.thePlayer.height, playerAABB.maxZ - 0.0001)
        };
        for (Vec3 corner : corners) {
            MovingObjectPosition mop = mc.theWorld.rayTraceBlocks(corner, corner.addVector(0, distance, 0), false, true, false);
            if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                return false;
            }
        }
        return true;
    }

    public static BlockPos getBlockPosLookingAt() {
        MovingObjectPosition mop = mc.thePlayer.rayTrace(5, 1);
        if (mop == null)
            return null;
        return mop.getBlockPos();
    }

    public static boolean isBlockVisible(BlockPos pos) {
        MovingObjectPosition mop = mc.theWorld.rayTraceBlocks(new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ), new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
        return mop == null || mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && mop.entityHit.getDistance(pos.getX(), pos.getY(), pos.getZ()) < 2 || mop.getBlockPos().equals(pos);
    }

    public static boolean isWater(Block block) {
        return block instanceof BlockLiquid && block.getMaterial() == Material.water;
    }

    public static BlockPos getEasiestBlock(ArrayList<BlockPos> list, Predicate<? super BlockPos> predicate) {
        EntityPlayerSP player = mc.thePlayer;
        BlockPos easiest = null;

        Rotation serverSideRotation = new Rotation(RotationHandler.getInstance().getServerSideYaw(), RotationHandler.getInstance().getServerSidePitch());

        for (BlockPos blockPos : list) {
            if (predicate.test(blockPos) && canBlockBeSeen(blockPos, 8, new Vec3(0, 0, 0), x -> false)) {
                if (easiest == null || RotationHandler.getInstance().getNeededChange(serverSideRotation, RotationHandler.getInstance().getRotation(blockPos)).getValue() < RotationHandler.getInstance().getNeededChange(serverSideRotation, RotationHandler.getInstance().getRotation(easiest)).getValue()) {
                    easiest = blockPos;
                }
            }
        }

        if (easiest != null) return easiest;

        for (BlockPos blockPos : list) {
            if (predicate.test(blockPos)) {
                if (easiest == null || RotationHandler.getInstance().getNeededChange(serverSideRotation, RotationHandler.getInstance().getRotation(blockPos)).getValue() < RotationHandler.getInstance().getNeededChange(serverSideRotation, RotationHandler.getInstance().getRotation(easiest)).getValue()) {
                    easiest = blockPos;
                }
            }
        }

        return easiest;
    }

    public static boolean canBlockBeSeen(BlockPos blockPos, double dist, Vec3 offset, Predicate<? super BlockPos> predicate) {
        Vec3 vec = new Vec3(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5).add(offset);
        MovingObjectPosition mop = rayTraceBlocks(mc.thePlayer.getPositionEyes(1.0f), vec, false, true, false, predicate);
        if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            return mop.getBlockPos().equals(blockPos) && vec.distanceTo(mc.thePlayer.getPositionEyes(1.0f)) < dist;
        }

        return false;
    }

    public static MovingObjectPosition rayTraceBlocks(Vec3 vec31, Vec3 vec32, boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock, Predicate<? super BlockPos> predicate) {
        return rayTraceBlocks(vec31, vec32, stopOnLiquid, ignoreBlockWithoutBoundingBox, returnLastUncollidableBlock, predicate, false);
    }

    public static MovingObjectPosition rayTraceBlocks(Vec3 vec31, Vec3 vec32, boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock, Predicate<? super BlockPos> predicate, boolean fullBlocks) {
        if (!(Double.isNaN(vec31.xCoord) || Double.isNaN(vec31.yCoord) || Double.isNaN(vec31.zCoord))) {
            if (!(Double.isNaN(vec32.xCoord) || Double.isNaN(vec32.yCoord) || Double.isNaN(vec32.zCoord))) {
                MovingObjectPosition movingobjectposition;
                int i = MathHelper.floor_double(vec32.xCoord);
                int j = MathHelper.floor_double(vec32.yCoord);
                int k = MathHelper.floor_double(vec32.zCoord);
                int l = MathHelper.floor_double(vec31.xCoord);
                int i1 = MathHelper.floor_double(vec31.yCoord);
                int j1 = MathHelper.floor_double(vec31.zCoord);
                BlockPos blockpos = new BlockPos(l, i1, j1);
                IBlockState iblockstate = getBlockState(blockpos);
                Block block = iblockstate.getBlock();
                if (!predicate.test(blockpos) && (!ignoreBlockWithoutBoundingBox || block.getCollisionBoundingBox(mc.theWorld, blockpos, iblockstate) != null) && block.canCollideCheck(iblockstate, stopOnLiquid) && (movingobjectposition = collisionRayTrace(block, blockpos, vec31, vec32, fullBlocks)) != null) {
                    return movingobjectposition;
                }
                MovingObjectPosition movingobjectposition2 = null;
                int k1 = 200;
                while (k1-- >= 0) {
                    EnumFacing enumfacing;
                    if (Double.isNaN(vec31.xCoord) || Double.isNaN(vec31.yCoord) || Double.isNaN(vec31.zCoord)) {
                        return null;
                    }
                    if (l == i && i1 == j && j1 == k) {
                        return returnLastUncollidableBlock ? movingobjectposition2 : null;
                    }
                    boolean flag2 = true;
                    boolean flag = true;
                    boolean flag1 = true;
                    double d0 = 999.0;
                    double d1 = 999.0;
                    double d2 = 999.0;
                    if (i > l) {
                        d0 = (double) l + 1.0;
                    } else if (i < l) {
                        d0 = (double) l + 0.0;
                    } else {
                        flag2 = false;
                    }
                    if (j > i1) {
                        d1 = (double) i1 + 1.0;
                    } else if (j < i1) {
                        d1 = (double) i1 + 0.0;
                    } else {
                        flag = false;
                    }
                    if (k > j1) {
                        d2 = (double) j1 + 1.0;
                    } else if (k < j1) {
                        d2 = (double) j1 + 0.0;
                    } else {
                        flag1 = false;
                    }
                    double d3 = 999.0;
                    double d4 = 999.0;
                    double d5 = 999.0;
                    double d6 = vec32.xCoord - vec31.xCoord;
                    double d7 = vec32.yCoord - vec31.yCoord;
                    double d8 = vec32.zCoord - vec31.zCoord;
                    if (flag2) {
                        d3 = (d0 - vec31.xCoord) / d6;
                    }
                    if (flag) {
                        d4 = (d1 - vec31.yCoord) / d7;
                    }
                    if (flag1) {
                        d5 = (d2 - vec31.zCoord) / d8;
                    }
                    if (d3 == -0.0) {
                        d3 = -1.0E-4;
                    }
                    if (d4 == -0.0) {
                        d4 = -1.0E-4;
                    }
                    if (d5 == -0.0) {
                        d5 = -1.0E-4;
                    }
                    if (d3 < d4 && d3 < d5) {
                        enumfacing = i > l ? EnumFacing.WEST : EnumFacing.EAST;
                        vec31 = new Vec3(d0, vec31.yCoord + d7 * d3, vec31.zCoord + d8 * d3);
                    } else if (d4 < d5) {
                        enumfacing = j > i1 ? EnumFacing.DOWN : EnumFacing.UP;
                        vec31 = new Vec3(vec31.xCoord + d6 * d4, d1, vec31.zCoord + d8 * d4);
                    } else {
                        enumfacing = k > j1 ? EnumFacing.NORTH : EnumFacing.SOUTH;
                        vec31 = new Vec3(vec31.xCoord + d6 * d5, vec31.yCoord + d7 * d5, d2);
                    }
                    l = MathHelper.floor_double(vec31.xCoord) - (enumfacing == EnumFacing.EAST ? 1 : 0);
                    i1 = MathHelper.floor_double(vec31.yCoord) - (enumfacing == EnumFacing.UP ? 1 : 0);
                    j1 = MathHelper.floor_double(vec31.zCoord) - (enumfacing == EnumFacing.SOUTH ? 1 : 0);
                    blockpos = new BlockPos(l, i1, j1);
                    IBlockState iblockstate1 = getBlockState(blockpos);
                    Block block1 = iblockstate1.getBlock();
                    if (ignoreBlockWithoutBoundingBox && block1.getCollisionBoundingBox(mc.theWorld, blockpos, iblockstate1) == null)
                        continue;
                    if (predicate.test(blockpos)) continue;
                    if (block1.canCollideCheck(iblockstate1, stopOnLiquid)) {
                        MovingObjectPosition movingobjectposition1 = collisionRayTrace(block1, blockpos, vec31, vec32, fullBlocks);
                        if (movingobjectposition1 == null) continue;
                        return movingobjectposition1;
                    }
                    movingobjectposition2 = new MovingObjectPosition(MovingObjectPosition.MovingObjectType.MISS, vec31, enumfacing, blockpos);
                }
                return returnLastUncollidableBlock ? movingobjectposition2 : null;
            }
            return null;
        }
        return null;
    }

    public static MovingObjectPosition collisionRayTrace(Block block, BlockPos pos, Vec3 start, Vec3 end, boolean fullBlocks) {
        start = start.addVector(-pos.getX(), -pos.getY(), -pos.getZ());
        end = end.addVector(-pos.getX(), -pos.getY(), -pos.getZ());

        Vec3 vec3 = start.getIntermediateWithXValue(end, fullBlocks ? 0.0 : block.getBlockBoundsMinX());
        Vec3 vec31 = start.getIntermediateWithXValue(end, fullBlocks ? 1.0 : block.getBlockBoundsMaxX());
        Vec3 vec32 = start.getIntermediateWithYValue(end, fullBlocks ? 0.0 : block.getBlockBoundsMinY());
        Vec3 vec33 = start.getIntermediateWithYValue(end, fullBlocks ? 1.0 : block.getBlockBoundsMaxY());
        Vec3 vec34 = start.getIntermediateWithZValue(end, fullBlocks ? 0.0 : block.getBlockBoundsMinZ());
        Vec3 vec35 = start.getIntermediateWithZValue(end, fullBlocks ? 1.0 : block.getBlockBoundsMaxZ());

        if (!isVecInsideYZBounds(block, vec3, fullBlocks)) {
            vec3 = null;
        }
        if (!isVecInsideYZBounds(block, vec31, fullBlocks)) {
            vec31 = null;
        }
        if (!isVecInsideXZBounds(block, vec32, fullBlocks)) {
            vec32 = null;
        }
        if (!isVecInsideXZBounds(block, vec33, fullBlocks)) {
            vec33 = null;
        }
        if (!isVecInsideXYBounds(block, vec34, fullBlocks)) {
            vec34 = null;
        }
        if (!isVecInsideXYBounds(block, vec35, fullBlocks)) {
            vec35 = null;
        }

        Vec3 vec36 = null;

        if (vec3 != null) {
            vec36 = vec3;
        }
        if (vec31 != null && (vec36 == null || start.squareDistanceTo(vec31) < start.squareDistanceTo(vec36))) {
            vec36 = vec31;
        }
        if (vec32 != null && (vec36 == null || start.squareDistanceTo(vec32) < start.squareDistanceTo(vec36))) {
            vec36 = vec32;
        }
        if (vec33 != null && (vec36 == null || start.squareDistanceTo(vec33) < start.squareDistanceTo(vec36))) {
            vec36 = vec33;
        }
        if (vec34 != null && (vec36 == null || start.squareDistanceTo(vec34) < start.squareDistanceTo(vec36))) {
            vec36 = vec34;
        }
        if (vec35 != null && (vec36 == null || start.squareDistanceTo(vec35) < start.squareDistanceTo(vec36))) {
            vec36 = vec35;
        }
        if (vec36 == null) {
            return null;
        }
        EnumFacing enumfacing = null;
        if (vec36 == vec3) {
            enumfacing = EnumFacing.WEST;
        }
        if (vec36 == vec31) {
            enumfacing = EnumFacing.EAST;
        }
        if (vec36 == vec32) {
            enumfacing = EnumFacing.DOWN;
        }
        if (vec36 == vec33) {
            enumfacing = EnumFacing.UP;
        }
        if (vec36 == vec34) {
            enumfacing = EnumFacing.NORTH;
        }
        if (vec36 == vec35) {
            enumfacing = EnumFacing.SOUTH;
        }
        return new MovingObjectPosition(vec36.addVector(pos.getX(), pos.getY(), pos.getZ()), enumfacing, pos);
    }

    private static boolean isVecInsideYZBounds(Block block, Vec3 point, boolean fullBlocks) {
        return point != null && point.yCoord >= (fullBlocks ? 0.0 : block.getBlockBoundsMinY()) && point.yCoord <= (fullBlocks ? 1.0 : block.getBlockBoundsMaxY()) && point.zCoord >= (fullBlocks ? 0.0 : block.getBlockBoundsMinZ()) && point.zCoord <= (fullBlocks ? 1.0 : block.getBlockBoundsMaxZ());
    }

    private static boolean isVecInsideXZBounds(Block block, Vec3 point, boolean fullBlocks) {
        return point != null && point.xCoord >= (fullBlocks ? 0.0 : block.getBlockBoundsMinX()) && point.xCoord <= (fullBlocks ? 1.0 : block.getBlockBoundsMaxX()) && point.zCoord >= (fullBlocks ? 0.0 : block.getBlockBoundsMinZ()) && point.zCoord <= (fullBlocks ? 1.0 : block.getBlockBoundsMaxZ());
    }

    private static boolean isVecInsideXYBounds(Block block, Vec3 point, boolean fullBlocks) {
        return point != null && point.xCoord >= (fullBlocks ? 0.0 : block.getBlockBoundsMinX()) && point.xCoord <= (fullBlocks ? 1.0 : block.getBlockBoundsMaxX()) && point.yCoord >= (fullBlocks ? 0.0 : block.getBlockBoundsMinY()) && point.yCoord <= (fullBlocks ? 1.0 : block.getBlockBoundsMaxY());
    }

    public static IBlockState getBlockState(BlockPos blockPos) {
        if (mc.theWorld == null) return null;
        return mc.theWorld.getBlockState(blockPos);
    }

    public static EnumFacing calculateEnumfacing(Vec3 vec) {
        int x = MathHelper.floor_double(vec.xCoord);
        int y = MathHelper.floor_double(vec.yCoord);
        int z = MathHelper.floor_double(vec.zCoord);
        MovingObjectPosition position = calculateIntercept(new AxisAlignedBB(x, y, z, x + 1, y + 1, z + 1), vec, 50.0f);
        return (position != null) ? position.sideHit : null;
    }

    public static MovingObjectPosition calculateIntercept(AxisAlignedBB aabb, Vec3 vec, float range) {
        Vec3 playerPositionEyes = mc.thePlayer.getPositionEyes(1f);
        Vec3 blockVector = getLook(vec);
        return aabb.calculateIntercept(playerPositionEyes, playerPositionEyes.addVector(blockVector.xCoord * range, blockVector.yCoord * range, blockVector.zCoord * range));
    }

    public static Vec3 getLook(final Vec3 vec) {
        final double diffX = vec.xCoord - mc.thePlayer.posX;
        final double diffY = vec.yCoord - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        final double diffZ = vec.zCoord - mc.thePlayer.posZ;
        final double dist = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ);
        return getVectorForRotation((float) (-(MathHelper.atan2(diffY, dist) * 180.0 / 3.141592653589793)), (float) (MathHelper.atan2(diffZ, diffX) * 180.0 / 3.141592653589793 - 90.0));
    }

    public static Vec3 getVectorForRotation(final float pitch, final float yaw) {
        final float f2 = -MathHelper.cos(-pitch * 0.017453292f);
        return new Vec3(MathHelper.sin(-yaw * 0.017453292f - 3.1415927f) * f2, MathHelper.sin(-pitch * 0.017453292f), MathHelper.cos(-yaw * 0.017453292f - 3.1415927f) * f2);
    }

    public static List<BlockPos> getBlocksAroundEntity(Entity entity) {
        List<BlockPos> blocks = new ArrayList<>();
        int x = (int) Math.floor(entity.posX);
        int y = (int) Math.floor(entity.posY);
        int z = (int) Math.floor(entity.posZ);
        blocks.add(new BlockPos(x + 1, y, z));
        blocks.add(new BlockPos(x - 1, y, z));
        blocks.add(new BlockPos(x, y, z + 1));
        blocks.add(new BlockPos(x, y, z - 1));
        return blocks;
    }

    public enum Direction {
        FORWARD,
        BACKWARD,
        LEFT,
        RIGHT
    }

    //================================================================
    //Pathfinding

    public static Block getBlockType(BlockPos blockPos) {
        return getBlockState(blockPos).getBlock();
    }

    public static BlockPos fromVecToBP(Vec3 block) {
        return new BlockPos(block.xCoord, block.yCoord, block.zCoord);
    }

    public static Vec3 fromBPToVec(BlockPos block) {
        return new Vec3(block.getX(), block.getY(), block.getZ());
    }

    public static boolean isBlockWalkable(BlockPos block) {
        Block blockType = getBlockType(block);
        return (
                blockType == Blocks.air ||
                        blockType == Blocks.red_flower ||
                        blockType == Blocks.tallgrass ||
                        blockType == Blocks.yellow_flower ||
                        blockType == Blocks.double_plant
        );
    }

    public static boolean isBlockSolid(BlockPos block) {
        Block blockType = getBlockType(block);
        return (
                blockType != Blocks.water &&
                        blockType != Blocks.lava &&
                        blockType != Blocks.air &&
                        blockType != Blocks.red_flower &&
                        blockType != Blocks.tallgrass &&
                        blockType != Blocks.yellow_flower &&
                        blockType != Blocks.double_plant &&
                        blockType != Blocks.flowing_water
        );
    }

    public static double distanceFromToXZ(BlockPos pos1, BlockPos pos2) {
        final double d1 = pos1.getX() - pos2.getX();
        final double d2 = pos1.getZ() - pos2.getZ();
        return MathHelper.sqrt_double(d1 * d1 + d2 * d2);
    }

    public static double distanceFromToXZ(Vec3 vec1, Vec3 vec2) {
        final double d1 = vec1.xCoord - vec2.xCoord;
        final double d2 = vec1.zCoord - vec2.zCoord;
        return MathHelper.sqrt_double(d1 * d1 + d2 * d2);
    }

    public static Vec3 getCenteredVec(Vec3 init) {
        return init.addVector(0.5, 0, 0.5);
    }

    public static int amountNonAir(Iterable<BlockPos> blocks) {
        AtomicInteger air = new AtomicInteger();
        blocks.forEach(i -> {
            if (!isBlockWalkable(i)) {
                air.getAndIncrement();
            }
        });

        return air.get();
    }

    public static double distanceFromTo(BlockPos pos1, BlockPos pos2) {
        double d1 = pos1.getX() - pos2.getX();
        double d2 = pos1.getY() - pos2.getY();
        double d3 = pos1.getZ() - pos2.getZ();

        return Math.sqrt(d1 * d1 + d2 * d2 + d3 * d3);
    }

    public static double distanceFromTo(Vec3 pos1, Vec3 pos2) {
        double d1 = pos1.xCoord - pos2.xCoord;
        double d2 = pos1.yCoord - pos2.yCoord;
        double d3 = pos1.zCoord - pos2.zCoord;

        return Math.sqrt(d1 * d1 + d2 * d2 + d3 * d3);
    }

    public static Vec3 getNormalVecBetweenVecsRev(Vec3 vec1, Vec3 vec2) {
        return vec2.subtract(vec1).normalize().rotateYaw(90);
    }

    public static BlockPos getClosest(List<BlockPos> blocks, BlockPos around) {
        double curClosest = 9999;
        BlockPos block = null;

        for (BlockPos pos : blocks) {
            double distance = distanceFromTo(pos, around);

            if (distance < curClosest) {
                curClosest = distance;
                block = pos;
            }
        }

        return block;
    }

    public static Vec3 getClosest(List<Vec3> blocks, Vec3 around) {
        double curClosest = 9999;
        Vec3 block = null;

        for (Vec3 pos : blocks) {
            double distance = distanceFromTo(pos, around);

            if (distance < curClosest) {
                curClosest = distance;
                block = pos;
            }
        }

        return block;
    }

    public static float getBlockDamage(BlockPos target) {
        try {
            Map<Integer, DestroyBlockProgress> map = ((RenderGlobalAccessor) Minecraft.getMinecraft().renderGlobal).getDamagedBlocks();

            for (DestroyBlockProgress destroyblockprogress : map.values()) {
                if (destroyblockprogress.getPosition().equals(target)) {
                    if (destroyblockprogress.getPartialBlockDamage() >= 0 && destroyblockprogress.getPartialBlockDamage() <= 10)
                        return destroyblockprogress.getPartialBlockDamage() / 10.0F;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0F;
    }

    public static BlockPos getClosest(List<BlockPos> blocks, HashSet<BlockPos> broken, BlockPos around) {
        double curClosest = 9999;
        BlockPos block = null;

        for (BlockPos pos : blocks) {
            if (!broken.contains(pos) && VectorUtils.isHittable(pos)) {
                double distance = distanceFromTo(pos, around);

                if (distance < curClosest) {
                    curClosest = distance;
                    block = pos;
                }
            }
        }

        return block;
    }

    public static boolean canMineBlock(BlockPos b){
        return !BlockUtils.getAllVisibilityLines(b, mc.thePlayer.getPositionVector().add(new Vec3(0, mc.thePlayer.getEyeHeight(), 0))).isEmpty();
    }

    public static ArrayList<Vec3> getAllVisibilityLines(BlockPos pos, Vec3 fromEye) {
        return getAllVisibilityLines(pos, fromEye, true);
    }

    public static ArrayList<Vec3> getAllVisibilityLines(BlockPos pos, Vec3 fromEye, boolean lowerY) {
        ArrayList<Vec3> lines = new ArrayList<>();
        int accuracyChecks = 8;
        float accuracy = 1f / accuracyChecks;
        float spaceFromEdge = lowerY ? 0.1f : 8;
        for (float x = pos.getX() + spaceFromEdge; x <= pos.getX() + (1f - spaceFromEdge); x += accuracy) {
            for (float y = pos.getY() + spaceFromEdge; y <= pos.getY() + (1f - spaceFromEdge); y += accuracy) {
                for (float z = pos.getZ() + spaceFromEdge; z <= pos.getZ() + (1f - spaceFromEdge); z += accuracy) {
                    Vec3 target = new Vec3(x, y, z);
                    if (fromEye.distanceTo(target) > 4f) {
                        continue;
                    }
                    BlockPos test = new BlockPos(target.xCoord, target.yCoord, target.zCoord);
                    MovingObjectPosition movingObjectPosition = mc.theWorld.rayTraceBlocks(fromEye, target, false, false, true);
                    if (movingObjectPosition != null) {
                        BlockPos obj = movingObjectPosition.getBlockPos();
                        if (obj.equals(test))
                            lines.add(target);
                    }
                }
            }
        }

        return lines;
    }
}
