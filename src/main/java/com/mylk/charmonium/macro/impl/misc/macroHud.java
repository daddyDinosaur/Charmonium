package com.mylk.charmonium.macro.impl.misc;

import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.hud.TextHud;
import com.mylk.charmonium.config.Config;
import com.mylk.charmonium.feature.impl.BanInfo;
import com.mylk.charmonium.feature.impl.LeaveTimer;
import com.mylk.charmonium.handler.GameStateHandler;
import com.mylk.charmonium.handler.MacroHandler;
import com.mylk.charmonium.macro.impl.*;
import com.mylk.charmonium.remote.DiscordBotHandler;
import com.mylk.charmonium.util.LogUtils;
import net.minecraftforge.fml.common.Loader;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class macroHud extends TextHud{
    public macroHud() {
        super(true, 0, 0, 1.0f, true, true, 10, 8, 8, new OneColor(0, 0, 0, 150), true, 2, new OneColor(0, 0, 0, 240));
    }

    private final boolean jdaDependencyPresent = Loader.isModLoaded("farmhelperjdadependency");

    @Override
    protected void getLines(List<String> lines, boolean example) {
        if (example) {
            String[] exampleLines = new String[]{
                    "§r§lStats:",
                    "§rMacro: §fNone",
                    "§r___ Nearby: §fNone",
                    "§rTime: §f0h 00m 00s",
                    "§rState: §fNone",
            };
            lines.addAll(Arrays.asList(exampleLines));
        } else if (GameStateHandler.getInstance().atProperIsland() || MacroHandler.getInstance().isMacroToggled()) {
            String[] macroLines = new String[0];
            if (MacroHandler.getInstance().getCrop() == Config.MacroEnum.FORAGING) {
                macroLines = ForagingMacro.drawInfo();
            } else if (MacroHandler.getInstance().getCrop() == Config.MacroEnum.ICE_WALKER) {
                macroLines = IceWalkerMacro.drawInfo();
            } else if (MacroHandler.getInstance().getCrop() == Config.MacroEnum.SLAYER_AURA) {
                macroLines = SlayerAura.drawInfo();
            } else if (MacroHandler.getInstance().getCrop() == Config.MacroEnum.SLAYER) {
                macroLines = SlayerMacro.drawInfo();
            } else if (MacroHandler.getInstance().getCrop() == Config.MacroEnum.FISHING) {
                macroLines = FishingMacro.drawInfo();
            }  else if (MacroHandler.getInstance().getCrop() == Config.MacroEnum.GEMSTONE) {
                macroLines = GemstoneMacro.drawInfo();
            }

            lines.addAll(Arrays.asList(macroLines));
            lines.add("");

            if (BanInfo.getInstance().isRunning() && Config.banwaveCheckerEnabled && BanInfo.getInstance().isConnected()) {
                lines.add("Ban stats from the last " + BanInfo.getInstance().getMinutes() + " minutes");
                lines.add("Staff bans: " + BanInfo.getInstance().getStaffBans());
                if (BanInfo.getInstance().getBansByMod() != -1)
                    lines.add("Detected by FarmHelper: " + BanInfo.getInstance().getBansByMod());
            } else if (!BanInfo.getInstance().isConnected() && Config.banwaveCheckerEnabled && !BanInfo.getInstance().isReceivedBanwaveInfo()) {
                lines.add("Connecting to the analytics server...");
                if (System.currentTimeMillis() - BanInfo.getInstance().getLastReceivedPacket() > 300_000) {
                    lines.add("If this takes too long, please restart client");
                }
            }
            if (LeaveTimer.getInstance().isRunning())
                lines.add("Leaving in " + LogUtils.formatTime(Math.max(LeaveTimer.leaveClock.getRemainingTime(), 0)));
            
            if (Config.enableRemoteControl && jdaDependencyPresent) {
                if (!Objects.equals(DiscordBotHandler.getInstance().getConnectingState(), "")) {
                    lines.add("");
                    lines.add(DiscordBotHandler.getInstance().getConnectingState());
                }
            }
        }
    }
}
