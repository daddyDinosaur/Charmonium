package com.mylk.charmonium.config.page;

import cc.polyfrost.oneconfig.config.annotations.Slider;

public class GemstoneDelaysPage {
    @Slider(
            name = "Rotation Delay",
            min = 0, max = 1250, step = 25
    )
    public static int gemstoneRotationDelay = 75;

    @Slider(
            name = "Teleport Rotation Delay",
            min = 0, max = 1250, step = 25
    )
    public static int gemstoneTeleportRotDelay = 50;

    @Slider(
            name = "Teleport Delay",
            min = 100, max = 2000, step = 50
    )
    public static int gemstoneTeleportDelay = 500;

    @Slider(
            name = "Stuck Delay",
            min = 1500, max = 7500, step = 100
    )
    public static int gemstoneStuckDelay = 2000;
}
