package com.charmonium.command;

import java.io.Serial;

public abstract class CommandException extends Exception {
    @Serial
    private static final long serialVersionUID = 1L;
    protected Command cmd;

    public CommandException(Command cmd) {
        this.cmd = cmd;
    }

    public abstract void PrintToChat();
}