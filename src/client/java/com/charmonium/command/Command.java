package com.charmonium.command;

import com.charmonium.CharmoniumClient;
import net.minecraft.client.MinecraftClient;

import java.util.Objects;

public abstract class Command {
    protected final String name;
    protected final String description;
    protected final String syntax;

    protected static final MinecraftClient mc = CharmoniumClient.mc;

    public Command(String name, String description, String syntax) {
        this.name = Objects.requireNonNull(name);
        this.description = Objects.requireNonNull(description);
        this.syntax = Objects.requireNonNull(syntax);
    }

    public boolean hasSyntax() {
        return !syntax.isEmpty();
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getSyntax() {
        return syntax;
    }

    public abstract void runCommand(String[] parameters) throws InvalidSyntaxException;

    public abstract String[] getAutocorrect(String previousParameter);
}
