package com.mylk.charmonium.failsafe.impl;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.mylk.charmonium.config.page.FailsafeNotificationsPage;
import com.mylk.charmonium.event.ReceivePacketEvent;
import com.mylk.charmonium.failsafe.Failsafe;
import com.mylk.charmonium.failsafe.FailsafeManager;
import com.mylk.charmonium.feature.impl.MovRecPlayer;
import com.mylk.charmonium.handler.MacroHandler;
import com.mylk.charmonium.macro.impl.GemstoneMacro;
import com.mylk.charmonium.macro.impl.SlayerMacro;
import com.mylk.charmonium.macro.impl.misc.Maddoxer;
import com.mylk.charmonium.macro.impl.misc.fuelFilling;
import com.mylk.charmonium.util.KeyBindUtils;
import com.mylk.charmonium.util.LogUtils;
import com.mylk.charmonium.util.PlayerUtils;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.network.play.server.S2FPacketSetSlot;

import java.util.concurrent.TimeUnit;

public class ItemChangeFailsafe extends Failsafe {
    private static ItemChangeFailsafe instance;
    public static ItemChangeFailsafe getInstance() {
        if (instance == null) {
            instance = new ItemChangeFailsafe();
        }
        return instance;
    }

    @Override
    public int getPriority() {
        return 3;
    }

    @Override
    public FailsafeManager.EmergencyType getType() {
        return FailsafeManager.EmergencyType.ITEM_CHANGE_CHECK;
    }

    @Override
    public boolean shouldSendNotification() {
        return FailsafeNotificationsPage.notifyOnItemChangeFailsafe;
    }

    @Override
    public boolean shouldPlaySound() {
        return FailsafeNotificationsPage.alertOnItemChangeFailsafe;
    }

    @Override
    public boolean shouldTagEveryone() {
        return FailsafeNotificationsPage.tagEveryoneOnItemChangeFailsafe;
    }

    @Override
    public boolean shouldAltTab() {
        return FailsafeNotificationsPage.autoAltTabOnItemChangeFailsafe;
    }

    @Override
    public void onReceivedPacketDetection(ReceivePacketEvent event) {
        if (MacroHandler.getInstance().isTeleporting()) return;

        if (!(event.packet instanceof S2FPacketSetSlot)) return;

        S2FPacketSetSlot packet = (S2FPacketSetSlot) event.packet;
        int slot = packet.func_149173_d();

//        if (slot >= 36 && slot < 45 && mc.thePlayer.inventory.currentItem + 36 == slot)
//            LogUtils.sendSuccess("[Failsafe] Failsafe triggered!");
        int farmingToolSlot = -1;
        try {
            farmingToolSlot = PlayerUtils.getFarmingTool(MacroHandler.getInstance().getCrop(), true, false);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        if (farmingToolSlot == -1) return;
        ItemStack farmingTool = mc.thePlayer.inventory.getStackInSlot(farmingToolSlot);
        if (slot == 36 + farmingToolSlot &&
                farmingTool != null &&
                !SlayerMacro.killedBoss &&
                !GemstoneMacro.stopChecks &&
                !fuelFilling.isRefueling() &&
                !GemstoneMacro.usingSpeed &&
                !(farmingTool.getItem() instanceof ItemAxe) &&
                !(farmingTool.getItem() instanceof ItemFishingRod) &&
                !(farmingTool.getItem() instanceof ItemPickaxe) &&
                !(farmingTool.getItem() instanceof ItemAxe) &&
                !(farmingTool.getItem() instanceof ItemSword) &&
                !(farmingTool.getItem() instanceof ItemBow) &&
                !(farmingTool.getItem() == Items.prismarine_shard) &&
                !(farmingTool.getItem() instanceof ItemSkull)) {
            LogUtils.sendDebug("[Failsafe] No farming tool in hand! Slot: " + slot);
            FailsafeManager.getInstance().possibleDetection(this);
        }
    }

    @Override
    public void duringFailsafeTrigger() {
        switch (itemChangeState) {
            case NONE:
                FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                itemChangeState = ItemChangeState.WAIT_BEFORE_START;
                break;
            case WAIT_BEFORE_START:
                MacroHandler.getInstance().pauseMacro();
                KeyBindUtils.stopMovement();
                itemChangeState = ItemChangeState.LOOK_AROUND;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 500);
                break;
            case LOOK_AROUND:
                MovRecPlayer.getInstance().playRandomRecording("ITEM_CHANGE_");
                itemChangeState = ItemChangeState.SWAP_BACK_ITEM;
                break;
            case SWAP_BACK_ITEM:
                if (MovRecPlayer.getInstance().isRunning()) return;
                itemChangeState = ItemChangeState.END;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                break;
            case END:
                PlayerUtils.getTool();
                endOfFailsafeTrigger();
                break;
        }
    }

    @Override
    public void endOfFailsafeTrigger() {
        FailsafeManager.getInstance().stopFailsafes();
        Multithreading.schedule(() -> {
            LogUtils.sendDebug("[Failsafe] Finished item change failsafe. Farming...");
            MacroHandler.getInstance().resumeMacro();
        }, 500, TimeUnit.MILLISECONDS);
    }

    @Override
    public void resetStates() {
        itemChangeState = ItemChangeState.NONE;
    }

    private ItemChangeState itemChangeState = ItemChangeState.NONE;
    enum ItemChangeState {
        NONE,
        WAIT_BEFORE_START,
        LOOK_AROUND,
        SWAP_BACK_ITEM,
        END
    }
}
