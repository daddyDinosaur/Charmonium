package com.charmonium.event.listeners;

import com.charmonium.event.events.MouseMoveEvent;

public interface MouseMoveListener extends AbstractListener {
    void onMouseMove(MouseMoveEvent mouseMoveEvent);
}
