package com.charmonium.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends EntityMixin {
    @Inject(at = { @At("HEAD") }, method = "setHealth(F)V")
    public void onSetHealth(float health, CallbackInfo ci) {
    }

    @Inject(at = { @At("HEAD") }, method = "tickNewAi()V", cancellable = true)
    public void onTickNewAi(CallbackInfo ci) {

    }

    @Inject(at = {
            @At("HEAD") }, method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z", cancellable = true)
    public void onDamage(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> ci) {
    }
}
