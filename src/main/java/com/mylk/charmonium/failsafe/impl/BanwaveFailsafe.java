package com.mylk.charmonium.failsafe.impl;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.mylk.charmonium.config.Config;
import com.mylk.charmonium.config.page.FailsafeNotificationsPage;
import com.mylk.charmonium.event.ReceivePacketEvent;
import com.mylk.charmonium.failsafe.Failsafe;
import com.mylk.charmonium.failsafe.FailsafeManager;
import com.mylk.charmonium.feature.impl.BanInfo;
import com.mylk.charmonium.handler.GameStateHandler;
import com.mylk.charmonium.handler.MacroHandler;
import com.mylk.charmonium.util.LogUtils;
import com.mylk.charmonium.util.helper.AudioManager;
import net.minecraft.util.ChatComponentText;

import java.util.concurrent.TimeUnit;

public class BanwaveFailsafe extends Failsafe {
    private static BanwaveFailsafe instance;
    public static BanwaveFailsafe getInstance() {
        if (instance == null) {
            instance = new BanwaveFailsafe();
        }
        return instance;
    }

    @Override
    public int getPriority() {
        return 6;
    }

    @Override
    public FailsafeManager.EmergencyType getType() {
        return FailsafeManager.EmergencyType.BANWAVE;
    }

    @Override
    public boolean shouldSendNotification() {
        return FailsafeNotificationsPage.notifyOnBanwaveFailsafe;
    }

    @Override
    public boolean shouldPlaySound() {
        return FailsafeNotificationsPage.alertOnBanwaveFailsafe;
    }

    @Override
    public boolean shouldTagEveryone() {
        return FailsafeNotificationsPage.tagEveryoneOnBanwaveFailsafe;
    }

    @Override
    public boolean shouldAltTab() {
        return FailsafeNotificationsPage.autoAltTabOnBanwaveFailsafe;
    }

    @Override
    public void duringFailsafeTrigger() {
        if (Config.banwaveAction) {
            // pause
            if (!MacroHandler.getInstance().isCurrentMacroPaused()) {
                LogUtils.sendFailsafeMessage("[Failsafe] Paused the macro because of banwave!", false);
                MacroHandler.getInstance().pauseMacro();
            } else {
                if (!BanInfo.getInstance().isBanwave()) {
                    endOfFailsafeTrigger();
                }
            }
        } else {
            // leave
            if (!MacroHandler.getInstance().isCurrentMacroPaused()) {
                LogUtils.sendFailsafeMessage("[Failsafe] Leaving because of banwave!", false);
                MacroHandler.getInstance().pauseMacro();
                Multithreading.schedule(() -> {
                    try {
                        mc.getNetHandler().getNetworkManager().closeChannel(new ChatComponentText("Will reconnect after end of banwave!"));
                        AudioManager.getInstance().resetSound();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, 500, TimeUnit.MILLISECONDS);
            }
        }
    }

    @Override
    public void endOfFailsafeTrigger() {
        LogUtils.sendFailsafeMessage("[Failsafe] Resuming the macro because banwave is over!", false);
        FailsafeManager.getInstance().stopFailsafes();
        FailsafeManager.getInstance().setHadEmergency(false);
        MacroHandler.getInstance().resumeMacro();
    }

    @Override
    public void onReceivedPacketDetection(ReceivePacketEvent event) {
        if (!BanInfo.getInstance().isBanwave()) return;
        if (!Config.banwaveCheckerEnabled) return;
        if (!Config.enableLeavePauseOnBanwave) return;
        FailsafeManager.getInstance().possibleDetection(this);
    }
}
