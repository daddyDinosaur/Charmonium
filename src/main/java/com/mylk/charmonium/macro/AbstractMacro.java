package com.mylk.charmonium.macro;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.mylk.charmonium.config.Config;
import com.mylk.charmonium.event.ReceivePacketEvent;
import com.mylk.charmonium.failsafe.FailsafeManager;
import com.mylk.charmonium.feature.FeatureManager;
import com.mylk.charmonium.feature.impl.*;
import com.mylk.charmonium.handler.GameStateHandler;
import com.mylk.charmonium.handler.MacroHandler;
import com.mylk.charmonium.handler.RotationHandler;
import com.mylk.charmonium.util.*;
import com.mylk.charmonium.util.helper.Clock;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("ALL")
@Getter
public abstract class AbstractMacro {
    public static final Minecraft mc = Minecraft.getMinecraft();
    private final RotationHandler rotation = RotationHandler.getInstance();
    private final Clock rewarpDelay = new Clock();
    private final Clock analyticsClock = new Clock();
    @Setter
    private boolean enabled = false;
    @Setter
    private boolean restoredState = false;
    @Getter
    @Setter
    public Vec3 spawnPos;
    @Setter
    private boolean rotated = false;
    @Setter
    private Optional<BlockPos> beforeTeleportationPos = Optional.empty();
    @Setter
    private RewarpState rewarpState = RewarpState.NONE;
    @Setter
    private int previousWalkingCoord = 0;


    public boolean isEnabled() {
        return enabled && !FeatureManager.getInstance().shouldPauseMacroExecution();
    }

    public boolean isPaused() {
        return !enabled;
    }

    private final Clock checkOnSpawnClock = new Clock();

    private boolean sentWarning = false;

    public void onTick() {
        if (FailsafeManager.getInstance().triggeredFailsafe.isPresent() || FailsafeManager.getInstance().getChooseEmergencyDelay().isScheduled()) {
            if (!sentWarning) {
                LogUtils.sendWarning("Failsafe is running! Blocking main onTick event!");
                sentWarning = true;
            }
            return;
        }
        if (mc.thePlayer.capabilities.isFlying) {
            KeyBindUtils.holdThese(mc.gameSettings.keyBindSneak);
            return;
        }
        checkForTeleport();
        LogUtils.webhookStatus();

        if (FailsafeManager.getInstance().triggeredFailsafe.isPresent()) {
            LogUtils.sendDebug("Blocking movement due to emergency!");
            return;
        }

        if (LagDetector.getInstance().isLagging()) {
            LogUtils.sendDebug("Blocking movement due to lag!");
            return;
        }

        PlayerUtils.getTool();

        invokeState();
    }

    public void onLastRender() {
    }

    public void onChatMessageReceived(String msg) {
    }

    public void onOverlayRender(RenderGameOverlayEvent.Post event) {
    }

    public void onPacketReceived(ReceivePacketEvent event) {
    }

    public abstract void invokeState();

    public void onEnable() {
        if (SkillTracker.skillStopwatch.isStarted() && SkillTracker.skillStopwatch.isSuspended()) {
            SkillTracker.skillStopwatch.resume();
        } else if (!SkillTracker.skillStopwatch.isStarted()) {
            SkillTracker.skillStopwatch.start();
        }

        checkOnSpawnClock.reset();
        GameStateHandler.getInstance().scheduleRewarp();
        setEnabled(true);
        analyticsClock.schedule(60_000);
    }

    public void onDisable() {
        if (SkillTracker.skillStopwatch.isStarted() && !SkillTracker.skillStopwatch.isSuspended()) {
            SkillTracker.skillStopwatch.suspend();
        }

        SkillTracker.resetSkills();

        DesyncChecker.getInstance().getClickedBlocks().clear();
        KeyBindUtils.stopMovement();
        rotation.reset();
        rewarpDelay.reset();
        setBeforeTeleportationPos(Optional.empty());
        sentWarning = false;
        setEnabled(false);
    }

    private void checkForTeleport() {
        if (!beforeTeleportationPos.isPresent()) return;
        if (mc.thePlayer.getPosition().distanceSq(beforeTeleportationPos.get()) > 2 && !PlayerUtils.isPlayerSuffocating()) {
            if (!mc.thePlayer.capabilities.isFlying && !mc.thePlayer.onGround) {
                return;
            } else if (mc.thePlayer.capabilities.isFlying && !mc.thePlayer.onGround) {
                if (!mc.gameSettings.keyBindSneak.isKeyDown()) {
                    KeyBindUtils.holdThese(mc.gameSettings.keyBindSneak);
                    Multithreading.schedule(() -> KeyBindUtils.stopMovement(), (long) (350 + Math.random() * 300), TimeUnit.MILLISECONDS);
                }
                return;
            }
            LogUtils.sendDebug("Teleported!");
            checkOnSpawnClock.reset();
            beforeTeleportationPos = Optional.empty();
            rewarpState = RewarpState.TELEPORTED;
            rotated = false;
            GameStateHandler.getInstance().scheduleNotMoving(750);
            rewarpDelay.schedule(Config.getRandomRewarpDelay());
        }
    }

    public void triggerWarpGarden() {
        triggerWarpGarden(false);
    }

    public void triggerWarpGarden(boolean force) {
        if (GameStateHandler.getInstance().notMoving()) {
            KeyBindUtils.stopMovement();
        }
        if (force || GameStateHandler.getInstance().canRewarp() && !beforeTeleportationPos.isPresent()) {
            rewarpState = RewarpState.TELEPORTING;
            LogUtils.sendDebug("Warping to spawn point");
            mc.thePlayer.sendChatMessage(GameStateHandler.getInstance().islandWarp);
            GameStateHandler.getInstance().scheduleRewarp();
            setBeforeTeleportationPos(Optional.ofNullable(mc.thePlayer.getPosition()));
        }
    }

    public enum RewarpState {
        NONE,
        TELEPORTING,
        TELEPORTED,
        POST_REWARP
    }
}
