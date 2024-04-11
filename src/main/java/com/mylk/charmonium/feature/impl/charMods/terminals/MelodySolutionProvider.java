package com.mylk.charmonium.feature.impl.charMods.terminals;

import com.mylk.charmonium.Charmonium;
import com.mylk.charmonium.util.LogUtils;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static com.mylk.charmonium.Charmonium.mc;

public class MelodySolutionProvider implements TerminalSolutionProvider {
    @Override
    public TerminalSolution provideSolution(ContainerChest chest, List<Slot> clicked) {
        int greenSlot = -1;
        int purpleSlot = -1;
        int clickSlot2 = 0;

        TerminalSolution ts = new TerminalSolution();
        ts.setCurrSlots(new ArrayList<Slot>());

//        List<Integer> colors = new ArrayList<>();
//        for (Slot inventorySlot : chest.inventorySlots) {
//            final ItemStack stack = inventorySlot.getStack();
//            if (stack != null) {
//                colors.add(stack.getItemDamage());
//            }
//        }
//        int targetPaneCol = colors.indexOf(EnumDyeColor.MAGENTA.getDyeDamage());
//        int movingPaneIndex = colors.indexOf(EnumDyeColor.LIME.getDyeDamage());
//        int movingPaneCol = movingPaneIndex % 9;
//        int clickSlot = (movingPaneIndex / 9) * 9 + 7;
//
//        boolean click = targetPaneCol == movingPaneCol;
//        LogUtils.sendDebug("Click: " + click + " | Target: " + targetPaneCol + " | Moving: " + movingPaneCol + " | Click Slot: " + clickSlot);
//
//        mc.fontRendererObj.drawStringWithShadow(
//                "Click: " + click + " | Target: " + targetPaneCol + " | Moving: " + movingPaneCol + " | Click Slot: " + clickSlot,
//                100,
//                100,
//                Color.GREEN.getRGB()
//        );

        for (Slot inventorySlot : chest.inventorySlots) {
            if (inventorySlot.inventory != chest.getLowerChestInventory()) continue;

            final ItemStack stack = inventorySlot.getStack();
            if (stack != null) {
                final EnumDyeColor color = EnumDyeColor.byDyeDamage(stack.getItemDamage());
                final Item item3 = stack.getItem();
                switch (color) {
                    case GREEN: {
                        if (item3 == Item.getItemFromBlock(Blocks.stained_glass_pane)) {
                            if (purpleSlot == -1) {
                                purpleSlot = inventorySlot.getSlotIndex() % 9;
                                break;
                            }
                        }
                        break;
                    }
                    case PURPLE: {
                        if (item3 == Item.getItemFromBlock(Blocks.stained_glass_pane)) {
                            if (greenSlot == -1) {
                                greenSlot = inventorySlot.getSlotIndex() % 9;
                                break;
                            }
                        } else {
                            if (item3 == Item.getItemFromBlock(Blocks.stained_hardened_clay)) {
                                clickSlot2 = inventorySlot.getSlotIndex();
                                break;
                            }
                        }
                        break;
                    }
                }
            }
        }

        boolean click = purpleSlot != -1 && clickSlot2 != 0 && greenSlot == purpleSlot;

        if (click)
            ts.getCurrSlots().add(chest.inventorySlots.get(clickSlot2));

        mc.fontRendererObj.drawStringWithShadow(
                "Click: " + click + " | Green Slot: " + greenSlot + " | Purple Slot: " + purpleSlot + " | Click Slot: " + clickSlot2,
                100,
                100,
                Color.GREEN.getRGB()
        );

        return ts;
    }

    @Override
    public boolean isApplicable(ContainerChest chest) {
        return chest.getLowerChestInventory().getName().equals("Click the button on time!");
    }
}