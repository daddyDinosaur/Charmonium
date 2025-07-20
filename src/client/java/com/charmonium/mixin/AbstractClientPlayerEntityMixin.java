package com.charmonium.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin extends PlayerEntityMixin {

}
