package com.charmonium.event.events;

import com.charmonium.event.listeners.AbstractListener;
import com.charmonium.event.listeners.BlockStateListener;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;

public class BlockStateEvent extends AbstractEvent {
    private final BlockPos blockPos;
    private final BlockState blockState;
    private final BlockState previousBlockState;

    public BlockStateEvent(BlockPos blockPos, BlockState state, BlockState previousState) {
        this.blockPos = blockPos;
        blockState = state;
        previousBlockState = previousState;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public BlockState getBlockState() {
        return blockState;
    }

    public BlockState getPreviousBlockState() {
        return previousBlockState;
    }

    @Override
    public void Fire(ArrayList<? extends AbstractListener> listeners) {
        for (AbstractListener listener : listeners) {
            BlockStateListener blockStateListener = (BlockStateListener) listener;
            blockStateListener.onBlockStateChanged(this);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<BlockStateListener> GetListenerClassType() {
        return BlockStateListener.class;
    }
}
