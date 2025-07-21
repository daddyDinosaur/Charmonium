package com.charmonium.mixin;

import com.charmonium.Charmonium;
import com.charmonium.event.events.PositionPacketEvent;
import com.charmonium.event.events.SendMovementPacketEvent;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayerEntityMixin {
    @Shadow
    private ClientPlayNetworkHandler networkHandler;

    @Shadow
    protected abstract void sendMovementPackets();

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V", ordinal = 0))
    private void onTickHasVehicleBeforeSendPackets(CallbackInfo info) {
        SendMovementPacketEvent.Pre sendMovementPacketPreEvent = new SendMovementPacketEvent.Pre();
        Charmonium.getInstance().eventManager.Fire(sendMovementPacketPreEvent);
    }

    @Inject(method = "sendMovementPackets", at = @At("HEAD"), cancellable = true)
    private void onSendMovementPacketsHead(CallbackInfo info) {
        SendMovementPacketEvent.Pre sendMovementPacketPreEvent = new SendMovementPacketEvent.Pre();
        Charmonium.getInstance().eventManager.Fire(sendMovementPacketPreEvent);
        if (sendMovementPacketPreEvent.isCancelled())
            info.cancel();
    }

    @Inject(method = "sendMovementPackets", at = @At("TAIL"), cancellable = true)
    private void onSendMovementPacketsTail(CallbackInfo info) {
        SendMovementPacketEvent.Post sendMovementPacketPostEvent = new SendMovementPacketEvent.Post();
        Charmonium.getInstance().eventManager.Fire(sendMovementPacketPostEvent);
        if (sendMovementPacketPostEvent.isCancelled())
            info.cancel();
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V", ordinal = 1, shift = At.Shift.AFTER))
    private void onTickHasVehicleAfterSendPackets(CallbackInfo info) {
        SendMovementPacketEvent.Post sendMovementPacketPostEvent = new SendMovementPacketEvent.Post();

        Charmonium.getInstance().eventManager.Fire(sendMovementPacketPostEvent);
    }
}
