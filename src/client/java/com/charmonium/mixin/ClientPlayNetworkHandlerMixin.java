package com.charmonium.mixin;

import com.charmonium.Charmonium;
import com.charmonium.event.events.PositionPacketEvent;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(at = @At("HEAD"), method = "onPlayerPositionLook")
    private void onPlayerPositionLook(PlayerPositionLookS2CPacket packet, CallbackInfo ci) {
        double x = packet.change().position().getX();
        double y = packet.change().position().getY();
        double z = packet.change().position().getZ();
        PositionPacketEvent event = new PositionPacketEvent(x, y, z);
        Charmonium.getInstance().eventManager.Fire(event);
    }
}
