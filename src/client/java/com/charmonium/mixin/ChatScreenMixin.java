package com.charmonium.mixin;

import com.charmonium.Charmonium;
import com.charmonium.CharmoniumClient;
import com.charmonium.managers.CommandManager;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public class ChatScreenMixin extends ScreenMixin {
    @Inject(method = "sendMessage", at = @At("HEAD"), cancellable = true)
    public void onSendMessage(String message, boolean addToHistory, CallbackInfo ci) {
        String prefix = "/char";
        if (message.startsWith(prefix)) {
            String withoutPrefix = message.substring(prefix.length()).trim();
            String[] args = withoutPrefix.isEmpty() ? new String[0] : withoutPrefix.split("\\s+");
            String[] input = new String[args.length + 1];
            input[0] = "char";
            System.arraycopy(args, 0, input, 1, args.length);

            Charmonium.getInstance().commandManager.executeCommand(input);
            ci.cancel();
        }
    }

    @Inject(at = {
            @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud;addToMessageHistory(Ljava/lang/String;)V", shift = At.Shift.AFTER) }, method = "sendMessage(Ljava/lang/String;Z)V", cancellable = true)
    public void addToHistory(String message, boolean addToHistory, CallbackInfo ci) {
        if (message.startsWith(CommandManager.PREFIX.getValue())) {
            Charmonium.getInstance().commandManager.executeCommand(message.split(" "));
            ci.cancel();
        }
    }
}
