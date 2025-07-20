package com.charmonium.utils.pathfinding;

import java.util.Comparator;

public class BlockNodeCompare implements Comparator<BlockNodeClass> {

    @Override
    public int compare(BlockNodeClass one, BlockNodeClass two) {
        int totalCostComparison = Double.compare(one.getTotalCost(), two.getTotalCost());
        int hCostComparison = Double.compare(one.getHCost(), two.getHCost());

        if (totalCostComparison < 0 && hCostComparison < 0) {
            return -1;
        }
        return Double.compare(one.getTotalCost() + one.getHCost(), two.getTotalCost() + two.getHCost());
    }
}
