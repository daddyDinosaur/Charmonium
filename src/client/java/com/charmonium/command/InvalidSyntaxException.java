package com.charmonium.command;

import com.charmonium.CharmoniumClient;
import net.minecraft.util.Formatting;

import java.io.Serial;

public class InvalidSyntaxException extends CommandException {
    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidSyntaxException(Command command) {
        super(command);
    }

    @Override
    public void printToChat() {
        CharmoniumClient.sendMessage("Invalid syntax! Correct usage: " +
                Formatting.LIGHT_PURPLE + "/char " + getCommand().getName() + " " + getCommand().getSyntax() +
                Formatting.RESET);
    }
}
