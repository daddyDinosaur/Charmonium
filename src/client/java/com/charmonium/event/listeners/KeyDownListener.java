package com.charmonium.event.listeners;

import com.charmonium.event.events.KeyDownEvent;

public interface KeyDownListener extends AbstractListener {
    void onKeyDown(KeyDownEvent event);
}
