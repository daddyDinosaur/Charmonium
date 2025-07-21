package com.charmonium.event.events;

import com.charmonium.event.listeners.AbstractListener;
import com.charmonium.event.listeners.PositionPacketListener;

import java.util.ArrayList;

public class PositionPacketEvent extends AbstractEvent {
    public final double x, y, z;

    public PositionPacketEvent(double x, double y, double z) {
        this.x = x; this.y = y; this.z = z;
    }

    @Override
    public void Fire(ArrayList<? extends AbstractListener> listeners) {
        for (AbstractListener listener : listeners) {
            if (listener instanceof PositionPacketListener) {
                ((PositionPacketListener) listener).onPositionPacket(this);
            }
        }
    }

    @Override
    public Class<PositionPacketListener> GetListenerClassType() {
        return PositionPacketListener.class;
    }
}
