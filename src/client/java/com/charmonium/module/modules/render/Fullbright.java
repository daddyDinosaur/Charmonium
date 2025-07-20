package com.charmonium.module.modules.render;

import com.charmonium.module.Module;
import com.charmonium.module.Category;

public class Fullbright extends Module {
    public Fullbright() {
        super("Fullbright");
        setCategory(Category.of("Render"));
        setDescription("Maxes out the brightness.");
    }

    @Override
    public void onDisable() {

    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onToggle() {

    }
}
