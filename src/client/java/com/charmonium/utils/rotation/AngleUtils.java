package com.charmonium.utils.rotation;

import com.charmonium.utils.misc.MathUtils;
import com.charmonium.utils.blocks.BlockUtils;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import java.util.ArrayList;
import java.util.List;

public class AngleUtils {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static float get360RotationYaw(float yaw) {
        return (yaw % 360 + 360) % 360;
    }

    public static float normalizeAngle(float angle) {
        while (angle > 180) {
            angle -= 360;
        }
        while (angle <= -180) {
            angle += 360;
        }
        return angle;
    }

    public static float normalizeYaw(float yaw) {
        float newYaw = yaw % 360F;
        if (newYaw < -180F) {
            newYaw += 360F;
        }
        if (newYaw > 180F) {
            newYaw -= 360F;
        }
        return newYaw;
    }

    public static float getRequiredYawSide(BlockPos blockLookingAt) {
        double deltaX = blockLookingAt.getX() - mc.player.getX() + 0.5d;
        double deltaZ = blockLookingAt.getZ() - mc.player.getZ() + 0.5d;

        BlockUtils.BlockSides blockSideToMine = getOptimalSide(blockLookingAt);

        switch (blockSideToMine) {
            case posX: deltaX += 0.5d; break;
            case negX: deltaX -= 0.5d; break;
            case posZ: deltaZ += 0.5d; break;
            case negZ: deltaZ -= 0.5d; break;
        }

        return getRequiredYaw(deltaX, deltaZ);
    }

    static final List<Block> lookAtCenterBlocks = new ArrayList<Block>() {{
        add(Blocks.GLASS_PANE);
    }};

    public static float getRequiredPitchSide(BlockPos blockLookingAt) {
        double deltaX = blockLookingAt.getX() - mc.player.getX() + 0.5d;
        double deltaZ = blockLookingAt.getZ() - mc.player.getZ() + 0.5d;
        double deltaY = (blockLookingAt.getY() + 0.5d) - (mc.player.getY() + 1.62d);

        BlockUtils.BlockSides blockSideToMine = getOptimalSide(blockLookingAt);

        switch (blockSideToMine) {
            case posX: deltaX += 0.5d; break;
            case negX: deltaX -= 0.5d; break;
            case posZ: deltaZ += 0.5d; break;
            case negZ: deltaZ -= 0.5d; break;
            case up: deltaY += 0.5d; break;
            case down: deltaY -= 0.5d; break;
        }

        return getRequiredPitch(deltaX, deltaY, deltaZ);
    }

    public static BlockUtils.BlockSides getOptimalSide(BlockPos blockPos) {
        ArrayList<BlockUtils.BlockSides> blockSidesNotCovered = BlockUtils.getAdjBlocksNotCovered(blockPos);
        BlockUtils.BlockSides blockSideToMine = BlockUtils.BlockSides.NONE;
        if (!blockSidesNotCovered.isEmpty() && !lookAtCenterBlocks.contains(BlockUtils.getBlock(blockPos))) {
            double lowestCost = 9999;
            blockSideToMine = blockSidesNotCovered.getFirst();

            for (BlockUtils.BlockSides blockSide : blockSidesNotCovered) {
                double tempCost = switch (blockSide) {
                    case posX -> MathUtils.getDistanceBetweenTwoPoints(
                            mc.player.getX(), mc.player.getY() + 1.62d, mc.player.getZ(),
                            blockPos.getX() + 0.5d, blockPos.getY(), blockPos.getZ());
                    case negX -> MathUtils.getDistanceBetweenTwoPoints(
                            mc.player.getX(), mc.player.getY() + 1.62d, mc.player.getZ(),
                            blockPos.getX() - 0.5d, blockPos.getY(), blockPos.getZ());
                    case posZ -> MathUtils.getDistanceBetweenTwoPoints(
                            mc.player.getX(), mc.player.getY() + 1.62d, mc.player.getZ(),
                            blockPos.getX(), blockPos.getY(), blockPos.getZ() + 0.5d);
                    case negZ -> MathUtils.getDistanceBetweenTwoPoints(
                            mc.player.getX(), mc.player.getY() + 1.62d, mc.player.getZ(),
                            blockPos.getX(), blockPos.getY(), blockPos.getZ() - 0.5d);
                    case up -> MathUtils.getDistanceBetweenTwoPoints(
                            mc.player.getX(), mc.player.getY() + 1.62d, mc.player.getZ(),
                            blockPos.getX(), blockPos.getY() + 0.5d, blockPos.getZ());
                    case down -> MathUtils.getDistanceBetweenTwoPoints(
                            mc.player.getX(), mc.player.getY() + 1.62d, mc.player.getZ(),
                            blockPos.getX(), blockPos.getY() - 0.5d, blockPos.getZ());
                    default -> 0;
                };
                if (tempCost < lowestCost) {
                    lowestCost = tempCost;
                    blockSideToMine = blockSide;
                }
            }
        }
        return blockSideToMine;
    }

    public static float get360RotationYaw() {
        if (mc.player == null)
            return 0;
        return get360RotationYaw(mc.player.getYaw());
    }

    public static float clockwiseDifference(float initialYaw360, float targetYaw360) {
        return get360RotationYaw(targetYaw360 - initialYaw360);
    }

    public static float getRequiredPitch(double deltaX, double deltaY, double deltaZ) {
        double deltaDis = MathUtils.getDistanceBetweenTwoPoints(deltaX, deltaZ);
        double pitch = -(Math.atan(deltaY / deltaDis) * 180 / Math.PI);
        if ((float) pitch > 90 || (float) pitch < -90) {
            System.out.println(pitch + " " + deltaX + " " + deltaZ);
            return 0;
        }
        return (float) pitch;
    }

    public static boolean isDiffLowerThan(float neededChangeYaw, float neededChangePitch, float diff) {
        float actualYaw = mc.player.getYaw();
        float actualPitch = mc.player.getPitch();
        return Math.abs(actualYaw - neededChangeYaw) < diff && Math.abs(actualPitch - neededChangePitch) < diff;
    }

    public static float getRequiredYaw(double deltaX, double deltaZ) {
        if (deltaX == 0 && deltaZ < 0) // special case
            return -180f;

        return (float) (Math.atan(-deltaX / deltaZ) * 180 / Math.PI) +
                ((deltaX > 0 && deltaZ < 0) ? -180 : 0) +
                ((deltaX < 0 && deltaZ < 0) ? 180 : 0);
    }

    public static Vec3d getVectorForRotation(float pitch, float yaw) {
        float pitchRad = (float) Math.toRadians(pitch);
        float yawRad = (float) Math.toRadians(yaw);

        float f = MathHelper.cos(-yawRad - MathHelper.PI);
        float f1 = MathHelper.sin(-yawRad - MathHelper.PI);
        float f2 = -MathHelper.cos(-pitchRad);
        float f3 = MathHelper.sin(-pitchRad);

        return new Vec3d(f1 * f2, f3, f * f2);
    }

    public static float antiClockwiseDifference(float initialYaw360, float targetYaw360) {
        return get360RotationYaw(initialYaw360 - targetYaw360);
    }

    public static float smallestAngleDifference(float initialYaw360, float targetYaw360) {
        return Math.min(clockwiseDifference(initialYaw360, targetYaw360), antiClockwiseDifference(initialYaw360, targetYaw360));
    }

    public static float getActualRotationYaw() { //f3
        return getActualRotationYaw(mc.player.getYaw());
    }

    public static float getActualRotationYaw(float yaw) { //f3
        return yaw > 0 ?
                (yaw % 360 > 180 ? -(180 - (yaw % 360 - 180)) : yaw % 360) :
                (-yaw % 360 > 180 ? (180 - (-yaw % 360 - 180)) : -(-yaw % 360));
    }

    public static int getYawRotationTime(float target_yaw, int angle_per_unit, int time_per_unit, int min) {
        return (int) Math.max(min, getAngleDifference(target_yaw, getActualRotationYaw()) / angle_per_unit * time_per_unit);
    }

    public static int getPitchRotationTime(float target_pitch, int angle_per_unit, int time_per_unit, int min) {
        return (int) Math.max(min, Math.abs(target_pitch - mc.player.getPitch()) / angle_per_unit * time_per_unit);
    }

    public static float getAngleDifference(float actualYaw1, float actualYaw2) {
        if (actualYaw1 - actualYaw2 > 180) {
            return Math.abs(actualYaw1 - 360 - actualYaw2);
        } else if (actualYaw1 - actualYaw2 < -180) {
            return Math.abs(actualYaw2 - 360 - actualYaw1);
        } else return Math.abs(actualYaw1 - actualYaw2);
    }

    public static float getRequiredPitchCenter(BlockPos blockLookingAt) {
        double deltaX = blockLookingAt.getX() - mc.player.getX() + 0.5d;
        double deltaZ = blockLookingAt.getZ() - mc.player.getZ() + 0.5d;
        double deltaY = (blockLookingAt.getY() + 0.5d) - (mc.player.getY() + 1.62d);
        return getRequiredPitch(deltaX, deltaY, deltaZ);
    }

    public static float getRequiredYawCenter(BlockPos blockLookingAt) {
        double deltaX = blockLookingAt.getX() - mc.player.getX() + 0.5d;
        double deltaZ = blockLookingAt.getZ() - mc.player.getZ() + 0.5d;
        return getRequiredYaw(deltaX, deltaZ);
    }

    public static boolean shouldRotateClockwise(float start, float target) {
        return clockwiseDifference(get360RotationYaw(start), target) < 180;
    }

    public static float getActualYawFrom360(float yaw360) {
        float currentYaw = yaw360;
        if (mc.player.getYaw() > yaw360) {
            while (mc.player.getYaw() - currentYaw < 180 || mc.player.getYaw() - currentYaw > 0) {
                if (Math.abs(currentYaw + 360 - mc.player.getYaw()) < Math.abs(currentYaw - mc.player.getYaw()))
                    currentYaw = currentYaw + 360;
                else break;
            }
        }
        if (mc.player.getYaw() < yaw360) {
            while (currentYaw - mc.player.getYaw() > 180 || mc.player.getYaw() - currentYaw < 0) {
                if (Math.abs(currentYaw - 360 - mc.player.getYaw()) < Math.abs(currentYaw - mc.player.getYaw()))
                    currentYaw = currentYaw - 360;
                else break;
            }
        }
        return currentYaw;
    }

    public static float getClosestDiagonal() {
        return getClosestDiagonal(get360RotationYaw());
    }

    public static float getClosestDiagonal(float yaw) {
        float rot = get360RotationYaw(yaw);
        if (rot < 90 && rot > 0) {
            return 45;
        } else if (rot < 180) {
            return 135f;
        } else if (rot < 270) {
            return 225f;
        } else {
            return 315f;
        }
    }

    public static float getClosest30() {
        float rot = get360RotationYaw();
        if (rot < 45) {
            return 30f;
        } else if (rot < 90) {
            return 60f;
        } else if (rot < 135) {
            return 120f;
        } else if (rot < 180) {
            return 150f;
        } else if (rot < 225) {
            return 210f;
        } else if (rot < 270) {
            return 240f;
        } else if (rot < 315) {
            return 300f;
        } else {
            return 330f;
        }
    }

    public static float getClosest45(float inputAngle) {
        float normalizedAngle = (inputAngle % 360 + 360) % 360;
        float remainder = normalizedAngle % 45;
        if (remainder <= 22.5) {
            return (float) (Math.floor(normalizedAngle / 45) * 45);
        } else {
            return (float) (Math.ceil(normalizedAngle / 45) * 45);
        }
    }

    public static float getClosest() {
        float rot = get360RotationYaw();
        if (rot < 45 || rot > 315) {
            return 0f;
        } else if (rot < 135) {
            return 90f;
        } else if (rot < 225) {
            return 180f;
        } else {
            return 270f;
        }
    }

    public static float getClosest(float yaw) {
        float rot = get360RotationYaw(yaw);
        if (rot < 45 || rot > 315) {
            return 0f;
        } else if (rot < 135) {
            return 90f;
        } else if (rot < 225) {
            return 180f;
        } else {
            return 270f;
        }
    }
}