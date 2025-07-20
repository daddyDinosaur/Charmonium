package com.charmonium.mixin;

import com.charmonium.Charmonium;
import com.charmonium.CharmoniumClient;
import com.charmonium.event.events.KeyDownEvent;
import com.charmonium.event.events.KeyUpEvent;
import net.minecraft.client.Keyboard;
import net.minecraft.client.gui.screen.ChatScreen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.event.KeyEvent;

import static com.charmonium.CharmoniumClient.mc;

@Mixin(Keyboard.class)
public class KeyboardMixin {

    @Inject(at = {@At("HEAD")}, method = {"onKey(JIIII)V"}, cancellable = true)
    private void OnKeyDown(long window, int key, int scancode,
                           int action, int modifiers, CallbackInfo ci) {
        CharmoniumClient charm = Charmonium.getInstance();

        if (action == GLFW.GLFW_PRESS) {
            if (charm != null &&charm.eventManager != null) {
                KeyDownEvent event = new KeyDownEvent(window, key, scancode, action, modifiers);

                charm.eventManager.Fire(event);

                if (event.isCancelled()) {
                    ci.cancel();
                }
            }

            if (mc.currentScreen == null && mc.getOverlay() == null) {
                if (key == KeyEvent.VK_PERIOD) {
                    mc.setScreen(new ChatScreen(""));
                }
            }
        } else if (action == GLFW.GLFW_RELEASE) {
            if (charm != null && charm.eventManager != null) {
                KeyUpEvent event = new KeyUpEvent(window, key, scancode, action, modifiers);

                charm.eventManager.Fire(event);

                if (event.isCancelled()) {
                    ci.cancel();
                }
            }
        }
    }
}
