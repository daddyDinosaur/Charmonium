package com.mylk.charmonium.feature.impl.charMods;

import java.util.HashSet;
import java.util.Set;

import com.mylk.charmonium.config.Config;
import com.mylk.charmonium.feature.impl.charMods.terminals.TerminalSolver;
import com.mylk.charmonium.handler.GameStateHandler;
import com.mylk.charmonium.handler.RotationHandler;
import com.mylk.charmonium.util.AngleUtils;
import com.mylk.charmonium.util.helper.Rotation;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLever;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.*;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class lightsDevice {
    private static lightsDevice instance;
    public static lightsDevice getInstance() {
        if (instance == null) {
            instance = new lightsDevice();
        }
        return instance;
    }
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Set<BlockPos> clicked = new HashSet<>();

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!Config.autoTerminals || !GameStateHandler.inDungeons || !GameStateHandler.inBoss || event.phase != TickEvent.Phase.START) return;
        BlockPos firstValidBlock = getFirstValidBlock();
        if (firstValidBlock != null) {
            // Get rotation
            AxisAlignedBB axisAlignedBB = mc.theWorld.getBlockState(firstValidBlock).getBlock().getSelectedBoundingBox(mc.theWorld, firstValidBlock);
            Rotation rotation = RotationHandler.getRotation(
                    mc.thePlayer.getPositionEyes(1f), new Vec3(
                            axisAlignedBB.minX + (axisAlignedBB.maxX - axisAlignedBB.minX) / 2.0,
                            axisAlignedBB.minY + (axisAlignedBB.maxY - axisAlignedBB.minY) / 2.0,
                            axisAlignedBB.minZ + (axisAlignedBB.maxZ - axisAlignedBB.minZ) / 2.0
                    )
            );
            MovingObjectPosition interactData = getInteractData(
                    firstValidBlock, rotation, mc.playerController.getBlockReachDistance()
            );
            if (interactData != null) {
                clickBlock(interactData.getBlockPos(), interactData.sideHit, interactData.hitVec);
                if (mc.theWorld.getBlockState(firstValidBlock).getBlock() == Blocks.lever) {
                    clickBlock(interactData.getBlockPos(), interactData.sideHit, interactData.hitVec);
                }
                clicked.add(firstValidBlock);
            }
        }
    }

    @SubscribeEvent
    public void onUnload(WorldEvent.Unload event) {
        clicked.clear();
    }

    private BlockPos getFirstValidBlock() {
        Vec3i vec3i = new Vec3i(7, 9, 7);
        for (BlockPos pos : BlockPos.getAllInBox(new BlockPos(mc.thePlayer).add(vec3i), new BlockPos(mc.thePlayer).subtract(vec3i))) {
            if (isValidBlock(pos) && inDistance(pos) && !clicked.contains(pos)) {
                return pos;
            }
        }
        return null;
    }

    private boolean isValidBlock(BlockPos blockPos) {
        Block block = mc.theWorld.getBlockState(blockPos).getBlock();
        return block == Blocks.lever && !mc.theWorld.getBlockState(blockPos).getValue(BlockLever.POWERED);
    }

    private boolean inDistance(BlockPos blockPos) {
        double dist = blockPos.distanceSq(
                mc.thePlayer.posX,
                mc.thePlayer.posY + mc.thePlayer.getEyeHeight(),
                mc.thePlayer.posZ
        );
        return dist < (mc.playerController.getBlockReachDistance() * mc.playerController.getBlockReachDistance());
    }

    private MovingObjectPosition getInteractData(BlockPos blockPos, Rotation rotation, float dist) {
        Vec3 eyePos = mc.thePlayer.getPositionEyes(1.0f);
        Vec3 lookVec = AngleUtils.getVectorForRotation(rotation.getPitch(), rotation.getYaw());
        Vec3 rayVec = eyePos.addVector(lookVec.xCoord * dist, lookVec.yCoord * dist, lookVec.zCoord * dist);
        return mc.theWorld.getBlockState(blockPos).getBlock().collisionRayTrace(mc.theWorld, blockPos, eyePos, rayVec);
    }

    private void clickBlock(BlockPos blockPos, EnumFacing enumFacing, Vec3 vec3) {
        mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem(), blockPos, enumFacing, vec3);
        mc.thePlayer.swingItem();
    }
}
