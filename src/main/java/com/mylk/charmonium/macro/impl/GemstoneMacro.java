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
import com.mylk.charmonium.mixin.block.RenderGlobalAccessor;
import com.mylk.charmonium.mixin.client.PlayerControllerMPAccessor;
import com.mylk.charmonium.util.*;
import com.mylk.charmonium.util.charHelpers.GemstoneUtils;
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
import net.minecraft.util.*;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static net.minecraftforge.fml.common.eventhandler.EventPriority.HIGHEST;

public class
GemstoneMacro extends AbstractMacro {
    private enum State { NONE, UPDATE, GET_GEMSTONE, MINE, TELEPORT, WAIT, FALLEN, FALLEN_RECOVER, KILL }
    private enum TeleportState { NONE }

    private static State currentState = State.NONE;
    private final RotationHandler rotation = RotationHandler.getInstance();
    public static BlockPos targetGem;
    private BlockPos nextWaypoint;
    private static GemstoneMacro instance;

    private final Timer stuckMining = new Timer();
    private final Timer rotatingWait = new Timer();
    private final Timer teleportWait = new Timer();
    private final Timer recoveryWait = new Timer();
    private final Timer itemSwap = new Timer();
    private final Timer tpStuckTimer = new Timer();
    private final Timer breakTimer = new Timer();
    private final Timer diaGobTimer = new Timer();
    private final Timer attackDelay = new Timer();
    private final Timer noKillTimer = new Timer();

    private static float emptyTeleport = 0;
    private final Set<BlockPos> excludedBlocks = new HashSet<>();
    private int currentWaypoint = -1;
    private boolean refueling = false;
    public static boolean usingSpeed = false;
    private boolean diaGobSpawned = false;
    public static String currentTool = "";
    private Vec3 rotPos = new Vec3(0, 0, 0);
    private Vec3 teleportRotPos = new Vec3(0, 0, 0);
    public static boolean tping = false;
    public static boolean stopChecks = false;
    public static boolean pickaxeSkillReady = true;
    public static boolean miningSpeedActive = false;
    public static Entity target;
    public static Entity targetStand;

    private static List<AOTVWaypointsStructs.Waypoint> waypoints;

    private static final List<String> autoKillMobs = Arrays.asList("Yog");

    public static GemstoneMacro getInstance() {
        if (instance == null) {
            instance = new GemstoneMacro();
        }
        return instance;
    }

    @Override
    public void onEnable() {
        Config.MacroEnum crop = Config.MacroEnum.GEMSTONE;
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
        GemstoneUtils.possibleBreaks.clear();
        GemstoneUtils.currentlyPossibleToSee.clear();
        excludedBlocks.clear();
        rotPos =new Vec3(0, 0, 0);
        teleportRotPos = new Vec3(0, 0, 0);
        emptyTeleport = 0;
        currentWaypoint = -1;
        rotation.reset();
        resetTimers();
        tping = false;
        diaGobSpawned = false;
        usingSpeed = false;
        target = null;
        targetStand = null;
        stopChecks = false;
        currentTool = Config.gemstoneTool;
        waypoints = Charmonium.aotvWaypoints.getSelectedRoute().waypoints;
    }

    private void resetTimers() {
        stuckMining.reset();
        diaGobTimer.reset();
        rotatingWait.reset();
        teleportWait.reset();
        tpStuckTimer.reset();
        recoveryWait.reset();
        breakTimer.reset();
    }

    private void initializeWaypoint() {
        BlockPos currentPos = BlockUtils.fromVecToBP(Charmonium.mc.thePlayer.getPositionVector().subtract(0, 1, 0));
        currentWaypoint = IntStream.range(0, waypoints.size())
                .filter(i -> new BlockPos(waypoints.get(i).x, waypoints.get(i).y, waypoints.get(i).z).equals(currentPos))
                .findFirst()
                .orElse(-1);

        if (currentWaypoint == -1) {
            Charmonium.sendMessage("Auto AOTV - You are not at a valid waypoint!");
            MacroHandler.getInstance().disableMacro();
        }
    }

    public void invokeState() {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        int heightCheckY = waypoints.get(currentWaypoint).y - 2;

        if (Config.autoKillGems) {
            handleAutoKill(heightCheckY);
        }

        switch (currentState) {
            case NONE:
                currentState = State.UPDATE;
                teleportRotPos = new Vec3(0, 0, 0);
                break;
            case UPDATE:
                handleUpdateState(heightCheckY);
                break;
            case GET_GEMSTONE:
                handleGetGemstoneState();
                break;
            case MINE:
                handleMineState();
                break;
            case TELEPORT:
                handleTeleportState();
                break;
            case WAIT:
                handleWaitState();
                break;
            case FALLEN:
                handleFallenState();
                break;
            case FALLEN_RECOVER:
                handleFallenRecoverState();
                break;
            case KILL:
                handleKillState();
                break;
        }
    }

    private void handleAutoKill(int heightCheckY) {
        List<Entity> entities = getEntities();
        if (!entities.isEmpty() && target == null) {
            Optional<Entity> optionalEntity = entities.stream()
                    .min(Comparator.comparingDouble(entity -> entity.getDistanceToEntity(mc.thePlayer)));

            if (optionalEntity.isPresent()) {
                Entity entity = optionalEntity.get();
                if (entity.getPosition().getY() >= heightCheckY) {
                    targetStand = entity;
                    target = npcUtils.getEntityCuttingOtherEntity(targetStand, null);
                    noKillTimer.schedule();
                    currentState = State.KILL;
                    Charmonium.sendMessage("Found Yog...");
                }
            }
        }
    }

    private void handleUpdateState(int heightCheckY) {
        if (!recoveryWait.hasPassed(600)) return;
        if (mc.thePlayer.getPosition().getY() < heightCheckY && recoveryWait.hasPassed(6500)) {
            currentState = State.FALLEN;
            return;
        }
        if (GemstoneUtils.possibleBreaks.isEmpty()) {
            handleEmptyPossibleBreaks();
        } else {
            GemstoneUtils.getAllBlocks();
            GemstoneUtils.updateListData((HashSet<BlockPos>) excludedBlocks);
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
        GemstoneUtils.getAllBlocks();
    }

    private void handleGetGemstoneState() {
        if (checks()) return;
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
        rotPos = GemstoneUtils.currentlyPossibleToSee.size() > 1
                ? VectorUtils.getClosestHittableToNextBlock(targetGem, GemstoneUtils.currentlyPossibleToSee.get(1))
                : VectorUtils.getRandomHittable(targetGem);
        excludedBlocks.clear();
        breakTimer.schedule();
        stuckMining.reset();
        currentState = State.MINE;
    }

    private void handleMineState() {
        emptyTeleport = 0;
        currentTool = Config.gemstoneTool;
        if (targetGem == null || Charmonium.mc.theWorld.getBlockState(targetGem).getBlock() == Blocks.air) {
            currentState = State.UPDATE;
            return;
        }
        if (rotPos == null || rotPos.equals(new Vec3(0, 0, 0))) {
            Charmonium.sendMessage("§7§orotPos is null or zero");
            targetGem = null;
            return;
        }
        if (breakTimer.hasPassed(GemstoneDelaysPage.gemstoneStuckDelay)) {
            handleStuckMining();
            return;
        }
        checkMiningSpeedBoost();
        if (GemstoneUtils.currentlyPossibleToSee.size() > 1 && Config.pingGlide != 0) {
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
                Charmonium.sendMessage("§7§oWaypoint isnt visible...");
                rotatingWait.reset();
                teleportRotPos = new Vec3(0, 0, 0);
                stopChecks = false;
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
                MacroHandler.getInstance().disableMacro();
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

    private void handleFallenState() {
        BlockPos checkWaypoint = new BlockPos(waypoints.get(currentWaypoint).x, waypoints.get(currentWaypoint).y, waypoints.get(currentWaypoint).z);
        if (!mc.thePlayer.onGround) return;
        if (GemstoneUtils.hasAnyLineOfSight(checkWaypoint)) {
            Charmonium.sendMessage("§7§oYou have fallen, teleporting to waypoint " + currentWaypoint);
            currentState = State.FALLEN_RECOVER;
            recoveryWait.reset();
            return;
        }
        Charmonium.sendMessage("§7§oCurrent isnt visible, trying again");
        for (int i = 0; i < waypoints.size(); i++) {
            BlockPos checkWaypoint2 = new BlockPos(waypoints.get(i).x, waypoints.get(i).y, waypoints.get(i).z);
            if (GemstoneUtils.hasAnyLineOfSight(checkWaypoint2)) {
                Charmonium.sendMessage("§7§oYou have fallen, teleporting to waypoint " + i);
                currentWaypoint = i;
                currentState = State.FALLEN_RECOVER;
                recoveryWait.reset();
                return;
            }
        }
    }

    private void handleFallenRecoverState() {
        BlockPos fallenRecoverWaypoint = new BlockPos(waypoints.get(currentWaypoint).x, waypoints.get(currentWaypoint).y, waypoints.get(currentWaypoint).z);
        BlockPos blockBelow = new BlockPos(mc.thePlayer.getPosition().getX(), mc.thePlayer.getPosition().getY() - 1, mc.thePlayer.getPosition().getZ());

        if (blockBelow.equals(fallenRecoverWaypoint)) {
            currentState = State.NONE;
            return;
        }

        KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindAttack, false);
        currentTool = "Aspect of the";

        if (!rotation.isRotating() && teleportRotPos.equals(new Vec3(0, 0, 0))) {
            teleportRotPos = VectorUtils.getClosestHittableToMiddleTeleport(fallenRecoverWaypoint);
            rotation.easeTo(new RotationConfiguration(rotation.getRotation(teleportRotPos), 125, null));
            if (!rotatingWait.isScheduled()) rotatingWait.schedule();
        }

        stopChecks = true;
        KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindSneak, true);

        if (!rotatingWait.hasPassed(625)) return;

        MovingObjectPosition movingRecoverObjectPosition = mc.thePlayer.rayTrace(55, 1);

        if (movingRecoverObjectPosition != null && movingRecoverObjectPosition.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            if (movingRecoverObjectPosition.getBlockPos().equals(fallenRecoverWaypoint)) {
                KeyBindUtils.rightClick();
                Charmonium.sendMessage("Fallen Recover - Teleported to waypoint " + currentWaypoint);
                tping = true;
                tpStuckTimer.reset();
                rotatingWait.reset();
                recoveryWait.schedule();
                teleportRotPos = new Vec3(0, 0, 0);
                currentState = State.NONE;
            } else if (tpStuckTimer.hasPassed(5000) && !rotation.isRotating()) {
                Charmonium.sendMessage("Fallen Recover - Path is not cleared. Block: " + movingRecoverObjectPosition.getBlockPos().toString() + " is on the way.");
                MacroHandler.getInstance().disableMacro();
            }
        } else if (movingRecoverObjectPosition != null) {
            Charmonium.sendMessage("Fallen Recover - Something is on the way!");
            MacroHandler.getInstance().disableMacro();
        }
    }

    private void handleKillState() {
        if (noKillTimer.hasPassed(6000)) {
            Charmonium.sendMessage("Stuck? Finding new");
            resetMacroState();
        }

        if (target == null || targetStand == null || target instanceof EntityXPOrb || ((EntityLivingBase) target).getHealth() <= 1 || target.isDead) {
            resetMacroState();
            return;
        }

        boolean visible = PlayerUtils.entityIsVisible(target);

        if (!visible) {
            Charmonium.sendMessage("Something is blocking target, moving on...");
            resetMacroState();
        } else {
            boolean targeted = npcUtils.entityIsTargeted(target);

            if (target != null && !targeted) {
                rotation.easeTo(new RotationConfiguration(rotation.getRotation(target.getPositionVector().add(new Vec3(0, target.height / 2, 0))), Config.getRandomRotationTime(), null));
            }

            assert target != null;

            if (Config.SGAttackType == Config.SGAttackEnum.LEFT_CLICK.ordinal()) {
                currentTool = Config.autoKillWGems;

                if (Minecraft.getMinecraft().thePlayer.getDistanceToEntity(target) <= Config.autoKillRGems) {
                    if (attackDelay.hasPassed(100)) {
                        KeyBindUtils.stopMovement();
                        KeyBindUtils.onTick(mc.gameSettings.keyBindAttack);
                        attackDelay.schedule();
                    }
                }
            } else {
                currentTool = Config.autoKillWGems;

                if (Minecraft.getMinecraft().thePlayer.getDistanceToEntity(target) <= Config.autoKillRGems) {
                    if (attackDelay.hasPassed(100)) {
                        KeyBindUtils.stopMovement();
                        KeyBindUtils.onTick(mc.gameSettings.keyBindUseItem);
                        attackDelay.schedule();
                    }
                }
            }
        }
    }

    private boolean checks() {
        if (Config.refuelWithAbiphone && fuelFilling.isRefueling()) {
            currentState = State.WAIT;
            return true;
        }

        if (GemstoneUtils.currentlyPossibleToSee.isEmpty()) {
            GemstoneUtils.possibleBreaks.clear();

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

    private void resetMacroState() {
        currentState = State.NONE;
        target = null;
        targetStand = null;
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

    private List<Entity> getEntities() {
        return mc.theWorld.getEntitiesInAABBexcluding(mc.thePlayer,
                        mc.thePlayer.getEntityBoundingBox().expand(Config.autoKillRGems, Config.autoKillRGems / 2, Config.autoKillRGems),
                        e -> e instanceof EntityArmorStand)
                .stream()
                .filter(this::isValidTarget)
                .filter(PlayerUtils::entityIsVisible)
                .collect(Collectors.toList());
    }

    private boolean isValidTarget(Entity entity) {
        double distance = entity.getDistanceToEntity(mc.thePlayer);
        double verticalDifference = mc.thePlayer.posY - entity.posY;
        return distance <= Config.autoKillRGems
                && !entity.getName().contains(mc.thePlayer.getName())
                && !entity.isDead
                && ((EntityLivingBase) entity).getHealth() > 0
                && autoKillMobs.stream().anyMatch(mob -> entity.getCustomNameTag().contains(mob))
                && verticalDifference >= -2 && verticalDifference <= 4;
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

            if (message.contains("Your pass to the Crystal Hollows will expire in 1 minute")) {
                if (Config.autoRenewCrystalHollowsPass) {
                    Charmonium.sendMessage("Auto renewing Crystal Hollows pass");
                    mc.thePlayer.sendChatMessage("/purchasecrystallhollowspass");
                }
            }

            if (message.contains("Diamond Goblin has spawned")) {
                if (Config.alertDiamondGob) {
                    diaGobSpawned = true;
                }
            }
        } catch (Exception ignored) {}
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;

        if (Config.alertDiamondGob && diaGobSpawned) {
            if (!diaGobTimer.isScheduled()) diaGobTimer.schedule();
            if (diaGobTimer.hasPassed(3000)) {
                diaGobSpawned = false;
                diaGobTimer.reset();
            }

            RenderUtils.drawCenterMiddleText("A DIAMOND GOBLIN", event, Color.CYAN);
        }
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (!GameStateHandler.getInstance().atProperIsland() || !MacroHandler.getInstance().isMacroToggled() || MacroHandler.getInstance().getCrop() != Config.MacroEnum.GEMSTONE) return;

        if (!GemstoneUtils.currentlyPossibleToSee.isEmpty() && Config.highlightGemBlocks) {
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

    public static String[] drawInfo() {
        return SkillTracker.skillsInitialized()
                ? (SkillTracker.hitMax("Mining") ? drawMaxSkillInfo() : drawSkillInfo())
                : drawDefaultInfo();
    }

    private static String[] drawMaxSkillInfo() {
        return new String[]{
                "§r§lStats:",
                "§rMacro: §fGemstone Miner",
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
                "§rMacro: §fGemstone Miner",
                "§rTime: §f" + getTimeString(),
                "§rXP Earned: §f" + formatNumber(xpToShow) + " [" + formatNumber(xpPerHour) + "/hr]",
                "§rTime til' Lvl. " + nextLevel + ": §f" + SkillTracker.getTimeBetween(0, xpLeft / (xpPerHour / 3600.0)),
                "§rState: §f" + currentState.name()
        };
    }

    private static String[] drawDefaultInfo() {
        return new String[]{
                "§r§lStats:",
                "§rMacro: §fGemstone Miner",
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
