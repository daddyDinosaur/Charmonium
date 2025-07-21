package com.charmonium.utils.pathfinding;

import com.charmonium.CharmoniumClient;
import com.charmonium.utils.blocks.BlockUtils;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.*;

public class AOTVPath {
    private static final int MAX_AOTV = 12;
    private static final int MIN_CHAIN_LEN = 3;
    private static final int[] Y_OFFSETS = {4, 6, 8, 10};

    public static class PathAction {
        public final Vec3d pos;
        public final ActionTypes actionType;

        public PathAction(Vec3d pos, ActionTypes actionType) {
            this.pos = pos;
            this.actionType = actionType;
        }
    }

    public static List<PathAction> optimize(List<Vec3d> walkerPath) {
        List<PathAction> out = new ArrayList<>();
        int lastIdx = walkerPath.size() - 1;
        int i = 0;

        int walkStart = i;
        for (int idx = 0; idx <= lastIdx; idx++) {
            if (walkerPath.get(idx).distanceTo(walkerPath.get(lastIdx)) <= 15) {
                walkStart = idx;
                break;
            }
        }

        while (i < walkStart) {
            boolean found = false;
            for (int dist = MAX_AOTV; dist >= 1; dist--) {
                int j = i + dist;
                if (j > walkStart) j = walkStart;
                if (j == i) continue;
                for (int yOffset : Y_OFFSETS) {
                    Vec3d from = walkerPath.get(i).add(0.5, yOffset, 0.5);
                    Vec3d to = walkerPath.get(j).add(0.5, yOffset, 0.5);
                    if (isAotvPathClear(from, to)) {
                        out.add(new PathAction(walkerPath.get(j).add(0, yOffset, 0), ActionTypes.AOTV));
                        i = j;
                        found = true;
                        break;
                    }
                }
                if (found) break;
            }
            if (!found) {
                out.add(new PathAction(walkerPath.get(i), ActionTypes.WALK));
                i++;
            }
        }

        for (int j = walkStart; j <= lastIdx; j++) {
            out.add(new PathAction(walkerPath.get(j), ActionTypes.WALK));
        }
        return out;
    }


    private static boolean isAotvPathClear(Vec3d from, Vec3d to) {
        double dist = from.distanceTo(to);
        if (dist > MAX_AOTV + 0.1) return false;
        Vec3d dir = to.subtract(from).normalize();
        double steps = dist / 0.8;
        for (int s = 0; s <= steps; s++) {
            Vec3d sample = from.add(dir.multiply(s * 0.8));
            BlockPos bp = new BlockPos((int) sample.x, (int) sample.y, (int) sample.z);
            if (BlockUtils.isBlockSolid(bp)) return false;
        }
        return true;
    }
}
