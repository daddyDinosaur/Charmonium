package com.charmonium.mixin;

import com.charmonium.Charmonium;
import com.charmonium.event.events.Render3DEvent;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Shadow
    private Frustum frustum;

    @Inject(at = @At("TAIL"), method = "render(Lnet/minecraft/client/util/ObjectAllocator;Lnet/minecraft/client/render/RenderTickCounter;ZLnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/GameRenderer;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V", cancellable = false)
    public void render(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline,
                       Camera camera, GameRenderer gameRenderer, Matrix4f positionMatrix, Matrix4f projectionMatrix,
                       CallbackInfo ci) {

        if (Charmonium.getInstance().moduleManager != null) {
            GL11.glEnable(GL11.GL_LINE_SMOOTH);

            RenderSystem.getModelViewStack().pushMatrix().mul(positionMatrix);

            MatrixStack matrixStack = new MatrixStack();
            Render3DEvent renderEvent = new Render3DEvent(matrixStack, frustum, camera, tickCounter);
            Charmonium.getInstance().eventManager.Fire(renderEvent);

            RenderSystem.getModelViewStack().popMatrix();

            GL11.glDisable(GL11.GL_LINE_SMOOTH);

        }
    }
/*
    @Inject(at = @At("HEAD"), method = "hasBlindnessOrDarkness(Lnet/minecraft/client/render/Camera;)Z", cancellable = true)
    private void onHasBlindnessOrDarknessEffect(Camera camera, CallbackInfoReturnable<Boolean> cir) {
        if (Charmonium.getInstance().moduleManager.norender.state.getValue()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(at = @At("HEAD"), method = "drawBlockOutline(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/entity/Entity;DDDLnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)V", cancellable = true)
    private void onDrawBlockOutline(MatrixStack matrices, VertexConsumer vertexConsumer, Entity entity, double cameraX,
                                    double cameraY, double cameraZ, BlockPos pos, BlockState state, int color, CallbackInfo ci) {
        if (Charmonium.getInstance().moduleManager.freecam.state.getValue())
            ci.cancel();
    }*/
}
