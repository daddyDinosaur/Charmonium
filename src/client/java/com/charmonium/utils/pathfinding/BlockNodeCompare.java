package com.charmonium.utils.pathfinding;

import java.util.Comparator;

public class BlockNodeCompare implements Comparator<BlockNodeClass> {
    @Override
    public int compare(BlockNodeClass one, BlockNodeClass two) {
        int c = Double.compare(one.getTotalCost() + one.getHCost(), two.getTotalCost()+ two.getHCost());
        if (c != 0) return c;
        return Double.compare(one.getGCost(), two.getGCost());
    }
}
