package com.mylk.charmonium.feature.impl;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.mylk.charmonium.config.Config;
import com.mylk.charmonium.event.ClickedBlockEvent;
import com.mylk.charmonium.failsafe.FailsafeManager;
import com.mylk.charmonium.feature.IFeature;
import com.mylk.charmonium.handler.MacroHandler;
import com.mylk.charmonium.util.LogUtils;
import com.mylk.charmonium.util.helper.CircularFifoQueue;
import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCocoa;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockNetherWart;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DesyncChecker implements IFeature {
    private static DesyncChecker instance;
    private final Minecraft mc = Minecraft.getMinecraft();
    @Getter
    private final CircularFifoQueue<ClickedBlockEvent> clickedBlocks = new CircularFifoQueue<>(120);
    private boolean enabled = false;

    public static DesyncChecker getInstance() {
        if (instance == null) {
            instance = new DesyncChecker();
        }
        return instance;
    }

    @Override
    public String getName() {
        return "Desync Checker";
    }

    @Override
    public boolean isRunning() {
        return enabled;
    }

    @Override
    public boolean shouldPauseMacroExecution() {
        return true;
    }

    @Override
    public boolean shouldStartAtMacroStart() {
        return isToggled();
    }

    @Override
    public void start() {
        clickedBlocks.clear();
    }

    @Override
    public void stop() {
        clickedBlocks.clear();
    }

    @Override
    public void resetStatesAfterMacroDisabled() {
        enabled = false;
    }

    @Override
    public boolean isToggled() {
        return Config.checkDesync;
    }

    @Override
    public boolean shouldCheckForFailsafes() {
        return false;
    }

    @SubscribeEvent
    public void onClickedBlock(ClickedBlockEvent event) {
        if (!isToggled()) return;
        if (!MacroHandler.getInstance().isMacroToggled()) return;
        if (FailsafeManager.getInstance().triggeredFailsafe.isPresent()) return;
        clickedBlocks.add(event);
        if (!clickedBlocks.isAtFullCapacity()) return;
        if (!checkIfDesync()) return;
        if (enabled) return;
        enabled = true;
        stop();
        LogUtils.sendWarning("[Desync Checker] Desync detected, pausing macro for " + Math.floor((double) Config.desyncPauseDelay / 1_000) + " seconds to prevent further desync.");
        MacroHandler.getInstance().pauseMacro();
        Multithreading.schedule(() -> {
            if (!MacroHandler.getInstance().isMacroToggled()) return;
            enabled = false;
            LogUtils.sendWarning("[Desync Checker] Desync should be over, resuming macro execution");
            MacroHandler.getInstance().resumeMacro();
        }, Config.desyncPauseDelay, TimeUnit.MILLISECONDS);
    }

    private boolean checkIfDesync() {
        float RATIO = 0.75f;
        List<ClickedBlockEvent> list = new ArrayList<>(clickedBlocks);
        int count = 0;
        for (ClickedBlockEvent pos : list) {
            IBlockState state = mc.theWorld.getBlockState(pos.getPos());
            if (state == null) continue;

            switch (MacroHandler.getInstance().getCrop()) {
                case FORAGING:
                    if (state.getBlock().equals(Blocks.log) || state.getBlock().equals(Blocks.log2  ))
                        count++;
                    break;
                default:
                    // Unknown crop
            }
        }
        return count / (float) list.size() >= RATIO;
    }
}
