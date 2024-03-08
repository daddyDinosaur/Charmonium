package com.mylk.charmonium.remote.command.discordCommands.impl;

import com.mylk.charmonium.remote.DiscordBotHandler;
import com.mylk.charmonium.remote.command.discordCommands.DiscordCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class Help extends DiscordCommand {
    public static final String name = "help";
    public static final String description = "Get information about commands";

    public Help() {
        super(Help.name, Help.description);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Charmonium Remote Control");
        builder.setDescription("Commands list");
        for (DiscordCommand command : DiscordBotHandler.getInstance().getCommands()) {
            builder.addField(command.name, command.description, false);
        }
        int random = (int) (Math.random() * 0xFFFFFF);
        builder.setColor(random);
        builder.setFooter("-> Charmonium Remote Control", "https://media.forgecdn.net/avatars/266/994/637234317818341856.png");
        MessageEmbed embed = builder.build();
        try {
            event.getHook().sendMessageEmbeds(embed).queue();
        } catch (Exception e) {
            event.getChannel().sendMessageEmbeds(embed).queue();
        }
    }
}
