package com.mylk.charmonium.remote.command.discordCommands.impl;

import com.google.gson.JsonObject;
import com.mylk.charmonium.remote.WebsocketHandler;
import com.mylk.charmonium.remote.command.discordCommands.DiscordCommand;
import com.mylk.charmonium.remote.command.discordCommands.Option;
import com.mylk.charmonium.remote.waiter.Waiter;
import com.mylk.charmonium.remote.waiter.WaiterHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.util.Objects;

public class SetSpeed extends DiscordCommand {
    public static final String name = "setspeed";
    public static final String description = "Set speed of rancher boots";

    public SetSpeed() {
        super(name, description);
        addOptions(new Option(OptionType.INTEGER, "speed", "The speed of the rancher boots", true, false),
                new Option(OptionType.STRING, "ign", "The IGN of the instance", false, true)
        );
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        WaiterHandler.register(new Waiter(
                15000,
                name,
                action -> {
                    String username = action.args.get("username").getAsString();
                    String speed = action.args.get("speed").getAsString();
                    String uuid = action.args.get("uuid").getAsString();
                    boolean error = action.args.has("error");

                    EmbedBuilder embedBuilder = new EmbedBuilder();
                    embedBuilder.addField("Username", username, false);
                    if (error) {
                        embedBuilder.addField("Error", action.args.get("error").getAsString(), false);
                    } else {
                        embedBuilder.addField("Set speed to", speed, false);
                    }
                    int random = (int) (Math.random() * 0xFFFFFF);
                    embedBuilder.setColor(random);
                    embedBuilder.setFooter("-> Charmonium Remote Control", "https://media.forgecdn.net/avatars/266/994/637234317818341856.png");
                    String avatar = "https://crafatar.com/avatars/" + uuid;
                    embedBuilder.setAuthor("Instance name -> " + username, avatar, avatar);

                    try {
                        event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
                    } catch (Exception e) {
                        event.getChannel().sendMessageEmbeds(embedBuilder.build()).queue();
                    }
                },
                timeoutAction -> event.getHook().sendMessage("Can't set speed").queue(),
                event
        ));

        int speed = Objects.requireNonNull(event.getOption("speed")).getAsInt();
        JsonObject args = new JsonObject();
        args.addProperty("speed", speed);
        if (event.getOption("ign") != null) {
            String ign = Objects.requireNonNull(event.getOption("ign")).getAsString();
            if (WebsocketHandler.getInstance().getWebsocketServer().minecraftInstances.containsValue(ign)) {
                WebsocketHandler.getInstance().getWebsocketServer().minecraftInstances.forEach((webSocket, s) -> {
                    if (s.equals(ign)) {
                        sendMessage(webSocket, args);
                    }
                });
            } else {
                event.getHook().sendMessage("There isn't any instances connected with that IGN").queue();
            }
        } else {
            WebsocketHandler.getInstance().getWebsocketServer().minecraftInstances.forEach((webSocket, s) -> sendMessage(webSocket, args));
        }
    }
}
