package com.mylk.charmonium.mixin.client;

import com.mylk.charmonium.config.Config;
import com.mylk.charmonium.failsafe.FailsafeManager;
import com.mylk.charmonium.handler.MacroHandler;
import com.mylk.charmonium.util.helper.AudioManager;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.audio.SoundPoolEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundManager.class)
public class MixinSoundManager {
    @Inject(method = "getNormalizedVolume", at = @At("RETURN"), cancellable = true)
    private void getNormalizedVolume(ISound sound, SoundPoolEntry entry, SoundCategory category, CallbackInfoReturnable<Float> cir) {
        if (MacroHandler.getInstance().isMacroToggled() && Config.muteTheGame && !AudioManager.getInstance().isSoundPlaying() && !FailsafeManager.getInstance().getChooseEmergencyDelay().isScheduled() && !FailsafeManager.getInstance().triggeredFailsafe.isPresent()) {
            cir.setReturnValue(0f);
        }
    }
}
