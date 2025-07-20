package com.charmonium.mixin;

import com.charmonium.Charmonium;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.tag.TagKey;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow
    protected DataTracker dataTracker;

    @Shadow
    public abstract boolean isSubmergedIn(TagKey<Fluid> fluidTag);

    @Shadow
    public abstract boolean isOnGround();

    @Inject(at = {
            @At("HEAD") }, method = "isInvisibleTo(Lnet/minecraft/entity/player/PlayerEntity;)Z", cancellable = true)
    private void onIsInvisibleCheck(PlayerEntity message, CallbackInfoReturnable<Boolean> cir) {
/*        if (Charmonium.getInstance().moduleManager.antiinvis.state.getValue()) {
            cir.setReturnValue(false);
        }*/
    }

    @Inject(at = { @At("HEAD") }, method = "getStepHeight()F", cancellable = true)
    public void onGetStepHeight(CallbackInfoReturnable<Float> cir) {
    }

    @Inject(at = { @At("HEAD") }, method = "getJumpVelocityMultiplier()F", cancellable = true)
    public void onGetJumpVelocityMultiplier(CallbackInfoReturnable<Float> cir) {
    }

    @Inject(at = { @At("HEAD") }, method = "changeLookDirection(DD)V", cancellable = true)
    public void onChangeLookDirection(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {

    }
}