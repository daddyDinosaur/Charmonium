package com.mylk.charmonium.macro.impl;

import com.mojang.realmsclient.gui.ChatFormatting;
import com.mylk.charmonium.Charmonium;
import com.mylk.charmonium.config.Config;
import com.mylk.charmonium.config.aotv.AOTVWaypointsStructs;
import com.mylk.charmonium.event.BlockChangeEvent;
import com.mylk.charmonium.event.ReceivePacketEvent;
import com.mylk.charmonium.failsafe.FailsafeManager;
import com.mylk.charmonium.failsafe.impl.RotationFailsafe;
import com.mylk.charmonium.feature.impl.Scheduler;
import com.mylk.charmonium.feature.impl.SkillTracker;
import com.mylk.charmonium.handler.GameStateHandler;
import com.mylk.charmonium.handler.MacroHandler;
import com.mylk.charmonium.handler.RotationHandler;
import com.mylk.charmonium.macro.AbstractMacro;
import com.mylk.charmonium.macro.impl.misc.fuelFilling;
import com.mylk.charmonium.mixin.block.RenderGlobalAccessor;
import com.mylk.charmonium.mixin.client.PlayerControllerMPAccessor;
import com.mylk.charmonium.util.*;
import com.mylk.charmonium.util.charHelpers.GemstoneUtils;
import com.mylk.charmonium.util.charHelpers.VectorUtils;
import com.mylk.charmonium.util.helper.RotationConfiguration;
import com.mylk.charmonium.util.helper.Timer;
import net.minecraft.client.renderer.DestroyBlockProgress;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.server.S25PacketBlockBreakAnim;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;

import static net.minecraftforge.fml.common.eventhandler.EventPriority.HIGHEST;

public class
GemstoneMacro extends AbstractMacro {
    private static State currentState = State.NONE;
    private final RotationHandler rotation = RotationHandler.getInstance();
    private BlockPos targetGem;
    private static GemstoneMacro instance;
    private final Timer stuckMining = new Timer();
    private final Timer rotatingWait = new Timer();
    private final Timer teleportWait = new Timer();
    private final Timer itemSwap = new Timer();
    static float emptyTeleport = 0;
    HashSet<BlockPos> excludedBlocks = new HashSet<>();
    private int currentWaypoint = -1;
    private boolean refueling = false;
    public static boolean usingSpeed = false;
    public static String currentTool = "";
    Vec3 rotPos = new Vec3(0, 0, 0);
    private final Timer tpStuckTimer = new Timer();
    private final Timer breakTimer = new Timer();
    public static boolean tping = false;
    public static boolean stopChecks = false;
    public static boolean pickaxeSkillReady = true;
    public static boolean miningSpeedActive = false;
    //public static ItemStack miningTool;
    private static ArrayList<AOTVWaypointsStructs.Waypoint> Waypoints;

    public static GemstoneMacro getInstance() {
        if (instance == null) {
            instance = new GemstoneMacro();
        }
        return instance;
    }
    public void onEnable() {
        Config.MacroEnum crop = Config.MacroEnum.GEMSTONE;
        currentState = State.NONE;

        targetGem = null;
        GemstoneUtils.possibleBreaks.clear();
        GemstoneUtils.currentlyPossibleToSee.clear();
        excludedBlocks.clear();
        rotPos = new Vec3(0, 0, 0);
        emptyTeleport = 0;
        currentWaypoint = -1;
        rotation.reset();
        stuckMining.reset();
        rotatingWait.reset();
        teleportWait.reset();
        tpStuckTimer.reset();
        breakTimer.reset();
        tping = false;
        usingSpeed = false;
        stopChecks = false;
        currentTool = Config.gemstoneTool;

        Waypoints = Charmonium.aotvWaypoints.getSelectedRoute().waypoints;
        BlockPos currentPos = BlockUtils.fromVecToBP(Charmonium.mc.thePlayer.getPositionVector().subtract(0, 1, 0));

        LogUtils.sendDebug("Macro: " + crop);
        MacroHandler.getInstance().setCrop(crop);
        PlayerUtils.getTool();


        for (int i = 0; i < Waypoints.size(); i++) {
            BlockPos waypoint = new BlockPos(Waypoints.get(i).x, Waypoints.get(i).y, Waypoints.get(i).z);
            if (waypoint.equals(currentPos)) {
                currentWaypoint = i;
                break;
            }
        }

        if (currentWaypoint == -1) {
            Charmonium.sendMessage("Auto AOTV - You are not at a valid waypoint!");
            MacroHandler.getInstance().disableMacro();
            return;
        }

        currentState = State.NONE;

        super.onEnable();
    }

    public void invokeState() {
        assert mc.thePlayer != null;
        assert mc.theWorld != null;

        switch (currentState) {
            case NONE:
                currentState = State.MINING;
                break;

            case MINING:
                handleMiningState();
                break;

            case TELEPORT:
                ArrayList<AOTVWaypointsStructs.Waypoint> Waypoints = Charmonium.aotvWaypoints.getSelectedRoute().waypoints;
                BlockPos waypoint = new BlockPos(Waypoints.get(currentWaypoint).x, Waypoints.get(currentWaypoint).y, Waypoints.get(currentWaypoint).z);

                KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindAttack, false);
                currentTool = "Aspect of the";

                if (!rotation.isRotating()) {
                    rotation.easeTo(new RotationConfiguration(rotation.getRotation(waypoint), Config.gemstoneTeleportRotDelay, null));
                    if (!rotatingWait.isScheduled()) rotatingWait.schedule();
                }

                stopChecks = true;

                KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindSneak, true);

                if (!rotatingWait.hasPassed(Config.gemstoneTeleportDelay)) return;

                MovingObjectPosition movingObjectPosition = mc.thePlayer.rayTrace(55, 1);
                if (movingObjectPosition != null && movingObjectPosition.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {

                    if (movingObjectPosition.getBlockPos().equals(waypoint)) {

                        KeyBindUtils.rightClick();
                        Charmonium.sendMessage("AOTV Macro - Teleported to waypoint " + currentWaypoint);
                        tping = true;
                        tpStuckTimer.reset();
                        rotatingWait.reset();
                        teleportWait.schedule();
                        currentState = State.MINING;
                    } else {
                        if (tpStuckTimer.hasPassed(5000) && !rotation.isRotating()) {
                            Charmonium.sendMessage("AOTV Macro - Path is not cleared. Block: " + movingObjectPosition.getBlockPos().toString() + " is on the way.");
                            MacroHandler.getInstance().disableMacro();
                            break;
                        }
                    }
                } else if (movingObjectPosition != null) {
                    Charmonium.sendMessage("AOTV Macro - Something is on the way!");
                    MacroHandler.getInstance().disableMacro();
                }


                break;

            case WAIT:
                if (fuelFilling.isRefueling() && !refueling) {
                    refueling = true;
                    return;
                } else if (!fuelFilling.isRefueling() && refueling) {
                    refueling = false;
                    KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindAttack, false);
                    mc.mouseHelper.grabMouseCursor();
                    tpStuckTimer.reset();
                    tping = false;
                    currentState = State.MINING;
                    return;
                }
                break;
        }
    }

    private void handleMiningState() {
        if (GemstoneUtils.possibleBreaks.isEmpty()) {
            emptyTeleport++;
            if (emptyTeleport >= 75 && !tping && currentState != State.TELEPORT) {
                if (currentWaypoint == Waypoints.size() - 1) {
                    currentWaypoint = 0;
                } else {
                    currentWaypoint++;
                }
                currentState = State.TELEPORT;
                teleportWait.reset();
                emptyTeleport = 0;
            }
            GemstoneUtils.getAllBlocks();
            return;
        }

        GemstoneUtils.getAllBlocks();
        GemstoneUtils.updateListData(excludedBlocks);

        if (Config.refuelWithAbiphone) {
            if (fuelFilling.isRefueling()) {
                currentState = State.WAIT;
                return;
            }
        }

        if (GemstoneUtils.currentlyPossibleToSee.isEmpty()) {
            GemstoneUtils.possibleBreaks.clear();

            if (!teleportWait.hasPassed(2500)) return;
            if (currentWaypoint == Waypoints.size() - 1) {
                currentWaypoint = 0;
            } else {
                currentWaypoint++;
            }
            currentState = State.TELEPORT;
            teleportWait.reset();
            return;
        }

        if (targetGem == null) {
            stopChecks = false;
            findAndSetTargetGem();
        } else {
            emptyTeleport = 0;
            currentTool = Config.gemstoneTool;
            mineTargetGem();
        }
    }

    private void findAndSetTargetGem() {
        if (GemstoneUtils.currentlyPossibleToSee.isEmpty()) {
            Charmonium.sendMessage("§7§oNo gemstones found");
            return;
        }

        targetGem = GemstoneUtils.currentlyPossibleToSee.get(0);
        if (targetGem == null) {
            Charmonium.sendMessage("§7§oLooking for a gemstone [null]");
            GemstoneUtils.currentlyPossibleToSee.clear();
            return;
        }

        if (GemstoneUtils.currentlyPossibleToSee.size() > 1) {
            rotPos = VectorUtils.getClosestHittableToNextBlock(targetGem, GemstoneUtils.currentlyPossibleToSee.get(1));
        } else {
            rotPos = VectorUtils.getRandomHittable(targetGem);
        }

        excludedBlocks.clear();

        breakTimer.schedule();
        stuckMining.reset();
    }

    private void mineTargetGem() {
        if (Charmonium.mc.theWorld == null || Charmonium.mc.thePlayer == null) {
            return;
        }

        if (targetGem == null) {
            Charmonium.sendMessage("§7§oTarget gem is null, finding a new one");
            findAndSetTargetGem();
            return;
        }

        if (Charmonium.mc.theWorld.getBlockState(targetGem).getBlock() == Blocks.air) {
            targetGem = null;
            return;
        }

        if (rotPos == null || rotPos.equals(new Vec3(0, 0, 0))) {
            Charmonium.sendMessage("§7§orotPos is null or zero");
            targetGem = null;
            return;
        }

        if (breakTimer.hasPassed(Config.gemstoneStuckDelay)) {
            Charmonium.sendMessage("§7§oStuck mining...");
            if (mc.thePlayer.getPositionVector().distanceTo(BlockUtils.fromBPToVec(targetGem)) > mc.playerController.getBlockReachDistance()) {
                Charmonium.sendMessage("§7§oToo Far?");
                targetGem = null;
            }
            KeyBindUtils.releaseAll();
            KeyBindUtils.leftClick();
            KeyBindUtils.releaseAll();
            KeyBindUtils.leftClick();
            KeyBindUtils.releaseAll();
            breakTimer.schedule();
            return;
        }

        checkMiningSpeedBoost();

        //checkProgress(targetGem);

        if (!mc.thePlayer.getHeldItem().getDisplayName().contains(Config.gemstoneTool)) return;

        rotation.easeTo(new RotationConfiguration(rotation.getRotation(rotPos), Config.gemstoneRotationDelay, null));
        KeyBindUtils.holdThese(mc.gameSettings.keyBindAttack);
    }

    @SubscribeEvent
    public void onBlockChange(BlockChangeEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (!GameStateHandler.getInstance().atProperIsland() || !MacroHandler.getInstance().isMacroToggled() || MacroHandler.getInstance().getCrop() != Config.MacroEnum.GEMSTONE) return;

        BlockPos pos = event.pos;

        if (event.old.getBlock() == Blocks.cobblestone) {
            if (event.update.getBlock() == Blocks.air) {
                ArrayList<AOTVWaypointsStructs.Waypoint> Waypoints = Charmonium.aotvWaypoints.getSelectedRoute().waypoints;

                AOTVWaypointsStructs.Waypoint wp = Waypoints.stream().filter(waypoint -> waypoint.x == pos.getX() && waypoint.y == pos.getY() && waypoint.z == pos.getZ()).findFirst().orElse(null);
                if (wp != null) {
                    Charmonium.sendMessage("Cobblestone at waypoint " + EnumChatFormatting.BOLD + wp.name + EnumChatFormatting.RESET + EnumChatFormatting.RED + " has been destroyed!");

                    FailsafeManager.getInstance().possibleDetection(RotationFailsafe.getInstance());
                }
            }
        }
    }

    @SubscribeEvent(receiveCanceled=true, priority=HIGHEST)
    public void onMessageReceived(ClientChatReceivedEvent event) {
        String message = ChatFormatting.stripFormatting(event.message.getUnformattedText());
        try {
            if (message.contains(":") || message.contains(">")) return;
            if(message.startsWith("You used your Mining Speed Boost")) {
                pickaxeSkillReady = false;
                miningSpeedActive = true;
            } else if(message.endsWith("is now available!")) {
                pickaxeSkillReady = true;
            }
            if (message.endsWith("Speed Boost has expired!")) {
                miningSpeedActive = false;
            }
        } catch (Exception ignored) {}

        if (message.contains("Your pass to the Crystal Hollows will expire in 1 minute")) {
            if (Config.autoRenewCrystalHollowsPass) {
                Charmonium.sendMessage("Auto renewing Crystal Hollows pass");
                mc.thePlayer.sendChatMessage("/purchasecrystallhollowspass");
            }
        }
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (!GameStateHandler.getInstance().atProperIsland() || !MacroHandler.getInstance().isMacroToggled() || MacroHandler.getInstance().getCrop() != Config.MacroEnum.GEMSTONE) return;

        if (!GemstoneUtils.currentlyPossibleToSee.isEmpty()) {
            for (BlockPos blockPos : GemstoneUtils.currentlyPossibleToSee) {
//                if (blockPos.equals(targetGem)) {
//                    Charmonium.sendMessage("Not null");
//                    RenderUtils.drawBlockBox(targetGem, new Color(255, 0, 183, 120));
//                    RenderUtils.drawText("§l§3[§f " + String.format("%.2f", ((double)checkTime(targetGem) / 20.0D) - 555) + " §3]", targetGem.getX() + 0.5, targetGem.getY() + 1, targetGem.getZ() + 0.5, 1);
//                    return;
//                }

                RenderUtils.drawBlockBox(blockPos, new Color(32, 178, 170, 120));
            }
        }

    }

//    public void checkProgress(BlockPos blockPos) {
//        if (blockPos != null) {
//            for (DestroyBlockProgress var2 : ((RenderGlobalAccessor) mc.renderGlobal).getDamagedBlocks().values()) {
//                if (var2.getPosition().equals(blockPos) && var2.getPartialBlockDamage() >= Config.pingGlide / 10) {
//                    //excludedBlocks.add(targetGem);
//                    GemstoneUtils.currentlyPossibleToSee.remove(0); //try this??
//                    targetGem = null;
//                    break;
//                }
//            }
//        }
//    }

    public int checkTime(BlockPos blockpos) {
        float var2 = ((PlayerControllerMPAccessor)mc.playerController).getCurBlockDamageMP();
        if (var2 != 0.0F) {
            float var3 = mc.theWorld.getBlockState(blockpos).getBlock().getPlayerRelativeBlockHardness(mc.thePlayer, mc.thePlayer.worldObj, blockpos);
            if (var3 == 0.0F) {
                return 0;
            }

            return (int)Math.ceil((double)((1.0F - var2) / var3));
        }

        return 0;
    }

    public void checkMiningSpeedBoost() {
        if (Config.useMiningSpeedBoost && pickaxeSkillReady) {
            if (!itemSwap.isScheduled()) itemSwap.schedule();
            int slotCache = mc.thePlayer.inventory.currentItem;
            int targetSlot = Config.blueCheeseOmeletteToggle ? InventoryUtils.getItemInHotbarFromLore(true, "Blue Cheese") : PlayerUtils.getItemInHotbar("Pick", "Gauntlet", "Drill");

            if(targetSlot == -1) {
                Charmonium.sendMessage("Blue cheese drill not found. Disabled blue cheese swap");
                Config.blueCheeseOmeletteToggle = false;
                targetSlot = PlayerUtils.getItemInHotbar(true, "Pick", "Gauntlet", "Drill");
                if (targetSlot == -1) {
                    Charmonium.sendMessage("Pickaxe not found. Disabling mining speed boost");
                    Config.useMiningSpeedBoost = false;
                    return;
                }
            }

            usingSpeed = true;

            mc.thePlayer.inventory.currentItem = targetSlot;

            if (!itemSwap.hasPassed(100)) return;

            mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.inventory.getStackInSlot(targetSlot));

            if (itemSwap.hasPassed(200)) return;

            mc.thePlayer.inventory.currentItem = slotCache;

            pickaxeSkillReady = false;
            usingSpeed = false;
        }
    }

    public static String[] drawInfo() {
        if (SkillTracker.skillsInitialized()) {
            double xpToShow = SkillTracker.getText("Mining");
            int xpPerHour = (int) Math.round(xpToShow / ((SkillTracker.skillStopwatch.getTime() + 1) / 3600000d));
            int nxtLvl = Config.Skill_Mining + 1;

            return new String[]{
                    "§r§lStats:",
                    "§rMacro: §fGemstone Miner",
                    "§rTime: §f" + (Scheduler.getInstance().isRunning() ? Scheduler.getInstance().getStatusString() : "Macroing"),
                    "§rXP Earned: §f" + NumberFormat.getNumberInstance(Locale.US).format(xpToShow) + " [" + NumberFormat.getNumberInstance(Locale.US).format(xpPerHour) + "/hr]",
                    "§rTime til' Lvl. " + nxtLvl + ": §f" + SkillTracker.getTimeBetween(0, SkillTracker.xpLeft / (xpPerHour / 3600D)),
                    "§rState: §f" + currentState.name(),
            };
        }

        return new String[]{
                "§r§lStats:",
                "§rMacro: §fGemstone Miner",
                "§rTime: §f" + (Scheduler.getInstance().isRunning() ? Scheduler.getInstance().getStatusString() : "Macroing"),
                "§r Open '/skills' to track xp",
                "§rState: §f" + currentState.name(),
        };
    }

    public enum State {
        NONE,
        MINING,
        ON_BLOCK,
        TELEPORT,
        WAIT
    }
}
