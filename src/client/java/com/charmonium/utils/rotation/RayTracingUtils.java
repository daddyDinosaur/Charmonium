package com.charmonium.utils.rotation;

import net.minecraft.util.math.Vec3d;
import net.minecraft.block.BlockState;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;

import static com.charmonium.CharmoniumClient.mc;

public class RayTracingUtils {
    public static boolean isHittable(final BlockPos pos) {
        Vec3d center = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        if (isRayTraceableLook(center, pos, 4.5f)) {
            return true;
        }

        // Check 8 subdivisions in each dimension
        for (int x = 1; x < 9; ++x) {
            for (int y = 1; y < 9; ++y) {
                for (int z = 1; z < 9; ++z) {
                    Vec3d point = new Vec3d(
                            pos.getX() + x/8.0 - 0.0625,
                            pos.getY() + y/8.0 - 0.0625,
                            pos.getZ() + z/8.0 - 0.0625
                    );
                    if (isRayTraceableLook(point, pos, 61f)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isRayTraceableLook(final Vec3d target, final BlockPos goal, final float range) {
        if (mc.player == null) return false;
        Vec3d eyePos = mc.player.getEyePos();
        return eyePos.squaredDistanceTo(target) <= range * range && rayTraceable(eyePos, target, goal);
    }

    private static boolean rayTraceable(Vec3d start, Vec3d end, BlockPos goal) {
        RaycastContext context = new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        );

        BlockHitResult result = mc.world.raycast(context);
        if (result == null) return false;

        BlockPos hitPos = result.getBlockPos();
        return hitPos.equals(goal);
    }
}
