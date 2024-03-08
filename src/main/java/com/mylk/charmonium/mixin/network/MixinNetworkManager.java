package com.mylk.charmonium.mixin.network;

import com.mylk.charmonium.event.ReceivePacketEvent;
import com.mylk.charmonium.event.SendPacketEvent;
import io.netty.channel.*;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetworkManager.class)
public class MixinNetworkManager {
    @Inject(method = "channelRead0*", at = @At("HEAD"))
    private void read(ChannelHandlerContext context, Packet<?> packet, CallbackInfo callback) {
        if (packet.getClass().getSimpleName().startsWith("S")) {
            MinecraftForge.EVENT_BUS.post(new ReceivePacketEvent(packet));
        } else if (packet.getClass().getSimpleName().startsWith("C")) {
            MinecraftForge.EVENT_BUS.post(new SendPacketEvent(packet));
        }
    }
}
