package com.charmonium.event.events;

import com.charmonium.event.listeners.AbstractListener;
import com.charmonium.event.listeners.Render3DListener;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;

import java.util.ArrayList;

public class Render3DEvent extends AbstractEvent {
    MatrixStack matrices;
    Frustum frustum;
    RenderTickCounter renderTickCounter;
    Camera camera;

    public MatrixStack GetMatrix() {
        return matrices;
    }

    public RenderTickCounter getRenderTickCounter() {
        return renderTickCounter;
    }

    public Frustum getFrustum() {
        return frustum;
    }

    public Camera getCamera() {
        return camera;
    }

    public Render3DEvent(MatrixStack matrix4f, Frustum frustum, Camera camera, RenderTickCounter renderTickCounter) {
        matrices = matrix4f;
        this.renderTickCounter = renderTickCounter;
        this.frustum = frustum;
        this.camera = camera;
    }

    @Override
    public void Fire(ArrayList<? extends AbstractListener> listeners) {
        for (AbstractListener listener : listeners) {
            Render3DListener renderListener = (Render3DListener) listener;
            renderListener.onRender(this);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<Render3DListener> GetListenerClassType() {
        return Render3DListener.class;
    }
}