package com.charmonium.mixin;

import java.net.InetSocketAddress;

import com.charmonium.Charmonium;
import com.charmonium.event.events.ReceivePacketEvent;
import com.charmonium.event.events.SendPacketEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {

    @Inject(at = {
            @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;handlePacket(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;)V", ordinal = 0) }, method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;)V", cancellable = true)
    protected void onChannelRead(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo ci) {
        ReceivePacketEvent event = new ReceivePacketEvent(packet);
        Charmonium.getInstance().eventManager.Fire(event);
    }

    @Inject(at = @At("HEAD"), method = "send(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;Z)V", cancellable = true)
    private void onSend(Packet<?> packet, @Nullable PacketCallbacks callback, boolean flush, CallbackInfo ci) {
        SendPacketEvent event = new SendPacketEvent(packet);
        Charmonium.getInstance().eventManager.Fire(event);

        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}