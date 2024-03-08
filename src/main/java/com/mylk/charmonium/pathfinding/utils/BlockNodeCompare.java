package com.mylk.charmonium.pathfinding.utils;

import java.util.Comparator;

public class BlockNodeCompare implements Comparator<BlockNodeClass> {

  @Override
  public int compare(BlockNodeClass one, BlockNodeClass two) {
    int totalCostComparison = Double.compare(one.totalCost, two.totalCost);
    int hCostComparison = Double.compare(one.hCost, two.hCost);

    if (totalCostComparison < 0 && hCostComparison < 0) {
      return -1;
    }

    return Double.compare(one.totalCost + one.hCost, two.totalCost + two.hCost);
  }
}
