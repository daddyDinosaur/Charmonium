package com.mylk.charmonium.macro.impl.misc;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.mylk.charmonium.Charmonium;
import com.mylk.charmonium.config.Config;
import com.mylk.charmonium.feature.impl.Scheduler;
import com.mylk.charmonium.handler.MacroHandler;
import com.mylk.charmonium.macro.impl.SlayerMacro;
import com.mylk.charmonium.util.InventoryUtils;
import com.mylk.charmonium.util.KeyBindUtils;
import com.mylk.charmonium.util.LogUtils;
import com.mylk.charmonium.util.PlayerUtils;
import com.mylk.charmonium.util.helper.Clock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.concurrent.TimeUnit;

public class Maddoxer {
    public static MaddoxS callMaddox = MaddoxS.NONE;
    private static final Clock maddoxDelay = new Clock();
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static boolean hasStarted = false;
    private static Maddoxer instance;

    public static Maddoxer getInstance() {
        if (instance == null) {
            instance = new Maddoxer();
        }
        return instance;
    }

    @SubscribeEvent
    public void onTickShouldEnable(TickEvent.ClientTickEvent event) {
        if (!SlayerMacro.killedBoss || !Config.autoMaddox) return;
        if (hasStarted) {
            callMaddox = MaddoxS.FINISH;
        }

        if (maddoxDelay.isScheduled() && !maddoxDelay.passed()) return;

        switch (callMaddox) {
            case NONE:
                LogUtils.sendWarning("[Auto Maddox] Enabled!");
                callMaddox = MaddoxS.FIND;
                maddoxDelay.schedule(1_500);
                break;
            case FIND:
                int abiphone = InventoryUtils.getSlotIdOfItemInHotbar("Abiphone");

                if (abiphone != -1) {
                    Charmonium.sendMessage("Calling Maddox W/ Abiphone");

                    if (mc.currentScreen != null) {
                        mc.thePlayer.closeScreen();
                        maddoxDelay.schedule(getRandomDelay());
                    }

                    mc.thePlayer.inventory.currentItem = abiphone;
                    callMaddox = MaddoxS.OPEN;
                    maddoxDelay.schedule(getRandomDelay());
                } else {
                    Charmonium.sendMessage("No Abiphone found");
                    MacroHandler.getInstance().disableMacro();
                    SlayerMacro.killedBoss = false;
                }

                break;
            case OPEN:
                KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindForward, false);
                KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindJump, false);
                if (mc.currentScreen != null) {
                    mc.thePlayer.closeScreen();
                    maddoxDelay.schedule(getRandomDelay());
                }

                KeyBindUtils.rightClick();
                callMaddox = MaddoxS.CALL;
                maddoxDelay.schedule(getRandomDelay());
                break;
            case CALL:
                if (mc.currentScreen == null) break;
                if (InventoryUtils.getInventoryName() == null || !InventoryUtils.getInventoryName().contains("Abiphone"))
                    break;

                int slotOfMaddox = InventoryUtils.getSlotIdOfItemInContainer("Maddox the Slayer");
                if (slotOfMaddox == -1) {
                    LogUtils.sendError("Something went wrong while trying to get the slot of Maddox! Restarting...");
                    return;
                }

                InventoryUtils.clickContainerSlot(slotOfMaddox, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                callMaddox = MaddoxS.RESTART;
                maddoxDelay.schedule(4_000);
                break;
            case RESTART:
                if (mc.currentScreen == null) break;
                if (InventoryUtils.getInventoryName() == null || !InventoryUtils.getInventoryName().contains("Slayer"))
                    break;

                int slotOfSComplete = InventoryUtils.getSlotIdOfItemInContainer("Slayer Quest Complete!");
                if (slotOfSComplete == -1) {
                    LogUtils.sendError("Something went wrong while trying to get the slot of Complete Quest! Restarting...");
                    callMaddox = MaddoxS.BOSS;
                    return;
                }

                InventoryUtils.clickContainerSlot(slotOfSComplete, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                callMaddox = MaddoxS.BOSS;
                maddoxDelay.schedule(getRandomDelay());
                break;
            case BOSS:
                if (mc.currentScreen == null) break;
                if (InventoryUtils.getInventoryName() == null || !InventoryUtils.getInventoryName().contains("Slayer"))
                    break;

                InventoryUtils.clickContainerSlot(10 + Config.SlayerType, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                callMaddox = MaddoxS.TIER;
                maddoxDelay.schedule(getRandomDelay());
                break;
            case TIER:
                if (mc.currentScreen == null) break;
                if (InventoryUtils.getInventoryName() == null ||
                    !(InventoryUtils.getInventoryName().contains("Revenant Horror") ||
                        InventoryUtils.getInventoryName().contains("Tarantula Broodfather") ||
                        InventoryUtils.getInventoryName().contains("Sven Packmaster") ||
                        InventoryUtils.getInventoryName().contains("Voidgloom Seraph")))
                    break;

                InventoryUtils.clickContainerSlot(11 + Config.SlayerTier, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                callMaddox = MaddoxS.CONFIRM;
                maddoxDelay.schedule(getRandomDelay());
                break;
            case CONFIRM:
                if (mc.currentScreen == null) break;
                if (InventoryUtils.getInventoryName() == null || !InventoryUtils.getInventoryName().contains("Confirm"))
                    break;

                InventoryUtils.clickContainerSlot(11, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                maddoxDelay.schedule(getRandomDelay());
                callMaddox = MaddoxS.FINISH;
                break;
            case FINISH:
                LogUtils.sendWarning("[Auto Maddox] Finished!");
                hasStarted = true;
                PlayerUtils.getTool();
                maddoxDelay.schedule(getRandomDelay());
                if (Config.enableScheduler)
                    Scheduler.getInstance().resume();
                MacroHandler.getInstance().resumeMacro();
                SlayerMacro.killedBoss = false;
                SlayerMacro.currentState = SlayerMacro.State.WALKING;
                break;
        }
    }

    @SubscribeEvent
    public void onChatReceive(ClientChatReceivedEvent event) {
        if (!SlayerMacro.killedBoss || !Config.autoMaddox) return;
        String message = StringUtils.stripControlCodes(event.message.getUnformattedText());
        if (message.contains("You can't use this menu while in combat!")) {
            maddoxDelay.schedule(2_500);
            MacroHandler.walker.stop();
            KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindForward, true);
            KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindJump, true);
            Multithreading.schedule(() -> callMaddox = MaddoxS.OPEN, 3, TimeUnit.SECONDS);
            event.setCanceled(true);
        }
    }

    private static int getRandomDelay() {
        return (int) (Math.random() * 1_000 + 500);
    }

    public static String getInventoryName(GuiScreen gui) {
        if (gui instanceof GuiChest) {
            return ((ContainerChest) ((GuiChest) gui).inventorySlots).getLowerChestInventory().getDisplayName().getUnformattedText();
        } else return "";
    }

    public static String getOpenInventoryName() {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return null;
        } else return mc.thePlayer.openContainer.inventorySlots.get(0).inventory.getName();
    }
    
    public enum MaddoxS {
        NONE,
        FIND,
        OPEN,
        CALL,
        BOSS,
        TIER,
        CONFIRM,
        FINISH,
        RESTART
    }
}
