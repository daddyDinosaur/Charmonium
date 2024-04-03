package com.mylk.charmonium.feature.impl;

import com.mylk.charmonium.Charmonium;
import com.mylk.charmonium.config.Config;
import com.mylk.charmonium.event.MillisecondEvent;
import com.mylk.charmonium.util.InventoryUtils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import java.awt.*;
import java.util.Arrays;
import java.util.Objects;

import static com.mylk.charmonium.Charmonium.mc;

public class GemstoneSackCompactor {
    private final boolean[] gemstoneToggles = {
            false, false, false, false, false, false, false
    };
    private final String[] gemstoneTypes = {
            "Jade", "Amber", "Topaz", "Sapphire", "Amethyst", "Jasper", "Ruby"
    };
    private long timestamp = 0;

    private static GemstoneSackCompactor instance;
    public static GemstoneSackCompactor getInstance() {
        if (instance == null) {
            instance = new GemstoneSackCompactor();
        }
        return instance;
    }

    @SubscribeEvent
    public void onGuiRender(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!Config.gemstoneSackCompactor) return;
        GuiScreen screen = event.gui;
        if (screen instanceof GuiChest) {
            GuiChest guiChest = (GuiChest) screen;
            String chestName = guiChest.inventorySlots.inventorySlots.get(0).inventory.getName();
            if (chestName.equals("Gemstones Sack")) {
                mc.fontRendererObj.drawStringWithShadow(
                        Arrays.toString(gemstoneToggles),
                        100,
                        100,
                        Color.GREEN.getRGB()
                );
            }
        }
    }

    @SubscribeEvent
    public void onGuiInitialized(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!Config.gemstoneSackCompactor) return;
        GuiScreen screen = event.gui;
        if (screen instanceof GuiChest) {
            GuiChest guiChest = (GuiChest) screen;
            String chestName = guiChest.inventorySlots.inventorySlots.get(0).inventory.getName();
            if (chestName.equals("Gemstones Sack")) {
                ScaledResolution scaledResolution = new ScaledResolution(mc);
                for (int i = 0; i < gemstoneTypes.length; i++) {
                    event.buttonList.add(new GuiButton(
                            i + 110,
                            (scaledResolution.getScaledWidth() - 50) / 2 - 180 + 60 * i,
                            (scaledResolution.getScaledHeight() - 20) / 2 - 105,
                            50,
                            20,
                            "§" + (gemstoneToggles[i] ? "a" : "c") + gemstoneTypes[i]
                    ));
                }
            }
        }
    }

    @SubscribeEvent
    public void onMillisecond(MillisecondEvent event) {
        if (mc.thePlayer == null) return;
        if (!Config.gemstoneSackCompactor) return;
        if (System.currentTimeMillis() - timestamp > Config.gemstoneSackCompactorClickDelay) {
            timestamp = System.currentTimeMillis();
            String chestName = getOpenInventoryName();
            if ("Gemstones Sack".equals(chestName)) {
                for (int i = 0; i < gemstoneToggles.length; i++) {
                    if (gemstoneToggles[i]) {
                        ItemStack itemStack = mc.thePlayer.openContainer.inventorySlots.get(i + 10).getStack();
                        if (itemStack != null) {
                            String stored = removeFormatting(Objects.requireNonNull(InventoryUtils.getItemLore(itemStack, 7))).replaceAll("[A-z,: ]", "");
                            if (!stored.startsWith("0")) {
                                clickSlot(i + 10);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onActionPerformedGui(final GuiScreenEvent.ActionPerformedEvent.Post event) {
        if (!Config.gemstoneSackCompactor) return;
        GuiScreen screen = event.gui;
        if (screen instanceof GuiChest) {
            GuiChest guiChest = (GuiChest) screen;
            String chestName = guiChest.inventorySlots.inventorySlots.get(0).inventory.getName();
            if (chestName.equals("Gemstones Sack")) {
                int buttonId = event.button.id;
                gemstoneToggles[buttonId - 110] = !gemstoneToggles[buttonId - 110];
                event.button.displayString = "§" + (gemstoneToggles[buttonId - 110] ? "a" : "c") + gemstoneTypes[buttonId - 110];
            }
        }
    }

    public static String getOpenInventoryName() {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return null;
        } else return mc.thePlayer.openContainer.inventorySlots.get(0).inventory.getName();
    }

    public static String removeFormatting(String input) {
        return input.replaceAll("§[0-9a-fk-or]", "");
    }

    private void clickSlot(int slot) {
        mc.playerController.windowClick(
                mc.thePlayer.openContainer.windowId,
                slot,
                0,
                0,
                mc.thePlayer
        );
    }
}
