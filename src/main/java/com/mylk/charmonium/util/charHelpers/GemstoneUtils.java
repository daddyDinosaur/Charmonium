package com.mylk.charmonium.util.charHelpers;

import com.mylk.charmonium.Charmonium;
import com.mylk.charmonium.config.Config;
import com.mylk.charmonium.handler.RotationHandler;
import com.mylk.charmonium.util.AngleUtils;
import com.mylk.charmonium.util.BlockUtils;
import com.mylk.charmonium.util.helper.Rotation;
import net.minecraft.block.Block;
import net.minecraft.block.BlockColored;
import net.minecraft.block.BlockStainedGlass;
import net.minecraft.block.BlockStainedGlassPane;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.*;
import java.util.stream.Collectors;

import static com.mylk.charmonium.Charmonium.mc;

public class GemstoneUtils {
    public static List<BlockPos> possibleBreaks = new ArrayList<>();
    public static List<BlockPos> currentlyPossibleToSee = new ArrayList<>();
    private static final RotationHandler rotation = RotationHandler.getInstance();

    public static void getAllBlocks() {
        int reach = (int) mc.playerController.getBlockReachDistance();
        possibleBreaks = getVeinAround(mc.thePlayer.getPosition(), reach, reach, reach);
    }

    public static boolean isBlockColored(BlockPos pos, EnumDyeColor[] requiredColors) {
        IBlockState blockState = Charmonium.mc.theWorld.getBlockState(pos);
        Block block = blockState.getBlock();

        if ((block instanceof BlockStainedGlass || block instanceof BlockStainedGlassPane) && blockState.getValue(BlockColored.COLOR) != null) {
            EnumDyeColor color = blockState.getValue(BlockColored.COLOR);
            for (EnumDyeColor requiredColor : requiredColors) {
                if (requiredColor == EnumDyeColor.BLACK)
                    return true;
                if (color == requiredColor) {
                    return true;
                }
            }
        }

        return false;
    }

    public static double getHardnessBlock(IBlockState blockState) {
        EnumDyeColor dyeColor = null;
        if (blockState.getBlock() == Blocks.stained_glass) {
            dyeColor = blockState.getValue(BlockStainedGlass.COLOR);
            if (dyeColor == EnumDyeColor.RED) {
                return 2300;
            } else if (dyeColor == EnumDyeColor.PURPLE) {
                return 3000;
            } else if (dyeColor == EnumDyeColor.LIME) {
                return 3000;
            } else if (dyeColor == EnumDyeColor.LIGHT_BLUE) {
                return 3000;
            } else if (dyeColor == EnumDyeColor.ORANGE) {
                return 3000;
            } else if (dyeColor == EnumDyeColor.YELLOW) {
                return 3800;
            } else if (dyeColor == EnumDyeColor.MAGENTA) {
                return 4800;
            }
        }
        if (blockState.getBlock() == Blocks.stained_glass_pane) {
            dyeColor = blockState.getValue(BlockStainedGlassPane.COLOR);
            if (dyeColor == EnumDyeColor.RED) {
                return 2300;
            } else if (dyeColor == EnumDyeColor.PURPLE) {
                return 3000;
            } else if (dyeColor == EnumDyeColor.LIME) {
                return 3000;
            } else if (dyeColor == EnumDyeColor.LIGHT_BLUE) {
                return 3000;
            } else if (dyeColor == EnumDyeColor.ORANGE) {
                return 3000;
            } else if (dyeColor == EnumDyeColor.YELLOW) {
                return 3800;
            } else if (dyeColor == EnumDyeColor.MAGENTA) {
                return 4800;
            }
        }

        return 0.0;
    }

    public static Vec3 getCylinderBaseVec(double[] a, double[] b, double degree, double radius) {
        double x1 = a[0];
        double y1 = a[1];
        double z1 = a[2];
        double x2 = b[0];
        double y2 = b[1];
        double z2 = b[2];

        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;

        if (Math.abs(dz) < 0.000001) {
            dz = 0.000001;
        }

        double planeCoef = x1 * dx + y1 * dy + z1 * dz;

        double xN = 0;
        double yN = 0;
        double zN = planeCoef / dz;
        double dxN = xN - x1;
        double dyN = yN - y1;
        double dzN = zN - z1;
        double lenN = Math.sqrt(dxN * dxN + dyN * dyN + dzN * dzN);
        dxN = dxN / lenN;
        dyN = dyN / lenN;
        dzN = dzN / lenN;

        double dxM = dy * dzN - dz * dyN;
        double dyM = dz * dxN - dx * dzN;
        double dzM = dx * dyN - dy * dxN;
        double cLen = Math.sqrt(dxM * dxM + dyM * dyM + dzM * dzM);
        dxM = dxM / cLen;
        dyM = dyM / cLen;
        dzM = dzM / cLen;

        double angle = degree * (Math.PI / 180);
        double dxP = dxN * Math.cos(angle) + dxM * Math.sin(angle);
        double dyP = dyN * Math.cos(angle) + dyM * Math.sin(angle);
        double dzP = dzN * Math.cos(angle) + dzM * Math.sin(angle);

        return new Vec3(dxP * radius, dyP * radius, dzP * radius);
    }

    public static Vec3 getPossibleLocDefault(Vec3 block1, BlockPos destBlock, Block[] blocksToIgnore) {
        double playerHeight = 1.54;

        Vec3 destBlockCenter = new Vec3(destBlock.getX() + 0.5, destBlock.getY() + 0.5, destBlock.getZ() + 0.5);

        double radiusStep = 0.1;
        double radiusMax = Math.sqrt(3) / 2 + radiusStep;

        for (double radius = radiusStep; radius < radiusMax; radius += radiusStep) {
            double angleStep = (radiusMax / radius) * 5;
            for (double angle = 0; angle < 360 + angleStep; angle += angleStep) {
                Vec3 vec = getCylinderBaseVec(
                        new double[] { block1.xCoord, block1.yCoord + playerHeight, block1.zCoord },
                        new double[] { destBlockCenter.xCoord, destBlockCenter.yCoord, destBlockCenter.zCoord },
                        angle,
                        radius
                );

                Vec3 point = new Vec3(
                        destBlockCenter.xCoord + vec.xCoord,
                        destBlockCenter.yCoord + vec.yCoord,
                        destBlockCenter.zCoord + vec.zCoord
                );

                MovingObjectPosition collisionPoint = mc.theWorld.rayTraceBlocks(
                        block1.addVector(0, playerHeight, 0),
                        point,
                        true,
                        true,
                        true
                );
                if (collisionPoint == null || collisionPoint.getBlockPos().equals(destBlock)) {
                    return point;
                }
            }
        }

        return null;
    }

//    public static void updateListData(HashSet<BlockPos> broken) {
//        List<BlockPos> tmp = new ArrayList<>(possibleBreaks);
//        tmp.removeIf(b ->
//                broken.contains(b) ||
//                        !isBlockColored(b, Config.getRequiredColors()) ||
//                        BlockUtils.distanceFromTo(
//                                BlockUtils.fromBPToVec(b),
//                                mc.thePlayer.getPositionVector()
//                                        .addVector(0, mc.thePlayer.eyeHeight, 0)) >= mc.playerController.getBlockReachDistance() ||
//                        getPossibleLocDefault(mc.thePlayer.getPositionVector(), b, new Block[]{Blocks.air}) == null);
//
//        currentlyPossibleToSee = tmp.stream()
//                .sorted(Comparator.comparingDouble(b -> getCost(rotation.getRotation(BlockUtils.fromBPToVec(b).addVector(0.5, 0, 0.5)))))
//                .sorted(Comparator.comparingDouble(b -> mc.thePlayer.getPositionVector().squareDistanceTo(new Vec3(b.getX() + 0.5, b.getY() + 0.5, b.getZ() + 0.5))))
//                .collect(Collectors.toList());
//
//        BlockPos bestBlock = currentlyPossibleToSee.isEmpty() ? null : currentlyPossibleToSee.get(0);
//
//        List<BlockPos> newlyVisibleBlocks = new ArrayList<>();
//        for (BlockPos blockPos : tmp) {
//            if (!hasAnyLineOfSight(blockPos) && hasAnyLineOfSight(bestBlock) && VectorUtils.getRandomHittable(blockPos) != null) {
//                newlyVisibleBlocks.add(blockPos);
//            }
//        }
//
//        currentlyPossibleToSee.addAll(newlyVisibleBlocks);
//
//        currentlyPossibleToSee.sort(Comparator.comparingDouble(b -> {
//            Rotation rotation = RotationHandler.getInstance().getRotation(BlockUtils.fromBPToVec((BlockPos)b).addVector(0.5, 0, 0.5));
//            return getCost(rotation);
//        }).thenComparingDouble(blockPos -> {
//            if (bestBlock != null && blockPos instanceof BlockPos) {
//                BlockPos nextBlock = (BlockPos) blockPos;
//                Vec3 bestBlockCenter = new Vec3(bestBlock.getX() + 0.5, bestBlock.getY() + 0.5, bestBlock.getZ() + 0.5);
//                Vec3 nextBlockCenter = new Vec3(nextBlock.getX() + 0.5, nextBlock.getY() + 0.5, nextBlock.getZ() + 0.5);
//                return bestBlockCenter.squareDistanceTo(nextBlockCenter);
//            }
//            return 0;
//        }));
//    }

    public static void updateListData(HashSet<BlockPos> broken) {
        List<BlockPos> tmp = new ArrayList<>(possibleBreaks);
        tmp.removeIf(b ->
                broken.contains(b) ||
                !isBlockColored(b, Config.getRequiredColors()) ||
                !hasAnyLineOfSight(b) ||
                BlockUtils.distanceFromTo(
                        BlockUtils.fromBPToVec(b),
                        mc.thePlayer.getPositionVector()
                                .addVector(0, mc.thePlayer.eyeHeight, 0)) >= mc.playerController.getBlockReachDistance() ||
                getPossibleLocDefault(mc.thePlayer.getPositionVector(), b, new Block[] { Blocks.air }) == null);

        currentlyPossibleToSee = tmp.stream()
                .sorted(Comparator.comparingDouble(b -> getCost(rotation.getRotation(BlockUtils.fromBPToVec(b).addVector(0.5, 0, 0.5)))))
                .sorted(Comparator.comparingDouble(b -> mc.thePlayer.getPositionVector().squareDistanceTo(new Vec3(b.getX() + 0.5, b.getY() + 0.5, b.getZ() + 0.5))))
                .collect(Collectors.toList());

        BlockPos bestBlock = currentlyPossibleToSee.isEmpty() ? null : currentlyPossibleToSee.get(0);

        currentlyPossibleToSee.sort(Comparator.comparingDouble(b -> {
            Rotation rotation = RotationHandler.getInstance().getRotation(BlockUtils.fromBPToVec((BlockPos)b).addVector(0.5, 0, 0.5));
            return getCost(rotation);
        }).thenComparingDouble(blockPos -> {
            if (bestBlock != null && blockPos instanceof BlockPos) {
                BlockPos nextBlock = (BlockPos) blockPos;
                Vec3 bestBlockCenter = new Vec3(bestBlock.getX() + 0.5, bestBlock.getY() + 0.5, bestBlock.getZ() + 0.5);
                Vec3 nextBlockCenter = new Vec3(nextBlock.getX() + 0.5, nextBlock.getY() + 0.5, nextBlock.getZ() + 0.5);
                return bestBlockCenter.squareDistanceTo(nextBlockCenter);
            }
            return 0;
        }));
    }

//    public static void updateListData(HashSet<BlockPos> broken) {
//        List<BlockPos> tmp = new ArrayList<>(possibleBreaks);
//        tmp.removeIf(b ->
//                broken.contains(b) ||
//                !isBlockColored(b, Config.getRequiredColors()) ||
//                !hasAnyLineOfSight(b) ||
//                BlockUtils.distanceFromTo(
//                        BlockUtils.fromBPToVec(b),
//                        mc.thePlayer.getPositionVector()
//                                .addVector(0, mc.thePlayer.eyeHeight, 0)) >= mc.playerController.getBlockReachDistance() ||
//                getPossibleLocDefault(mc.thePlayer.getPositionVector(), b, new Block[] { Blocks.air }) == null);
//
//        tmp.sort(Comparator.comparingDouble(b -> getCost(rotation.getRotation(BlockUtils.fromBPToVec(b).addVector(0.5, 0, 0.5)))));
//        currentlyPossibleToSee = tmp;
//    }

    private static double getCost(Rotation neededRotation) {
        double cost = 0;

        double yaw = Math.abs(neededRotation.getYaw());
        cost += yaw / 100;

        return cost;
    }

    public static boolean hasAnyLineOfSight(BlockPos blockPos) {
        EntityPlayerSP player = mc.thePlayer;

        if (player == null) {
            return false;
        }

        Vec3 playerEyes = new Vec3(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        double offsetX = blockPos.getX() + 0.5 - player.posX;
        double offsetY = blockPos.getY() + 0.5 - (player.posY + player.getEyeHeight());
        double offsetZ = blockPos.getZ() + 0.5 - player.posZ;
        double blockHalfSize = 0.5;
        double buffer = 0.125;
        double[] edgeOffsets = {-blockHalfSize + buffer, 0, blockHalfSize - buffer};

        for (double edgeOffsetX : edgeOffsets) {
            for (double edgeOffsetY : edgeOffsets) {
                for (double edgeOffsetZ : edgeOffsets) {
                    Vec3 toBlock = new Vec3(edgeOffsetX, edgeOffsetY, edgeOffsetZ).addVector(offsetX, offsetY, offsetZ);
                    MovingObjectPosition result = mc.theWorld.rayTraceBlocks(playerEyes, playerEyes.add(toBlock), false, true, false);
                    if (result != null && result.getBlockPos().equals(blockPos)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }


    public static boolean hasLineOfSight(BlockPos blockPos) {
        EntityPlayerSP player = mc.thePlayer;

        if (player == null) {
            return false;
        }

        Vec3 playerEyes = new Vec3(player.posX, player.posY + player.getEyeHeight(), player.posZ);

        Vec3 toBlock = new Vec3(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5).subtract(playerEyes);

        MovingObjectPosition result = mc.theWorld.rayTraceBlocks(playerEyes, playerEyes.add(toBlock), false, true, false);

        return result != null && result.getBlockPos().equals(blockPos);
    }


    private static List<BlockPos> getVeinAround(BlockPos posAround, int addX, int addY, int addZ) {
        return getBlocksInRadius(
                addX,
                addY,
                addZ,
                posAround,
                new Block[] { Blocks.stained_glass_pane, Blocks.stained_glass }
        );
    }

    public static List<BlockPos> getBlocksInRadius(int x, int y, int z, BlockPos around, Block[] blocks) {
        List<BlockPos> returnList = new ArrayList<>();

        for (int i = -x; i <= x; i++) {
            for (int j = -y; j <= y; j++) {
                for (int k = -z; k <= z; k++) {
                    BlockPos newBlock = around.add(i, j, k);

                    if (
                            Arrays
                                    .stream(blocks)
                                    .anyMatch(block -> {
                                        return BlockUtils.getBlockType(newBlock) == block;
                                    })
                    ) {
                        returnList.add(newBlock);
                    }
                }
            }
        }

        return returnList;
    }

}
