package com.charmonium.event.listeners;

import com.charmonium.event.events.ParticleEvent;

public interface ParticleListener extends AbstractListener {
    void onParticle(ParticleEvent particleEvent);
}
