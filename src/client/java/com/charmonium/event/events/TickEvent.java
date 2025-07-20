package com.charmonium.event.events;

import com.charmonium.event.listeners.AbstractListener;
import com.charmonium.event.listeners.TickListener;

import java.util.ArrayList;
import java.util.List;

public class TickEvent {
    public static class Pre extends AbstractEvent {
        @Override
        public void Fire(ArrayList<? extends AbstractListener> listeners) {
            for (AbstractListener listener : List.copyOf(listeners)) {
                TickListener tickListener = (TickListener) listener;
                tickListener.onTick(this);
            }
        }

        @Override
        public Class<TickListener> GetListenerClassType() {
            return TickListener.class;
        }
    }

    public static class Post extends AbstractEvent {
        @Override
        public void Fire(ArrayList<? extends AbstractListener> listeners) {
            new ArrayList<>(listeners).forEach(listener -> {
                if (listener instanceof TickListener) {
                    ((TickListener) listener).onTick(this);
                }
            });
        }

        @Override
        public Class<TickListener> GetListenerClassType() {
            return TickListener.class;
        }
    }
}