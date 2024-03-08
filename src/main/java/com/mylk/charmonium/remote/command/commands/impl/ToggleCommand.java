package com.mylk.charmonium.remote.command.commands.impl;

import com.google.gson.JsonObject;
import com.mylk.charmonium.failsafe.FailsafeManager;
import com.mylk.charmonium.handler.GameStateHandler;
import com.mylk.charmonium.handler.MacroHandler;
import com.mylk.charmonium.remote.command.commands.ClientCommand;
import com.mylk.charmonium.remote.command.commands.Command;
import com.mylk.charmonium.remote.struct.RemoteMessage;

@Command(label = "toggle")
public class ToggleCommand extends ClientCommand {

    @Override
    public void execute(RemoteMessage message) {
        JsonObject data = new JsonObject();
        data.addProperty("username", mc.getSession().getUsername());
        data.addProperty("uuid", mc.getSession().getPlayerID());
        data.addProperty("toggled", !MacroHandler.getInstance().isMacroToggled());

        if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.LOBBY && !MacroHandler.getInstance().isMacroToggled()) {
            data.addProperty("info", "You are in the lobby! Teleporting");
            mc.thePlayer.sendChatMessage("/skyblock");
            new Thread(() -> {
                try {
                    Thread.sleep(2500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!GameStateHandler.getInstance().atProperIsland()) {
                    mc.thePlayer.sendChatMessage(GameStateHandler.getInstance().islandWarp);
                }

                try {
                    Thread.sleep(2500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!GameStateHandler.getInstance().atProperIsland()) {
                    data.addProperty("info", "Can't teleport to the garden!");
                } else {
                    MacroHandler.getInstance().toggleMacro();
                    data.addProperty("info", "You are in garden! Macroing");
                }
                RemoteMessage response = new RemoteMessage(label, data);
                send(response);
            }).start();
            return;
        } else if (!GameStateHandler.getInstance().atProperIsland() && !MacroHandler.getInstance().isMacroToggled()) {
            data.addProperty("info", "You are outside the garden! Teleporting");
            mc.thePlayer.sendChatMessage(GameStateHandler.getInstance().islandWarp);
            new Thread(() -> {
                try {
                    Thread.sleep(2500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!GameStateHandler.getInstance().atProperIsland()) {
                    data.addProperty("info", "Can't teleport to the garden!");
                } else {
                    MacroHandler.getInstance().toggleMacro();
                    data.addProperty("info", "You are in garden! Macroing");
                }
                RemoteMessage response = new RemoteMessage(label, data);
                send(response);
            }).start();
            return;
        } else {
            if (FailsafeManager.getInstance().isHadEmergency()) {
                FailsafeManager.getInstance().setHadEmergency(false);
                FailsafeManager.getInstance().getRestartMacroAfterFailsafeDelay().reset();
            }
            MacroHandler.getInstance().toggleMacro();
        }


        RemoteMessage response = new RemoteMessage(label, data);
        send(response);
    }
}