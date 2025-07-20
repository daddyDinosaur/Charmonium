package com.charmonium.event.listeners;

import com.charmonium.event.events.MouseScrollEvent;

public interface MouseScrollListener extends AbstractListener {
    void onMouseScroll(MouseScrollEvent event);
}
