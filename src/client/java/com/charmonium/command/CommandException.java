package com.charmonium.command;

import java.io.Serial;

public abstract class CommandException extends Exception {
    @Serial
    private static final long serialVersionUID = 1L;
    private final Command command;

    public CommandException(Command command) {
        this.command = command;
    }

    public Command getCommand() {
        return this.command;
    }

    public abstract void printToChat();
}
