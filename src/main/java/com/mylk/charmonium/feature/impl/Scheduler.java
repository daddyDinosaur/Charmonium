package com.mylk.charmonium.feature.impl;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.mylk.charmonium.config.Config;
import com.mylk.charmonium.feature.FeatureManager;
import com.mylk.charmonium.feature.IFeature;
import com.mylk.charmonium.handler.GameStateHandler;
import com.mylk.charmonium.handler.MacroHandler;
import com.mylk.charmonium.handler.RotationHandler;
import com.mylk.charmonium.macro.AbstractMacro;
import com.mylk.charmonium.util.InventoryUtils;
import com.mylk.charmonium.util.KeyBindUtils;
import com.mylk.charmonium.util.LogUtils;
import com.mylk.charmonium.util.PlayerUtils;
import com.mylk.charmonium.util.helper.Clock;
import com.mylk.charmonium.util.helper.Rotation;
import com.mylk.charmonium.util.helper.RotationConfiguration;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.concurrent.TimeUnit;

public class Scheduler implements IFeature {
    private static Scheduler instance;
    private final Minecraft mc = Minecraft.getMinecraft();
    private final RotationHandler rotation = RotationHandler.getInstance();
    @Getter
    private final Clock schedulerClock = new Clock();
    @Getter
    @Setter
    private SchedulerState schedulerState = SchedulerState.NONE;

    public static Scheduler getInstance() {
        if (instance == null) {
            instance = new Scheduler();
        }
        return instance;
    }

    @Override
    public String getName() {
        return "Scheduler";
    }

    @Override
    public boolean isRunning() {
        return schedulerClock.isScheduled();
    }

    @Override
    public boolean shouldPauseMacroExecution() {
        return false;
    }

    @Override
    public boolean shouldStartAtMacroStart() {
        return isToggled();
    }

    @Override
    public void start() {
        schedulerState = SchedulerState.FARMING;
        schedulerClock.schedule((long) (Config.schedulerFarmingTime * 60_000f + (Math.random() * Config.schedulerFarmingTimeRandomness * 60_000f)));
    }

    @Override
    public void stop() {
        schedulerState = SchedulerState.NONE;
        schedulerClock.reset();
    }

    @Override
    public void resetStatesAfterMacroDisabled() {

    }

    @Override
    public boolean isToggled() {
        return Config.enableScheduler;
    }

    @Override
    public boolean shouldCheckForFailsafes() {
        return false;
    }

    public boolean isFarming() {
        return !Config.enableScheduler || schedulerState == SchedulerState.FARMING;
    }

    public String getStatusString() {
        if (Config.enableScheduler) {
            return EnumChatFormatting.BOLD + (schedulerState == SchedulerState.FARMING ? (EnumChatFormatting.GREEN + "Farming") : (EnumChatFormatting.DARK_AQUA + "Break")) + EnumChatFormatting.RESET + " for " +
                    EnumChatFormatting.BOLD + EnumChatFormatting.GOLD + LogUtils.formatTime(Math.max(schedulerClock.getRemainingTime(), 0)) + EnumChatFormatting.RESET + (schedulerClock.isPaused() ? " (Paused)" : "");
        } else {
            return "Farming";
        }
    }

    public void pause() {
        LogUtils.sendDebug("[Scheduler] Pausing");
        schedulerClock.pause();
    }

    public void resume() {
        LogUtils.sendDebug("[Scheduler] Resuming");
        schedulerClock.resume();
    }

    public void farmingTime() {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        schedulerState = SchedulerState.FARMING;
        if (mc.currentScreen != null) {
            PlayerUtils.closeScreen();
        }
        MacroHandler.getInstance().resumeMacro();
        schedulerClock.schedule((long) ((Config.schedulerFarmingTime * 60_000f) + (Math.random() * Config.schedulerFarmingTimeRandomness * 60_000f)));
    }

    public void breakTime() {
        schedulerState = SchedulerState.BREAK;
        MacroHandler.getInstance().pauseMacro(true);
        schedulerClock.schedule((long) ((Config.schedulerBreakTime * 60_000f) + (Math.random() * Config.schedulerBreakTimeRandomness * 60_000f)));
        KeyBindUtils.stopMovement();
        Multithreading.schedule(() -> {
            long randomTime4 = Config.getRandomRotationTime();
            this.rotation.easeTo(
                    new RotationConfiguration(
                            new Rotation(
                                    (float) (mc.thePlayer.rotationYaw + Math.random() * 20 - 10),
                                    (float) Math.min(90, Math.max(-90, (mc.thePlayer.rotationPitch + Math.random() * 30 - 15)))
                            ),
                            randomTime4, null
                    )
            );
            if (Config.openInventoryOnSchedulerBreaks) {
                Multithreading.schedule(InventoryUtils::openInventory, randomTime4 + 350, TimeUnit.MILLISECONDS);
            }
        }, (long) (300 + Math.random() * 200), TimeUnit.MILLISECONDS);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!Config.enableScheduler || event.phase == TickEvent.Phase.END || mc.thePlayer == null || mc.theWorld == null || !MacroHandler.getInstance().getCurrentMacro().isPresent())
            return;
        if (FeatureManager.getInstance().isAnyOtherFeatureEnabled(this)) return;

        if (MacroHandler.getInstance().isMacroToggled() && MacroHandler.getInstance().isCurrentMacroEnabled() && schedulerState == SchedulerState.FARMING && !schedulerClock.isPaused() && schedulerClock.passed()) {
            LogUtils.sendDebug("[Scheduler] Farming time has passed, stopping");
            if (SkillTracker.skillStopwatch.isStarted() && !SkillTracker.skillStopwatch.isSuspended()) {
                SkillTracker.skillStopwatch.suspend();
            }
            breakTime();
        } else if (MacroHandler.getInstance().isMacroToggled() && schedulerState == SchedulerState.BREAK && !schedulerClock.isPaused() && schedulerClock.passed()) {
            LogUtils.sendDebug("[Scheduler] Break time has passed, starting");
            if (SkillTracker.skillStopwatch.isStarted() && SkillTracker.skillStopwatch.isSuspended()) {
                SkillTracker.skillStopwatch.resume();
            } else if (!SkillTracker.skillStopwatch.isStarted()) {
                SkillTracker.skillStopwatch.start();
            }
            farmingTime();
        }
    }

    public enum SchedulerState {
        NONE,
        FARMING,
        BREAK
    }
}
