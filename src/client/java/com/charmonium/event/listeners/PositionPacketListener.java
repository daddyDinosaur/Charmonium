package com.charmonium.event.listeners;

import com.charmonium.event.events.PositionPacketEvent;

public interface PositionPacketListener extends AbstractListener {
    void onPositionPacket(PositionPacketEvent event);
}
