package com.mylk.charmonium.config;

import cc.polyfrost.oneconfig.config.annotations.Number;
import cc.polyfrost.oneconfig.config.annotations.*;
import cc.polyfrost.oneconfig.config.core.OneKeyBind;
import cc.polyfrost.oneconfig.config.data.*;
import com.mylk.charmonium.config.page.AOTVWaypointsPage;
import com.mylk.charmonium.config.page.AutoSellNPCItemsPage;
import com.mylk.charmonium.config.page.CustomFailsafeMessagesPage;
import com.mylk.charmonium.config.page.FailsafeNotificationsPage;
import com.mylk.charmonium.failsafe.FailsafeManager;
import com.mylk.charmonium.feature.impl.*;
import com.mylk.charmonium.handler.GameStateHandler;
import com.mylk.charmonium.handler.MacroHandler;
import com.mylk.charmonium.util.LogUtils;
import com.mylk.charmonium.util.PlayerUtils;
import com.mylk.charmonium.util.helper.AudioManager;
import net.minecraft.client.Minecraft;
import net.minecraft.item.EnumDyeColor;
import net.minecraftforge.fml.common.Loader;
import org.lwjgl.input.Keyboard;
import com.mylk.charmonium.macro.impl.misc.macroHud;

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Config extends cc.polyfrost.oneconfig.config.Config {
    private transient static final Minecraft mc = Minecraft.getMinecraft();
    private transient static final String GENERAL = "General";
    private transient static final String MISCELLANEOUS = "Miscellaneous";
    private transient static final String FAILSAFE = "Failsafe";
    private transient static final String SCHEDULER = "Scheduler";
    private transient static final String DISCORD_INTEGRATION = "Discord Integration";
    private transient static final String DELAYS = "Delays";
    private transient static final String HUD = "HUD";
    private transient static final String DEBUG = "Debug";
    private transient static final String EXPERIMENTAL = "Experimental";
    private transient static final String MOB_MACROS = "Mob Killer Macros";
    private transient static final String MITHRIL_MACRO = "Mithril Macro";
    private transient static final String SLAYER_MACRO = "Slayer Macro";
    private transient static final String FISHING_MACRO = "Fishing Macro";
    private transient static final String GEMSTONE_MACRO = "Gemstone Macro";

    //<editor-fold desc="GENERAL">
    public enum MacroEnum {
        NONE, FORAGING, ICE_WALKER, MITHRIL_MINER, SLAYER, FISHING, GEMSTONE
    }

    public enum FailsafeException {
        BEDROCK_CAGE_CHECK, ROTATION_CHECK, TELEPORT_CHECK
    }

    //Nuker
    @Dropdown(
            name = "Macros List", category = GENERAL,
            description = "Macro Select Type",
            options = {
                    "None", // 0
                    "Foraging", // 1
                    "Ice Walker", // 2
                    "Mithril Miner", // 3
                    "Slayers", // 4
                    "Fishing", // 5
                    "Gemstone", // 6
            }
    )
    public static int SMacroType = 0;

    public static MacroEnum getMacro() {
        return MacroEnum.values()[SMacroType];
    }

    public static Set<FailsafeException> getFailsafeExceptions() {
        Set<FailsafeException> exceptions = new HashSet<>();

        if (getMacro() == MacroEnum.SLAYER || getMacro() == MacroEnum.MITHRIL_MINER) {
            exceptions.add(FailsafeException.BEDROCK_CAGE_CHECK);
        }

        if (getMacro() == MacroEnum.SLAYER || getMacro() == MacroEnum.FISHING || getMacro() == MacroEnum.GEMSTONE) {
            exceptions.add(FailsafeException.ROTATION_CHECK);
        }

        if (getMacro() == MacroEnum.SLAYER || getMacro() == MacroEnum.FISHING || getMacro() == MacroEnum.GEMSTONE) {
            exceptions.add(FailsafeException.TELEPORT_CHECK);
        }

        return exceptions;
    }

    @Slider(
            name = "Scanning Range", category = GENERAL, subcategory = "All",
            min = 5, max = 50, step = 1
    )
    public static int scanRange = 8;
    @Switch(
            name = "Shift while killing", category = GENERAL, subcategory = "All",
            size = 2
    )
    public static boolean shiftWhenKill = false;
    @Switch(
            name = "Jump when walking around", category = GENERAL, subcategory = "All",
            size = 2
    )
    public static boolean jumpWhenWalking = false;

    public enum SAttackEnum {
        LEFT_CLICK,
        RANGED
    }
    @Dropdown(
            name = "Attack mode",
            description = "Select attack type", category = GENERAL, subcategory = "All",
            options = {
                    "Left Click", // 0
                    "Right Click", // 1
            }
    )
    public static int SAttackType = 0;

    @Dropdown(
            name = "Mithril Location",
            description = "Where you wanna mine", category = GENERAL, subcategory = MITHRIL_MACRO,
            options = {
                    "A", // 0
                    "B", // 1
            }
    )
    public static int SMSpotType = 0;

    @Dropdown(
            name = "Mithril Tool", category = GENERAL, subcategory = MITHRIL_MACRO,
            options = {
                    "Gemstone Gauntlet", // 0
                    "Titanium Drill", // 1
                    "Pickaxe", // 2
                    "Pickonimbus" // 3
            }
    )
    public static int commTool = 0;

    public static String getCommTool() {
        switch (commTool) {
            case 0:
                return "Gemstone Gauntlet";
            case 1:
                return "Titanium Drill";
            case 3:
                return "Pickonimbus";
            default:
                return "Pickaxe";
        }
    }

    @Text(name = "Slayer Weapon", placeholder = "Type your weapon name here", category = GENERAL, subcategory = SLAYER_MACRO)
    public static String slayerWeaponName = null;

    @Switch(name = "Auto Maddox (abiphone)", category = GENERAL, subcategory = SLAYER_MACRO)
    public static boolean autoMaddox = false;

    @Dropdown(
            name = "Boss Type",
            category = GENERAL,
            subcategory = SLAYER_MACRO,
            options = {"Revenant", "Broodfather", "Sven (Howl)", "Sven (Crypts)", "Voidgloom (Sepeture)", "Voidgloom (Bruser)"}
    )
    public static int SlayerType = 0;

    public static int getSlayerType() {
        switch (SlayerType) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 2;
            case 4:
                return 3;
            case 5:
                return 3;
            default:
                return 0;
        }
    }

    @Dropdown(
            name = "Boss Level",
            category = GENERAL,
            subcategory = SLAYER_MACRO,
            options = {"1", "2", "3", "4", "5"}
    )
    public static int SlayerTier = 0;

    @Switch(name = "Use Warps", category = GENERAL, subcategory = SLAYER_MACRO)
    public static boolean useWarps = false;

    @Switch(name = "Auto Heal", category = GENERAL, subcategory = SLAYER_MACRO)
    public static boolean autoHeal = false;

    @Dropdown(
            name = "Healing Tool", category = GENERAL, subcategory = SLAYER_MACRO,
            options = {
                    "Wand of ...", // 0
                    "Zombie Sword" // 1
            }
    )
    public static int healingItem = 0;

    public static String getHealingItem() {
        switch (commTool) {
            case 0:
                return "Wand of";
            case 1:
                return "Zombie Sword";
            default:
                return "Wand of";
        }
    }

    @Slider(
            name = "Sea Creature Range", category = GENERAL, subcategory = FISHING_MACRO,
            min = 0, max = 12, step = 1
    )
    public static int seaCeatureRange = 6;

    @Slider(
            name = "Recast Delay", category = GENERAL, subcategory = FISHING_MACRO,
            min = 0, max = 2500, step = 25
    )
    public static int recastDelay = 750;

    @Text(name = "Fishing Weapon", placeholder = "Type your weapon name here", category = GENERAL, subcategory = FISHING_MACRO)
    public static String fishingWeaponName = null;

    @Text(name = "Fishing Rod", placeholder = "Type your rod name here", category = GENERAL, subcategory = FISHING_MACRO)
    public static String fishingRodName = null;

    @Switch(name = "Make Glass Panes Full", category = GENERAL, subcategory = GEMSTONE_MACRO)
    public static boolean glassPanesFullBlock = false;

    @Text(name = "Gemstone Tool", placeholder = "Type your mining tool here", category = GENERAL, subcategory = GEMSTONE_MACRO)
    public static String gemstoneTool = null;

    @Page(
            name = "List of waypoints",
            location = PageLocation.BOTTOM,
            category = GENERAL,
            subcategory = GEMSTONE_MACRO
    )
    public AOTVWaypointsPage aotvWaypointsPage = new AOTVWaypointsPage();

    @Switch(name = "Render Waypoints", category = GENERAL, subcategory = GEMSTONE_MACRO)
    public static boolean aotvHighlightRouteBlocks = false;

    @Switch(name = "Show Number", category = GENERAL, subcategory = GEMSTONE_MACRO)
    public static boolean aotvShowNumber = false;

    @Switch(name = "Route lines", category = GENERAL, subcategory = GEMSTONE_MACRO)
    public static boolean aotvShowRouteLines = false;

    @Switch(name = "Refuel with abiphone", category = GENERAL, subcategory = GEMSTONE_MACRO)
    public static boolean refuelWithAbiphone = false;

    @Slider(name = "Refuel if less than", category = GENERAL, subcategory = GEMSTONE_MACRO, max = 3000, step = 50, min = 0.0F)
    public static int refuelThreshold = 200;

    @Dropdown(name = "Type of fuel to use", category = GENERAL, subcategory = GEMSTONE_MACRO, options = {"Goblin Egg", "Biofuel", "Volta", "Oil Barrel"})
    public static int typeOfFuelIndex = 0;

    @Switch(name = "Use Mining Speed Boost", category = GENERAL, subcategory = GEMSTONE_MACRO)
    public static boolean useMiningSpeedBoost = false;

    @Switch(name = "Blue Cheese Omlette Swap", category = GENERAL, subcategory = GEMSTONE_MACRO)
    public static boolean blueCheeseOmeletteToggle = false;

    @Slider(
            name = "Rotation Delay", category = GENERAL, subcategory = GEMSTONE_MACRO,
            min = 0, max = 1250, step = 25
    )
    public static int gemstoneRotationDelay = 75;

    @Slider(
            name = "Teleport Rotation Delay", category = GENERAL, subcategory = GEMSTONE_MACRO,
            min = 0, max = 1250, step = 25
    )
    public static int gemstoneTeleportRotDelay = 50;

    @Slider(
            name = "Teleport Delay", category = GENERAL, subcategory = GEMSTONE_MACRO,
            min = 100, max = 2000, step = 50
    )
    public static int gemstoneTeleportDelay = 500;

    @Slider(
            name = "Stuck Delay", category = GENERAL, subcategory = GEMSTONE_MACRO,
            min = 1500, max = 7500, step = 100
    )
    public static int gemstoneStuckDelay = 2000;

//    @Slider(
//            name = "Ping Glide", category = GENERAL, subcategory = GEMSTONE_MACRO,
//            min = 0, max = 90, step = 5
//    )
//    public static int pingGlide = 85;

    @Dropdown(name = "Type of glass to mine", category = GENERAL, subcategory = GEMSTONE_MACRO, options = {"Ruby", "Amethyst", "Topaz", "Sapphire", "Amber", "Jade", "Jasper", "All"})
    public static int glassFilter = 0;

    public static EnumDyeColor[] getRequiredColors() {
        switch (glassFilter) {
            case 0:
                return new EnumDyeColor[]{EnumDyeColor.RED};
            case 1:
                return new EnumDyeColor[]{EnumDyeColor.PURPLE};
            case 2:
                return new EnumDyeColor[]{EnumDyeColor.YELLOW};
            case 3:
                return new EnumDyeColor[]{EnumDyeColor.LIGHT_BLUE};
            case 4:
                return new EnumDyeColor[]{EnumDyeColor.ORANGE};
            case 5:
                return new EnumDyeColor[]{EnumDyeColor.LIME};
            case 6:
                return new EnumDyeColor[]{EnumDyeColor.MAGENTA};
            case 7:
                return new EnumDyeColor[]{EnumDyeColor.BLACK};
            default:
                return null;
        }
    }

//    @Switch(
//            name = "Test", category = GENERAL, subcategory = MISCELLANEOUS,
//            description = "da test"
//    )
//    public static boolean test = false;

    @HUD(
            name = "Macro HUD",  category = HUD
    )
    public static macroHud macroHud = new macroHud();

    //</editor-fold>

    //</editor-fold>

    //<editor-fold desc="MISC">
    //<editor-fold desc="Keybinds">
    @KeyBind(
            name = "Toggle Charmonium", category = MISCELLANEOUS, subcategory = "Keybinds",
            description = "Toggles the macro on/off", size = 2
    )
    public static OneKeyBind toggleMacro = new OneKeyBind(Keyboard.KEY_GRAVE);
    @KeyBind(
            name = "Open GUI", category = MISCELLANEOUS, subcategory = "Keybinds",
            description = "Opens Charmonium configuration menu", size = 2
    )

    public static OneKeyBind openGuiKeybind = new OneKeyBind(Keyboard.KEY_F);
    @KeyBind(
            name = "Freelook", category = MISCELLANEOUS, subcategory = "Keybinds",
            description = "Locks rotation, lets you freely look", size = 2
    )
    public static OneKeyBind freelookKeybind = new OneKeyBind(Keyboard.KEY_L);

    @Info(
            text = "Freelook doesn't work properly with Oringo!", type = InfoType.WARNING,
            category = MISCELLANEOUS, subcategory = "Keybinds"
    )
    private int freelookWarning;
    //</editor-fold>

    //<editor-fold desc="Miscellaneous">
    @Switch(
            name = "Mute The Game", category = MISCELLANEOUS, subcategory = "Miscellaneous",
            description = "Mutes the game while farming"
    )
    public static boolean muteTheGame = false;

    @Switch(name = "Auto Renew Crystal Hollows Pass", category = MISCELLANEOUS, subcategory = "Miscellaneous")
    public static boolean autoRenewCrystalHollowsPass = false;
    @Switch(
            name = "Auto Cookie", category = MISCELLANEOUS, subcategory = "Miscellaneous",
            description = "Automatically purchases and consumes a booster cookie"
    )
    public static boolean autoCookie = false;
    @Switch(
            name = "Auto Ungrab Mouse", category = MISCELLANEOUS, subcategory = "Miscellaneous",
            description = "Automatically ungrabs your mouse, so you can safely alt-tab"
    )
    public static boolean autoUngrabMouse = true;

    @Switch(
            name = "Auto Rune Combiner", category = MISCELLANEOUS, subcategory = "Miscellaneous",
            description = "Rune bot go brr"
    )
    public static boolean runeCombiner = false;
    //</editor-fold>

    //<editor-fold desc="God Pot">
    @Switch(
            name = "Auto God Pot", category = MISCELLANEOUS, subcategory = "God Pot",
            description = "Automatically purchases and consumes a God Pot", size = 2
    )
    public static boolean autoGodPot = false;

    @Switch(
            name = "Get God Pot from Backpack", category = MISCELLANEOUS, subcategory = "God Pot", size = 2
    )
    public static boolean autoGodPotFromBackpack = true;

    @DualOption(
            name = "Storage Type", category = MISCELLANEOUS, subcategory = "God Pot",
            description = "The storage type to get god pots from",
            left = "Backpack",
            right = "Ender Chest"
    )
    public static boolean autoGodPotStorageType = true;

    @Number(
            name = "Backpack Number", category = MISCELLANEOUS, subcategory = "God Pot",
            description = "The backpack number, that contains god pots",
            min = 1, max = 18
    )
    public static int autoGodPotBackpackNumber = 1;

    @Switch(
            name = "Buy God Pot using Bits", category = MISCELLANEOUS, subcategory = "God Pot"
    )
    public static boolean autoGodPotFromBits = false;

    @Switch(
            name = "Get God Pot from Auction House", category = MISCELLANEOUS, subcategory = "God Pot",
            description = "If the user doesn't have a cookie, it will go to the hub and buy from AH"
    )
    public static boolean autoGodPotFromAH = false;

    @Info(
            text = "Priority getting God Pot is: Backpack -> Bits -> AH",
            type = InfoType.INFO, size = 2, category = MISCELLANEOUS, subcategory = "God Pot"
    )
    private static int godPotInfo;

    //</editor-fold>

    //<editor-fold desc="Auto Sell">
    @Info(
            text = "Click ESC during Auto Sell, to stop it and pause for the next 15 minutes",
            category = MISCELLANEOUS, subcategory = "Auto Sell", type = InfoType.INFO, size = 2
    )
    public static boolean autoSellInfo;

    @Switch(
            name = "Enable Auto Sell", category = MISCELLANEOUS, subcategory = "Auto Sell",
            description = "Enables auto sell"
    )
    public static boolean enableAutoSell = false;

    @DualOption(
            name = "Market type", category = MISCELLANEOUS, subcategory = "Auto Sell",
            description = "The market type to sell crops to",
            left = "BZ",
            right = "NPC"
    )
    public static boolean autoSellMarketType = false;

    @Switch(
            name = "Sell Items In Sacks", category = MISCELLANEOUS, subcategory = "Auto Sell",
            description = "Sells items in your sacks and inventory"
    )
    public static boolean autoSellSacks = false;

    @DualOption(
            name = "Sacks placement",
            category = MISCELLANEOUS, subcategory = "Auto Sell",
            description = "The sacks placement",
            left = "Inventory",
            right = "Sack of sacks"
    )
    public static boolean autoSellSacksPlacement = true;

    @Number(
            name = "Inventory Full Time", category = MISCELLANEOUS, subcategory = "Auto Sell",
            description = "The time to wait to test if inventory fullness ratio is still the same (or higher)",
            min = 1, max = 20
    )
    public static int inventoryFullTime = 6;

    @Number(
            name = "Inventory Full Ratio", category = MISCELLANEOUS, subcategory = "Auto Sell",
            description = "After reaching this ratio, the macro will start counting from 0 to Inventory Full Time. If the fullness ratio is still the same (or higher) after the time has passed, it will start selling items.",
            min = 1, max = 100
    )
    public static int inventoryFullRatio = 65;

    @Button(
            name = "Sell Inventory Now", category = MISCELLANEOUS, subcategory = "Auto Sell",
            description = "Sells crops in your inventory",
            text = "Sell Inventory Now"
    )
    Runnable autoSellFunction = () -> {
        PlayerUtils.closeScreen();
        AutoSell.getInstance().enable(true);
    };

    @Page(
            name = "Customize items sold to NPC", category = MISCELLANEOUS, subcategory = "Auto Sell", location = PageLocation.BOTTOM,
            description = "Click here to customize items that are sold to NPC automatically"
    )
    public AutoSellNPCItemsPage autoSellNPCItemsPage = new AutoSellNPCItemsPage();
    //</editor-fold>

    //<editor-fold desc="Crop Utils">
    @Switch(
            name = "Increase Cocoa Hitboxes", category = MISCELLANEOUS, subcategory = "Crop Utils",
            description = "Allows you to farm cocoa beans more efficiently at higher speeds by making the hitboxes bigger"
    )
    public static boolean increasedCocoaBeans = true;

    @Switch(
            name = "Increase Crop Hitboxes", category = MISCELLANEOUS, subcategory = "Crop Utils",
            description = "Allows you to farm crops more efficient by making the hitboxes bigger"
    )
    public static boolean increasedCrops = true;

    @Switch(
            name = "Increase Nether Wart Hitboxes", category = MISCELLANEOUS, subcategory = "Crop Utils",
            description = "Allows you to farm nether warts more efficiently at higher speeds by making the hitboxes bigger"
    )
    public static boolean increasedNetherWarts = true;

    @Switch(
            name = "Increase Mushroom Hitboxes", category = MISCELLANEOUS, subcategory = "Crop Utils",
            description = "Allows you to farm mushrooms more efficiently at higher speeds by making the hitboxes bigger"
    )
    public static boolean increasedMushrooms = true;

    @Switch(
            name = "Pingless Cactus", category = MISCELLANEOUS, subcategory = "Crop Utils",
            description = "Allows you to farm cactus more efficiently at higher speeds by making the cactus pingless"
    )
    public static boolean pinglessCactus = true;
    //</editor-fold>

    //</editor-fold>

    //<editor-fold desc="FAILSAFES">
    //<editor-fold desc="Failsafe Misc">
    @Switch(
            name = "Pop-up Notification", category = FAILSAFE, subcategory = "Miscellaneous",
            description = "Enable pop-up notification"
    )
    public static boolean popUpNotification = true;
    @Switch(
            name = "Auto alt-tab when failsafe triggered", category = FAILSAFE, subcategory = "Miscellaneous",
            description = "Automatically alt-tabs to the game when the dark times come"
    )
    public static boolean autoAltTab = true;
    @Switch(
            name = "Try to use jumping and flying in failsafes reactions", category = FAILSAFE, subcategory = "Miscellaneous",
            description = "Tries to use jumping and flying in failsafes reactions"
    )
    public static boolean tryToUseJumpingAndFlying = true;
    @Slider(
            name = "Failsafe Stop Delay", category = FAILSAFE, subcategory = "Miscellaneous",
            description = "The delay to stop the macro after failsafe has been triggered (in milliseconds)",
            min = 1_000, max = 7_500
    )
    public static int failsafeStopDelay = 2_000;
    @Switch(
            name = "Auto TP back on World Change", category = FAILSAFE, subcategory = "Miscellaneous",
            description = "Automatically warps back to the garden on server reboot, server update, etc"
    )
    public static boolean autoTPOnWorldChange = true;
    @Switch(
            name = "Auto Evacuate on World update", category = FAILSAFE, subcategory = "Miscellaneous",
            description = "Automatically evacuates the island on server reboot, server update, etc"
    )
    public static boolean autoEvacuateOnWorldUpdate = true;
    @Switch(
            name = "Auto reconnect on disconnect", category = FAILSAFE, subcategory = "Miscellaneous",
            description = "Automatically reconnects to the server when disconnected"
    )
    public static boolean autoReconnect = true;
    @Switch(
            name = "Pause the macro when a guest arrives", category = FAILSAFE, subcategory = "Miscellaneous",
            description = "Pauses the macro when a guest arrives"
    )
    public static boolean pauseWhenGuestArrives = false;
    @Slider(
            name = "Teleport Check Lag Sensitivity", category = FAILSAFE, subcategory = "Miscellaneous",
            description = "Variation in distance between expected and actual positions when lagging",
            min = 0, max = 2
    )
    public static float teleportCheckLagSensitivity = 0.5f;
    @Slider(
            name = "Rotation Check Sensitivity", category = FAILSAFE, subcategory = "Miscellaneous",
            description = "The sensitivity of the rotation check; the lower the sensitivity, the more accurate the check is, but it will also increase the chance of getting false positives.",
            min = 1, max = 10
    )
    public static float rotationCheckSensitivity = 2;
    @Slider(
            name = "Teleport Check Sensitivity", category = FAILSAFE, subcategory = "Miscellaneous",
            description = "The minimum distance between the previous and teleported position to trigger failsafe",
            min = 0.5f, max = 20f
    )
    public static float teleportCheckSensitivity = 4;

    @Switch(
            name = "Average BPS Drop check", category = FAILSAFE, subcategory = "Miscellaneous",
            description = "Checks for average BPS drop"
    )
    public static boolean averageBPSDropCheck = true;

    @Slider(
            name = "Average BPS Drop %", category = FAILSAFE, subcategory = "Miscellaneous",
            description = "The minimum BPS drop to trigger failsafe",
            min = 2, max = 50
    )
    public static int averageBPSDrop = 15;

    @Button(
            name = "Test failsafe", category = FAILSAFE, subcategory = "Miscellaneous",
            description = "Tests failsafe",
            text = "Test failsafe"
    )
    Runnable _testFailsafe = () -> {
        if (!MacroHandler.getInstance().isMacroToggled()) {
            LogUtils.sendError("You need to start the macro first!");
            return;
        }
        LogUtils.sendWarning("Testing failsafe...");
        PlayerUtils.closeScreen();
        if (testFailsafeTypeSelected == 0)
            FailsafeManager.getInstance().possibleDetection(FailsafeManager.getInstance().failsafes.get(testFailsafeTypeSelected));
        else
            FailsafeManager.getInstance().possibleDetection(FailsafeManager.getInstance().failsafes.get(testFailsafeTypeSelected + 2));
    };

    @Dropdown(
            name = "Test Failsafe Type", category = FAILSAFE, subcategory = "Miscellaneous",
            description = "The failsafe type to test",
            options = {
                    "Banwave",
                    "Disconnect",
                    "Evacuate",
                    "Item Change Check",
                    "Rotation Check",
                    "Teleport Check",
                    "World Change Check"
            }
    )
    public static int testFailsafeTypeSelected = 0;

    //</editor-fold>

    //<editor-fold desc="Failsafes conf page">
    @Page(
            name = "Failsafe Notifications", category = FAILSAFE, subcategory = "Failsafe Notifications", location = PageLocation.BOTTOM,
            description = "Click here to customize failsafe notifications"
    )
    public FailsafeNotificationsPage failsafeNotificationsPage = new FailsafeNotificationsPage();
    //</editor-fold>

    //<editor-fold desc="Desync">
    @Switch(
            name = "Check Desync", category = FAILSAFE, subcategory = "Desync",
            description = "If client desynchronization is detected, it activates a failsafe. Turn this off if the network is weak or if it happens frequently."
    )
    public static boolean checkDesync = true;
    @Slider(
            name = "Pause for X milliseconds after desync triggered", category = FAILSAFE, subcategory = "Desync",
            description = "The delay to pause after desync triggered (in milliseconds)",
            min = 3_000, max = 10_000
    )
    public static int desyncPauseDelay = 5_000;
    //</editor-fold>

    //<editor-fold desc="Failsafe Trigger Sound">
    @Switch(
            name = "Enable Failsafe Trigger Sound", category = FAILSAFE, subcategory = "Failsafe Trigger Sound", size = OptionSize.DUAL,
            description = "Makes a sound when a failsafe has been triggered"
    )
    public static boolean enableFailsafeSound = true;
    @DualOption(
            name = "Failsafe Sound Type", category = FAILSAFE, subcategory = "Failsafe Trigger Sound",
            description = "The failsafe sound type to play when a failsafe has been triggered",
            left = "Minecraft",
            right = "Custom"
    )
    public static boolean failsafeSoundType = false;
    @Dropdown(
            name = "Minecraft Sound", category = FAILSAFE, subcategory = "Failsafe Trigger Sound",
            description = "The Minecraft sound to play when a failsafe has been triggered",
            options = {
                    "Ping", // 0
                    "Anvil" // 1
            }
    )
    public static int failsafeMcSoundSelected = 1;

    @Dropdown(
            name = "Custom Sound", category = FAILSAFE, subcategory = "Failsafe Trigger Sound",
            description = "The custom sound to play when a failsafe has been triggered",
            options = {
                    "Custom", // 0
                    "Voice", // 1
                    "Metal Pipe", // 2
                    "AAAAAAAAAA", // 3
                    "Loud Buzz", // 4
            }
    )
    public static int failsafeSoundSelected = 1;
    @Number(
            name = "Number of times to play custom sound", category = FAILSAFE, subcategory = "Failsafe Trigger Sound",
            description = "The number of times to play custom sound when a failsafe has been triggered",
            min = 1, max = 10
    )
    public static int failsafeSoundTimes = 13;
    @Info(
            text = "If you want to use your own WAV file, rename it to 'charmonium_sound.wav' and put it in your Minecraft directory.",
            type = InfoType.WARNING,
            category = FAILSAFE,
            subcategory = "Failsafe Trigger Sound",
            size = 2
    )
    public static boolean customFailsafeSoundWarning;
    @Slider(
            name = "Failsafe Sound Volume (in %)", category = FAILSAFE, subcategory = "Failsafe Trigger Sound",
            description = "The volume of the failsafe sound",
            min = 0, max = 100
    )
    public static float failsafeSoundVolume = 50.0f;
    @Switch(
            name = "Max out Master category sounds while pinging", category = FAILSAFE, subcategory = "Failsafe Trigger Sound",
            description = "Maxes out the sounds while failsafe"
    )
    public static boolean maxOutMinecraftSounds = false;

    @Button(
            name = "", category = FAILSAFE, subcategory = "Failsafe Trigger Sound",
            description = "Plays the selected sound",
            text = "Play"
    )
    Runnable _playFailsafeSoundButton = () -> AudioManager.getInstance().playSound();
    @Button(
            name = "", category = FAILSAFE, subcategory = "Failsafe Trigger Sound",
            description = "Stops playing the selected sound",
            text = "Stop"
    )
    Runnable _stopFailsafeSoundButton = () -> AudioManager.getInstance().resetSound();

    //</editor-fold>

    //<editor-fold desc="Restart after failsafe">
    @Switch(
            name = "Enable Restart After FailSafe", category = FAILSAFE, subcategory = "Restart After FailSafe",
            description = "Restarts the macro after a while when a failsafe has been triggered"
    )
    public static boolean enableRestartAfterFailSafe = true;
    @Slider(
            name = "Restart Delay", category = FAILSAFE, subcategory = "Restart After FailSafe",
            description = "The delay to restart after failsafe (in minutes)",
            min = 0, max = 60
    )
    public static int restartAfterFailSafeDelay = 5;

    @Switch(
            name = "Always teleport to /warp garden after the failsafe",
            category = FAILSAFE, subcategory = "Restart After FailSafe",
            description = "Always teleports to /warp garden after the failsafe"
    )
    public static boolean alwaysTeleportToGarden = false;

    //</editor-fold>

    //<editor-fold desc="Anti Stuck">
    @Switch(
            name = "Enable Anti Stuck", category = FAILSAFE, subcategory = "Anti Stuck",
            description = "Prevents the macro from getting stuck in the same position"
    )
    public static boolean enableAntiStuck = true;
    //</editor-fold>

    //<editor-fold desc="Failsafe Messages">
    @Switch(
            name = "Send Chat Message During Failsafe", category = FAILSAFE, subcategory = "Failsafe Messages",
            description = "Sends a chat message when a failsafe has been triggered"
    )
    public static boolean sendFailsafeMessage = true;
    @Page(
            name = "Custom Failsafe Messages", category = FAILSAFE, subcategory = "Failsafe Messages", location = PageLocation.BOTTOM,
            description = "Click here to edit custom failsafe messages"
    )
    public static CustomFailsafeMessagesPage customFailsafeMessagesPage = new CustomFailsafeMessagesPage();
    //</editor-fold>
    //</editor-fold>

    //<editor-fold desc="SCHEDULER">
    //<editor-fold desc="Scheduler">
    @Switch(
            name = "Enable Scheduler", category = SCHEDULER, subcategory = "Scheduler", size = OptionSize.DUAL,
            description = "Farms for X amount of minutes then takes a break for X amount of minutes"
    )
    public static boolean enableScheduler = false;
    @Slider(
            name = "Farming time (in minutes)", category = SCHEDULER, subcategory = "Scheduler",
            description = "How long to farm",
            min = 1, max = 300, step = 1
    )
    public static int schedulerFarmingTime = 30;
    @Slider(
            name = "Farming time randomness (in minutes)", category = SCHEDULER, subcategory = "Scheduler",
            description = "How much randomness to add to the farming time",
            min = 0, max = 15, step = 1
    )
    public static int schedulerFarmingTimeRandomness = 0;
    @Slider(
            name = "Break time (in minutes)", category = SCHEDULER, subcategory = "Scheduler",
            description = "How long to take a break",
            min = 1, max = 120, step = 1
    )
    public static int schedulerBreakTime = 5;
    @Slider(
            name = "Break time randomness (in minutes)", category = SCHEDULER, subcategory = "Scheduler",
            description = "How much randomness to add to the break time",
            min = 0, max = 15, step = 1
    )
    public static int schedulerBreakTimeRandomness = 0;
    @Switch(
            name = "Open inventory on scheduler breaks", category = SCHEDULER, subcategory = "Scheduler",
            description = "Opens inventory on scheduler breaks"
    )
    public static boolean openInventoryOnSchedulerBreaks = true;
    //</editor-fold>

    //<editor-fold desc="Leave timer">
    @Switch(
            name = "Enable leave timer", category = SCHEDULER, subcategory = "Leave Timer",
            description = "Leaves the server after the timer has ended"
    )
    public static boolean leaveTimer = false;
    @Slider(
            name = "Leave time", category = SCHEDULER, subcategory = "Leave Timer",
            description = "The time to leave the server (in minutes)",
            min = 15, max = 720, step = 5
    )
    public static int leaveTime = 60;
    //</editor-fold>
    //</editor-fold>

    //<editor-fold desc="DISCORD INTEGRATION">
    //<editor-fold desc="Webhook Discord">
    @Switch(
            name = "Enable Webhook Messages", category = DISCORD_INTEGRATION, subcategory = "Discord Webhook",
            description = "Allows to send messages via Discord webhooks"
    )
    public static boolean enableWebHook = false;
    @Switch(
            name = "Send Logs", category = DISCORD_INTEGRATION, subcategory = "Discord Webhook",
            description = "Sends all messages about the macro, staff checks, etc"
    )
    public static boolean sendLogs = false;
    @Switch(
            name = "Send Status Updates", category = DISCORD_INTEGRATION, subcategory = "Discord Webhook",
            description = "Sends messages about the macro, such as profits, harvesting crops, etc"
    )
    public static boolean sendStatusUpdates = false;
    @Number(
            name = "Status Update Interval (in minutes)", category = DISCORD_INTEGRATION, subcategory = "Discord Webhook",
            description = "The interval between sending messages about status updates",
            min = 1, max = 60
    )
    public static int statusUpdateInterval = 5;
    @Switch(
            name = "Send Visitors Macro Logs", category = DISCORD_INTEGRATION, subcategory = "Discord Webhook",
            description = "Sends messages about the visitors macro, such as which visitor got rejected or accepted and with what items"
    )
    public static boolean sendVisitorsMacroLogs = true;
    @Switch(
            name = "Ping everyone on Visitors Macro Logs", category = DISCORD_INTEGRATION, subcategory = "Discord Webhook",
            description = "Pings everyone on Visitors Macro Logs"
    )
    public static boolean pingEveryoneOnVisitorsMacroLogs = false;
    @Text(
            name = "WebHook URL", category = DISCORD_INTEGRATION, subcategory = "Discord Webhook",
            description = "The URL to use for the webhook",
            placeholder = "https://discord.com/api/webhooks/...",
            secure = true
    )
    public static String webHookURL = "";
    //</editor-fold>

    //<editor-fold desc="Remote Control">
    @Switch(
            name = "Enable Remote Control", category = DISCORD_INTEGRATION, subcategory = "Remote Control",
            description = "Enables remote control via Discord messages"
    )
    public static boolean enableRemoteControl = false;
    @Text(
            name = "Discord Remote Control Bot Token",
            category = DISCORD_INTEGRATION, subcategory = "Remote Control",
            description = "The bot token to use for remote control",
            secure = true
    )
    public static String discordRemoteControlToken;
    @Text(
            name = "Discord Remote Control Address",
            category = DISCORD_INTEGRATION, subcategory = "Remote Control",
            description = "The address to use for remote control. If you are unsure what to put there, leave \"localhost\".",
            placeholder = "localhost"
    )
    public static String discordRemoteControlAddress = "localhost";

    @Number(
            name = "Remote Control Port", category = DISCORD_INTEGRATION, subcategory = "Remote Control",
            description = "The port to use for remote control. Change this if you have port conflicts.",
            min = 1, max = 65535
    )
    public static int remoteControlPort = 21370;

    @Info(
            text = "If you want to use the remote control feature, you need to put Charmonium JDA Dependency inside your mods folder.",
            type = InfoType.ERROR,
            category = DISCORD_INTEGRATION,
            subcategory = "Remote Control",
            size = 2
    )
    public static boolean infoRemoteControl;
    //</editor-fold>
    //</editor-fold>

    //<editor-fold desc="DELAYS">
    //<editor-fold desc="Changing Rows">
    @Slider(
            name = "Time between changing rows", category = DELAYS, subcategory = "Changing rows",
            description = "The minimum time to wait before changing rows (in milliseconds)",
            min = 70, max = 2000
    )
    public static float timeBetweenChangingRows = 400f;
    @Slider(
            name = "Additional random time between changing rows", category = DELAYS, subcategory = "Changing rows",
            description = "The maximum time to wait before changing rows (in milliseconds)",
            min = 0, max = 2000
    )
    public static float randomTimeBetweenChangingRows = 200f;
    //</editor-fold>

    //<editor-fold desc="Rotation Time">
    @Slider(
            name = "Rotation Time", category = DELAYS, subcategory = "Rotations",
            description = "The time it takes to rotate the player",
            min = 200f, max = 2000f
    )
    public static float rotationTime = 500f;
    @Slider(
            name = "Additional random Rotation Time", category = DELAYS, subcategory = "Rotations",
            description = "The maximum random time added to the delay time it takes to rotate the player (in seconds)",
            min = 0f, max = 2000f
    )
    public static float rotationTimeRandomness = 300;
    //</editor-fold>

    //<editor-fold desc="Gui Delay">
    @Slider(
            name = "GUI Delay", category = DELAYS, subcategory = "GUI Delays",
            description = "The delay between clicking during GUI macros (in milliseconds)",
            min = 250f, max = 2000f
    )
    public static float macroGuiDelay = 400f;
    @Slider(
            name = "Additional random GUI Delay", category = DELAYS, subcategory = "GUI Delays",
            description = "The maximum random time added to the delay time between clicking during GUI macros (in milliseconds)",
            min = 0f, max = 2000f
    )
    public static float macroGuiDelayRandomness = 350f;
    //</editor-fold>

    //<editor-fold desc="Rewarp Time">
    @Slider(
            name = "Rewarp Delay", category = DELAYS, subcategory = "Rewarp",
            description = "The delay between rewarping (in milliseconds)",
            min = 250f, max = 2000f
    )
    public static float rewarpDelay = 400f;
    @Slider(
            name = "Additional random Rewarp Delay", category = DELAYS, subcategory = "Rewarp",
            description = "The maximum random time added to the delay time between rewarping (in milliseconds)",
            min = 0f, max = 2000f
    )
    public static float rewarpDelayRandomness = 350f;
    //</editor-fold>
    //</editor-fold>

    //<editor-fold desc="DEBUG">
    //<editor-fold desc="Debug">
    @KeyBind(
            name = "Debug Keybind", category = DEBUG, subcategory = "Debug"
    )
    public static OneKeyBind debugKeybind = new OneKeyBind(Keyboard.KEY_NONE);
    //    @KeyBind(
//            name = "Debug Keybind 2", category = DEBUG
//    )
//    public static OneKeyBind debugKeybind2 = new OneKeyBind(Keyboard.KEY_H);
//    @KeyBind(
//            name = "Debug Keybind 3", category = DEBUG
//    )
//    public static OneKeyBind debugKeybind3 = new OneKeyBind(Keyboard.KEY_J);
    @Switch(
            name = "Debug Mode", category = DEBUG, subcategory = "Debug",
            description = "Prints to chat what the bot is currently executing. Useful if you are having issues."
    )
    public static boolean debugMode = false;
    @Switch(
            name = "Hide Logs (Not Recommended)", category = DEBUG, subcategory = "Debug",
            description = "Hides all logs from the console. Not recommended."
    )
    public static boolean hideLogs = false;
    @Switch(
            name = "Show rotation debug messages", category = DEBUG, subcategory = "Debug",
            description = "Shows rotation debug messages"
    )
    public static boolean showRotationDebugMessages = false;
    //</editor-fold>

    //<editor-fold desc="EXPERIMENTAL">
    //<editor-fold desc="Fastbreak">
    @Switch(
            name = "Enable Fast Break (DANGEROUS)", category = EXPERIMENTAL, subcategory = "Fast Break",
            description = "Fast Break is very risky and using it will most likely result in a ban. Proceed with caution."
    )
    public static boolean fastBreak = false;

    @Info(
            text = "Fast Break will most likely ban you. Use at your own risk.",
            type = InfoType.ERROR,
            category = EXPERIMENTAL,
            subcategory = "Fast Break"
    )
    public static boolean fastBreakWarning;
    @Slider(
            name = "Fast Break Speed", category = EXPERIMENTAL, subcategory = "Fast Break",
            description = "Fast Break speed",
            min = 1, max = 3
    )
    public static int fastBreakSpeed = 1;
    //</editor-fold>

    //<editor-fold desc="Fly Path Finder">
    @Slider(
            name = "Allowed Overshoot Threshold", category = EXPERIMENTAL, subcategory = "Flight",
            description = "The minimum distance from the block at which the fly path finder would allow overshooting",
            min = 0.05f, max = 0.4f
    )
    public static float flightAllowedOvershootThreshold = 0.1f;
    @Slider(
            name = "Max stuck time without motion (in ticks)", category = EXPERIMENTAL, subcategory = "Flight",
            description = "The maximum time to wait before unstucking (in ticks)",
            min = 30, max = 150
    )
    public static int flightMaxStuckTimeWithoutMotion = 40;
    @Slider(
            name = "Max stuck time with motion (in ticks)", category = EXPERIMENTAL, subcategory = "Flight",
            description = "The maximum time to wait before unstucking (in ticks)",
            min = 30, max = 150
    )
    public static int flightMaxStuckTimeWithMotion = 100;
    @Slider(
            name = "Deceleration offset", category = EXPERIMENTAL, subcategory = "Flight",
            description = "",
            min = 0, max = 15
    )
    public static int flightDecelerationOffset = 5;
    @Slider(
            name = "Maximum stuck distance threshold", category = EXPERIMENTAL, subcategory = "Flight",
            description = "The maximum distance threshold before unstucking (Vec3)",
            min = 0.3f, max = 1.5f
    )
    public static float flightMaximumStuckDistanceThreshold = 0.75f;
    @Switch(
            name = "Lock rotation to multipliers of 45 degrees", category = EXPERIMENTAL, subcategory = "Flight",
            description = "Locks the rotation to multipliers of 45 degrees"
    )
    public static boolean flightLockRotationToMultipliersOf45Degrees = false;
    //</editor-fold>
    //</editor-fold>

    @Number(name = "Config Version", category = EXPERIMENTAL, subcategory = "Experimental", min = 0, max = 1337)
    public static int configVersion = 2;
    @Switch(
            name = "Shown Welcome GUI", category = EXPERIMENTAL, subcategory = "Experimental"
    )
    public static boolean shownWelcomeGUI = false;

    public static int Skill_Alchemy = 0;
    public static int Skill_Carpentry = 0;
    public static int Skill_Combat = 0;
    public static int Skill_Enchanting = 0;
    public static int Skill_Farming = 0;
    public static int Skill_Fishing = 0;
    public static int Skill_Foraging = 0;
    public static int Skill_Mining = 0;

    private static boolean isAllowed(String macroName) {
        if (macroName.equals("scanRange") && (getMacro() == MacroEnum.ICE_WALKER || getMacro() == MacroEnum.SLAYER))
            return true;
        if (macroName.equals("shiftWhenKill") && (getMacro() == MacroEnum.ICE_WALKER || getMacro() == MacroEnum.FISHING || getMacro() == MacroEnum.SLAYER))
            return true;
        if (macroName.equals("jumpWhenWalking") && (getMacro() == MacroEnum.ICE_WALKER || getMacro() == MacroEnum.FISHING || getMacro() == MacroEnum.SLAYER))
            return true;
        return macroName.equals("SAttackType") && (getMacro() == MacroEnum.ICE_WALKER || getMacro() == MacroEnum.FISHING || getMacro() == MacroEnum.SLAYER);
    }

    public Config() {
        super(new Mod("Charmonium", ModType.SKYBLOCK, "/charmonium/char.png"), "/charmonium/config.json");
        initialize();

        this.addDependency("macroType", "Macro Type", () -> !MacroHandler.getInstance().isMacroToggled());

        this.addDependency("customPitchLevel", "customPitch");
        this.addDependency("customYawLevel", "customYaw");

        this.hideIf("healingItem", () -> !autoHeal);
        this.hideIf("autoHeal", () -> getMacro() != MacroEnum.SLAYER);
        this.hideIf("slayerWeaponName", () -> getMacro() != MacroEnum.SLAYER);
        this.hideIf("autoMaddox", () -> getMacro() != MacroEnum.SLAYER);
        this.hideIf("SlayerType", () -> getMacro() != MacroEnum.SLAYER);
        this.hideIf("SlayerTier", () -> getMacro() != MacroEnum.SLAYER);
        this.hideIf("useWarps", () -> getMacro() != MacroEnum.SLAYER);

        this.hideIf("commTool", () -> getMacro() != MacroEnum.MITHRIL_MINER);
        this.hideIf("SMSpotType", () -> getMacro() != MacroEnum.MITHRIL_MINER);

        this.hideIf("glassPanesFullBlock", () -> getMacro() != MacroEnum.GEMSTONE);
        this.hideIf("aotvWaypointsPage", () -> getMacro() != MacroEnum.GEMSTONE);
        this.hideIf("gemstoneTool", () -> getMacro() != MacroEnum.GEMSTONE);
        this.hideIf("aotvShowRouteLines", () -> getMacro() != MacroEnum.GEMSTONE);
        this.hideIf("aotvShowNumber", () -> getMacro() != MacroEnum.GEMSTONE);
        this.hideIf("aotvHighlightRouteBlocks", () -> getMacro() != MacroEnum.GEMSTONE);
        this.hideIf("refuelWithAbiphone", () -> getMacro() != MacroEnum.GEMSTONE);
        this.hideIf("refuelThreshold", () -> getMacro() != MacroEnum.GEMSTONE);
        this.hideIf("typeOfFuelIndex", () -> getMacro() != MacroEnum.GEMSTONE);
        this.hideIf("blueCheeseOmeletteToggle", () -> getMacro() != MacroEnum.GEMSTONE);
        this.hideIf("useMiningSpeedBoost", () -> getMacro() != MacroEnum.GEMSTONE);
        this.hideIf("glassFilter", () -> getMacro() != MacroEnum.GEMSTONE);

        this.hideIf("scanRange", () -> !isAllowed("scanRange"));
        this.hideIf("shiftWhenKill", () -> !isAllowed("shiftWhenKill"));
        this.hideIf("jumpWhenWalking", () -> !isAllowed("jumpWhenWalking"));
        this.hideIf("SAttackType", () -> !isAllowed("SAttackType"));

        this.hideIf("seaCeatureRange", () -> getMacro() != MacroEnum.FISHING);
        this.hideIf("recastDelay", () -> getMacro() != MacroEnum.FISHING);
        this.hideIf("fishingWeaponName", () -> getMacro() != MacroEnum.FISHING);
        this.hideIf("fishingRodName", () -> getMacro() != MacroEnum.FISHING);

        this.addDependency("inventoryFullTime", "enableAutoSell");
        this.addDependency("autoSellMarketType", "enableAutoSell");
        this.addDependency("autoSellSacks", "enableAutoSell");
        this.addDependency("autoSellSacksPlacement", "enableAutoSell");
        this.addDependency("autoSellFunction", "enableAutoSell");

        this.addDependency("autoUngrabMouse", "This feature doesn't work properly on Mac OS!", () -> !Minecraft.isRunningOnMac);

        this.addDependency("desyncPauseDelay", "checkDesync");
        this.addDependency("failsafeSoundType", "Play Button", () -> enableFailsafeSound && !AudioManager.getInstance().isSoundPlaying());
        this.addDependency("_playFailsafeSoundButton", "enableFailsafeSound");
        this.addDependency("_stopFailsafeSoundButton", "enableFailsafeSound");
        this.hideIf("_playFailsafeSoundButton", () -> AudioManager.getInstance().isSoundPlaying());
        this.hideIf("_stopFailsafeSoundButton", () -> !AudioManager.getInstance().isSoundPlaying());
        this.addDependency("failsafeMcSoundSelected", "Minecraft Sound", () -> !failsafeSoundType && enableFailsafeSound);
        this.addDependency("failsafeSoundSelected", "Custom Sound", () -> failsafeSoundType && enableFailsafeSound);
        this.addDependency("failsafeSoundVolume", "Custom Sound", () -> failsafeSoundType && enableFailsafeSound);
        this.addDependency("maxOutMinecraftSounds", "Minecraft Sound", () -> !failsafeSoundType && enableFailsafeSound);
        this.hideIf("customFailsafeSoundWarning", () -> !failsafeSoundType || !enableFailsafeSound || failsafeSoundSelected != 0);
        this.addDependency("restartAfterFailSafeDelay", "enableRestartAfterFailSafe");
        this.addDependency("alwaysTeleportToGarden", "enableRestartAfterFailSafe");

        this.addDependency("schedulerFarmingTime", "enableScheduler");
        this.addDependency("schedulerFarmingTimeRandomness", "enableScheduler");
        this.addDependency("schedulerBreakTime", "enableScheduler");
        this.addDependency("schedulerBreakTimeRandomness", "enableScheduler");
        this.addDependency("pauseSchedulerDuringJacobsContest", "enableScheduler");

        this.addDependency("startKillingPestsAt", "enablePestsDestroyer");
        this.addDependency("pestAdditionalGUIDelay", "enablePestsDestroyer");
        this.addDependency("sprintWhileFlying", "enablePestsDestroyer");


        this.hideIf("infoCookieBuffRequired", () -> GameStateHandler.getInstance().atProperIsland() || GameStateHandler.getInstance().getCookieBuffState() == GameStateHandler.BuffState.NOT_ACTIVE);

        this.addDependency("sendLogs", "enableWebHook");
        this.addDependency("sendStatusUpdates", "enableWebHook");
        this.addDependency("statusUpdateInterval", "enableWebHook");
        this.addDependency("webHookURL", "enableWebHook");
        this.addDependency("enableRemoteControl", "Enable Remote Control", () -> Loader.isModLoaded("farmhelperjdadependency"));
        this.addDependency("discordRemoteControlAddress", "enableRemoteControl");
        this.addDependency("remoteControlPort", "enableRemoteControl");


        this.hideIf("infoRemoteControl", () -> Loader.isModLoaded("farmhelperjdadependency"));
        this.hideIf("failsafeSoundTimes", () -> true);

        this.addDependency("debugMode", "Debug Mode", () -> !hideLogs);
        this.addDependency("hideLogs", "Hide Logs (Not Recommended)", () -> !debugMode);

        this.addDependency("fastBreakSpeed", "fastBreak");

        this.addDependency("autoGodPotFromBackpack", "autoGodPot");
        this.addDependency("autoGodPotFromBits", "autoGodPot");
        this.addDependency("autoGodPotFromAH", "autoGodPot");

        this.hideIf("autoGodPotBackpackNumber", () -> !autoGodPotFromBackpack);
        this.hideIf("autoGodPotStorageType", () -> !autoGodPotFromBackpack);

        this.addDependency("sendWebhookLogIfPestsDetectionNumberExceeded", "enableWebHook");
        this.addDependency("pingEveryoneOnPestsDetectionNumberExceeded", "sendWebhookLogIfPestsDetectionNumberExceeded");
        this.addDependency("pingEveryoneOnPestsDetectionNumberExceeded", "enableWebHook");


        this.addDependency("leaveTime", "leaveTimer");

        this.hideIf("shownWelcomeGUI", () -> true);

        this.hideIf("configVersion", () -> true);

        registerKeyBind(openGuiKeybind, this::openGui);
        registerKeyBind(toggleMacro, () -> MacroHandler.getInstance().toggleMacro());
        registerKeyBind(freelookKeybind, () -> Freelook.getInstance().toggle());
    }

    public static long getRandomTimeBetweenChangingRows() {
        return (long) (timeBetweenChangingRows + (float) Math.random() * randomTimeBetweenChangingRows);
    }

    public static long getMaxTimeBetweenChangingRows() {
        return (long) (timeBetweenChangingRows + randomTimeBetweenChangingRows);
    }

    public static long getRandomRotationTime() {
        return (long) (rotationTime + (float) Math.random() * rotationTimeRandomness);
    }

    public static long getRandomGUIMacroDelay() {
        return (long) (macroGuiDelay + (float) Math.random() * macroGuiDelayRandomness);
    }

    public static long getRandomRewarpDelay() {
        return (long) (rewarpDelay + (float) Math.random() * rewarpDelayRandomness);
    }

    public String getJson() {
        String json = gson.toJson(this);
        if (json == null || json.equals("{}")) {
            json = nonProfileSpecificGson.toJson(this);
        }
        return json;
    }
}
