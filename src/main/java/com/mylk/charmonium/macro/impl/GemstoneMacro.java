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

import static net.minecraftforge.fml.common.eventhandler.EventPriority.HIGHEST;

public class
GemstoneMacro extends AbstractMacro {
    private static State currentState = State.NONE;
    private static TeleportState currentTeleportState = TeleportState.NONE;
    private final RotationHandler rotation = RotationHandler.getInstance();
    public static BlockPos targetGem;
    private BlockPos nextWaypoint;
    private static GemstoneMacro instance;
    private final Timer stuckMining = new Timer();
    private final Timer rotatingWait = new Timer();
    private final Timer teleportWait = new Timer();
    private final Timer recoveryWait = new Timer();
    private final Timer itemSwap = new Timer();
    static float emptyTeleport = 0;
    HashSet<BlockPos> excludedBlocks = new HashSet<>();
    private int currentWaypoint = -1;
    private boolean refueling = false;
    public static boolean usingSpeed = false;
    private boolean diaGobSpawned = false;
    public static String currentTool = "";
    Vec3 rotPos = new Vec3(0, 0, 0);
    Vec3 teleportRotPos = new Vec3(0, 0, 0);
    private final Timer tpStuckTimer = new Timer();
    private final Timer breakTimer = new Timer();
    private final Timer diaGobTimer = new Timer();
    public static boolean tping = false;
    public static boolean stopChecks = false;
    public static boolean pickaxeSkillReady = true;
    public static boolean miningSpeedActive = false;
    public static Entity target;
    public static Entity targetStand;
    private final Timer attackDelay = new Timer();
    private final Timer noKillTimer = new Timer();
    private static ArrayList<AOTVWaypointsStructs.Waypoint> Waypoints;

    public static GemstoneMacro getInstance() {
        if (instance == null) {
            instance = new GemstoneMacro();
        }
        return instance;
    }

    private static final List<String> autoKillMobs = new ArrayList<String>() {
        {
            add("Yog");
        }
    };

    public void onEnable() {
        Config.MacroEnum crop = Config.MacroEnum.GEMSTONE;
        currentState = State.NONE;

        targetGem = null;
        GemstoneUtils.possibleBreaks.clear();
        GemstoneUtils.currentlyPossibleToSee.clear();
        excludedBlocks.clear();
        rotPos = new Vec3(0, 0, 0);
        teleportRotPos = new Vec3(0, 0, 0);
        emptyTeleport = 0;
        currentWaypoint = -1;
        rotation.reset();
        stuckMining.reset();
        diaGobTimer.reset();
        rotatingWait.reset();
        teleportWait.reset();
        tpStuckTimer.reset();
        recoveryWait.reset();
        breakTimer.reset();
        tping = false;
        diaGobSpawned = false;
        usingSpeed = false;
        target = null;
        targetStand = null;
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
        ArrayList<AOTVWaypointsStructs.Waypoint> Waypoints = Charmonium.aotvWaypoints.getSelectedRoute().waypoints;
        int heightCheckY = Waypoints.get(currentWaypoint).y - 2;

        if (Config.autoKillGems) {
            List<Entity> entities = getEntity();

            if (!entities.isEmpty() && target == null) {
                Optional<Entity> optional = entities.stream().min(Comparator.comparingDouble(entity -> entity.getDistanceToEntity(mc.thePlayer)));

                if (optional.get().getPosition().getY() < heightCheckY) { entities.remove(optional.get()); return; }
                targetStand = optional.get();
                target = npcUtils.getEntityCuttingOtherEntity(targetStand, null);
                noKillTimer.schedule();
                currentState = State.KILL;
                Charmonium.sendMessage("Found Yog...");
                return;
            }
        }

        switch (currentState) {
            case NONE:
                currentState = State.UPDATE;
                teleportRotPos = new Vec3(0, 0, 0);
                break;

            case UPDATE:
                if (!recoveryWait.hasPassed(600)) return;
                if (mc.thePlayer.getPosition().getY() < heightCheckY && recoveryWait.hasPassed(6500)) {
                    currentState = State.FALLEN;
                    return;
                }

                if (GemstoneUtils.possibleBreaks.isEmpty()) {
                    if (excludedBlocks.isEmpty()) {
                        emptyTeleport++;
                        if (emptyTeleport >= 75 && !tping) {
                            if (currentWaypoint == Waypoints.size() - 1) {
                                currentWaypoint = 0;
                            } else {
                                currentWaypoint++;
                            }
                            teleportRotPos = new Vec3(0, 0, 0);
                            currentState = State.TELEPORT;
                            teleportWait.reset();
                            emptyTeleport = 0;
                        }
                    } else {
                        excludedBlocks.clear();
                    }
                    GemstoneUtils.getAllBlocks();
                    return;
                }

                GemstoneUtils.getAllBlocks();
                GemstoneUtils.updateListData(excludedBlocks);

                if (checks()) return;

                currentState = State.GET_GEMSTONE;

                break;

            case GET_GEMSTONE:
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

                if (GemstoneUtils.currentlyPossibleToSee.size() > 1) {
                    rotPos = VectorUtils.getClosestHittableToNextBlock(targetGem, GemstoneUtils.currentlyPossibleToSee.get(1));
                } else {
                    rotPos = VectorUtils.getRandomHittable(targetGem);
                }

                excludedBlocks.clear();

                breakTimer.schedule();
                stuckMining.reset();

                currentState = State.MINE;
                break;

            case MINE:
                emptyTeleport = 0;
                currentTool = Config.gemstoneTool;

                if (targetGem == null) {
                    currentState = State.UPDATE;
                    return;
                }

                if (Charmonium.mc.theWorld.getBlockState(targetGem).getBlock() == Blocks.air) {
                    targetGem = null;
                    return;
                }

                if (rotPos == null  || rotPos.equals(new Vec3(0, 0, 0))) {
                    Charmonium.sendMessage("§7§orotPos is null or zero");
                    targetGem = null;
                    return;
                }

                if (breakTimer.hasPassed(GemstoneDelaysPage.gemstoneStuckDelay)) {
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

                if (GemstoneUtils.currentlyPossibleToSee.size() > 1 && Config.pingGlide != 0)
                    checkProgress(targetGem);

                if (!mc.thePlayer.getHeldItem().getDisplayName().contains(Config.gemstoneTool)) return;

                Rotation rotationCheck = rotation.getRotation(rotPos);

                if (rotationCheck.getPitch() > 360 || rotationCheck.getPitch() < -360) {
                    Charmonium.sendMessage("§7§oToo Big Rotation");
                    targetGem = null;
                    return;
                }

                if (rotationCheck.getYaw() > 360 || rotationCheck.getYaw() < -360) {
                    Charmonium.sendMessage("§7§oToo Big Rotation");
                    targetGem = null;
                    return;
                }

                rotation.easeTo(new RotationConfiguration(rotationCheck, GemstoneDelaysPage.gemstoneRotationDelay, null));
                KeyBindUtils.holdThese(mc.gameSettings.keyBindAttack);

                break;

            case TELEPORT:
                BlockPos waypoint = new BlockPos(Waypoints.get(currentWaypoint).x, Waypoints.get(currentWaypoint).y, Waypoints.get(currentWaypoint).z);

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
                    } else {
                        if (tpStuckTimer.hasPassed(5000) && !rotation.isRotating()) {
                            Charmonium.sendMessage("AOTV Macro - Path is not cleared. Block: " + movingObjectPosition.getBlockPos().toString() + " is on the way.");
                            MacroHandler.getInstance().disableMacro();
                            return;
                        }
                    }
                } else if (movingObjectPosition != null) {
                    Charmonium.sendMessage("AOTV Macro - Something is on the way!");
                    rotatingWait.reset();
                    teleportRotPos = new Vec3(0, 0, 0);
                    stopChecks = false;
                    return;
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
                    currentState = State.NONE;
                    return;
                }
                break;
            case FALLEN:
                BlockPos checkWaypoint = new BlockPos(Waypoints.get(currentWaypoint).x, Waypoints.get(currentWaypoint).y, Waypoints.get(currentWaypoint).z);

                if (!mc.thePlayer.onGround) return;

                if (GemstoneUtils.hasAnyLineOfSight(checkWaypoint)) {
                    Charmonium.sendMessage("§7§oYou have fallen, teleporting to waypoint " + currentWaypoint);
                    currentState = State.FALLEN_RECOVER;
                    recoveryWait.reset();
                    return;
                }

                Charmonium.sendMessage("§7§oCurrent isnt visible, trying again");

                for (int i = 0; i < Waypoints.size(); i++) {
                    BlockPos checkWaypoint2 = new BlockPos(Waypoints.get(i).x, Waypoints.get(i).y, Waypoints.get(i).z);
                    if (GemstoneUtils.hasAnyLineOfSight(checkWaypoint2)) {
                        Charmonium.sendMessage("§7§oYou have fallen, teleporting to waypoint " + i);
                        currentWaypoint = i;
                        currentState = State.FALLEN_RECOVER;
                        recoveryWait.reset();
                        return;
                    }
                }
//                int closest = 0;
//                double closestDistance = Double.MAX_VALUE;
//                for (int i = 0; i < FallenWaypoints.size(); i++) {
//                    BlockPos checkWaypoint2 = new BlockPos(FallenWaypoints.get(i).x, FallenWaypoints.get(i).y, FallenWaypoints.get(i).z);
//                    double distance = mc.thePlayer.getPositionVector().distanceTo(BlockUtils.fromBPToVec(checkWaypoint2));
//                    if (distance < closestDistance) {
//                        closest = i;
//                        closestDistance = distance;
//                    }
//                }

                break;
            case FALLEN_RECOVER:
                BlockPos FallenRecoverwaypoint = new BlockPos(Waypoints.get(currentWaypoint).x, Waypoints.get(currentWaypoint).y, Waypoints.get(currentWaypoint).z);

                BlockPos blockBelow = new BlockPos(mc.thePlayer.getPosition().getX(), mc.thePlayer.getPosition().getY() - 1, mc.thePlayer.getPosition().getZ());
                if (blockBelow.equals(FallenRecoverwaypoint))
                    currentState = State.NONE;

                KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindAttack, false);
                currentTool = "Aspect of the";

                if (!rotation.isRotating() && teleportRotPos.equals(new Vec3(0, 0, 0))) {
                    teleportRotPos = VectorUtils.getClosestHittableToMiddleTeleport(FallenRecoverwaypoint);
                    rotation.easeTo(new RotationConfiguration(rotation.getRotation(teleportRotPos), 125, null));
                    if (!rotatingWait.isScheduled()) rotatingWait.schedule();
                }

                stopChecks = true;

                KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindSneak, true);

                if (!rotatingWait.hasPassed(625)) return;

                MovingObjectPosition movingRecoverObjectPosition = mc.thePlayer.rayTrace(55, 1);
                if (movingRecoverObjectPosition != null && movingRecoverObjectPosition.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                    if (movingRecoverObjectPosition.getBlockPos().equals(FallenRecoverwaypoint)) {
                        KeyBindUtils.rightClick();
                        Charmonium.sendMessage("Fallen Recover - Teleported to waypoint " + currentWaypoint);
                        tping = true;
                        tpStuckTimer.reset();
                        rotatingWait.reset();
                        recoveryWait.schedule();
                        teleportRotPos = new Vec3(0, 0, 0);
                        currentState = State.NONE;
                    } else {
                        if (tpStuckTimer.hasPassed(5000) && !rotation.isRotating()) {
                            Charmonium.sendMessage("Fallen Recover - Path is not cleared. Block: " + movingRecoverObjectPosition.getBlockPos().toString() + " is on the way.");
                            MacroHandler.getInstance().disableMacro();
                            break;
                        }
                    }
                } else if (movingRecoverObjectPosition != null) {
                    Charmonium.sendMessage("Fallen Recover - Something is on the way!");
                    MacroHandler.getInstance().disableMacro();
                }
                return;
            case KILL:
                if (noKillTimer.hasPassed(6000)) {
                    Charmonium.sendMessage("Stuck? Finding new");
                    resetMacroState();
                }

                if (target == null || targetStand == null  || target instanceof EntityXPOrb || ((EntityLivingBase) target).getHealth() <= 1 || target.isDead) {
                    resetMacroState();
                    break;
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

                break;
        }
    }

    private boolean checks() {
        if (Config.refuelWithAbiphone) {
            if (fuelFilling.isRefueling()) {
                currentState = State.WAIT;
                return true;
            }
        }

        if (GemstoneUtils.currentlyPossibleToSee.isEmpty()) {
            GemstoneUtils.possibleBreaks.clear();

            if (!teleportWait.hasPassed(2500)) return true;
            if (currentWaypoint == Waypoints.size() - 1) {
                currentWaypoint = 0;
            } else {
                currentWaypoint++;
            }
            teleportRotPos = new Vec3(0, 0, 0);
            currentState = State.TELEPORT;
            teleportWait.reset();
            return true;
        }

        return false;
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

    private List<Entity> getEntity() {
        return mc.theWorld.getEntitiesInAABBexcluding(mc.thePlayer,
                        mc.thePlayer.getEntityBoundingBox().expand(Config.autoKillRGems, (Config.autoKillRGems >> 1), Config.autoKillRGems),
                        e -> e instanceof EntityArmorStand).stream()
                .filter((v) -> {
                    double distance = v.getDistanceToEntity(mc.thePlayer);
                    double verticalDifference = mc.thePlayer.posY - v.posY;
                    return distance <= Config.autoKillRGems &&
                            !v.getName().contains(mc.thePlayer.getName()) &&
                            !v.isDead &&
                            ((EntityLivingBase) v).getHealth() > 0 &&
                            autoKillMobs.stream().anyMatch((a) -> v.getCustomNameTag().contains(a)) &&
                            verticalDifference >= -2 && verticalDifference <= 4;
                })
                .filter(PlayerUtils::entityIsVisible)
                .collect(Collectors.toList());
    }


    public void checkProgress(BlockPos blockPos) {
        if (blockPos != null) {
            for (DestroyBlockProgress var2 : ((RenderGlobalAccessor) mc.renderGlobal).getDamagedBlocks().values()) {
                if (var2.getPosition().equals(blockPos) && var2.getPartialBlockDamage() >= Config.pingGlide / 10) {
                    excludedBlocks.add(targetGem);
                    //GemstoneUtils.currentlyPossibleToSee.remove(0); //try this??
                    targetGem = null;
                    break;
                }
            }
        }
    }

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

    private void resetMacroState() {
        currentState = State.NONE;
        target = null;
        targetStand = null;
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
            if (SkillTracker.hitMax("Mining")) {
                return drawMaxSkillInfo();
            } else {
                return drawSkillInfo();
            }
        } else {
            return drawDefaultInfo();
        }
    }

    private static String[] drawMaxSkillInfo() {
        return new String[]{
                "§r§lStats:",
                "§rMacro: §fGemstone Miner",
                "",
                "§rTime: §f" + (Scheduler.getInstance().isRunning() ? Scheduler.getInstance().getStatusString() : "Macroing"),
                "§rMAX SKILL",
                "§rState: §f" + currentState.name(),
        };
    }

    private static String[] drawSkillInfo() {
        double xpToShow = SkillTracker.getText("Mining");
        int xpPerHour = (int) Math.round(xpToShow / ((SkillTracker.skillStopwatch.getTime() + 1) / 3600000d));
        int nxtLvl = Config.Skill_Mining + 1;

        return new String[]{
                "§r§lStats:",
                "§rMacro: §fGemstone Miner",
                "",
                "§rTime: §f" + (Scheduler.getInstance().isRunning() ? Scheduler.getInstance().getStatusString() : "Macroing"),
                "§rXP Earned: §f" + NumberFormat.getNumberInstance(Locale.US).format(xpToShow) + " [" + NumberFormat.getNumberInstance(Locale.US).format(xpPerHour) + "/hr]",
                "§rTime til' Lvl. " + nxtLvl + ": §f" + SkillTracker.getTimeBetween(0, SkillTracker.xpLeft / (xpPerHour / 3600D)),
                "§rState: §f" + currentState.name(),
        };
    }

    private static String[] drawDefaultInfo() {
        return new String[]{
                "§r§lStats:",
                "§rMacro: §fGemstone Miner",
                "",
                "§rTime: §f" + (Scheduler.getInstance().isRunning() ? Scheduler.getInstance().getStatusString() : "Macroing"),
                "§rOpen '/skills' to track xp",
                "§rState: §f" + currentState.name(),
        };
    }


    public enum State {
        NONE,
        UPDATE,
        FALLEN,
        FALLEN_RECOVER,
        KILL,
        GET_GEMSTONE,
        MINE,
        TELEPORT,
        WAIT
    }

    public enum TeleportState {
        NONE,
        GET_WAYPOINT,
        ROTATE,
        TELEPORT,
        CHECK,
        FAILSAFE,
        CONTINUE,
        WAIT
    }
}
