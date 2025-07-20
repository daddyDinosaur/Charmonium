package com.charmonium;

import net.fabricmc.api.ModInitializer;

public class Charmonium implements ModInitializer {
    private static CharmoniumClient INSTANCE;

    @Override
    public void onInitialize() {
        INSTANCE = new CharmoniumClient();
        INSTANCE.Initialize();
    }

    public static CharmoniumClient getInstance() {
        return INSTANCE;
    }
}
