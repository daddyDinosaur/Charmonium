package com.charmonium.event.listeners;

import com.charmonium.event.events.ItemUsedEvent;

public interface ItemUsedListener extends AbstractListener {
    void onItemUsed(ItemUsedEvent.Pre event);

    void onItemUsed(ItemUsedEvent.Post event);
}
