package com.charmonium.event.events;

import com.charmonium.event.listeners.AbstractListener;

import java.util.ArrayList;

public abstract class AbstractEvent {
    boolean isCancelled;

    public AbstractEvent() {
        isCancelled = false;
    }

    public boolean isCancelled() {
        return isCancelled;
    }

    public void cancel() {
        isCancelled = true;
    }

    public void setCancelled(boolean state) {
        isCancelled = state;
    }

    public abstract void Fire(ArrayList<? extends AbstractListener> listeners);

    public abstract <T extends AbstractListener> Class<T> GetListenerClassType();
}
