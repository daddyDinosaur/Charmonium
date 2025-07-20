package com.charmonium.event.listeners;

import com.charmonium.event.events.ReceivePacketEvent;

public interface ReceivePacketListener extends AbstractListener {
    void onReceivePacket(ReceivePacketEvent readPacketEvent);
}