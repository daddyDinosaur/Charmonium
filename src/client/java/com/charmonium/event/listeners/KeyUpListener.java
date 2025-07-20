package com.charmonium.event.listeners;

import com.charmonium.event.events.KeyUpEvent;

public interface KeyUpListener extends AbstractListener {
    void onKeyUp(KeyUpEvent event);
}
