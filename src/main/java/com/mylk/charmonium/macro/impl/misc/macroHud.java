package com.mylk.charmonium.macro.impl.misc;

import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.hud.TextHud;
import com.mylk.charmonium.config.Config;
import com.mylk.charmonium.handler.GameStateHandler;
import com.mylk.charmonium.handler.MacroHandler;
import com.mylk.charmonium.macro.impl.*;

import java.util.Arrays;
import java.util.List;

public class macroHud extends TextHud{
    public macroHud() {
        super(true, 0, 0, 1.0f, true, true, 10, 8, 8, new OneColor(0, 0, 0, 150), true, 2, new OneColor(0, 0, 0, 240));
    }

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
        }
    }
}
