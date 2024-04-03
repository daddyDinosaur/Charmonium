package com.mylk.charmonium.feature.impl;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.mylk.charmonium.config.Config;
import com.mylk.charmonium.event.ClickEvent;
import com.mylk.charmonium.handler.GameStateHandler;
import com.mylk.charmonium.util.InventoryUtils;
import com.mylk.charmonium.util.KeyBindUtils;
import com.mylk.charmonium.util.LogUtils;
import com.mylk.charmonium.util.helper.Clock;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.concurrent.TimeUnit;

public class AutoBoom {
    private final Minecraft mc = Minecraft.getMinecraft();
    private static AutoBoom instance;

    public static AutoBoom getInstance() {
        if (instance == null) {
            instance = new AutoBoom();
        }
        return instance;
    }

    private final Clock delay = new Clock();

    public boolean isRunning() {
        return Config.autoBoomTNT;
    }

    private int previousSlot = -1;
    private boolean shouldDetonate = false;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isRunning()) return;
        if (event.phase == TickEvent.Phase.START) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!delay.passed()) return;
        if (GameStateHandler.getInstance().getLocation() != GameStateHandler.Location.DUNGEON) return;
        ItemStack currentStack = mc.thePlayer.getHeldItem();
        if (shouldDetonate) {
            if (currentStack == null || !currentStack.getItem().equals(Item.getItemFromBlock(Blocks.tnt))) {
                previousSlot = -1;
                shouldDetonate = false;
            }
            return;
        }
        MovingObjectPosition objectMouseOver = mc.objectMouseOver;

        if (currentStack != null && currentStack.getItem().equals(Item.getItemFromBlock(Blocks.tnt))) return;
        if (objectMouseOver == null || objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;

        Block block = mc.theWorld.getBlockState(objectMouseOver.getBlockPos()).getBlock();
        if (!block.equals(Blocks.stonebrick)) return;

        boolean isCracked = mc.theWorld.getBlockState(objectMouseOver.getBlockPos()).getBlock().getMetaFromState(mc.theWorld.getBlockState(objectMouseOver.getBlockPos())) == 2;
        if (!isCracked) return;

        int count = 0;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Block blockAround = mc.theWorld.getBlockState(objectMouseOver.getBlockPos().add(x, y, z)).getBlock();
                    if (blockAround.equals(Blocks.stonebrick)) {
                        boolean isCrackedAround = mc.theWorld.getBlockState(objectMouseOver.getBlockPos().add(x, y, z)).getBlock().getMetaFromState(mc.theWorld.getBlockState(objectMouseOver.getBlockPos().add(x, y, z))) == 2;
                        if (isCrackedAround) {
                            count++;
                        }
                        if (count >= 3) {
                            break;
                        }
                    }
                }
            }
        }
        if (count < 3) return;

        if (previousSlot == -1) {
            previousSlot = mc.thePlayer.inventory.currentItem;
            int superBoomSlot = InventoryUtils.getSlotIdOfItemInHotbar("TNT");
            if (superBoomSlot != -1) {
                mc.thePlayer.inventory.currentItem = superBoomSlot;
            } else {
                LogUtils.sendError("[Auto Superboom TNT] No superboom TNT found in hotbar!");
                Config.autoBoomTNT = false;
                previousSlot = -1;
            }
            shouldDetonate = true;
            if (Config.autoBoomTNTAutoPlacement) {
                Multithreading.schedule(() -> {
                    KeyBindUtils.rightClick();
                    Multithreading.schedule(() -> {
                        if (previousSlot != -1) {
                            mc.thePlayer.inventory.currentItem = previousSlot;
                            previousSlot = -1;
                            delay.schedule(2_000);
                        }
                    }, (long) (150 + Math.random() * 150), TimeUnit.MILLISECONDS);
                }, (long) (150 + Math.random() * 150), TimeUnit.MILLISECONDS);
            }
        }
    }

    @SubscribeEvent
    public void onRightClick(ClickEvent.Right event) {
        if (!isRunning()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (Config.autoBoomTNTAutoPlacement) return;
        if (GameStateHandler.getInstance().getLocation() != GameStateHandler.Location.DUNGEON) return;
        if (!shouldDetonate) return;
        ItemStack currentStack = mc.thePlayer.getHeldItem();
        if (currentStack != null && currentStack.getItem().equals(Item.getItemFromBlock(Blocks.tnt))) {
            Multithreading.schedule(() -> {
                if (previousSlot != -1) {
                    mc.thePlayer.inventory.currentItem = previousSlot;
                    previousSlot = -1;
                    delay.schedule(2_000);
                }
            }, (long) (150 + Math.random() * 150), TimeUnit.MILLISECONDS);
        }
    }
}
