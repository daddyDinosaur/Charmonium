package com.mylk.charmonium.feature.impl.charMods.terminals;

import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class WhatStartsWithSolutionProvider implements TerminalSolutionProvider{
    private static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)§[0-9A-FK-OR]");

    public static String stripColor(String input) {
        return STRIP_COLOR_PATTERN.matcher(input).replaceAll("");
    }
    @Override
    public TerminalSolution provideSolution(ContainerChest chest, List<Slot> clicked) {
        String that = chest.getLowerChestInventory().getName().replace("What starts with: '", "").replace("'?", "").trim().toLowerCase();

        TerminalSolution ts = new TerminalSolution();
        ts.setCurrSlots(new ArrayList<Slot>());
        for (Slot inventorySlot : chest.inventorySlots) {
            if (inventorySlot.inventory != chest.getLowerChestInventory()) continue;
            if (inventorySlot.getHasStack() && inventorySlot.getStack() != null && !inventorySlot.getStack().isItemEnchanted() ) {
                String name = stripColor(inventorySlot.getStack().getDisplayName()).toLowerCase();
                if (name.startsWith(that))
                    ts.getCurrSlots().add(inventorySlot);
            }
        }
        return ts;
    }

    @Override
    public boolean isApplicable(ContainerChest chest) {
        return chest.getLowerChestInventory().getName().startsWith("What starts with: '");
    }
}