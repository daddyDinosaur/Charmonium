package com.mylk.charmonium.feature.impl;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;

import java.util.ArrayList;
import java.util.List;

public class brush {
    private static final List<BlockPos> placedBlocks = new ArrayList<>();
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static MovingObjectPosition objectMouseOver;

    private static Entity renderViewEntity;
    public static PlayerControllerMP playerController;

    public static void setBlockInFrontOfPlayer(IBlockState blockState, boolean wildcard) {
        if (blockState == null) return;
        MovingObjectPosition hitResult = mc.thePlayer.rayTrace(5.0, 0.0F);
        if (hitResult.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            BlockPos blockPos = hitResult.getBlockPos().offset(hitResult.sideHit);
            mc.theWorld.setBlockState(blockPos, wildcard ? blockState.getBlock().getDefaultState() : blockState);
            placedBlocks.add(blockPos);
        }
    }

    public static void removeBlockInFrontOfPlayer() {
        if (mc.theWorld != null && mc.thePlayer != null) {
            MovingObjectPosition hitResult = mc.thePlayer.rayTrace(5.0, 0.0F);
            if (hitResult.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                BlockPos blockPos = hitResult.getBlockPos();

                mc.theWorld.setBlockState(blockPos, Block.getBlockById(0).getDefaultState());
            }
        }
    }

    public void deletePlacedBlocks() {
        if (mc.theWorld != null) {
            for (BlockPos blockPos : placedBlocks) {
                mc.theWorld.setBlockState(blockPos, Block.getBlockById(0).getDefaultState());
            }
        }
    }
}
