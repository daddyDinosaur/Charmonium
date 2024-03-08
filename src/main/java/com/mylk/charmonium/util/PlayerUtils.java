package com.mylk.charmonium.util;

import com.mylk.charmonium.config.Config;
import com.mylk.charmonium.failsafe.FailsafeManager;
import com.mylk.charmonium.handler.MacroHandler;
import com.mylk.charmonium.macro.impl.FishingMacro;
import com.mylk.charmonium.macro.impl.GemstoneMacro;
import com.mylk.charmonium.util.helper.Clock;
import net.minecraft.block.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

public class PlayerUtils {

    public static final Clock changeItemEveryClock = new Clock();
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static boolean itemChangedByStaff = false;

    public static boolean isInventoryEmpty(EntityPlayer player) {
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            if (player.inventory.getStackInSlot(i) != null) {
                return false;
            }
        }
        return true;
    }

    public static void getTool() {
        if (itemChangedByStaff) {
            LogUtils.sendDebug("Item changed by staff, not changing item");
            return;
        }

        if (changeItemEveryClock.isScheduled() && !changeItemEveryClock.passed()) {
            return;
        }

        changeItemEveryClock.schedule(500L);
        int id = PlayerUtils.getFarmingTool(MacroHandler.getInstance().getCrop(), true, false);
        if (id == -1) {
            LogUtils.sendDebug("No tool found!");
            return;
        }
        LogUtils.sendDebug("Tool id: " + id + " current item: " + mc.thePlayer.inventory.currentItem);
        if (id == mc.thePlayer.inventory.currentItem) return;
        mc.thePlayer.inventory.currentItem = id;
    }


    public static int getItemInHotbar(boolean returnErrorCode, final String... itemName) {
        for (int i = 0; i < 8; ++i) {
            final ItemStack is = mc.thePlayer.inventory.getStackInSlot(i);
            for(String s : itemName) {
                if (is != null && StringUtils.stripControlCodes(is.getDisplayName()).contains(s)) {
                    return i;
                }
            }
        }
        return returnErrorCode ? -1 : 0;
    }
    public static int getItemInHotbar(final String... itemName) {
        return getItemInHotbar(false, itemName);
    }

    public static int getFarmingTool(Config.MacroEnum crop) {
        return getFarmingTool(crop, false, false);
    }

    public static int getFarmingTool(Config.MacroEnum crop, boolean withError, boolean anyHoe) {
        if (crop == null) return withError ? -1 : 0;
        for (int i = 36; i < 44; i++) {
            if (mc.thePlayer.inventoryContainer.inventorySlots.get(i).getStack() != null) {
                String name = mc.thePlayer.inventoryContainer.inventorySlots.get(i).getStack().getDisplayName();
                if (anyHoe) {
                    if (name.contains("Hoe") || name.contains("Dicer") || name.contains("Chopper") || name.contains("Fungi") || name.contains("Knife")) {
                        return i - 36;
                    }
                    continue;
                }
                switch (crop) {
                    case FORAGING:
                        if (mc.thePlayer.inventoryContainer.inventorySlots.get(i).getStack().getDisplayName().contains("Treecapitator")) {
                            return i - 36;
                        }
                        if (mc.thePlayer.inventoryContainer.inventorySlots.get(i).getStack().getDisplayName().contains("Axe")) {
                            return i - 36;
                        }
                        continue;
                    case ICE_WALKER:
                        if (mc.thePlayer.inventoryContainer.inventorySlots.get(i).getStack().getDisplayName().contains("Pickaxe")) {
                            return i - 36;
                        }
                        continue;
                    case MITHRIL_MINER:
                        if (mc.thePlayer.inventoryContainer.inventorySlots.get(i).getStack().getDisplayName().contains(Config.getCommTool())) {
                            return i - 36;
                        }
                        continue;
                    case SLAYER:
                        if (mc.thePlayer.inventoryContainer.inventorySlots.get(i).getStack().getDisplayName().contains(Config.slayerWeaponName)) {
                            return i - 36;
                        }
                        continue;
                    case FISHING:
                        if (mc.thePlayer.inventoryContainer.inventorySlots.get(i).getStack().getDisplayName().contains(FishingMacro.currentTool)) {
                            return i - 36;
                        }
                        continue;
                    case GEMSTONE:
                        if (mc.thePlayer.inventoryContainer.inventorySlots.get(i).getStack().getDisplayName().contains(GemstoneMacro.currentTool)) {
                            return i - 36;
                        }
                        continue;
                }
            }
        }

        int gardeningHoe = InventoryUtils.getSlotIdOfItemInHotbar("Gardening Hoe");
        if (gardeningHoe != -1) {
            return gardeningHoe;
        }

        return withError ? -1 : 0;
    }

    public static boolean shouldPushBack() {
        if (FailsafeManager.getInstance().triggeredFailsafe.isPresent()) return false;
        float angle = AngleUtils.getClosest();
        double x = mc.thePlayer.posX % 1;
        double z = mc.thePlayer.posZ % 1;
        Block blockBehind = BlockUtils.getRelativeBlock(0, 0, -1);
        if (!(blockBehind.getMaterial().isSolid() || (blockBehind instanceof BlockSlab) || blockBehind.equals(Blocks.carpet) || (blockBehind instanceof BlockDoor)) || blockBehind.getMaterial().isLiquid())
            return false;
        if (angle == 0) {
            return (z > -0.65 && z < -0.1) || (z < 0.9 && z > 0.35);
        } else if (angle == 90) {
            return (x > -0.9 && x < -0.35) || (x < 0.65 && x > 0.1);
        } else if (angle == 180) {
            return (z > -0.9 && z < -0.35) || (z < 0.65 && z > 0.1);
        } else if (angle == 270) {
            return (x > -0.65 && x < -0.1) || (x < 0.9 && x > 0.35);
        }
        return false;
    }

    public static Entity getEntityCuttingOtherEntity(Entity e) {
        return getEntityCuttingOtherEntity(e, entity -> true);
    }

    public static Entity getEntityCuttingOtherEntity(Entity e, Predicate<Entity> predicate) {
        List<Entity> possible = mc.theWorld.getEntitiesInAABBexcluding(e, e.getEntityBoundingBox().expand(0.3D, 2.0D, 0.3D), a -> {
            boolean flag1 = (!a.isDead && !a.equals(mc.thePlayer));
            boolean flag2 = !(a instanceof net.minecraft.entity.projectile.EntityFireball);
            boolean flag3 = !(a instanceof net.minecraft.entity.projectile.EntityFishHook);
            boolean flag4 = predicate.test(a);
            return flag1 && flag2 && flag3 && flag4;
        });
        if (!possible.isEmpty())
            return Collections.min(possible, Comparator.comparing(e2 -> e2.getDistanceToEntity(e)));
        return null;
    }

    public static boolean isPlayerSuffocating() {
        AxisAlignedBB playerBB = mc.thePlayer.getEntityBoundingBox().expand(-0.15, -0.15, -0.15);
        List<AxisAlignedBB> collidingBoxes = mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, playerBB);
        return !collidingBoxes.isEmpty();
    }

    public static EnumFacing getHorizontalFacing(float yaw) {
        return EnumFacing.getHorizontal(MathHelper.floor_double((double) (yaw * 4.0F / 360.0F) + 0.5) & 3);
    }

    public static void closeScreen() {
        if (mc.currentScreen != null && mc.thePlayer != null) {
            mc.thePlayer.closeScreen();
        }
    }

    //================================================================
    //char helpers

    public static Vec3 playerLookVec() {
        return mc.thePlayer.getLookVec();
    }

    public static Vec3 playerEyePosVec() {
        return mc.thePlayer.getPositionEyes(1.0f);
    }

    public static boolean entityIsVisible(Entity entityToCheck) {
        return mc.thePlayer.canEntityBeSeen(entityToCheck);
    }

    public static Vec3 getSpawnLocation() {
        AtomicReference<Vec3> spawn = new AtomicReference<>(new Vec3(0, 0, 0));
        MacroHandler.getInstance().getCurrentMacro().ifPresent(cm -> spawn.set(cm.getSpawnPos()));
        return new Vec3(spawn.get().xCoord, + 0.5, spawn.get().yCoord);
    }
}
