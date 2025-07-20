package com.charmonium.command.commands;

import com.charmonium.Charmonium;
import com.charmonium.CharmoniumClient;
import com.charmonium.command.Command;
import com.charmonium.command.InvalidSyntaxException;
import com.charmonium.managers.CommandManager;
import net.minecraft.util.Formatting;

import java.util.Map;

public class CmdHelp extends Command {
    private static final int COMMANDS_PER_PAGE = 8;

    public CmdHelp() {
        super("help", "Displays command list and syntax", "[page]");
    }

    @Override
    public void runCommand(String[] parameters) throws InvalidSyntaxException {
        CommandManager manager = Charmonium.getInstance().commandManager;
        Map<String, Command> commands = manager.getCommands();

        int page = parsePageNumber(parameters);
        int totalPages = (int) Math.ceil(commands.size() / (double) COMMANDS_PER_PAGE);

        if (page < 1 || page > totalPages) {
            CharmoniumClient.sendMessage(Formatting.RED + "Invalid page number. " +
                    "Use /char help [page] (1-" + totalPages + ")");
        }

        displayHeader(page, totalPages);
        displayCommands(commands, page);
    }

    private int parsePageNumber(String[] parameters) throws InvalidSyntaxException {
        if (parameters.length == 0) return 1;

        try {
            int page = Integer.parseInt(parameters[0]);
            if (page < 1) throw new NumberFormatException();
            return page;
        } catch (NumberFormatException e) {
            throw new InvalidSyntaxException(this);
        }
    }

    private void displayHeader(int page, int totalPages) {
        String header = Formatting.GOLD + "Command List " + Formatting.DARK_GRAY +
                "(" + page + "/" + totalPages + ")";
        CharmoniumClient.sendMessage(header);
    }

    private void displayCommands(Map<String, Command> commands, int page) {
        commands.values().stream()
                .skip((page - 1) * COMMANDS_PER_PAGE)
                .limit(COMMANDS_PER_PAGE)
                .forEach(cmd -> {
                    String entry = Formatting.LIGHT_PURPLE + "/char " + cmd.getName() + " " +
                            Formatting.GRAY + cmd.getSyntax() + "\n" +
                            Formatting.WHITE + cmd.getDescription();
                    CharmoniumClient.sendMessage(entry);
                });
    }

    @Override
    public String[] getAutocorrect(String previousParameter) {
        return new String[] { "1" }; // Suggest first page by default
    }
}
