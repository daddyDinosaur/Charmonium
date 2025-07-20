package com.charmonium.command.commands;

import com.charmonium.Charmonium;
import com.charmonium.CharmoniumClient;
import com.charmonium.command.Command;
import com.charmonium.command.InvalidSyntaxException;
import com.charmonium.module.modules.render.Fullbright;

public class CmdFullbright extends Command {
    public CmdFullbright() {
        super("fullbright", "Brightens up the world!", "[toggle] [value]");
    }

    @Override
    public void runCommand(String[] parameters) throws InvalidSyntaxException {
        if (parameters.length != 2)
            throw new InvalidSyntaxException(this);

        Fullbright module = Charmonium.getInstance().moduleManager.fullbright;

        switch (parameters[0]) {
            case "toggle":
                String state = parameters[1].toLowerCase();
                if (state.equals("on")) {
                    module.state.setValue(true);
                    CharmoniumClient.sendMessage("Fullbright toggled ON");
                } else if (state.equals("off")) {
                    module.state.setValue(false);
                    CharmoniumClient.sendMessage("Fullbright toggled OFF");
                } else {
                    CharmoniumClient.sendMessage("Invalid value. [ON/OFF]");
                }
                break;
            default:
                throw new InvalidSyntaxException(this);
        }
    }

    @Override
    public String[] getAutocorrect(String previousParameter) {
        switch (previousParameter) {
            case "toggle":
                return new String[] { "on", "off" };
            default:
                return new String[] { "toggle" };
        }
    }
}
