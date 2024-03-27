package com.mylk.charmonium.handler;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.mylk.charmonium.Charmonium;
import com.mylk.charmonium.config.Config;
import com.mylk.charmonium.config.aotv.AOTVWaypointsStructs;
import com.mylk.charmonium.event.ReceivePacketEvent;
import com.mylk.charmonium.failsafe.FailsafeManager;
import com.mylk.charmonium.failsafe.impl.WorldChangeFailsafe;
import com.mylk.charmonium.feature.FeatureManager;
import com.mylk.charmonium.feature.impl.*;
import com.mylk.charmonium.macro.AbstractMacro;
import com.mylk.charmonium.macro.impl.*;
import com.mylk.charmonium.pathfinding.main.AStarPathFinder;
import com.mylk.charmonium.pathfinding.utils.PathFinderConfig;
import com.mylk.charmonium.pathfinding.walker.WalkerMain;
import com.mylk.charmonium.util.BlockUtils;
import com.mylk.charmonium.util.KeyBindUtils;
import com.mylk.charmonium.util.LogUtils;
import com.mylk.charmonium.util.PlayerUtils;
import com.mylk.charmonium.util.helper.AudioManager;
import com.mylk.charmonium.util.helper.FlyPathfinder;
import com.mylk.charmonium.util.helper.Timer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class MacroHandler {
    private static MacroHandler instance;
    private final Minecraft mc = Minecraft.getMinecraft();
    @Getter
    private final Timer macroingTimer = new Timer();
    @Getter
    private final Timer analyticsTimer = new Timer();
    @Getter
    @Setter
    private Optional<AbstractMacro> currentMacro = Optional.empty();
    @Getter
    @Setter
    private boolean isMacroToggled = false;
    @Getter
    @Setter
    private boolean startingUp = false;
    public static BlockPos waypoint;
    public static List<Vec3> path;
    public static AStarPathFinder finder = new AStarPathFinder();
    public static WalkerMain walker = new WalkerMain();
    Runnable startCurrent = () -> {
        KeyBindUtils.stopMovement();
        if (isMacroToggled()) {
            currentMacro.ifPresent(AbstractMacro::onEnable);
        }
        startingUp = false;
    };
    @Getter
    @Setter
    private Config.MacroEnum crop = Config.MacroEnum.NONE;

    public static MacroHandler getInstance() {
        if (instance == null) {
            instance = new MacroHandler();
        }
        return instance;
    }

    public boolean isCurrentMacroEnabled() {
        return currentMacro.isPresent() && currentMacro.get().isEnabled();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isCurrentMacroPaused() {
        return isMacroToggled() && currentMacro.isPresent() && currentMacro.get().isPaused();
    }

    public boolean isTeleporting() {
        return currentMacro.isPresent() && currentMacro.get().getRewarpState() != AbstractMacro.RewarpState.NONE;
    }

    public <T extends AbstractMacro> T getMacro() {
        switch (Config.getMacro()) {
            case FORAGING:
                GameStateHandler.allowedIslands.add(GameStateHandler.Location.HUB);
                GameStateHandler.islandWarp = "/warp Hub";
                return Macros.MACRO_FORAGING.getMacro();
            case ICE_WALKER:
                GameStateHandler.allowedIslands.add(GameStateHandler.Location.DWARVEN_MINES);
                GameStateHandler.islandWarp = "/warp Forge";
                return Macros.MACRO_ICE_WALKER.getMacro();
            case SLAYER_AURA:
                switch (Config.SlayerAuraType) {
                    case 0:
                        GameStateHandler.allowedIslands.add(GameStateHandler.Location.HUB);
                        GameStateHandler.islandWarp = "/warp Crypt";
                        return Macros.MACRO_SLAYER_AURA.getMacro();
                    case 1:
                        GameStateHandler.allowedIslands.add(GameStateHandler.Location.SPIDER_DEN);
                        GameStateHandler.islandWarp = "/warp Spider";
                        return Macros.MACRO_SLAYER_AURA.getMacro();
                    case 2:
                        GameStateHandler.allowedIslands.add(GameStateHandler.Location.THE_PARK);
                        GameStateHandler.islandWarp = "/warp Howl";
                        return Macros.MACRO_SLAYER_AURA.getMacro();
                    case 3:
                        GameStateHandler.allowedIslands.add(GameStateHandler.Location.THE_END);
                        GameStateHandler.islandWarp = "/warp End";
                        return Macros.MACRO_SLAYER_AURA.getMacro();
                }
            case GEMSTONE:
                GameStateHandler.allowedIslands.add(GameStateHandler.Location.CRYSTAL_HOLLOWS);
                GameStateHandler.islandWarp = "/warp ch";
                return Macros.MACRO_GEMSTONE.getMacro();
            case SLAYER:
                switch (Config.SlayerType) {
                    case 0:
                        GameStateHandler.allowedIslands.add(GameStateHandler.Location.HUB);
                        GameStateHandler.islandWarp = "/warp Crypt";
                        return Macros.MACRO_SLAYERS.getMacro();
                    case 1:
                        GameStateHandler.allowedIslands.add(GameStateHandler.Location.SPIDER_DEN);
                        GameStateHandler.islandWarp = "/warp Spider";
                        return Macros.MACRO_SLAYERS.getMacro();
                    case 2:
                        GameStateHandler.allowedIslands.add(GameStateHandler.Location.THE_PARK);
                        GameStateHandler.islandWarp = "/warp Howl";
                        return Macros.MACRO_SLAYERS.getMacro();
                    case 3:
                        GameStateHandler.allowedIslands.add(GameStateHandler.Location.HUB);
                        GameStateHandler.islandWarp = "/warp Museum";
                        return Macros.MACRO_SLAYERS.getMacro();
                    case 4:
                        GameStateHandler.allowedIslands.add(GameStateHandler.Location.THE_END);
                        GameStateHandler.islandWarp = "/warp Void";
                        return Macros.MACRO_SLAYERS.getMacro();
                    case 5:
                        GameStateHandler.allowedIslands.add(GameStateHandler.Location.THE_END);
                        GameStateHandler.islandWarp = "/warp Drag";
                        return Macros.MACRO_SLAYERS.getMacro();
                }
            case FISHING:
                GameStateHandler.allowedIslands.add(GameStateHandler.Location.HUB);
                GameStateHandler.islandWarp = "/warp hub";
                return Macros.MACRO_FISHING.getMacro();
            default:
                throw new IllegalArgumentException("Invalid crop type: " + Config.SMacroType);
        }
    }

    public void setMacro(AbstractMacro macro) {
        setCurrentMacro(Optional.ofNullable(macro));
    }

    public void toggleMacro() {
        if (isMacroToggled()) {
            this.disableMacro();
        } else {
            if (FailsafeManager.getInstance().isHadEmergency()) {
                if (FailsafeManager.getInstance().triggeredFailsafe.isPresent()) {
                    FailsafeManager.getInstance().stopFailsafes();
                }
                FailsafeManager.getInstance().setHadEmergency(false);
                FailsafeManager.getInstance().getRestartMacroAfterFailsafeDelay().reset();
                LogUtils.sendWarning("Farm manually and DO NOT restart the macro too soon! The staff might still be spectating you for a while!");
                return;
            }
            this.enableMacro();
        }
    }

    public void enableMacro() {
        GameStateHandler.allowedIslands.clear();

        setMacro(getMacro());

        if (!currentMacro.isPresent()) {
            LogUtils.sendError("Invalid macro type: " + Config.SMacroType);
            return;
        }

        LogUtils.sendDebug("Selected macro: " + LogUtils.capitalize(currentMacro.get().getClass().getSimpleName()));

        if (!GameStateHandler.getInstance().atProperIsland()) {
            LogUtils.sendError("You must be in the proper island to start the macro!");
            return;
        }

        PlayerUtils.closeScreen();
        LogUtils.sendSuccess("Macro enabled!");
        LogUtils.webhookLog("Macro enabled!");

        analyticsTimer.reset();
        Multithreading.schedule(() -> {
            if (!macroingTimer.isScheduled()) {
                macroingTimer.schedule();
                macroingTimer.pause();
            }
        }, 300, TimeUnit.MILLISECONDS);

        if (mc.currentScreen != null) {
            PlayerUtils.closeScreen();
        }
        AudioManager.getInstance().setSoundBeforeChange(mc.gameSettings.getSoundLevel(SoundCategory.MASTER));

        FeatureManager.getInstance().enableAll();

        setMacroToggled(true);
        enableCurrentMacro();
    }

    public void disableMacro() {
        setMacroToggled(false);
        LogUtils.sendSuccess("Macro disabled!");
        LogUtils.webhookLog("Macro disabled!");
        currentMacro.ifPresent(m -> {
            m.getRotation().reset();
        });

        SkillTracker.resetSkills();

        waypoint = null;
        walker.stop();

        macroingTimer.pause();
        analyticsTimer.pause();

        setCrop(Config.MacroEnum.NONE);
        FeatureManager.getInstance().disableAll();
        FeatureManager.getInstance().resetAllStates();
        FailsafeManager.getInstance().resetAfterMacroDisable();
        if (UngrabMouse.getInstance().isToggled())
            UngrabMouse.getInstance().stop();
        disableCurrentMacro();
        setCurrentMacro(Optional.empty());
        if (FlyPathfinder.getInstance().isRunning()) {
            FlyPathfinder.getInstance().stop();
        }
        if (BaritoneHandler.isPathing()) {
            BaritoneHandler.stopPathing();
        }
    }

    public void pauseMacro(boolean scheduler) {
        currentMacro.ifPresent(cm -> {
            KeyBindUtils.stopMovement();
            if (cm.isPaused()) return;
            cm.onDisable();
            macroingTimer.pause();
            analyticsTimer.pause();
            if (scheduler && Freelook.getInstance().isRunning()) {
                Freelook.getInstance().stop();
            }
            if (Scheduler.getInstance().isFarming())
                Scheduler.getInstance().pause();
        });
    }

    public void pauseMacro() {
        pauseMacro(false);
    }

    public void resumeMacro() {
        currentMacro.ifPresent(cm -> {
            if (!cm.isPaused()) return;
            mc.inGameHasFocus = true;
            cm.onEnable();
            PlayerUtils.getTool();
            macroingTimer.resume();
            analyticsTimer.resume();
            Scheduler.getInstance().resume();
        });
    }

    public void enableCurrentMacro() {
        if (currentMacro.isPresent() && !currentMacro.get().isEnabled() && !startingUp) {
            mc.displayGuiScreen(null);
            mc.inGameHasFocus = true;
            mc.mouseHelper.grabMouseCursor();
            startingUp = true;
            PlayerUtils.itemChangedByStaff = false;
            PlayerUtils.changeItemEveryClock.reset();
            KeyBindUtils.holdThese(mc.thePlayer.capabilities.isFlying ? mc.gameSettings.keyBindSneak : null);
            Multithreading.schedule(() -> {
                startCurrent.run();
                macroingTimer.resume();
            }, 300, TimeUnit.MILLISECONDS);
        }
    }

    public void disableCurrentMacro() {
        currentMacro.ifPresent(AbstractMacro::onDisable);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END || !isMacroToggled()) {
            return;
        }
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.currentScreen != null) {
            KeyBindUtils.stopMovement();
            return;
        }

        if (!GameStateHandler.getInstance().atProperIsland()) {
            if (
//                    FeatureManager.getInstance().isAnyOtherFeatureEnabled(Failsafe.getInstance()) &&
                    !FeatureManager.getInstance().shouldIgnoreFalseCheck() &&
                            !FailsafeManager.getInstance().triggeredFailsafe.isPresent()) {
                FailsafeManager.getInstance().possibleDetection(WorldChangeFailsafe.getInstance());
            }
            return;
        }

        currentMacro.ifPresent(cm -> {
            if (!cm.isEnabled()) return;
            cm.onTick();
        });
    }

    @SubscribeEvent
    public void onChatMessageReceived(ClientChatReceivedEvent event) {
        if (!isMacroToggled()) {
            return;
        }

        currentMacro.ifPresent(m -> {
            if (!m.isEnabled()) return;
            m.onChatMessageReceived(event.message.getUnformattedText());
        });
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (!isMacroToggled()) {
            return;
        }
        currentMacro.ifPresent(m -> {
            if (!m.isEnabled()) return;
            m.onLastRender();
        });
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (!isMacroToggled()) {
            return;
        }
        currentMacro.ifPresent(m -> {
            if (!m.isEnabled()) return;
            m.onOverlayRender(event);
        });
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent event) {
        if (!isMacroToggled()) {
            return;
        }
        currentMacro.ifPresent(m -> {
            if (!m.isEnabled()) return;
            m.onPacketReceived(event);
        });
    }

    public static void walkTo(Entity entity) {
        new Thread(() -> {
            PathFinderConfig newConfig = new PathFinderConfig(
                    1000,
                    BlockUtils.fromVecToBP(Charmonium.mc.thePlayer.getPositionVector().addVector(-0.5, 0, -0.5)),
                    BlockUtils.fromVecToBP(entity.getPositionVector())
            );

            path = finder.fromClassToVec(finder.run(newConfig));
            walker.run(path, true, false, 2);
        }).start();
    }

    public static void walkTo(BlockPos pos) {
        new Thread(() -> {
            PathFinderConfig newConfig = new PathFinderConfig(
                    10000,
                    BlockUtils.fromVecToBP(Charmonium.mc.thePlayer.getPositionVector().addVector(-0.5, 0, -0.5)),
                    pos
            );

            path = finder.fromClassToVec(finder.run(newConfig));
            walker.run(path, true, false, 2);
        }).start();
    }

    public static void walkTo(ArrayList<AOTVWaypointsStructs.Waypoint> thePath, int currentWaypoint) {
        walkTo(thePath, currentWaypoint, BlockUtils.fromVecToBP(Charmonium.mc.thePlayer.getPositionVector().addVector(-0.5, 0, -0.5)));
    }

    public static void walkTo(ArrayList<AOTVWaypointsStructs.Waypoint> thePath, int currentWaypoint, BlockPos startingPos) {
        waypoint = new BlockPos(thePath.get(currentWaypoint).x, thePath.get(currentWaypoint).y + 1, thePath.get(currentWaypoint).z);

        new Thread(() -> {
            PathFinderConfig newConfig = new PathFinderConfig(
                    10000,
                    startingPos,
                    waypoint
            );

            List<Vec3> path = finder.fromClassToVec(finder.run(newConfig));
            walker.run(path, true, false, 2);
        }).start();
    }

    @AllArgsConstructor
    public enum Macros {
        MACRO_FORAGING(ForagingMacro.class),
        MACRO_ICE_WALKER(IceWalkerMacro.class),
        MACRO_SLAYER_AURA(SlayerAura.class),
        MACRO_GEMSTONE(GemstoneMacro.class),
        MACRO_SLAYERS(SlayerMacro.class),
        MACRO_FISHING(FishingMacro.class);

        private static final Map<Macros, AbstractMacro> macros = new HashMap<>();
        private final Class<? extends AbstractMacro> macroClass;

        public <T extends AbstractMacro> T getMacro() {
            if (!macros.containsKey(this)) {
                try {
                    macros.put(this, macroClass.newInstance());
                } catch (InstantiationException | IllegalAccessException e) {
                    LogUtils.sendError("Failed to instantiate macro: " + this.name());
                    e.printStackTrace();
                }
            }
            return (T) macros.get(this);
        }
    }
}
