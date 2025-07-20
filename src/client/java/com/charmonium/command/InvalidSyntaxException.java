package com.charmonium.command;

import com.charmonium.CharmoniumClient;
import net.minecraft.util.Formatting;
import com.charmonium.managers.CommandManager;

import java.io.Serial;

public class InvalidSyntaxException extends CommandException {
    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidSyntaxException(Command cmd) {
        super(cmd);
    }

    @Override
    public void PrintToChat() {
        CharmoniumClient.sendMessage("Invalid syntax! Correct usage: " +
                Formatting.LIGHT_PURPLE + "/char " + cmd.getName() + " " + cmd.getSyntax() +
                Formatting.RESET);
    }
}