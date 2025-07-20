package com.charmonium.managers;

import com.charmonium.CharmoniumClient;
import com.charmonium.command.Command;
import com.charmonium.command.InvalidSyntaxException;
import com.charmonium.command.commands.*;
import com.charmonium.settings.types.StringSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CommandManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandManager.class);
    private final Map<String, Command> commands = new HashMap<>();
    private final Deque<String> commandHistory = new ArrayDeque<>();
    private static final int HISTORY_LIMIT = 50;

    public static StringSetting PREFIX = StringSetting.builder().id("char_prefix").displayName("Prefix")
            .defaultValue("/char").build();

    public final CmdHelp help = new CmdHelp();
    public final CmdFullbright fullbright = new CmdFullbright();
    public final CmdChestESP chestESP = new CmdChestESP();
    public final CmdPathfind pathFind = new CmdPathfind();
    public final CmdWalking cmdwalk = new CmdWalking();

    public CommandManager() {
        SettingManager.registerSetting(PREFIX);
        registerBuiltinCommands();
    }

    public Map<String, Command> getCommands() {
        return Collections.unmodifiableMap(commands);
    }

    public Command getCommandBySyntax(String syntax) {
        return commands.get(syntax);
    }

    private void registerBuiltinCommands() {
        Arrays.stream(getClass().getDeclaredFields())
                .filter(field -> Command.class.isAssignableFrom(field.getType()))
                .forEach(field -> {
                    try {
                        Command cmd = (Command) field.get(this);
                        commands.put(cmd.getName().toLowerCase(), cmd);
                    } catch (IllegalAccessException e) {
                        LOGGER.error("Command registration failed: {}", e.getMessage());
                    }
                });
    }

    public void executeCommand(String[] input) {
        if (input == null || input.length < 1 || !input[0].equalsIgnoreCase("char")) {
            CharmoniumClient.sendMessage("Use " + Formatting.LIGHT_PURPLE + "/char help");
            return;
        }

        String rawMessage = "/" + String.join(" ", input);
        addToHistory(rawMessage);

        try {
            if (input.length < 2) {
                CharmoniumClient.sendMessage("Usage: " + Formatting.LIGHT_PURPLE + "/char <command>");
                return;
            }

            Command command = commands.get(input[1].toLowerCase());
            if (command == null) {
                sendUnknownCommand(input[1]);
            } else {
                String[] parameters = Arrays.copyOfRange(input, 2, input.length);
                command.runCommand(parameters);
            }
        } catch (InvalidSyntaxException e) {
            e.printToChat();
        }
    }

    private void addToHistory(String message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.inGameHud != null && mc.inGameHud.getChatHud() != null) {
            mc.inGameHud.getChatHud().addToMessageHistory(message);
        }
        if (commandHistory.size() >= HISTORY_LIMIT) {
            commandHistory.removeFirst();
        }
        commandHistory.addLast(message);
    }

    private void sendUnknownCommand(String commandName) {
        String message = "Unknown command: " + Formatting.LIGHT_PURPLE + commandName + "\n" +
                Formatting.RESET + "Use " + Formatting.LIGHT_PURPLE + "/char help" +
                Formatting.RESET + " for command list";
        CharmoniumClient.sendMessage(message);
    }
}
