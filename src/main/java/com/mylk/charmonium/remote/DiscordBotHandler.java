package com.mylk.charmonium.remote;

import cc.polyfrost.oneconfig.utils.Notifications;
import com.mylk.charmonium.config.Config;
import com.mylk.charmonium.remote.command.discordCommands.DiscordCommand;
import com.mylk.charmonium.remote.command.discordCommands.impl.*;
import com.mylk.charmonium.remote.event.InteractionAutoComplete;
import com.mylk.charmonium.remote.event.InteractionCreate;
import com.mylk.charmonium.util.LogUtils;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

// Big thanks to Cephetir for the idea of standalone JDA Dependency

public class DiscordBotHandler extends ListenerAdapter {
    private static DiscordBotHandler instance;
    @Getter
    public final ArrayList<DiscordCommand> commands = new ArrayList<>();
    @Getter
    @Setter
    public boolean finishedLoading = false;
    @Getter
    @Setter
    private String connectingState = "";
    @Getter
    @Setter
    private JDA jdaClient;
    private Thread tryConnectThread;

    public DiscordBotHandler() {
        commands.addAll(Arrays.asList(
                new Help(),
                new Toggle(),
                new Reconnect(),
                new Disconnect(),
                new Screenshot(),
                new Info()));
        LogUtils.sendDebug("Registered " + commands.size() + " commands");
        connect();
    }

    public static DiscordBotHandler getInstance() {
        if (instance == null) {
            instance = new DiscordBotHandler();
        }
        return instance;
    }

    public void connect() {
        if (!Config.enableRemoteControl) return;
        if (Config.discordRemoteControlToken.isEmpty()) return;

        if (WebsocketHandler.getInstance().isServerAlive()) {
            LogUtils.sendWarning("Discord Bot is already connected, connecting as a client...");
            WebsocketHandler.getInstance().setWebsocketState(WebsocketHandler.WebsocketState.CLIENT);
            finishedLoading = true;
            tryConnectThread = null;
            return;
        }
        try {
            jdaClient = JDABuilder.createLight(Config.discordRemoteControlToken.replace(" ", "").replace("\n", "").trim())
                    .disableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE, CacheFlag.ACTIVITY)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                    .build();
            jdaClient.awaitReady();
            jdaClient.updateCommands()
                    .addCommands(commands.stream().map(DiscordCommand::getSlashCommand).collect(Collectors.toList()))
                    .queue();
            jdaClient.addEventListener(new InteractionAutoComplete());
            jdaClient.addEventListener(new InteractionCreate());
            WebsocketHandler.getInstance().setWebsocketState(WebsocketHandler.WebsocketState.SERVER);
            Notifications.INSTANCE.send("Charmonium", "Connected to the Discord Bot!");
            LogUtils.sendSuccess("Connected to the Discord Bot!");
        } catch (InvalidTokenException | ErrorResponseException e) {
            e.printStackTrace();
            Notifications.INSTANCE.send("Charmonium", "Failed to connect to the Discord Bot! Check your token. Disabling remote control and removing the token...");
            LogUtils.sendError("Failed to connect to the Discord Bot! Check your token. Disabling remote control and removing the token...");
            WebsocketHandler.getInstance().setWebsocketState(WebsocketHandler.WebsocketState.NONE);
            Config.discordRemoteControlToken = "";
        } catch (IllegalStateException e) {
            if (e.getMessage().contains("im-getting-closecode4014-disallowed-intents")) {
                Notifications.INSTANCE.send("Charmonium", "You need to enable Intents in the Discord Developer Portal! Disabling remote control...");
                LogUtils.sendError("You need to enable Intents in the Discord Developer Portal! Disabling remote control...");
                Config.enableRemoteControl = false;
            } else {
                Notifications.INSTANCE.send("Charmonium", "Discord Bot is already connected, connecting as a client...");
                LogUtils.sendWarning("Discord Bot is already connected, connecting as a client...");
                WebsocketHandler.getInstance().setWebsocketState(WebsocketHandler.WebsocketState.CLIENT);
            }
        } catch (InterruptedException e) {
            Notifications.INSTANCE.send("Charmonium", "Unexpected error while connecting to the Discord Bot, disabling remote control...");
            LogUtils.sendError("Unexpected error while connecting to the Discord Bot, disabling remote control...");
            Config.enableRemoteControl = false;
            WebsocketHandler.getInstance().setWebsocketState(WebsocketHandler.WebsocketState.NONE);
        }
        tryConnectThread = null;
        finishedLoading = true;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.START) return;

        if (!Config.enableRemoteControl) {
            if (jdaClient != null) {
                System.out.println("[Remote Control] Removing jdaClient");
                jdaClient.shutdownNow();
                jdaClient = null;
            }
            if (WebsocketHandler.getInstance().getWebsocketServer() != null) {
                try {
                    System.out.println("[Remote Control] Stopping websocket server");
                    WebsocketHandler.getInstance().getWebsocketServer().stop();
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                System.out.println("[Remote Control] Removing websocket server");
                WebsocketHandler.getInstance().setWebsocketServer(null);
            }
            WebsocketHandler.getInstance().setWebsocketState(WebsocketHandler.WebsocketState.NONE);
            finishedLoading = false;
            connectingState = EnumChatFormatting.YELLOW + "Connecting to Socket...";
            return;
        }

        if (!Loader.isModLoaded("farmhelperjdadependency")) {
            Notifications.INSTANCE.send("Charmonium", "CharmoniumJDA is not loaded, disabling remote control..");
            LogUtils.sendDebug("CharmoniumJDA is not loaded, disabling remote control..");
            Config.enableRemoteControl = false;
            return;
        }
        if (Minecraft.getMinecraft().thePlayer == null || Minecraft.getMinecraft().theWorld == null) return;
        if (Config.discordRemoteControlToken == null || Config.discordRemoteControlToken.isEmpty()) {
            if (WebsocketHandler.getInstance().getWebsocketState() != WebsocketHandler.WebsocketState.CLIENT) {
                LogUtils.sendDebug("Connecting as a client...");
                WebsocketHandler.getInstance().setWebsocketState(WebsocketHandler.WebsocketState.CLIENT);
            }
            return;
        } else if (Config.discordRemoteControlToken.startsWith("https")) {
            LogUtils.sendError("You have put a webhook link in the Discord Remote Control Token field! Read the guide before using this feature, dummy! Clearing the field...");
            Config.discordRemoteControlToken = "";
            return;
        }

        if (jdaClient == null && WebsocketHandler.getInstance().getWebsocketState() == WebsocketHandler.WebsocketState.NONE) {
            if (tryConnectThread == null) {
                tryConnectThread = new Thread(() -> {
                    try {
                        System.out.println("[Remote Control] Connecting to Discord Bot...");
                        connect();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
                tryConnectThread.start();
            }
            return;
        }

        if (WebsocketHandler.getInstance().getWebsocketState() == WebsocketHandler.WebsocketState.CLIENT) {
            if (WebsocketHandler.getInstance().getWebsocketClient() != null && WebsocketHandler.getInstance().getWebsocketClient().isOpen()) {
                connectingState = EnumChatFormatting.GREEN + "Connected to the Discord Bot as a client";
            } else {
                connectingState = EnumChatFormatting.GOLD + "Connecting to the Discord Bot as a client...";
            }
            return;
        }

        if (jdaClient != null && jdaClient.getStatus() == JDA.Status.CONNECTED) {
            connectingState = EnumChatFormatting.DARK_GREEN + "Connected to Discord Bot as a server";
        } else {
            connectingState = EnumChatFormatting.GOLD + "Connecting to the Discord Bot as a server...";
        }
    }
}
