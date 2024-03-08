package com.mylk.charmonium.remote.command.commands.impl;

import com.google.gson.JsonObject;
import com.mylk.charmonium.handler.MacroHandler;
import com.mylk.charmonium.remote.command.commands.ClientCommand;
import com.mylk.charmonium.remote.command.commands.Command;
import com.mylk.charmonium.remote.struct.RemoteMessage;
import com.mylk.charmonium.util.LogUtils;

@Command(label = "info")
public class InfoCommand extends ClientCommand {
    @Override
    public void execute(RemoteMessage message) {
        JsonObject data = new JsonObject();

        data.addProperty("username", mc.getSession().getUsername());
        data.addProperty("runtime", LogUtils.getRuntimeFormat());
        data.addProperty("cropType", String.valueOf(MacroHandler.getInstance().getCrop() == null ? "None" : MacroHandler.getInstance().getCrop()));
        data.addProperty("uuid", mc.getSession().getPlayerID());
        data.addProperty("image", getScreenshot());

        RemoteMessage response = new RemoteMessage(label, data);
        send(response);
    }
}
