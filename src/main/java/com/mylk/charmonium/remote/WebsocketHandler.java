package com.mylk.charmonium.remote;

import cc.polyfrost.oneconfig.utils.Notifications;
import com.google.gson.JsonObject;
import com.mylk.charmonium.Charmonium;
import com.mylk.charmonium.config.Config;
import com.mylk.charmonium.remote.command.commands.ClientCommand;
import com.mylk.charmonium.remote.command.commands.impl.*;
import com.mylk.charmonium.remote.struct.RemoteMessage;
import com.mylk.charmonium.remote.struct.WebsocketClient;
import com.mylk.charmonium.remote.struct.WebsocketServer;
import com.mylk.charmonium.remote.waiter.WaiterHandler;
import com.mylk.charmonium.util.LogUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.java_websocket.enums.ReadyState;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;

public class WebsocketHandler {
    public static final ArrayList<ClientCommand> commands = new ArrayList<>();
    private static WebsocketHandler instance;
    public final Minecraft mc = Minecraft.getMinecraft();
    @Getter
    @Setter
    private WebsocketState websocketState = WebsocketState.NONE;

    @Getter
    @Setter
    private WebsocketServer websocketServer;

    @Getter
    @Setter
    private WebsocketClient websocketClient;
    private int reconnectAttempts = 0;
    public WebsocketHandler() {
        commands.addAll(Arrays.asList(
                new InfoCommand(),
                new ReconnectCommand(),
                new ScreenshotCommand(),
                new ToggleCommand(),
                new DisconnectCommand()
        ));
        LogUtils.sendDebug("[Remote Control] Registered " + commands.size() + " commands.");
    }

    public static WebsocketHandler getInstance() {
        if (instance == null) {
            instance = new WebsocketHandler();
        }
        return instance;
    }

    public boolean isServerAlive() {
        try {
            URI uri = new URI("ws://" + Config.discordRemoteControlAddress + ":" + Config.remoteControlPort);
            websocketClient = new WebsocketClient(uri);
            JsonObject data = new JsonObject();
            data.addProperty("name", Minecraft.getMinecraft().getSession().getUsername());
            websocketClient.addHeader("auth", Charmonium.gson.toJson(data));
            LogUtils.sendDebug("[Remote Control] Connecting to websocket server...");
            return websocketClient.connectBlocking();
        } catch (URISyntaxException | InterruptedException e) {
            websocketClient = null;
            LogUtils.sendDebug("[Remote Control] Failed to connect to the websocket server!");
            return false;
        }
    }

    public void send(String json) {
        if (websocketState == WebsocketState.CLIENT && websocketClient != null && websocketClient.isOpen()) {
            websocketClient.send(json);
        } else if (websocketState == WebsocketState.SERVER && websocketServer != null && websocketServer.websocketServerState == WebsocketServer.WebsocketServerState.CONNECTED) {
            WaiterHandler.onMessage(Charmonium.gson.fromJson(json, RemoteMessage.class));
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!Loader.isModLoaded("farmhelperjdadependency")) {
            if (Config.enableRemoteControl) {
                Config.enableRemoteControl = false;
                LogUtils.sendError("[Remote Control] Charmonium JDA Dependency is not installed, disabling remote control..");
                Notifications.INSTANCE.send("Charmonium", "Charmonium JDA Dependency is not installed, disabling remote control..");
            }
            return;
        }
        if (!DiscordBotHandler.getInstance().isFinishedLoading()) return;

        switch (websocketState) {
            case NONE: {
                if (websocketClient != null && websocketClient.isOpen()) {
                    try {
                        websocketClient.closeBlocking();
                        websocketClient = null;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
                if (websocketServer != null && websocketServer.websocketServerState == WebsocketServer.WebsocketServerState.CONNECTED) {
                    try {
                        websocketServer.stop();
                        websocketServer = null;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
            }
            case CLIENT: {
                if (websocketClient == null) {
                    try {
                        URI uri = new URI("ws://" + Config.discordRemoteControlAddress + ":" + Config.remoteControlPort);
                        websocketClient = new WebsocketClient(uri);
                        JsonObject data = new JsonObject();
                        data.addProperty("name", mc.getSession().getUsername());
                        websocketClient.addHeader("auth", Charmonium.gson.toJson(data));
                        LogUtils.sendDebug("[Remote Control] Connecting to websocket server...");
                        websocketClient.connectBlocking();
                        Notifications.INSTANCE.send("Charmonium", "Connected to websocket server as a client!");
                    } catch (URISyntaxException | InterruptedException e) {
                        LogUtils.sendDebug("[Remote Control] Failed to connect to the websocket server!");
                        e.printStackTrace();
                    }
                } else if (!websocketClient.isOpen() && websocketClient.getReadyState() != ReadyState.NOT_YET_CONNECTED) {
                    if (reconnectAttempts > 5) {
                        reconnectAttempts = 0;
                        websocketState = WebsocketState.NONE;
                        Notifications.INSTANCE.send("Charmonium", "Failed to connect to the websocket server, disabling remote control..");
                        LogUtils.sendError("[Remote Control] Failed to connect to the websocket server, disabling remote control..");
                        Config.enableRemoteControl = false;
                        return;
                    }
                    try {
                        reconnectAttempts++;
                        websocketClient.reconnectBlocking();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                break;
            }
            case SERVER: {
                if (websocketServer == null) {
                    websocketServer = new WebsocketServer(Config.remoteControlPort);
                    websocketServer.start();
                    Notifications.INSTANCE.send("Charmonium", "Started websocket server on port " + Config.remoteControlPort);
                } else if (websocketServer.websocketServerState == WebsocketServer.WebsocketServerState.NOT_CONNECTED) {
                    try {
                        websocketServer.stop();
                        websocketServer = null;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
            }
        }
    }

    public enum WebsocketState {
        SERVER,
        CLIENT,
        NONE
    }
}
