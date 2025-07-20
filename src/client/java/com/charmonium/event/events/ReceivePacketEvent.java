package com.charmonium.event.events;

import com.charmonium.event.listeners.AbstractListener;
import com.charmonium.event.listeners.ReceivePacketListener;
import net.minecraft.network.packet.Packet;

import java.util.ArrayList;

public class ReceivePacketEvent extends AbstractEvent {

    private final Packet<?> packet;

    public Packet<?> GetPacket() {
        return packet;
    }

    public ReceivePacketEvent(Packet<?> packet) {
        this.packet = packet;
    }

    @Override
    public void Fire(ArrayList<? extends AbstractListener> listeners) {
        for (AbstractListener listener : listeners) {
            ReceivePacketListener readPacketListener = (ReceivePacketListener) listener;
            readPacketListener.onReceivePacket(this);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<ReceivePacketListener> GetListenerClassType() {
        return ReceivePacketListener.class;
    }
}