package com.charmonium.mixin;

import com.charmonium.Charmonium;
import com.charmonium.CharmoniumClient;
import com.charmonium.event.events.TickEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.session.Session;
import net.minecraft.client.world.ClientWorld;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Shadow
    private int itemUseCooldown;
    @Shadow
    @Final
    private Session session;

    @Shadow
    @Final
    private Mouse mouse;

    @Shadow
    public ClientWorld world;

    @Shadow
    public ClientPlayerEntity player;

    private Session CharmoniumSession;

    @Shadow
    public abstract boolean isWindowFocused();

    @Shadow
    @Final
    public GameOptions options;

    @Inject(at = @At("HEAD"), method = "onFinishedLoading(Lnet/minecraft/client/MinecraftClient$LoadingContext;)V")
    private void onfinishedloading(CallbackInfo info) {
        Charmonium.getInstance().loadAssets();
    }

    @Inject(at = @At("HEAD"), method = "tick()V")
    public void onPreTick(CallbackInfo info) {
        if (world != null && player != null) {
            TickEvent.Pre updateEvent = new TickEvent.Pre();
            Charmonium.getInstance().eventManager.Fire(updateEvent);
        }
    }

    @Inject(at = @At("TAIL"), method = "tick()V")
    public void onPostTick(CallbackInfo info) {
        if (world != null && player != null) {
            TickEvent.Post updateEvent = new TickEvent.Post();
            Charmonium.getInstance().eventManager.Fire(updateEvent);
        }
    }

    @Inject(at = { @At("HEAD") }, method = { "getSession()Lnet/minecraft/client/session/Session;" }, cancellable = true)
    private void onGetSession(CallbackInfoReturnable<Session> cir) {
        if (CharmoniumSession == null)
            return;
        cir.setReturnValue(CharmoniumSession);
    }

    @Redirect(at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;session:Lnet/minecraft/client/session/Session;", opcode = Opcodes.GETFIELD, ordinal = 0), method = {
            "getSession()Lnet/minecraft/client/session/Session;" })
    private Session getSessionForSessionProperties(MinecraftClient mc) {
        if (CharmoniumSession != null)
            return CharmoniumSession;
        return session;
    }
}
