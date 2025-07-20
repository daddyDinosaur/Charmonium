package com.charmonium.utils.misc;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import java.util.concurrent.ThreadLocalRandom;

public class MathUtils {
    private static final ThreadLocalRandom random = ThreadLocalRandom.current();

    public static float randomFloat() {
        return (System.currentTimeMillis() % 2L == 0L) ? random.nextFloat() : -random.nextFloat();
    }

    public static int randomNum(int min, int max) {
        return random.nextInt(min, max + 1);
    }

    public static double getDistanceBetweenTwoBlock(BlockPos b1, BlockPos b2) {
        return Math.sqrt(b1.getSquaredDistance(b2));
    }

    public static double getDistanceBetweenTwoPoints(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Math.sqrt(square(x1 - x2) + square(y1 - y2) + square(z1 - z2));
    }

    public static double getDistanceBetweenTwoPoints(double xLength, double yLength) {
        return Math.hypot(xLength, yLength);
    }

    public static int getBlockDistanceBetweenTwoBlock(BlockPos b1, BlockPos b2) {
        return Math.abs(b1.getX() - b2.getX()) +
                Math.abs(b1.getY() - b2.getY()) +
                Math.abs(b1.getZ() - b2.getZ());
    }

    public static double square(double d) {
        return d * d;
    }

    public static Vec3d getVectorForRotation(float pitch, float yaw) {
        double pitchRad = Math.toRadians(pitch);
        double yawRad = Math.toRadians(yaw);

        float f = MathHelper.cos((float) (-yawRad - MathHelper.PI));
        float f1 = MathHelper.sin((float) (-yawRad - MathHelper.PI));
        float f2 = -MathHelper.cos((float) -pitchRad);
        float f3 = MathHelper.sin((float) -pitchRad);

        return new Vec3d(f1 * f2, f3, f * f2);
    }
}
