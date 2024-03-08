package com.mylk.charmonium.failsafe.impl;

import cc.polyfrost.oneconfig.utils.Notifications;
import com.mylk.charmonium.config.page.FailsafeNotificationsPage;
import com.mylk.charmonium.failsafe.Failsafe;
import com.mylk.charmonium.failsafe.FailsafeManager;
import com.mylk.charmonium.feature.impl.AutoReconnect;
import com.mylk.charmonium.handler.MacroHandler;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

public class DisconnectFailsafe extends Failsafe {
    private static DisconnectFailsafe instance;
    public static DisconnectFailsafe getInstance() {
        if (instance == null) {
            instance = new DisconnectFailsafe();
        }
        return instance;
    }

    @Override
    public int getPriority() {
        return 1;
    }

    @Override
    public FailsafeManager.EmergencyType getType() {
        return FailsafeManager.EmergencyType.DISCONNECT;
    }

    @Override
    public boolean shouldSendNotification() {
        return FailsafeNotificationsPage.notifyOnDisconnectFailsafe;
    }

    @Override
    public boolean shouldPlaySound() {
        return FailsafeNotificationsPage.alertOnDisconnectFailsafe;
    }

    @Override
    public boolean shouldTagEveryone() {
        return FailsafeNotificationsPage.tagEveryoneOnDisconnectFailsafe;
    }

    @Override
    public boolean shouldAltTab() {
        return FailsafeNotificationsPage.autoAltTabOnDisconnectFailsafe;
    }

    @Override
    public void onDisconnectDetection(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        if (MacroHandler.getInstance().isTeleporting()) return;

        FailsafeManager.getInstance().possibleDetection(this);
    }

    @Override
    public void duringFailsafeTrigger() {
        if (!AutoReconnect.getInstance().isRunning() && AutoReconnect.getInstance().isToggled()) {
            System.out.println("[Reconnect] Disconnected from server! Trying to reconnect...");
            Notifications.INSTANCE.send("Charmonium", "Disconnected from server! Trying to reconnect...");
            AutoReconnect.getInstance().getReconnectDelay().schedule(5_000);
            AutoReconnect.getInstance().start();
        } else if (!AutoReconnect.getInstance().isRunning() && !AutoReconnect.getInstance().isToggled()) {
            System.out.println("[Reconnect] Disconnected from server! Stopping macro...");
            Notifications.INSTANCE.send("Charmonium", "Disconnected from server! Stopping macro...");
            MacroHandler.getInstance().disableMacro();
            FailsafeManager.getInstance().stopFailsafes();
        } else if (AutoReconnect.getInstance().isRunning()) {
            System.out.println("[Reconnect] Disconnected from server! Reconnect is already running!");
            Notifications.INSTANCE.send("Charmonium", "Disconnected from server! Reconnect is already running!");
            FailsafeManager.getInstance().stopFailsafes();
        }
    }

    @Override
    public void endOfFailsafeTrigger() {

    }
}
