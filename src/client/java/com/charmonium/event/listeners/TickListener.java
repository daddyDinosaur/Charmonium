package com.charmonium.event.listeners;

import com.charmonium.event.events.TickEvent;

public interface TickListener extends AbstractListener {
    void onTick(TickEvent.Pre event);
    void onTick(TickEvent.Post event);
}
