package com.charmonium.event.events;

import com.charmonium.event.listeners.AbstractListener;
import com.charmonium.event.listeners.MouseScrollListener;

import java.util.ArrayList;

public class MouseScrollEvent extends AbstractEvent {
    private final double horizontal;
    private final double vertical;

    public MouseScrollEvent(double horizontal2, double vertical2) {
        horizontal = horizontal2;
        vertical = vertical2;
    }

    public double GetVertical() {
        return vertical;
    }

    public double GetHorizontal() {
        return horizontal;
    }

    @Override
    public void Fire(ArrayList<? extends AbstractListener> listeners) {
        for (AbstractListener listener : listeners) {
            MouseScrollListener mouseScrollListener = (MouseScrollListener) listener;
            mouseScrollListener.onMouseScroll(this);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<MouseScrollListener> GetListenerClassType() {
        return MouseScrollListener.class;
    }
}
