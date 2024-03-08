package com.mylk.charmonium.event;

import lombok.Getter;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.eventhandler.Event;

@Getter
public class ChunkLoadEvent extends Event {
    private final Chunk chunk;

    public ChunkLoadEvent(Chunk chunk) {
        this.chunk = chunk;
    }
}
