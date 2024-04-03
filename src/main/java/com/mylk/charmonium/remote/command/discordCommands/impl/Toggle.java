package com.mylk.charmonium.remote.command.discordCommands.impl;

import com.mylk.charmonium.remote.WebsocketHandler;
import com.mylk.charmonium.remote.command.discordCommands.DiscordCommand;
import com.mylk.charmonium.remote.command.discordCommands.Option;
import com.mylk.charmonium.remote.waiter.Waiter;
import com.mylk.charmonium.remote.waiter.WaiterHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.util.Objects;

public class Toggle extends DiscordCommand {
    public static final String name = "toggle";
    public static final String description = "Toggle the bot";

    public Toggle() {
        super(Toggle.name, Toggle.description);
        Option ign = new Option(OptionType.STRING, "ign", "The IGN of the player you want to toggle this account", false, true);
        addOptions(ign);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        WaiterHandler.register(new Waiter(
                5000,
                name,
                action -> {
                    String username = action.args.get("username").getAsString();
                    boolean toggled = action.args.get("toggled").getAsBoolean();
                    String uuid = action.args.get("uuid").getAsString();
                    boolean error = action.args.has("info");

                    EmbedBuilder embedBuilder = new EmbedBuilder();
                    embedBuilder.addField("Username", username, false);
                    if (error) {
                        embedBuilder.addField("Info", action.args.get("info").getAsString(), false);
                    }
                    embedBuilder.addField("Turned macro", (toggled && !error) ? "On" : "Off", false);
                    int random = (int) (Math.random() * 0xFFFFFF);
                    embedBuilder.setColor(random);
                    embedBuilder.setFooter("-> Charmonium Remote Control", "https://media.forgecdn.net/avatars/266/994/637234317818341856.png");
                    String avatar = "https://minotar.net/helm/" + username + "/600.png";
                    embedBuilder.setAuthor("Instance name -> " + username, avatar, avatar);

                    try {
                        event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
                    } catch (Exception e) {
                        event.getChannel().sendMessageEmbeds(embedBuilder.build()).queue();
                    }
                },
                timeoutAction -> event.getHook().sendMessage("Can't toggle the bot").queue(),
                event
        ));

        if (event.getOption("ign") != null) {
            String ign = Objects.requireNonNull(event.getOption("ign")).getAsString();
            if (WebsocketHandler.getInstance().getWebsocketServer().minecraftInstances.containsValue(ign)) {
                WebsocketHandler.getInstance().getWebsocketServer().minecraftInstances.forEach((webSocket, s) -> {
                    if (s.equals(ign)) {
                        sendMessage(webSocket);
                    }
                });
            } else {
                event.getHook().sendMessage("There isn't any instances connected with that IGN").queue();
            }
        } else {
            WebsocketHandler.getInstance().getWebsocketServer().minecraftInstances.forEach((webSocket, s) -> sendMessage(webSocket));
        }
    }
}
