package com.charmonium.settings.types;

import com.charmonium.settings.Setting;
import net.minecraft.block.Block;
import java.util.HashSet;
import java.util.function.Consumer;

public class BlocksSetting extends Setting<HashSet<Block>> {
    protected BlocksSetting(String ID, String displayName, String description, HashSet<Block> default_value, Consumer<HashSet<Block>> onUpdate) {
        super(ID, displayName, description, default_value, onUpdate);
        type = TYPE.BLOCKS;
    }

    @Override
    protected boolean isValueValid(HashSet<Block> value) {
        return true;
    }

    public static BUILDER builder() {
        return new BUILDER();
    }

    public static class BUILDER extends Setting.BUILDER<BUILDER, BlocksSetting, HashSet<Block>> {
        protected BUILDER() {
        }

        @Override
        public BlocksSetting build() {
            return new BlocksSetting(id, displayName, description, defaultValue, onUpdate);
        }
    }
}