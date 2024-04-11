package com.mylk.charmonium;

import baritone.api.BaritoneAPI;
import cc.polyfrost.oneconfig.utils.Notifications;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mylk.charmonium.command.Commands;
import com.mylk.charmonium.config.Config;
import com.mylk.charmonium.config.aotv.AOTVWaypoints;
import com.mylk.charmonium.config.aotv.AOTVWaypointsStructs;
import com.mylk.charmonium.event.MillisecondEvent;
import com.mylk.charmonium.failsafe.FailsafeManager;
import com.mylk.charmonium.feature.FeatureManager;
import com.mylk.charmonium.feature.impl.GemstoneSackCompactor;
import com.mylk.charmonium.feature.impl.MovRecPlayer;
import com.mylk.charmonium.feature.impl.SkillTracker;
import com.mylk.charmonium.feature.impl.charMods.lightsDevice;
import com.mylk.charmonium.feature.impl.charMods.replaceDiorite;
import com.mylk.charmonium.feature.impl.charMods.terminals.TerminalSolver;
import com.mylk.charmonium.feature.impl.runeCombining;
import com.mylk.charmonium.handler.*;
import com.mylk.charmonium.macro.impl.*;
import com.mylk.charmonium.macro.impl.misc.Maddoxer;
import com.mylk.charmonium.macro.impl.misc.fuelFilling;
import com.mylk.charmonium.remote.DiscordBotHandler;
import com.mylk.charmonium.remote.WebsocketHandler;
import com.mylk.charmonium.util.FailsafeUtils;
import com.mylk.charmonium.util.LogUtils;
import com.mylk.charmonium.util.ReflectionUtils;
import com.mylk.charmonium.util.RenderUtils;
import com.mylk.charmonium.util.charHelpers.pathingRotations;
import com.mylk.charmonium.util.helper.AudioManager;
import com.mylk.charmonium.util.helper.BaritoneEventListener;
import com.mylk.charmonium.util.helper.FlyPathfinder;
import com.mylk.charmonium.util.helper.TickTask;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.Display;

import java.awt.*;
import java.io.File;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.sql.Driver;

@Mod(modid = "charmonium", useMetadata = true)
public class Charmonium {
    public static final String VERSION = "%%VERSION%%";
    public static final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();
    public static Config config;
    public static boolean sentInfoAboutShittyClient = false;
    public static boolean isDebug = false;
    public static Driver driver;
    public static AOTVWaypoints aotvWaypoints;
    public static final Minecraft mc = Minecraft.getMinecraft();

    public static void sendMessage(Object object) {
        String message = "null";
        if (object != null) {
            message = object.toString().replace("&", "§");
        }
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText("§d§lCharmonium §8» §7" + message));
        }
    }

    public void createNewWaypointsConfig(FMLPreInitializationEvent event) {
        File directory = new File(event.getModConfigurationDirectory().getAbsolutePath());
        File coordsFile = new File(directory, "aotv_coords_char.json");

        if (!coordsFile.exists()) {
            try {
                Files.createFile(Paths.get(coordsFile.getPath()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            Reader reader = Files.newBufferedReader(Paths.get("./config/aotv_coords_char.json"));
            aotvWaypoints = gson.fromJson(reader, AOTVWaypoints.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (aotvWaypoints != null) {
            System.out.println(aotvWaypoints.getRoutes());
        } else {
            System.out.println("aotvWaypoints is null");
            System.out.println("Creating new CoordsConfig");
            aotvWaypoints = new AOTVWaypoints();
            AOTVWaypointsStructs.SaveWaypoints();
            System.out.println(aotvWaypoints.getRoutes());
        }
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        createNewWaypointsConfig(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        initializeFields();
        initializeListeners();
        initializeCommands();
        FeatureManager.getInstance().fillFeatures().forEach(MinecraftForge.EVENT_BUS::register);

        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.gammaSetting = 1000;
        isDebug = java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments().toString().contains("-agentlib:jdwp");
        Display.setTitle("Charmonium 〔v" + VERSION + "〕 ☛ " + (!isDebug ? "Bing Chilling" : "wazzup dev?"));
        FailsafeUtils.getInstance();

        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        executorService.scheduleAtFixedRate(() -> MinecraftForge.EVENT_BUS.post(new MillisecondEvent()), 0, 1, TimeUnit.MILLISECONDS);
        BaritoneAPI.getProvider().getPrimaryBaritone().getGameEventHandler().registerEventListener(new BaritoneEventListener());
    }

    @SubscribeEvent
    public void onTickSendInfoAboutShittyClient(TickEvent.PlayerTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (sentInfoAboutShittyClient) return;

        if (ReflectionUtils.hasPackageInstalled("feather")) {
            Notifications.INSTANCE.send("Charmonium", "You've got Feather Client installed! Be aware, you might have a lot of bugs because of this shitty client!", 15000);
            LogUtils.sendError("You've got §6§lFeather Client §cinstalled! Be aware, you might have a lot of bugs because of this shitty client!");
        }
        if (ReflectionUtils.hasPackageInstalled("cc.woverflow.hytils.HytilsReborn")) {
            Notifications.INSTANCE.send("Charmonium", "You've got Hytils installed in your mods folder! This will cause many issues with rewarping as it sends tons of commands every minute.", 15000);
            LogUtils.sendError("You've got §6§lHytils §cinstalled in your mods folder! This will cause many issues with rewarping as it sends tons of commands every minute.");
        }
        if (Minecraft.isRunningOnMac && Config.autoUngrabMouse) {
            Config.autoUngrabMouse = false;
            Notifications.INSTANCE.send("Charmonium", "Auto Ungrab Mouse feature doesn't work properly on Mac OS. It has been disabled automatically.", 15000);
            LogUtils.sendError("Auto Ungrab Mouse feature doesn't work properly on Mac OS. It has been disabled automatically.");
        }
        if (Config.configVersion == 1)
            Config.configVersion = 2;
        sentInfoAboutShittyClient = true;
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (Charmonium.aotvWaypoints != null && Charmonium.aotvWaypoints.getSelectedRoute() != null && Charmonium.aotvWaypoints.getSelectedRoute().waypoints != null) {
            ArrayList<AOTVWaypointsStructs.Waypoint> Waypoints = Charmonium.aotvWaypoints.getSelectedRoute().waypoints;

            if (Config.aotvHighlightRouteBlocks) {
                for (AOTVWaypointsStructs.Waypoint waypoint : Waypoints) {
                    RenderUtils.drawBlockBox(new BlockPos(waypoint.x, waypoint.y, waypoint.z), new Color(68, 215, 250, 94));
                }
                if (Config.aotvShowNumber) {
                    for (AOTVWaypointsStructs.Waypoint waypoint : Waypoints) {
                        BlockPos pos = new BlockPos(waypoint.x, waypoint.y, waypoint.z);
                        RenderUtils.drawText("§l§3[§f " + waypoint.name + " §3]", pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 1);
                    }
                }
            }
            if (Config.aotvShowRouteLines) {
                if (Waypoints.size() > 1) {
                    ArrayList<BlockPos> coords = new ArrayList<>();
                    for (AOTVWaypointsStructs.Waypoint waypoint : Waypoints) {
                        coords.add(new BlockPos(waypoint.x, waypoint.y, waypoint.z));
                    }
                    RenderUtils.drawCoordsRoute(coords, event, Color.pink);
                }
            }
        }
    }



    private void initializeListeners() {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(FailsafeManager.getInstance());
        MinecraftForge.EVENT_BUS.register(GameStateHandler.getInstance());
        MinecraftForge.EVENT_BUS.register(MacroHandler.getInstance());
        MinecraftForge.EVENT_BUS.register(TickTask.getInstance());
        MinecraftForge.EVENT_BUS.register(MovRecPlayer.getInstance());
        MinecraftForge.EVENT_BUS.register(WebsocketHandler.getInstance());
        if (Loader.isModLoaded("farmhelperjdadependency"))
            MinecraftForge.EVENT_BUS.register(DiscordBotHandler.getInstance());
        MinecraftForge.EVENT_BUS.register(AudioManager.getInstance());
        MinecraftForge.EVENT_BUS.register(RotationHandler.getInstance());
        MinecraftForge.EVENT_BUS.register(FlyPathfinder.getInstance());
        MinecraftForge.EVENT_BUS.register(pathingRotations.getInstance());
        MinecraftForge.EVENT_BUS.register(ForagingMacro.getInstance());
        MinecraftForge.EVENT_BUS.register(IceWalkerMacro.getInstance());
        MinecraftForge.EVENT_BUS.register(SlayerAura.getInstance());
        MinecraftForge.EVENT_BUS.register(GemstoneMacro.getInstance());
        MinecraftForge.EVENT_BUS.register(SlayerMacro.getInstance());
        MinecraftForge.EVENT_BUS.register(slayerHandler.getInstance());
        MinecraftForge.EVENT_BUS.register(Maddoxer.getInstance());
        MinecraftForge.EVENT_BUS.register(FishingMacro.getInstance());
        MinecraftForge.EVENT_BUS.register(SkillTracker.getInstance());
        MinecraftForge.EVENT_BUS.register(fuelFilling.getInstance());
        MinecraftForge.EVENT_BUS.register(runeCombining.getInstance());
        MinecraftForge.EVENT_BUS.register(GemstoneSackCompactor.getInstance());
        MinecraftForge.EVENT_BUS.register(replaceDiorite.getInstance());
        MinecraftForge.EVENT_BUS.register(lightsDevice.getInstance());
        MinecraftForge.EVENT_BUS.register(TerminalSolver.getInstance());
    }

    private void initializeFields() {
        config = new Config();
    }

    private void initializeCommands() {
        ClientCommandHandler.instance.registerCommand(new Commands());
    }
}
