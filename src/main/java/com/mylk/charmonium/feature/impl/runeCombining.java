package com.mylk.charmonium.feature.impl;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.mylk.charmonium.Charmonium;
import com.mylk.charmonium.config.Config;
import com.mylk.charmonium.event.ScreenClosedEvent;
import com.mylk.charmonium.macro.impl.misc.fuelFilling;
import com.mylk.charmonium.util.InventoryUtils;
import com.mylk.charmonium.util.helper.Timer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.NotNull;
import scala.Char;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.mylk.charmonium.Charmonium.mc;

public class runeCombining {
    private boolean inRunicPedestal;
    private Slot runeSlot = null;
    private static runeCombining instance;
    private static Timer waitTimer = new Timer();
    private static List<Slot> ignoreSlots = new ArrayList<Slot>();
    private static states state = states.NONE;

    public static runeCombining getInstance() {
        if (instance == null) {
            instance = new runeCombining();
        }
        return instance;
    }

    @SubscribeEvent
    public void onGuiRender(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (mc.thePlayer == null || mc.theWorld == null || !Config.runeCombiner) return;
        GuiScreen screen = event.gui;
        if (screen instanceof GuiChest) {
            GuiChest guiChest = (GuiChest) screen;
            String chestName = guiChest.inventorySlots.inventorySlots.get(0).inventory.getName();
            if (chestName.contains("Runic")) {
                mc.fontRendererObj.drawStringWithShadow(
                        "Rune Combiner On",
                        100,
                        100,
                        Color.GREEN.getRGB()
                );
                if (!inRunicPedestal) {
                    inRunicPedestal = true;
                    state = states.NONE;
                    waitTimer.schedule();
                }
            } else {
                if (inRunicPedestal) {
                    inRunicPedestal = false;
                    waitTimer.reset();
                    runeSlot = null;
                    ignoreSlots = new ArrayList<Slot>();
                    state = states.NONE;
                }
            }
        } else {
            if (inRunicPedestal) {
                inRunicPedestal = false;
                waitTimer.reset();
                runeSlot = null;
                ignoreSlots = new ArrayList<Slot>();
                state = states.NONE;
            }
        }
    }

    @SubscribeEvent
    public void onBackgroundDraw(GuiScreenEvent.BackgroundDrawnEvent event) {
        if (!inRunicPedestal || !Config.runeCombiner) return;

        if (mc.thePlayer.openContainer instanceof ContainerChest) {

            ContainerChest runicPedestal = (ContainerChest) mc.thePlayer.openContainer;

            switch (state) {
                case NONE: {
                    Charmonium.sendMessage("Finding runes...");
                    state = states.FIND;
                    waitTimer.schedule();
                    break;
                }
                case FIND: {
                    if (checkRune()) {
                        Charmonium.sendMessage("Continued using: " + runeSlot.getStack().getDisplayName());
                        state = states.RUNE1;
                        waitTimer.schedule();

                        return;
                    }

                    for (Slot slot : runicPedestal.inventorySlots) {
                        if (slot == null || slot.getStack() == null || slot.getStack().getDisplayName() == null || slot.getStack().getDisplayName().trim().isEmpty()) {
                            continue;
                        }

                        if (slot.getStack().getDisplayName().toLowerCase().contains("rune")) {
                            if (ignoreSlots.contains(slot)) return;
                            if (slot.getStack().stackSize < 2 || slot.getStack().getDisplayName().toLowerCase().contains("iii")) {
                                ignoreSlots.add(slot);
                                Charmonium.sendMessage("Ignoring rune: " + slot.getStack().getDisplayName());
                                continue;
                            }

                            runeSlot = slot;
                            Charmonium.sendMessage("Rune found: " + runeSlot.getStack().getDisplayName());
                            break;
                        }
                    }

                    state = states.RUNE1;
                    waitTimer.schedule();
                    break;
                }
                case RUNE1: {
                    Charmonium.sendMessage("Placing rune: " + runeSlot.getStack().getDisplayName());
                    if (!waitTimer.hasPassed(400)) return;
                    if (!checkRune()) return;

                    mc.playerController.windowClick(runicPedestal.windowId, runeSlot.slotNumber, 0, 1, mc.thePlayer);

                    state = states.RUNE2;
                    waitTimer.schedule();
                    break;
                }
                case RUNE2: {
                    if (!waitTimer.hasPassed(400)) return;

                    mc.playerController.windowClick(runicPedestal.windowId, runeSlot.slotNumber, 0, 1, mc.thePlayer);

                    state = states.COMBINE;
                    waitTimer.schedule();
                    break;
                }
                case COMBINE: {
                    if (!waitTimer.hasPassed(500)) return;

                    if (runicPedestal.getSlot(19) != null && runicPedestal.getSlot(19).getStack() != null &&
                            runicPedestal.getSlot(25) != null && runicPedestal.getSlot(25).getStack() != null) {
                        mc.playerController.windowClick(runicPedestal.windowId, 13, 0, 0, mc.thePlayer);
                        waitTimer.reset();
                        state = states.WAIT;
                        Charmonium.sendMessage("Both runes in, combining...");
                        waitTimer.schedule();
                        return;
                    }
                    break;
                }
                case WAIT: {
                    if (waitTimer.hasPassed(3000)) {
                        state = states.DONE;
                        Charmonium.sendMessage("Rune combining complete.");
                    }
                    break;
                }
                case DONE: {
                    if (!waitTimer.hasPassed(300)) return;

                    mc.playerController.windowClick(runicPedestal.windowId, 31, 0, 1, mc.thePlayer);

                    state = states.NONE;
                    break;
                }
            }
        }
    }

    private boolean checkRune() {
        if (runeSlot == null || runeSlot.getStack() == null || runeSlot.getStack().stackSize < 2 || runeSlot.getStack().getDisplayName() == null || runeSlot.getStack().getDisplayName().trim().isEmpty()) {
            runeSlot = null;
            state = states.FIND;
            return false;
        }

        return true;
    }

    public enum states {
        NONE,
        FIND,
        RUNE1,
        RUNE2,
        COMBINE,
        WAIT,
        DONE
    }
}
