package com.charmonium.event.listeners;

import com.charmonium.event.events.Render3DEvent;

public interface Render3DListener extends AbstractListener {
    void onRender(Render3DEvent event);
}
