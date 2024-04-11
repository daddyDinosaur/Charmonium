package com.mylk.charmonium.handler;

import com.mojang.realmsclient.gui.ChatFormatting;
import com.mylk.charmonium.Charmonium;
import com.mylk.charmonium.config.Config;
import com.mylk.charmonium.failsafe.FailsafeManager;
import com.mylk.charmonium.failsafe.impl.DirtFailsafe;
import com.mylk.charmonium.mixin.gui.IGuiPlayerTabOverlayAccessor;
import com.mylk.charmonium.util.*;
import com.mylk.charmonium.util.helper.Timer;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.scoreboard.Score;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import scala.Char;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GameStateHandler {
    private static GameStateHandler INSTANCE;
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Pattern areaPattern = Pattern.compile("Area:\\s(.+)");
    private final Timer notMovingTimer = new Timer();
    private final Timer reWarpTimer = new Timer();
    private final Pattern serverClosingPattern = Pattern.compile("Server closing: (?<minutes>\\d+):(?<seconds>\\d+) .*");
    @Getter
    private Location lastLocation = Location.TELEPORTING;
    @Getter
    private Location location = Location.TELEPORTING;
    @Getter
    private boolean frontWalkable;
    @Getter
    private boolean rightWalkable;
    @Getter
    private boolean backWalkable;
    @Getter
    private boolean leftWalkable;
    @Getter
    private double dx;
    @Getter
    private double dz;
    @Getter
    private double dy;
    @Getter
    private String serverIP;
    private long randomValueToWait = Config.getRandomTimeBetweenChangingRows();
    private long randomRewarpValueToWait = Config.getRandomRewarpDelay();
    @Getter
    private BuffState cookieBuffState = BuffState.UNKNOWN;
    @Getter
    private BuffState godPotState = BuffState.UNKNOWN;
    @Getter
    private double currentPurse = 0;
    @Getter
    private double previousPurse = 0;
    @Getter
    private long bits = 0;
    @Getter
    private long copper = 0;
    private long randomValueToWaitNextTime = -1;
    @Getter
    @Setter
    private Optional<Integer> serverClosingSeconds = Optional.empty();
    @Getter
    private int speed = 0;
    public static ArrayList<Location> allowedIslands = new ArrayList<>();;
    public static String islandWarp = "";
    public static int dungeonFloor = -1;
    public static boolean inDungeons = false;
    public static boolean inBoss = false;

    public static GameStateHandler getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new GameStateHandler();
        }
        return INSTANCE;
    }

    @SubscribeEvent
    public void onWorldChange(WorldEvent.Unload event) {
        lastLocation = location;
        location = Location.TELEPORTING;
        inDungeons = false;
        dungeonFloor = -1;
        inBoss = false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onChat(ClientChatReceivedEvent event) {
        if (!inDungeons) return;
        String message = event.message.getFormattedText();

        if ((message.startsWith("§r§c[BOSS] ") && !message.contains(" The Watcher§r§f:")) || message.startsWith("§r§4[BOSS] ")) {
            if (!inBoss) {
                inBoss = true;
            }
        }
    }

    @SubscribeEvent
    public void onTickCheckCoins(TickEvent.PlayerTickEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;

        List<String> scoreboardLines = ScoreboardUtils.getScoreboardLines();
        if (scoreboardLines.isEmpty()) return;

        for (String line : scoreboardLines) {
            String cleanedLine = StringUtils.stripControlCodes(ScoreboardUtils.cleanSB(line));
            Matcher serverClosingMatcher = serverClosingPattern.matcher(StringUtils.stripControlCodes(ScoreboardUtils.cleanSB(line)));
            if (serverClosingMatcher.find()) {
                int minutes = Integer.parseInt(serverClosingMatcher.group("minutes"));
                int seconds = Integer.parseInt(serverClosingMatcher.group("seconds"));
                serverClosingSeconds = Optional.of(minutes * 60 + seconds);
            } else {
                serverClosingSeconds = Optional.empty();
            }
            if (cleanedLine.contains("Purse:") || cleanedLine.contains("Piggy:")) {
                try {
                    String stringPurse = cleanedLine.split(" ")[1].replace(",", "").trim();
                    if (stringPurse.contains("(+")) {
                        stringPurse = stringPurse.substring(0, stringPurse.indexOf("("));
                    }
                    long tempCurrentPurse = Long.parseLong(stringPurse);
                    previousPurse = currentPurse;
                    currentPurse = tempCurrentPurse;
                } catch (NumberFormatException ignored) {
                }
            } else if (cleanedLine.contains("Bits:")) {
                try {
                    String stringBits = cleanedLine.split(" ")[1].replace(",", "").trim();
                    if (stringBits.contains("(+")) {
                        stringBits = stringBits.substring(0, stringBits.indexOf("("));
                    }
                    bits = Long.parseLong(stringBits);
                } catch (NumberFormatException ignored) {
                }
            } else if (cleanedLine.contains("Copper:")) {
                try {
                    String stringCopper = cleanedLine.split(" ")[1].replace(",", "").trim();
                    if (stringCopper.contains("(+")) {
                        stringCopper = stringCopper.substring(0, stringCopper.indexOf("("));
                    }
                    copper = Long.parseLong(stringCopper);
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    @SubscribeEvent
    public void onTickCheckBuffs(TickEvent.ClientTickEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;

        boolean foundGodPotBuff = false;
        boolean foundCookieBuff = false;
        boolean foundPestRepellent = false;
        boolean loaded = false;

        IGuiPlayerTabOverlayAccessor tabOverlay = (IGuiPlayerTabOverlayAccessor) mc.ingameGUI.getTabList();
        if (tabOverlay == null) return;
        IChatComponent footer = tabOverlay.getFooter();
        if (footer == null || footer.getFormattedText().isEmpty()) return;
        String[] footerString = footer.getFormattedText().split("\n");

        for (String line : footerString) {
            String unformattedLine = StringUtils.stripControlCodes(line);
            if (unformattedLine.contains("Active Effects")) {
                loaded = true;
            }
            if (unformattedLine.contains("You have a God Potion active!")) {
                foundGodPotBuff = true;
                continue;
            }
            if (unformattedLine.contains("Cookie Buff")) {
                foundCookieBuff = true;
                continue;
            }
            if (foundCookieBuff) {
                if (unformattedLine.contains("Not active")) {
                    foundCookieBuff = false;
                }
                break;
            }
        }

        if (!loaded) {
            cookieBuffState = BuffState.UNKNOWN;
            godPotState = BuffState.UNKNOWN;
            return;
        }

        cookieBuffState = foundCookieBuff ? BuffState.ACTIVE : BuffState.NOT_ACTIVE;
        godPotState = foundGodPotBuff ? BuffState.ACTIVE : BuffState.NOT_ACTIVE;
    }

    @SubscribeEvent
    public void onTickCheckLocation(TickEvent.ClientTickEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;

        if (mc.getCurrentServerData() != null && mc.getCurrentServerData().serverIP != null) {
            serverIP = mc.getCurrentServerData().serverIP;
        }

        if (TablistUtils.getTabList().size() == 1 && ScoreboardUtils.getScoreboardLines().isEmpty() && PlayerUtils.isInventoryEmpty(mc.thePlayer)) {
            lastLocation = location;
            location = Location.LIMBO;
            return;
        }

        if (!inDungeons && !ScoreboardUtils.getScoreboardLines().isEmpty()) {
            Optional<String> dungeonLine = ScoreboardUtils.getScoreboardLines().stream()
                    .filter(lineD -> ScoreboardUtils.cleanSB(lineD).contains("The Catacombs ("))
                    .filter(lineD -> !ScoreboardUtils.cleanSB(lineD).contains("Queue"))
                    .findFirst();

            dungeonLine.ifPresent(lineD -> {
                inDungeons = true;
                dungeonFloor = extractDungeonFloor(lineD);
            });
        }

        for (String line : TablistUtils.getTabList()) {
            Matcher matcher = areaPattern.matcher(line);
            if (matcher.find()) {
                String area = matcher.group(1);
                for (Location island : Location.values()) {
                    if (area.equals(island.getName())) {
                        lastLocation = location;
                        location = island;

                        return;
                    }
                }
            }
        }

        if (!ScoreboardUtils.getScoreboardTitle().contains("SKYBLOCK") && !ScoreboardUtils.getScoreboardLines().isEmpty() && ScoreboardUtils.cleanSB(ScoreboardUtils.getScoreboardLines().get(0)).contains("www.hypixel.net")) {
            lastLocation = location;
            location = Location.LOBBY;
            return;
        }
        if (location != Location.TELEPORTING) {
            lastLocation = location;
        }
        location = Location.TELEPORTING;
    }

    @SubscribeEvent
    public void onTickCheckSpeed(TickEvent.ClientTickEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;

        float speed = mc.thePlayer.capabilities.getWalkSpeed();
        this.speed = (int) (speed * 1_000);
    }

    @SubscribeEvent
    public void onTickCheckMoving(TickEvent.ClientTickEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;

        dx = Math.abs(mc.thePlayer.motionX);
        dy = Math.abs(mc.thePlayer.motionY);
        dz = Math.abs(mc.thePlayer.motionZ);


        if (notMoving() && mc.currentScreen == null) {
            if (hasPassedSinceStopped()) {
                if (DirtFailsafe.getInstance().hasDirtBlocks() && DirtFailsafe.getInstance().isTouchingDirtBlock()) {
                    FailsafeManager.getInstance().possibleDetection(DirtFailsafe.getInstance());
                } else {
                    randomValueToWaitNextTime = -1;
                    notMovingTimer.reset();
                }
            }
        } else {
            if (!notMovingTimer.isScheduled())
                randomValueToWait = Config.getRandomTimeBetweenChangingRows();
            notMovingTimer.schedule();
        }
        float yaw = mc.thePlayer.rotationYaw;
        frontWalkable = BlockUtils.canWalkThroughDoor(BlockUtils.Direction.FORWARD) && BlockUtils.canWalkThrough(BlockUtils.getRelativeBlockPos(0, 0, 1, yaw), BlockUtils.Direction.FORWARD);
        backWalkable = BlockUtils.canWalkThroughDoor(BlockUtils.Direction.BACKWARD) && BlockUtils.canWalkThrough(BlockUtils.getRelativeBlockPos(0, 0, -1, yaw), BlockUtils.Direction.BACKWARD);
        rightWalkable = BlockUtils.canWalkThroughDoor(BlockUtils.Direction.RIGHT) && BlockUtils.canWalkThrough(BlockUtils.getRelativeBlockPos(1, 0, 0, yaw), BlockUtils.Direction.RIGHT);
        leftWalkable = BlockUtils.canWalkThroughDoor(BlockUtils.Direction.LEFT) && BlockUtils.canWalkThrough(BlockUtils.getRelativeBlockPos(-1, 0, 0, yaw), BlockUtils.Direction.LEFT);
    }

    @SubscribeEvent
    public void onTickCheckRewarp(TickEvent.ClientTickEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;
        if (!MacroHandler.getInstance().isMacroToggled()) return;

        reWarpTimer.reset();
    }

    private int extractDungeonFloor(String line) {
        Pattern pattern = Pattern.compile("[FM]\\d+");
        Matcher matcher = pattern.matcher(line);

        if (matcher.find()) {
            String floorSubstring = matcher.group();

            try {
                return Integer.parseInt(floorSubstring.substring(1));
            } catch (NumberFormatException e) {
                return -1;
            }
        }

        return -1;
    }

    private boolean isSimilar(String message, String entry) {
        String pattern = ".*" + Pattern.quote(entry) + ".*";
        Matcher matcher = Pattern.compile(pattern).matcher(message);
        return matcher.matches();
    }

    public static String getPhase() {
        if (dungeonFloor != 7 || !inBoss) return null;

        int posY = Charmonium.mc.thePlayer.getPosition().getY();
        if (posY > 210) {
            return "P1";
        } else if (posY > 155) {
            return "P2";
        } else if (posY > 100) {
            return "P3";
        } else if (posY > 45) {
            return "P4";
        } else {
            return "P5";
        }
    }


    public boolean canRewarp() {
        return reWarpTimer.hasPassed(randomRewarpValueToWait);
    }

    public void scheduleRewarp() {
        randomRewarpValueToWait = Config.getRandomRewarpDelay();
        reWarpTimer.reset();
    }

    public boolean hasPassedSinceStopped() {
        return notMovingTimer.hasPassed(randomValueToWaitNextTime != -1 ? randomValueToWaitNextTime : randomValueToWait);
    }

    public boolean notMoving() {
        return (dx < 0.01 && dz < 0.01 && dyIsRest() && mc.currentScreen == null) || (!holdingKeybindIsWalkable() && mc.thePlayer != null && (playerIsInFlowingWater(0) || playerIsInFlowingWater(1)) && mc.thePlayer.isInWater());
    }

    private boolean dyIsRest() {
        return dy < 0.05 || dy <= 0.079 && dy >= 0.078; // weird calculation of motionY being -0.0784000015258789 while resting at block and 0.0 is while flying for some reason
    }

    private boolean playerIsInFlowingWater(int y) {
        IBlockState state = mc.theWorld.getBlockState(BlockUtils.getRelativeBlockPos(0, y, 0));
        if (!state.getBlock().equals(Blocks.water)) return false;
        int level = state.getValue(BlockLiquid.LEVEL);
        if (level == 0) {
            double motionX = mc.thePlayer.motionX;
            double motionZ = mc.thePlayer.motionZ;
            return motionX > 0.01 || motionX < -0.01 || motionZ > 0.01 || motionZ < -0.01;
        } else {
            return true;
        }
    }

    public boolean holdingKeybindIsWalkable() {
        KeyBinding[] holdingKeybinds = KeyBindUtils.getHoldingKeybinds();
        for (KeyBinding key : holdingKeybinds) {
            if (key != null && key.isKeyDown()) {
                if (key == mc.gameSettings.keyBindForward && !frontWalkable) {
                    return false;
                } else if (key == mc.gameSettings.keyBindBack && !backWalkable) {
                    return false;
                } else if (key == mc.gameSettings.keyBindRight && !rightWalkable) {
                    return false;
                } else if (key == mc.gameSettings.keyBindLeft && !leftWalkable) {
                    return false;
                }
            }
        }
        return true;
    }

    public void scheduleNotMoving(int time) {
        randomValueToWaitNextTime = time;
        notMovingTimer.schedule();
    }

    public void scheduleNotMoving() {
        randomValueToWait = Config.getRandomTimeBetweenChangingRows();
        notMovingTimer.schedule();
    }

    public boolean atProperIsland() {
        if (this.location == Location.TELEPORTING) return false;
        else return allowedIslands.contains(this.location);
    }

    @Getter
    public enum Location {
        PRIVATE_ISLAND("Private Island"),
        HUB("Hub"),
        THE_PARK("The Park"),
        THE_FARMING_ISLANDS("The Farming Islands"),
        SPIDER_DEN("Spider's Den"),
        THE_END("The End"),
        CRIMSON_ISLE("Crimson Isle"),
        GOLD_MINE("Gold Mine"),
        DEEP_CAVERNS("Deep Caverns"),
        DWARVEN_MINES("Dwarven Mines"),
        CRYSTAL_HOLLOWS("Crystal Hollows"),
        JERRY_WORKSHOP("Jerry's Workshop"),
        DUNGEON_HUB("Dungeon Hub"),
        LIMBO("UNKNOWN"),
        LOBBY("PROTOTYPE"),
        GARDEN("Garden"),
        DUNGEON("Dungeon"),
        TELEPORTING("Teleporting");

        private final String name;

        Location(String name) {
            this.name = name;
        }
    }

    public enum BuffState {
        ACTIVE,
        FAILSAFE,
        NOT_ACTIVE,
        UNKNOWN
    }
}
