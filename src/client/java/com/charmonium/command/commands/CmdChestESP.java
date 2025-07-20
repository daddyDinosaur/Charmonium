package com.charmonium.command.commands;

import com.charmonium.Charmonium;
import com.charmonium.CharmoniumClient;
import com.charmonium.command.Command;
import com.charmonium.command.InvalidSyntaxException;
import com.charmonium.module.modules.render.ChestESP;

public class CmdChestESP extends Command {

    public CmdChestESP() {
        super("chestesp", "Allows the player to see chest locations through ESP", "[toggle] [value]");
    }

    @Override
    public void runCommand(String[] parameters) throws InvalidSyntaxException {
        if (parameters.length != 2)
            throw new InvalidSyntaxException(this);

        ChestESP module = Charmonium.getInstance().moduleManager.chestesp;

        switch (parameters[0]) {
            case "toggle":
                String state = parameters[1].toLowerCase();
                if (state.equals("on")) {
                    module.state.setValue(true);
                    CharmoniumClient.sendMessage("ChestESP toggled ON");
                } else if (state.equals("off")) {
                    module.state.setValue(false);
                    CharmoniumClient.sendMessage("ChestESP toggled OFF");
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
