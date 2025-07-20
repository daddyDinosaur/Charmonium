package com.charmonium.event.events;

import com.charmonium.event.listeners.AbstractListener;
import com.charmonium.event.listeners.SendPacketListener;
import net.minecraft.network.packet.Packet;

import java.util.ArrayList;

public class SendPacketEvent extends AbstractEvent {

    private final Packet<?> packet;

    public SendPacketEvent(Packet<?> packet) {
        this.packet = packet;
    }

    public Packet<?> GetPacket() {
        return packet;
    }

    @Override
    public void Fire(ArrayList<? extends AbstractListener> listeners) {
        for (AbstractListener listener : listeners) {
            SendPacketListener sendPacketListener = (SendPacketListener) listener;
            sendPacketListener.onSendPacket(this);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<SendPacketListener> GetListenerClassType() {
        return SendPacketListener.class;
    }
}
