package com.charmonium.event.listeners;

import com.charmonium.event.events.SendMovementPacketEvent;

public interface SendMovementPacketListener extends AbstractListener {
    void onSendMovementPacket(SendMovementPacketEvent.Pre event);
    void onSendMovementPacket(SendMovementPacketEvent.Post event);
}
