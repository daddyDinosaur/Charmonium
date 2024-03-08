package com.mylk.charmonium.macro.impl;

import com.mylk.charmonium.Charmonium;
import com.mylk.charmonium.config.Config;
import com.mylk.charmonium.config.aotv.AOTVWaypointsStructs;
import com.mylk.charmonium.event.MillisecondEvent;
import com.mylk.charmonium.feature.impl.Scheduler;
import com.mylk.charmonium.feature.impl.SkillTracker;
import com.mylk.charmonium.handler.GameStateHandler;
import com.mylk.charmonium.handler.MacroHandler;
import com.mylk.charmonium.handler.RotationHandler;
import com.mylk.charmonium.macro.AbstractMacro;
import com.mylk.charmonium.macro.impl.misc.routes;
import com.mylk.charmonium.util.*;
import com.mylk.charmonium.util.charHelpers.VectorUtils;
import com.mylk.charmonium.util.helper.RotationConfiguration;
import com.mylk.charmonium.util.charHelpers.GemstoneUtils;
import com.mylk.charmonium.util.helper.Timer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStone;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.StringUtils;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.*;
import java.text.NumberFormat;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;

public class MithrilMacro extends AbstractMacro {
    private static State currentState = State.NONE;
    private static int currentWaypoint = 0;
    public static boolean miningSpeedActive = false;
    private final Timer closestPointCooldown = new Timer();
    private static ArrayList<BlockPos> mithril;
    public static boolean pickaxeSkillReady = true;
    private static ArrayList<AOTVWaypointsStructs.Waypoint> thePath;
    private static BlockPos closest;
    HashSet<BlockPos> broken = new HashSet<>();
    private final RotationHandler rotation = RotationHandler.getInstance();
    private final Timer diedDelay = new Timer();
    private boolean hasDied = false;
    private boolean focusTit = false;
    private Vec3 rotPos = new Vec3(0, 0, 0);
    private int noDmgCount = 0;
    private static MithrilMacro instance;

    public static MithrilMacro getInstance() {
        if (instance == null) {
            instance = new MithrilMacro();
        }
        return instance;
    }
    public void onEnable() {
        Config.MacroEnum crop = Config.MacroEnum.MITHRIL_MINER;
        currentWaypoint = 0;
        closest = null;
        closestPointCooldown.reset();
        currentState = State.NONE;
        diedDelay.reset();
        rotPos = new Vec3(0, 0, 0);
        noDmgCount = 0;
        hasDied = false;
        super.setSpawnPos(new Vec3(-49, 200, -122));

        LogUtils.sendDebug("Macro: " + crop);
        MacroHandler.getInstance().setCrop(crop);
        PlayerUtils.getTool();

        switch (Config.SMSpotType) {
            case 0:
                thePath = routes.hubForaging;
                break;
            case 1:
                thePath = routes.hubForaging;
                break;
        }

//        if (isNearSpawn()) {
//            currentState = State.AT_SPAWN;
//            MacroHandler.walkTo(thePath, currentWaypoint);
//            Charmonium.sendMessage("Walking to mithril");
//            currentWaypoint++;
//        } else {
//            Charmonium.sendMessage("U in the middle of no where, retard");
//            diedDelay.schedule();
//            hasDied = true;
//        }
        Charmonium.sendMessage("idk bro");
        currentState = State.AT_MITHRIL;

        super.onEnable();
    }

    public void invokeState() {
        switch (currentState) {
            case NONE:
                currentState = State.AT_SPAWN;
                break;
            case AT_SPAWN:
                if (Charmonium.mc.thePlayer.getPositionVector().distanceTo(new Vec3(-111, 74, -32)) < 5) {
                    currentWaypoint = 0;
                    currentState = State.AT_MITHRIL;
                    Charmonium.sendMessage("Arrived at mithril");
                }

                if (MacroHandler.walker.isDone()) {
                    MacroHandler.walkTo(thePath, currentWaypoint);
                    currentWaypoint++;
                }

                break;
            case AT_MITHRIL:
                mithril = getMithAround();

                if (mithril.isEmpty()) {
                    Charmonium.sendMessage("No mithril or titanium nearby");
                    resetMacroState();
                    return;
                }

                closest = BlockUtils.getEasiestBlock(mithril, (Predicate<? super BlockPos>) broken);

//                closest = BlockUtils.getClosest(
//                        mithril,
//                        broken,
//                        new BlockPos(mc.thePlayer.getPositionVector())
//                );

                if (closest != null && Charmonium.mc.theWorld.getBlockState(closest).getBlock() != Blocks.bedrock && !mithril.isEmpty()) {
                    currentState = State.MINING;
                } else {
                    resetMacroState();
                }

                break;
            case MINING:
                broken.add(closest);

                if (BlockUtils.distanceFromTo(mc.thePlayer.getPositionVector(), BlockUtils.fromBPToVec(closest)) < mc.playerController.getBlockReachDistance()) {
                    if (Objects.equals(rotPos, new Vec3(0, 0, 0))) {
                        rotPos = VectorUtils.getRandomHittable(closest);
                    }
                    rotation.easeTo(new RotationConfiguration(rotation.getRotation(rotPos), Config.getRandomRotationTime() * 2, null));
                    KeyBindUtils.holdThese(mc.gameSettings.keyBindAttack);
                }

                if (Charmonium.mc.theWorld.getBlockState(closest).getBlock() == Blocks.bedrock || closest == null || mithril.isEmpty()) {
                    resetMacroState();
                }

                break;
        }
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (!GameStateHandler.getInstance().atProperIsland() || !MacroHandler.getInstance().isMacroToggled() || MacroHandler.getInstance().getCrop() != Config.MacroEnum.MITHRIL_MINER) return;

        if (currentState == State.AT_SPAWN) {
            for (AOTVWaypointsStructs.Waypoint value : thePath) {
                BlockPos waypoint = new BlockPos(value.x, value.y, value.z);
                RenderUtils.drawBlockBox(waypoint, Color.PINK);
            }
            return;
        }

        if (closest != null && !mithril.isEmpty()) {
            RenderUtils.drawBlockBox(closest, new Color(32,178,170, 120));
        }
    }

    @SubscribeEvent
    public void onMillisecond(MillisecondEvent event) {
        if (!MacroHandler.getInstance().isMacroToggled() || MacroHandler.getInstance().getCrop() != Config.MacroEnum.MITHRIL_MINER) {
            return;
        }

        if (hasDied) {
            if (!diedDelay.hasPassed(2000)) return;

            if (!Objects.equals(mc.thePlayer.getPosition(), new BlockPos(1, 149, -68))) {
                Charmonium.sendMessage("Recovering from death...");
                mc.thePlayer.sendChatMessage("/warp forge");
                diedDelay.schedule();
                return;
            }

            currentWaypoint = 0;
            MacroHandler.walkTo(thePath, currentWaypoint);
            currentWaypoint++;
            diedDelay.reset();
            hasDied = false;
            currentState = State.AT_SPAWN;
        }
    }

    @SubscribeEvent
    public void onChatReceive(ClientChatReceivedEvent event) {
        String message = StringUtils.stripControlCodes(event.message.getUnformattedText());

        if (message.contains("You were killed by") && MacroHandler.getInstance().getCrop() == Config.MacroEnum.MITHRIL_MINER && MacroHandler.getInstance().isMacroToggled()) {
            Charmonium.sendMessage("Warping to forge");
            MacroHandler.walker.stop();
            hasDied = true;
            diedDelay.schedule();
        }
    }

    private void resetMacroState() {
        broken.clear();
        rotPos = new Vec3(0, 0, 0);
        noDmgCount = 0;
        closest = null;
        currentState = State.AT_MITHRIL;
        mithril.clear();
    }

    private boolean isNearSpawn() {
        Vec3 spawn = new Vec3(-3, 70, -70);
        return Charmonium.mc.thePlayer.getPositionVector().distanceTo(spawn) < 3;
    }

    ArrayList<BlockPos> getMithAround() {
        double playerReach = mc.playerController.getBlockReachDistance() * 0.8;
        ArrayList<BlockPos> returnBlocks = new ArrayList<>();
        Iterable<BlockPos> blocks = BlockPos.getAllInBox(
                BlockUtils.fromVecToBP(Charmonium.mc.thePlayer.getPositionVector()).add(-playerReach, -playerReach, -playerReach),
                BlockUtils.fromVecToBP(Charmonium.mc.thePlayer.getPositionVector()).add(playerReach, playerReach, playerReach)
        );
        blocks.forEach(i -> {
            Block block = BlockUtils.getBlock(i);
            if (isMithrilBlock(block)) {
                if (BlockUtils.distanceFromTo(mc.thePlayer.getPositionVector(), BlockUtils.fromBPToVec(i)) < mc.playerController.getBlockReachDistance() * 0.8)
                    returnBlocks.add(i);
            }
        });

        return returnBlocks;
    }

    List<BlockPos> getTitAround() {
        double playerReach = mc.playerController.getBlockReachDistance() * 0.8;
        List<BlockPos> returnBlocks = new ArrayList<>();
        Iterable<BlockPos> blocks = BlockPos.getAllInBox(
                BlockUtils.fromVecToBP(Charmonium.mc.thePlayer.getPositionVector()).add(-playerReach, -playerReach, -playerReach),
                BlockUtils.fromVecToBP(Charmonium.mc.thePlayer.getPositionVector()).add(playerReach, playerReach, playerReach)
        );
        blocks.forEach(i -> {
            Block block = BlockUtils.getBlock(i);
            if (block == Blocks.stone.getDefaultState().withProperty(BlockStone.VARIANT, BlockStone.EnumType.DIORITE_SMOOTH).getBlock()) {
                if (BlockUtils.distanceFromTo(mc.thePlayer.getPositionVector(), BlockUtils.fromBPToVec(i)) < mc.playerController.getBlockReachDistance() * 0.8)
                    returnBlocks.add(i);
            }
        });

        return returnBlocks;
    }

    private boolean isMithrilBlock(Block block) {
        return block == Blocks.wool || block == Blocks.stained_hardened_clay || block == Blocks.prismarine;
    }

    public static String[] drawInfo() {
        if (SkillTracker.skillsInitialized()) {
            double xpToShow = SkillTracker.getText("Mining");
            int xpPerHour = (int) Math.round(xpToShow / ((SkillTracker.skillStopwatch.getTime() + 1) / 3600000d));
            int nxtLvl = Config.Skill_Mining + 1;

            return new String[]{
                    "§r§lStats:",
                    "§rMacro: §fMithril Miner",
                    "§rMithril Nearby: §f" + (!mithril.isEmpty() ? mithril.size() : "None"),
                    "§rTime: §f" + (Scheduler.getInstance().isRunning() ? Scheduler.getInstance().getStatusString() : "Macroing"),
                    "§rXP Earned: §f" + NumberFormat.getNumberInstance(Locale.US).format(xpToShow) + " [" + NumberFormat.getNumberInstance(Locale.US).format(xpPerHour) + "/hr]",
                    "§rTime til' Lvl. " + nxtLvl + ": §f" + SkillTracker.getTimeBetween(0, SkillTracker.xpLeft / (xpPerHour / 3600D)),
                    "§rState: §f" + currentState.name(),
            };
        }

        return new String[]{
                "§r§lStats:",
                "§rMacro: §fMithril Miner",
                "§rMithril Nearby: §f" + (!mithril.isEmpty() ? mithril.size() : "None"),
                "§rTime: §f" + (Scheduler.getInstance().isRunning() ? Scheduler.getInstance().getStatusString() : "Macroing"),
                "§r Open '/skills' to track xp",
                "§rState: §f" + currentState.name(),
        };
    }

    public enum State {
        NONE,
        AT_SPAWN,
        AT_MITHRIL,
        MINING,
    }
}
