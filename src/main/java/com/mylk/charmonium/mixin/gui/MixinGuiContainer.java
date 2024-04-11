package com.mylk.charmonium.mixin.gui;

import com.mylk.charmonium.event.DrawScreenAfterEvent;
import com.mylk.charmonium.event.GuiContainerEvent;
import com.mylk.charmonium.util.InventoryUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiContainer.class)
public class MixinGuiContainer {
    private final GuiContainer gui = (GuiContainer) (Object) this;

    @Shadow
    public Container inventorySlots;

    @Inject(method = "drawScreen", at = @At("RETURN"))
    public void drawScreen_after(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        String name = InventoryUtils.getInventoryName();
        MinecraftForge.EVENT_BUS.post(new DrawScreenAfterEvent(Minecraft.getMinecraft().currentScreen));
    }

    @Inject(method = "drawSlot", at = @At("HEAD"), cancellable = true)
    private void onDrawSlot(Slot slot, CallbackInfo ci) {
        if (MinecraftForge.EVENT_BUS.post(new GuiContainerEvent.DrawSlotEvent(inventorySlots, gui, slot)))
            ci.cancel();
    }

}
