package com.mylk.charmonium.mixin.render;

import com.mylk.charmonium.handler.RotationHandler;
import com.mylk.charmonium.mixin.client.EntityPlayerSPAccessor;
import com.mylk.charmonium.mixin.client.MinecraftAccessor;
import com.mylk.charmonium.util.helper.RotationConfiguration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ModelBiped.class, priority = Integer.MAX_VALUE)
public class MixinModelBiped {
    @Unique
    private final Minecraft Charmonium$mc = Minecraft.getMinecraft();
    @Shadow
    public ModelRenderer bipedHead;

    @Inject(method = {"setRotationAngles"}, at = {@At(value = "FIELD", target = "Lnet/minecraft/client/model/ModelBiped;swingProgress:F")})
    public void onSetRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn, CallbackInfo ci) {
        if (!RotationHandler.getInstance().isRotating() || RotationHandler.getInstance().getConfiguration() != null && RotationHandler.getInstance().getConfiguration().getRotationType() != RotationConfiguration.RotationType.SERVER)
            return;

        if (entityIn != null && entityIn.equals(Minecraft.getMinecraft().thePlayer)) {
            this.bipedHead.rotateAngleX = ((EntityPlayerSPAccessor) entityIn).getLastReportedPitch() / 57.295776f;
            float partialTicks = ((MinecraftAccessor) Charmonium$mc).getTimer().renderPartialTicks;
            float yawOffset = Charmonium$interpolateRotation(Charmonium$mc.thePlayer.prevRenderYawOffset, Charmonium$mc.thePlayer.renderYawOffset, partialTicks);
            float fakeHead = ((EntityPlayerSPAccessor) entityIn).getLastReportedYaw();
            float calcNetHead = fakeHead - yawOffset;
            calcNetHead = MathHelper.wrapAngleTo180_float(calcNetHead);

            bipedHead.rotateAngleY = calcNetHead / 57.295776f;
        }
    }

    @Unique
    protected float Charmonium$interpolateRotation(float par1, float par2, float par3) {
        float f;

        for (f = par2 - par1; f < -180.0F; f += 360.0F) {
        }

        while (f >= 180.0F) {
            f -= 360.0F;
        }

        return par1 + par3 * f;
    }
}
