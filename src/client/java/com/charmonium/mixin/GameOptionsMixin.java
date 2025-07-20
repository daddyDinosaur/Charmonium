package com.charmonium.mixin;

import com.charmonium.Charmonium;
import com.charmonium.CharmoniumClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameOptions.class)
public class GameOptionsMixin {
    private static final SimpleOption<Double> fullbrightOption = new SimpleOption<Double>("fullbright_gamma", null, null,
            null, null, 99999.9, null);

    @Inject(at = { @At("HEAD") }, method = {
            "getGamma()Lnet/minecraft/client/option/SimpleOption;" }, cancellable = true)
    private void onGetGamma(CallbackInfoReturnable<SimpleOption<Double>> cir) {
        MinecraftClient MC = MinecraftClient.getInstance();
        if (MC.currentScreen instanceof GameOptionsScreen)
            return;

        CharmoniumClient charm = Charmonium.getInstance();
        if (charm == null)
            return;

        if (charm.moduleManager.fullbright.state.getValue()) {
            cir.setReturnValue(fullbrightOption);
        }
    }
}
