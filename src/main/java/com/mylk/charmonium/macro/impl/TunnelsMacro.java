package com.mylk.charmonium.macro.impl;

import com.mojang.realmsclient.gui.ChatFormatting;
import com.mylk.charmonium.Charmonium;
import com.mylk.charmonium.config.Config;
import com.mylk.charmonium.config.aotv.AOTVWaypointsStructs;
import com.mylk.charmonium.config.page.GemstoneDelaysPage;
import com.mylk.charmonium.event.BlockChangeEvent;
import com.mylk.charmonium.failsafe.FailsafeManager;
import com.mylk.charmonium.failsafe.impl.RotationFailsafe;
import com.mylk.charmonium.feature.impl.Scheduler;
import com.mylk.charmonium.feature.impl.SkillTracker;
import com.mylk.charmonium.handler.GameStateHandler;
import com.mylk.charmonium.handler.MacroHandler;
import com.mylk.charmonium.handler.RotationHandler;
import com.mylk.charmonium.macro.AbstractMacro;
import com.mylk.charmonium.macro.impl.misc.fuelFilling;
import com.mylk.charmonium.macro.impl.misc.routes;
import com.mylk.charmonium.mixin.block.RenderGlobalAccessor;
import com.mylk.charmonium.util.*;
import com.mylk.charmonium.util.charHelpers.TunnelUtils;
import com.mylk.charmonium.util.charHelpers.VectorUtils;
import com.mylk.charmonium.util.charHelpers.npcUtils;
import com.mylk.charmonium.util.helper.Rotation;
import com.mylk.charmonium.util.helper.RotationConfiguration;
import com.mylk.charmonium.util.helper.Timer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DestroyBlockProgress;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.*;
import java.text.NumberFormat;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static net.minecraftforge.fml.common.eventhandler.EventPriority.HIGHEST;

public class
TunnelsMacro extends AbstractMacro {
    private enum State { NONE, UPDATE, GET_GEMSTONE, MINE, TELEPORT, WALK, AT_SPAWN, WAIT, INCREASE, ADJUST }

    private static State currentState = State.NONE;
    private final RotationHandler rotation = RotationHandler.getInstance();
    public static BlockPos targetGem;
    private BlockPos nextWaypoint;
    private static TunnelsMacro instance;

    private final Timer stuckMining = new Timer();
    private final Timer rotatingWait = new Timer();
    private final Timer teleportWait = new Timer();
    private final Timer itemSwap = new Timer();
    private final Timer tpStuckTimer = new Timer();
    private final Timer breakTimer = new Timer();

    private static float emptyTeleport = 0;
    private final Set<BlockPos> excludedBlocks = new HashSet<>();
    private int currentWaypoint = -1;
    private boolean refueling = false;
    public static boolean usingSpeed = false;
    public static String currentTool = "";
    private Vec3 rotPos = new Vec3(0, 0, 0);
    private Vec3 teleportRotPos = new Vec3(0, 0, 0);
    public static boolean tping = false;
    public static boolean atSpawn = false;
    public static boolean stopChecks = false;
    public static boolean pickaxeSkillReady = true;
    public static boolean miningSpeedActive = false;

    private static List<AOTVWaypointsStructs.Waypoint> waypoints;

    public static TunnelsMacro getInstance() {
        if (instance == null) {
            instance = new TunnelsMacro();
        }
        return instance;
    }

    @Override
    public void onEnable() {
        Config.MacroEnum crop = Config.MacroEnum.TUNNELS;
        resetState();
        LogUtils.sendDebug("Macro: " + crop);
        MacroHandler.getInstance().setCrop(crop);
        PlayerUtils.getTool();
        initializeWaypoint();
        super.onEnable();
    }

    private void resetState() {
        currentState = State.NONE;
        targetGem = null;
        TunnelUtils.possibleBreaks.clear();
        TunnelUtils.currentlyPossibleToSee.clear();
        excludedBlocks.clear();
        rotPos =new Vec3(0, 0, 0);
        teleportRotPos = new Vec3(0, 0, 0);
        emptyTeleport = 0;
        currentWaypoint = -1;
        rotation.reset();
        resetTimers();
        tping = false;
        usingSpeed = false;
        stopChecks = false;
        atSpawn = false;
        currentTool = Config.gemstoneTool;
        waypoints = Charmonium.aotvWaypoints.getSelectedRoute().waypoints;
    }

    private void resetTimers() {
        stuckMining.reset();
        rotatingWait.reset();
        teleportWait.reset();
        tpStuckTimer.reset();
        breakTimer.reset();
    }

    private void initializeWaypoint() {

        //Loop Process:
        //step 1:
        // check if at spawn
        // if at spawn, do case AT_SPAWN:
        // if not, walk to closest waypoint

        // check umber
        //  this needs to basically be adjusted so it checks if blocks are mined at the umber spots
        //  then 'follow the route' of the closest unmined spot
        //  if all spots are mined, swap lobbies with '/is' and '/warp camp'

        // move to
        //  after one is found, next function will use mix of teleport and walk to get to the spot

        // mine
        //  then mine umber

        // check if next location is bedrock, if it is, skip it

        // repeat from move to

        //blocks to mine (these are umber blocks):
        // minecraft:hardened_clay
        // minecraft:hardened_clay, color: brown
        // minecraft:double_stone_slab2, seamless: true, variant: red_sandstone

        BlockPos currentPos = BlockUtils.fromVecToBP(Charmonium.mc.thePlayer.getPositionVector());
        double closestDistance = Double.MAX_VALUE;
        int closestWaypoint = -1;

        for (int i = 0; i < waypoints.size(); i++) {
            BlockPos waypointPos = new BlockPos(waypoints.get(i).x, waypoints.get(i).y, waypoints.get(i).z);
            double distance = BlockUtils.distanceFromTo(currentPos, waypointPos);

            if (distance <= 10 && distance < closestDistance) {
                closestDistance = distance;
                closestWaypoint = i;
            }
        }

        if (closestWaypoint == -1) {
            Charmonium.sendMessage("Auto AOTV - No valid waypoint found within 10 blocks!");
            mc.thePlayer.sendChatMessage("/warp camp");
            currentState = State.AT_SPAWN;
            currentWaypoint = 0;
            atSpawn = true;
            // MacroHandler.getInstance().disableMacro();
        } else {
            currentWaypoint = closestWaypoint;
            Charmonium.sendMessage("Auto AOTV - Closest waypoint found: " + currentWaypoint);
            currentState = State.NONE;
            atSpawn = false;
            MacroHandler.walkTo(new BlockPos(waypoints.get(currentWaypoint).x, waypoints.get(currentWaypoint).y + 1, waypoints.get(currentWaypoint).z));
        }
    }

    public void invokeState() {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        switch (currentState) {
            case ADJUST:
                if (!MacroHandler.walker.isDone()) return;
                if (mc.thePlayer.getPositionVector().distanceTo(new Vec3(waypoints.get(currentWaypoint).x + 0.5, waypoints.get(currentWaypoint).y + 1, waypoints.get(currentWaypoint).z + 0.5)) < 0.7) {
                    currentState = State.INCREASE;
                }
                if (mc.thePlayer.getPositionVector().distanceTo(new Vec3(waypoints.get(currentWaypoint).x + 0.5, waypoints.get(currentWaypoint).y + 1, waypoints.get(currentWaypoint).z + 0.5)) > 4) {
                    Charmonium.sendMessage("Adjusting w/ walk to: " + waypoints.get(currentWaypoint).x + " " + waypoints.get(currentWaypoint).y + " " + waypoints.get(currentWaypoint).z);
                    MacroHandler.walkTo(new BlockPos(waypoints.get(currentWaypoint).x, waypoints.get(currentWaypoint).y + 1, waypoints.get(currentWaypoint).z));
                } else {
                    BlockPos waypoint = new BlockPos(waypoints.get(currentWaypoint).x, waypoints.get(currentWaypoint).y + 0.5, waypoints.get(currentWaypoint).z);
                    KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindAttack, false);
                    currentTool = "Aspect of the";
                    if (!rotation.isRotating()) {
                        Rotation newRot = rotation.getRotation(waypoint);
                        if (newRot != null) {
                            stopChecks = true;
                            rotation.easeTo(new RotationConfiguration(newRot, GemstoneDelaysPage.gemstoneTeleportRotDelay, null));
                            if (!rotatingWait.isScheduled()) rotatingWait.schedule();
                            if (!rotatingWait.hasPassed(GemstoneDelaysPage.gemstoneTeleportDelay)) return;
                            KeyBindUtils.rightClick();
                            Charmonium.sendMessage("Adjusted w/ AOTV to " + currentWaypoint);
                        }
                    }
                }
                break;
            case INCREASE:
                Charmonium.sendMessage("Increasing currentWaypoint");
                currentWaypoint = (currentWaypoint == waypoints.size() - 1) ? 0 : currentWaypoint + 1;
                currentState = State.NONE;
                break;
            case NONE:
                stopChecks = false;
                currentState = State.UPDATE;
                teleportRotPos = new Vec3(0, 0, 0);
                break;
            case AT_SPAWN:
                if (Charmonium.mc.thePlayer.getPositionVector().distanceTo(new Vec3(0, 128, 200)) > 5) return;

                Charmonium.sendMessage("Arrived at camp base");

                if (MacroHandler.walker.isDone() && atSpawn) {
                    atSpawn = false;
                    MacroHandler.walkTo(new BlockPos(waypoints.get(currentWaypoint).x, waypoints.get(currentWaypoint).y + 1, waypoints.get(currentWaypoint).z));
                    currentWaypoint++;
                    currentState = State.NONE;
                }
            case UPDATE:
                if (MacroHandler.walker.isDone()) {
                    handleUpdateState();
                }
                break;
            case GET_GEMSTONE:
                handleGetGemstoneState();
                break;
            case MINE:
                handleMineState();
                break;
            case TELEPORT:
                currentState = State.WALK;
                //handleTeleportState();
                break;
            case WAIT:
                handleWaitState();
                break;
            case WALK:
                if (MacroHandler.walker.isDone()) {
//                    if (!checks()) {
//                        Charmonium.sendMessage("Failed walk checks");
//                        tpStuckTimer.reset();
//                        rotatingWait.reset();
//                        teleportWait.schedule();
//                        currentState = State.GET_GEMSTONE;
//                        return;
//                    }

                    Charmonium.sendMessage("Walking to: " + waypoints.get(currentWaypoint).x + " " + waypoints.get(currentWaypoint).y + " " + waypoints.get(currentWaypoint).z);
                    MacroHandler.walkTo(new BlockPos(waypoints.get(currentWaypoint).x, waypoints.get(currentWaypoint).y + 1, waypoints.get(currentWaypoint).z));
                    currentState = State.ADJUST;
                }
                break;
        }
    }

    private void handleUpdateState() {
        if (TunnelUtils.possibleBreaks.isEmpty()) {
            handleEmptyPossibleBreaks();
        } else {
            TunnelUtils.getAllBlocks();
            TunnelUtils.updateListData((HashSet<BlockPos>) excludedBlocks);
            if (!checks()) {
                currentState = State.GET_GEMSTONE;
            }
        }
    }

    private void handleEmptyPossibleBreaks() {
        if (excludedBlocks.isEmpty()) {
            emptyTeleport++;
            if (emptyTeleport >= 75 && !tping) {
                currentWaypoint = (currentWaypoint + 1) % waypoints.size();
                teleportRotPos = new Vec3(0, 0, 0);
                currentState = State.TELEPORT;
                teleportWait.reset();
                emptyTeleport = 0;
            }
        } else {
            excludedBlocks.clear();
        }
        TunnelUtils.getAllBlocks();
    }

    private void handleGetGemstoneState() {
        if (checks()) return;
        if (TunnelUtils.currentlyPossibleToSee.isEmpty()) {
            Charmonium.sendMessage("§7§oNo umber found");
            return;
        }
        targetGem = TunnelUtils.currentlyPossibleToSee.get(0);
        if (targetGem == null) {
            Charmonium.sendMessage("§7§oLooking for a umber [null]");
            TunnelUtils.currentlyPossibleToSee.clear();
            return;
        }
//        rotPos = TunnelUtils.currentlyPossibleToSee.size() > 1
//                ? VectorUtils.getClosestHittableToNextBlock(targetGem, TunnelUtils.currentlyPossibleToSee.get(1))
//                : VectorUtils.getRandomHittable(targetGem);
        rotPos = VectorUtils.getRandomHittable(targetGem);
        excludedBlocks.clear();
        breakTimer.schedule();
        stuckMining.reset();
        currentState = State.MINE;
    }

    private void handleMineState() {
        emptyTeleport = 0;
        currentTool = Config.gemstoneTool;
        if (targetGem == null || Charmonium.mc.theWorld.getBlockState(targetGem).getBlock() == Blocks.air || Charmonium.mc.theWorld.getBlockState(targetGem).getBlock() == Blocks.bedrock) {
            currentState = State.UPDATE;
            KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindAttack, false);
            return;
        }
        if (rotPos == null || rotPos.equals(new Vec3(0, 0, 0))) {
            Charmonium.sendMessage("§7§orotPos is null or zero");
            excludedBlocks.add(targetGem);
            targetGem = null;
            return;
        }
        if (breakTimer.hasPassed(GemstoneDelaysPage.gemstoneStuckDelay)) {
            handleStuckMining();
            return;
        }
        checkMiningSpeedBoost();
        if (TunnelUtils.currentlyPossibleToSee.size() > 1 && Config.pingGlide != 0) {
            checkProgress(targetGem);
        }
        if (!mc.thePlayer.getHeldItem().getDisplayName().contains(Config.gemstoneTool)) return;
        Rotation rotationCheck = rotation.getRotation(rotPos);
        if (!isValidRotation(rotationCheck)) {
            Charmonium.sendMessage("§7§oToo Big Rotation");
            targetGem = null;
            return;
        }
        rotation.easeTo(new RotationConfiguration(rotationCheck, GemstoneDelaysPage.gemstoneRotationDelay, null));
        KeyBindUtils.holdThese(mc.gameSettings.keyBindAttack);
    }

    private void handleStuckMining() {
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
        currentState = State.UPDATE;
    }

    private boolean isValidRotation(Rotation rotation) {
        return Math.abs(rotation.getPitch()) <= 360 && Math.abs(rotation.getYaw()) <= 360;
    }

    private void handleTeleportState() {
        BlockPos waypoint = new BlockPos(waypoints.get(currentWaypoint).x, waypoints.get(currentWaypoint).y, waypoints.get(currentWaypoint).z);
        KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindAttack, false);
        currentTool = "Aspect of the";
        if (!rotation.isRotating() && teleportRotPos.equals(new Vec3(0, 0, 0))) {
            teleportRotPos = VectorUtils.getClosestHittableToMiddleTeleport(waypoint);
            if (teleportRotPos == null) {
                Charmonium.sendMessage("§7§oWaypoint isnt visible... walking to it");
                rotatingWait.reset();
                teleportRotPos = new Vec3(0, 0, 0);
                stopChecks = false;

                currentState = State.WALK;

                return;
            }
            rotation.easeTo(new RotationConfiguration(rotation.getRotation(teleportRotPos), GemstoneDelaysPage.gemstoneTeleportRotDelay, null));
            if (!rotatingWait.isScheduled()) rotatingWait.schedule();
        }
        stopChecks = true;
        KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindSneak, true);
        if (!rotatingWait.hasPassed(GemstoneDelaysPage.gemstoneTeleportDelay)) return;
        MovingObjectPosition movingObjectPosition = mc.thePlayer.rayTrace(55, 1);
        if (movingObjectPosition != null && movingObjectPosition.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            if (movingObjectPosition.getBlockPos().equals(waypoint)) {
                KeyBindUtils.rightClick();
                Charmonium.sendMessage("AOTV Macro - Teleported to waypoint " + currentWaypoint);
                tping = true;
                tpStuckTimer.reset();
                rotatingWait.reset();
                teleportWait.schedule();
                teleportRotPos = new Vec3(0, 0, 0);
                currentState = State.NONE;
            } else if (tpStuckTimer.hasPassed(5000) && !rotation.isRotating()) {
                Charmonium.sendMessage("AOTV Macro - Path is not cleared. Block: " + movingObjectPosition.getBlockPos().toString() + " is on the way.");
                currentState = State.WALK;
            }
        } else if (movingObjectPosition != null) {
            Charmonium.sendMessage("AOTV Macro - Something is on the way!");
            rotatingWait.reset();
            teleportRotPos = new Vec3(0, 0, 0);
            stopChecks = false;
        }
    }

    private void handleWaitState() {
        if (fuelFilling.isRefueling() && !refueling) {
            refueling = true;
        } else if (!fuelFilling.isRefueling() && refueling) {
            refueling = false;
            KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindAttack, false);
            mc.mouseHelper.grabMouseCursor();
            tpStuckTimer.reset();
            tping = false;
            currentState = State.NONE;
        }
    }

    private boolean checks() {
        if (Config.refuelWithAbiphone && fuelFilling.isRefueling()) {
            currentState = State.WAIT;
            return true;
        }

        if (TunnelUtils.currentlyPossibleToSee.isEmpty()) {
            Charmonium.sendMessage("[checks]§7§oNo umber found");
            TunnelUtils.possibleBreaks.clear();

            if (!teleportWait.hasPassed(2500)) return true;

            currentWaypoint = (currentWaypoint == waypoints.size() - 1) ? 0 : currentWaypoint + 1;

            teleportRotPos = new Vec3(0, 0, 0);
            currentState = State.TELEPORT;
            teleportWait.reset();

            return true;
        }

        return false;
    }

    public void checkProgress(BlockPos blockPos) {
        if (blockPos == null) return;

        Map<Integer, DestroyBlockProgress> damagedBlocks = ((RenderGlobalAccessor) mc.renderGlobal).getDamagedBlocks();

        for (DestroyBlockProgress destroyProgress : damagedBlocks.values()) {
            if (destroyProgress.getPosition().equals(blockPos) &&
                    destroyProgress.getPartialBlockDamage() >= Config.pingGlide / 10) {

                excludedBlocks.add(targetGem);
                targetGem = null;
                break;
            }
        }
    }

    public void checkMiningSpeedBoost() {
        if (!Config.useMiningSpeedBoost || !pickaxeSkillReady) return;

        if (!itemSwap.isScheduled()) itemSwap.schedule();

        int slotCache = mc.thePlayer.inventory.currentItem;
        int targetSlot = getTargetSlot();

        if (targetSlot == -1) {
            Charmonium.sendMessage("Pickaxe not found. Disabling mining speed boost");
            Config.useMiningSpeedBoost = false;
            return;
        }

        usingSpeed = true;
        mc.thePlayer.inventory.currentItem = targetSlot;

        if (itemSwap.hasPassed(100)) {
            mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.inventory.getStackInSlot(targetSlot));

            if (itemSwap.hasPassed(200)) {
                mc.thePlayer.inventory.currentItem = slotCache;
                pickaxeSkillReady = false;
                usingSpeed = false;
            }
        }
    }

    private int getTargetSlot() {
        int targetSlot = Config.blueCheeseOmeletteToggle
                ? InventoryUtils.getItemInHotbarFromLore(true, "Blue Cheese")
                : PlayerUtils.getItemInHotbar("Pick", "Gauntlet", "Drill");

        if (targetSlot == -1 && Config.blueCheeseOmeletteToggle) {
            Charmonium.sendMessage("Blue cheese drill not found. Disabled blue cheese swap");
            Config.blueCheeseOmeletteToggle = false;
            targetSlot = PlayerUtils.getItemInHotbar(true, "Pick", "Gauntlet", "Drill");
        }
        return targetSlot;
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
    }

    private boolean isNearWaypoint() {
        for (AOTVWaypointsStructs.Waypoint waypoint : waypoints) {
            if (Charmonium.mc.thePlayer.getPositionVector().distanceTo(new Vec3(waypoint.x, waypoint.y, waypoint.z)) < 5) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (!GameStateHandler.getInstance().atProperIsland() || !MacroHandler.getInstance().isMacroToggled() || MacroHandler.getInstance().getCrop() != Config.MacroEnum.TUNNELS) return;

        if (!TunnelUtils.currentlyPossibleToSee.isEmpty() && Config.highlightGemBlocks) {
            for (BlockPos blockPos : TunnelUtils.currentlyPossibleToSee) {
//                if (blockPos.equals(targetGem)) {
//                    Charmonium.sendMessage("Not null");
//                    RenderUtils.drawBlockBox(targetGem, new Color(255, 0, 183, 120));
//                    RenderUtils.drawText("§l§3[§f " + String.format("%.2f", ((double)checkTime(targetGem) / 20.0D) - 555) + " §3]", targetGem.getX() + 0.5, targetGem.getY() + 1, targetGem.getZ() + 0.5, 1);
//                    return;
//                }

                RenderUtils.drawBlockBox(blockPos, new Color(93, 32, 178, 120));
            }
        }

    }

    public static String[] drawInfo() {
        return SkillTracker.skillsInitialized()
                ? (SkillTracker.hitMax("Mining") ? drawMaxSkillInfo() : drawSkillInfo())
                : drawDefaultInfo();
    }

    private static String[] drawMaxSkillInfo() {
        return new String[]{
                "§r§lStats:",
                "§rMacro: §fTunnels Miner",
                "§rTime: §f" + getTimeString(),
                "§rMAX SKILL",
                "§rState: §f" + currentState.name()
        };
    }

    private static String[] drawSkillInfo() {
        double xpToShow = SkillTracker.getText("Mining");
        long elapsedTime = SkillTracker.skillStopwatch.getTime();
        int xpPerHour = (int) Math.round(xpToShow / (elapsedTime / 3600000.0));
        int nextLevel = Config.Skill_Mining + 1;
        double xpLeft = SkillTracker.xpLeft;

        return new String[]{
                "§r§lStats:",
                "§rMacro: §fTunnels Miner",
                "§rTime: §f" + getTimeString(),
                "§rXP Earned: §f" + formatNumber(xpToShow) + " [" + formatNumber(xpPerHour) + "/hr]",
                "§rTime til' Lvl. " + nextLevel + ": §f" + SkillTracker.getTimeBetween(0, xpLeft / (xpPerHour / 3600.0)),
                "§rState: §f" + currentState.name()
        };
    }

    private static String[] drawDefaultInfo() {
        return new String[]{
                "§r§lStats:",
                "§rMacro: §fTunnels Miner",
                "§rTime: §f" + getTimeString(),
                "§rOpen '/skills' to track xp",
                "§rState: §f" + currentState.name()
        };
    }

    private static String getTimeString() {
        return Scheduler.getInstance().isRunning() ? Scheduler.getInstance().getStatusString() : "Macroing";
    }

    private static String formatNumber(double number) {
        return NumberFormat.getNumberInstance(Locale.US).format(number);
    }
}
