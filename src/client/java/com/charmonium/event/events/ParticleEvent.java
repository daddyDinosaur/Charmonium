package com.charmonium.event.events;

import com.charmonium.event.listeners.AbstractListener;
import com.charmonium.event.listeners.ParticleListener;
import net.minecraft.particle.ParticleEffect;

import java.util.ArrayList;

public class ParticleEvent extends AbstractEvent {
    private final ParticleEffect particleEffect;

    public ParticleEvent(ParticleEffect particleEffect) {
        this.particleEffect = particleEffect;
    }

    public ParticleEffect getParticleEffect() {
        return particleEffect;
    }

    @Override
    public void Fire(ArrayList<? extends AbstractListener> listeners) {
        for (AbstractListener listener : listeners) {
            ParticleListener particleListener = (ParticleListener) listener;
            particleListener.onParticle(this);
        }
    }

    @Override
    public Class<ParticleListener> GetListenerClassType() {
        return ParticleListener.class;
    }
}
