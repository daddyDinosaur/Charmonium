package com.charmonium.module.modules.render;

import com.charmonium.Charmonium;
import com.charmonium.event.events.Render3DEvent;
import com.charmonium.event.listeners.Render3DListener;
import com.charmonium.settings.types.ColorSetting;
import com.charmonium.settings.types.FloatSetting;
import com.charmonium.utils.render.Color;
import com.charmonium.utils.render.Render3D;
import com.charmonium.utils.ModuleUtils;
import com.charmonium.module.Module;
import com.charmonium.module.Category;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import net.minecraft.util.math.Box;

public class ChestESP extends Module implements Render3DListener {

    private final ColorSetting color = ColorSetting.builder().id("chestesp_color").displayName("Color")
            .description("Color").defaultValue(new Color(0, 1f, 1f, 0.3f)).build();

    private final FloatSetting lineThickness = FloatSetting.builder().id("chestesp_linethickness")
            .displayName("Line Thickness").description("Adjust the thickness of the ESP box lines").defaultValue(2f)
            .minValue(0f).maxValue(5f).step(0.1f).build();

    public ChestESP() {
        super("ChestESP");
        setCategory(Category.of("Render"));
        setDescription("Allows the player to see Chests with an ESP.");

        addSettings(color, lineThickness);
    }

    @Override
    public void onDisable() {
        Charmonium.getInstance().eventManager.RemoveListener(Render3DListener.class, this);
    }

    @Override
    public void onEnable() {
        Charmonium.getInstance().eventManager.AddListener(Render3DListener.class, this);
    }

    @Override
    public void onToggle() {

    }

    @Override
    public void onRender(Render3DEvent event) {
        ModuleUtils.getTileEntities().forEach(blockEntity -> {
            if (blockEntity instanceof ChestBlockEntity || blockEntity instanceof TrappedChestBlockEntity
                    || blockEntity instanceof BarrelBlockEntity) {
                Box box = new Box(blockEntity.getPos());
                Render3D.draw3DBox(event.GetMatrix(), event.getCamera(), box, color.getValue(),
                        lineThickness.getValue());
            }
        });
    }
}
