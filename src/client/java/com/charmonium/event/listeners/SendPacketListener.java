package com.charmonium.event.listeners;

import com.charmonium.event.events.SendPacketEvent;

public interface SendPacketListener extends AbstractListener {
    void onSendPacket(SendPacketEvent event);
}
