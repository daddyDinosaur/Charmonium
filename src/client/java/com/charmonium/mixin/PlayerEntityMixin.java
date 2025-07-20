package com.charmonium.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntityMixin {

    @Shadow
    private PlayerInventory inventory;

    @Inject(method = "isSpectator()Z", at = @At("HEAD"), cancellable = true)
    public void onIsSpectator(CallbackInfoReturnable<Boolean> cir) {

    }

    @Inject(at = { @At("HEAD") }, method = "getOffGroundSpeed()F", cancellable = true)
    protected void onGetOffGroundSpeed(CallbackInfoReturnable<Float> cir) {
    }
}
