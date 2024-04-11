package com.mylk.charmonium.feature.impl.charMods.terminals;

import com.mylk.charmonium.Charmonium;
import com.mylk.charmonium.config.Config;
import com.mylk.charmonium.handler.GameStateHandler;
import com.mylk.charmonium.util.helper.Timer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.List;

public class TerminalSolver {
    private static TerminalSolver instance;
    public static TerminalSolver getInstance() {
        if (instance == null) {
            instance = new TerminalSolver();
        }
        return instance;
    }
    private static final Timer clickDelay = new Timer();
    //private static Timer rescan = new Timer();
    public static final List<TerminalSolutionProvider> solutionProviders = new ArrayList<TerminalSolutionProvider>();

    static  {
        solutionProviders.add(new WhatStartsWithSolutionProvider());
        solutionProviders.add(new SelectAllColorSolutionProvider());
        solutionProviders.add(new SelectInOrderSolutionProvider());
        solutionProviders.add(new CorrectThePaneSolutionProvider());
        solutionProviders.add(new MelodySolutionProvider());
    }

    private TerminalSolutionProvider solutionProvider;
    private TerminalSolution solution;
    private final List<Slot> clicked = new ArrayList<Slot>();

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        //if (!Config.autoTerminals || GameStateHandler.dungeonFloor != 7) return;
        solution = null;
        solutionProvider = null;
        clicked.clear();
        clickDelay.reset();
        if (event.gui instanceof GuiChest) {
            ContainerChest cc = (ContainerChest) ((GuiChest) event.gui).inventorySlots;
            for (TerminalSolutionProvider solutionProvider : solutionProviders) {
                if (solutionProvider.isApplicable(cc)) {
                    solution = solutionProvider.provideSolution(cc, clicked);
                    this.solutionProvider = solutionProvider;
                    //rescan.schedule();
                }
            }
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent tickEvent) {
        //if (!Config.autoTerminals || GameStateHandler.dungeonFloor != 7) return;
        if (solutionProvider == null) return;
        //if (!rescan.hasPassed(750)) return;
        if (!(Minecraft.getMinecraft().currentScreen instanceof GuiChest)) {
            solution = null;
            solutionProvider = null;
            clicked.clear();
            clickDelay.reset();
            //rescan.reset();
            return;
        }
        ContainerChest cc = (ContainerChest) ((GuiChest) Minecraft.getMinecraft().currentScreen).inventorySlots;

        solution = solutionProvider.provideSolution(cc, clicked);
        //rescan.schedule();
    }

    @SubscribeEvent
    public void onGuiPostRender(GuiScreenEvent.DrawScreenEvent.Post rendered) {
        if (solutionProvider == null) return;
        if (!(Minecraft.getMinecraft().currentScreen instanceof GuiChest)) {
            solution = null;
            solutionProvider = null;
            clicked.clear();
            clickDelay.reset();
            //rescan.reset();
            return;
        }

        if (solution != null) {
            int i = 222;
            int j = i - 108;
            int ySize = j + (((ContainerChest)(((GuiChest) Minecraft.getMinecraft().currentScreen).inventorySlots)).getLowerChestInventory().getSizeInventory() / 9) * 18;
            int left = (rendered.gui.width - 176) / 2;
            int top = (rendered.gui.height - ySize ) / 2;
            GlStateManager.pushMatrix();
            GlStateManager.disableDepth();
            GlStateManager.disableLighting();
            GlStateManager.colorMask(true, true, true, false);
            GlStateManager.translate(left, top, 0);
            if (solution.getCurrSlots() != null && !solution.getCurrSlots().isEmpty()) {
                for (Slot currSlot : solution.getCurrSlots()) {
                    int x = currSlot.xDisplayPosition;
                    int y = currSlot.yDisplayPosition;
                    Gui.drawRect(x, y, x + 16, y + 16, 0x7700FFFF);
                }

                if (clickDelay.hasPassed(Config.termClickDelay)) {
                    Minecraft.getMinecraft().playerController.windowClick(((((GuiChest) Minecraft.getMinecraft().currentScreen).inventorySlots)).windowId, solution.getCurrSlots().get(0).getSlotIndex(), 0, 0, Minecraft.getMinecraft().thePlayer);
                    clickDelay.schedule();
                }
            }
            if (solution.getNextSlots() != null && !solution.getNextSlots().isEmpty()) {
                for (Slot nextSlot : solution.getNextSlots()) {
                    int x = nextSlot.xDisplayPosition;
                    int y = nextSlot.yDisplayPosition;
                    Gui.drawRect(x, y, x + 16, y + 16, 0x77FFFF00);
                }

                if (clickDelay.hasPassed(Config.termClickDelay) && solution.getCurrSlots().isEmpty()) {
                    Minecraft.getMinecraft().playerController.windowClick(((((GuiChest) Minecraft.getMinecraft().currentScreen).inventorySlots)).windowId, solution.getNextSlots().get(0).getSlotIndex(), 0, 0, Minecraft.getMinecraft().thePlayer);
                    clickDelay.schedule();
                }
            }
            GlStateManager.colorMask(true, true, true, true);
            GlStateManager.popMatrix();
        }
        GlStateManager.enableBlend();
        GlStateManager.enableLighting();
    }

    @SubscribeEvent
    public void onMouseInput(GuiScreenEvent.MouseInputEvent.Pre mouseInputEvent) {
        if (!Config.autoTerminals || GameStateHandler.dungeonFloor != 7) return;
        if (Mouse.getEventButton() == -1) return;
        if (solutionProvider == null) return;
        if (solution == null) return;
        if (solution.getCurrSlots() == null) {
            return;
        }
        Slot s = ((GuiChest) Minecraft.getMinecraft().currentScreen).getSlotUnderMouse();
        if (solution.getCurrSlots().contains(s)) {
            clicked.add(s);
        } else {
            mouseInputEvent.setCanceled(true);
        }
    }

//    long lastInteractTime = 1;
//    long sleepTime = 1;
//
//    @SubscribeEvent
//    public void onRenderGui(final ChestBackgroundDrawnEvent event) {
//        final List<Slot> invSlots = event.slots;
//
//        if (System.currentTimeMillis() - lastInteractTime >= sleepTime) {
//            int greenSlot = -1;
//            int purpleSlot = -1;
//            int clickSlot2 = 0;
//            for (int k = 1; k < 51; ++k) {
//                final ItemStack stack = invSlots.get(k).getStack();
//                if (stack != null) {
//                    final EnumDyeColor color = EnumDyeColor.byDyeDamage(stack.getItemDamage());
//                    switch (color) {
//                        case PURPLE: {
//                            if (purpleSlot == -1) {
//                                purpleSlot = k % 9;
//                                break;
//                            }
//                            break;
//                        }
//                        case LIME: {
//                            final Item item3 = stack.getItem();
//                            if (item3 == Item.getItemFromBlock((Block) Blocks.stained_glass_pane)) {
//                                if (greenSlot == -1) {
//                                    greenSlot = k % 9;
//                                    break;
//                                }
//                                break;
//                            }
//                            else {
//                                if (item3 == Item.getItemFromBlock(Blocks.stained_hardened_clay)) {
//                                    clickSlot2 = k;
//                                    break;
//                                }
//                                break;
//                            }
//                            break;
//                        }
//                    }
//                }
//            }
//            if (purpleSlot != -1 && clickSlot2 != 0 && greenSlot == purpleSlot) {
//                Charmonium.mc.playerController.windowClick(Charmonium.mc.thePlayer.openContainer.windowId, clickSlot2, 0, 0, Charmonium.mc.thePlayer);
//                lastInteractTime = System.currentTimeMillis();
//            }
//        }
//
//    }

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        if (!Config.autoTerminals || GameStateHandler.dungeonFloor != 7) return;
        if (solutionProvider == null) return;
        event.toolTip.clear();
    }


}
