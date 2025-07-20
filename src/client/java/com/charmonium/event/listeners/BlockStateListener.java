package com.charmonium.event.listeners;

import com.charmonium.event.events.BlockStateEvent;

public interface BlockStateListener extends AbstractListener {
    void onBlockStateChanged(BlockStateEvent event);
}
