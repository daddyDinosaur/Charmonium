package com.charmonium.event.listeners;

import com.charmonium.event.events.MouseClickEvent;

public interface MouseClickListener extends AbstractListener {
    void onMouseClick(MouseClickEvent mouseClickEvent);
}
