package com.mylk.charmonium.feature.impl;

import com.mylk.charmonium.Charmonium;
import com.mylk.charmonium.config.Config;
import com.mylk.charmonium.util.MarkdownFormatter;
import com.mylk.charmonium.util.ScoreboardUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.lang3.time.StopWatch;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SkillTracker {
    static String lastSkill = "Farming";
    public static StopWatch skillStopwatch = new StopWatch();
    static double farmingXP = 0;
    public static double farmingXPGained = 0;
    static boolean ignoreFarming = false;
    static double miningXP = 0;
    public static double miningXPGained = 0;
    static boolean ignoreMining = false;
    static double combatXP = 0;
    public static double combatXPGained = 0;
    static boolean ignoreCombat = false;
    static double foragingXP = 0;
    public static double foragingXPGained = 0;
    static boolean ignoreForaging = false;
    static double fishingXP = 0;
    public static double fishingXPGained = 0;
    static boolean ignoreFishing = false;
    static double enchantingXP = 0;
    public static double enchantingXPGained = 0;
    static boolean ignoreEnchanting = false;
    static double alchemyXP = 0;
    public static double alchemyXPGained = 0;
    static boolean ignoreAlchemy = false;
    public static double xpLeft = 0;
    static double timeSinceGained = 0;
    public static int tickAmount = 1;
    static final NumberFormat nf = NumberFormat.getInstance(Locale.US);
    private static SkillTracker instance;

    public static SkillTracker getInstance() {
        if (instance == null) {
            instance = new SkillTracker();
        }
        return instance;
    }
    public static int[] skillXPPerLevel = {0, 50, 125, 200, 300, 500, 750, 1000, 1500, 2000, 3500, 5000, 7500, 10000, 15000, 20000, 30000, 50000,
            75000, 100000, 200000, 300000, 400000, 500000, 600000, 700000, 800000, 900000, 1000000, 1100000,
            1200000, 1300000, 1400000, 1500000, 1600000, 1700000, 1800000, 1900000, 2000000, 2100000, 2200000,
            2300000, 2400000, 2500000, 2600000, 2750000, 2900000, 3100000, 3400000, 3700000, 4000000, 4300000,
            4600000, 4900000, 5200000, 5500000, 5800000, 6100000, 6400000, 6700000, 7000000};


    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onChat(ClientChatReceivedEvent event) throws ParseException {
        if (!ScoreboardUtils.getScoreboardTitle().contains("SKYBLOCK") || event.type != 2) return;

        String[] actionBarSections = event.message.getUnformattedText().split(" {3,}");

        for (String section : actionBarSections) {
            if (section.contains("+") && section.contains("(") && section.contains(")") && !section.contains("Runecrafting") && !section.contains("Carpentry") && !section.contains("SkyBlock XP")) {
                if (System.currentTimeMillis() / 1000 - timeSinceGained <= 2) {
                    if (skillStopwatch.isStarted() && skillStopwatch.isSuspended()) {
                        skillStopwatch.resume();
                    } else if (!skillStopwatch.isStarted()) {
                        skillStopwatch.start();
                    }
                }
                timeSinceGained = (double) System.currentTimeMillis() / 1000;

                String skill = section.substring(section.indexOf(" ") + 1, section.lastIndexOf(" "));
                double totalXP;

                if (section.contains("/")) {
                    int limit = section.contains("Farming") || section.contains("Enchanting") || section.contains("Mining") || section.contains("Combat") ? 60 : 50;
                    double currentXP;
                    try {
                        currentXP = nf.parse(section.substring(section.indexOf("(") + 1, section.indexOf("/")).replace(",", "")).doubleValue();
                    } catch (NumberFormatException ex) {
                        ex.printStackTrace();
                        return;
                    }

                    int xpToLevelUp;
                    String nextLevelXpString = section.substring(section.indexOf("/") + 1, section.indexOf(")")).replaceAll(",", "");
                    if (nextLevelXpString.contains("k")) {
                        xpToLevelUp = (int) (nf.parse(nextLevelXpString.substring(0, nextLevelXpString.indexOf("k"))).doubleValue() * 1000);
                    } else {
                        xpToLevelUp = nf.parse(nextLevelXpString).intValue();
                    }

                    xpLeft = xpToLevelUp - currentXP;
                    int previousXP = getPastXpEarned(xpToLevelUp, limit);
                    totalXP = currentXP + previousXP;
                } else {
                    if (!skillsInitialized()) {
                        return;
                    }

                    int level = 1;
                    if (section.contains("Farming")) {
                        level = Config.Skill_Farming;
                    } else if (section.contains("Mining")) {
                        level = Config.Skill_Mining;
                    } else if (section.contains("Combat")) {
                        level = Config.Skill_Combat;
                    } else if (section.contains("Foraging")) {
                        level = Config.Skill_Foraging;
                    } else if (section.contains("Fishing")) {
                        level = Config.Skill_Fishing;
                    } else if (section.contains("Enchanting")) {
                        level = Config.Skill_Enchanting;
                    } else if (section.contains("Alchemy")) {
                        level = Config.Skill_Alchemy;
                    } else if (section.contains("Carpentry")) {
                        level = Config.Skill_Carpentry;
                    }

                    totalXP = getTotalXpEarned(level, nf.parse(section.substring(section.indexOf("(") + 1, section.indexOf("%"))).doubleValue());
                    xpLeft = getTotalXpEarned(level + 1, 0) - totalXP;
                }

                double add;
                switch (skill) {
                    case "Farming":
                        lastSkill = "Farming";
                        if (ignoreFarming) {
                            ignoreFarming = false;
                            return;
                        }
                        add = addXP(totalXP, farmingXP);
                        if (add < 0) {
                            ignoreFarming = true;
                            return;
                        }
                        farmingXPGained += add;
                        farmingXP = totalXP;
                        break;
                    case "Mining":
                        lastSkill = "Mining";
                        if (ignoreMining) {
                            ignoreMining = false;
                            return;
                        }
                        add = addXP(totalXP, miningXP);
                        if (add < 0) {
                            ignoreMining = true;
                            return;
                        }
                        miningXPGained += add;
                        miningXP = totalXP;
                        break;
                    case "Combat":
                        lastSkill = "Combat";
                        if (ignoreCombat) {
                            ignoreCombat = false;
                            return;
                        }
                        add = addXP(totalXP, combatXP);
                        if (add < 0) {
                            ignoreCombat = true;
                            return;
                        }
                        combatXPGained += add;
                        combatXP = totalXP;
                        break;
                    case "Foraging":
                        lastSkill = "Foraging";
                        if (ignoreForaging) {
                            ignoreForaging = false;
                            return;
                        }
                        add = addXP(totalXP, foragingXP);
                        if (add < 0) {
                            ignoreForaging = true;
                            return;
                        }
                        foragingXPGained += add;
                        foragingXP = totalXP;
                        break;
                    case "Fishing":
                        lastSkill = "Fishing";
                        if (ignoreFishing) {
                            ignoreFishing = false;
                            return;
                        }
                        add = addXP(totalXP, fishingXP);
                        if (add < 0) {
                            ignoreFishing = true;
                            return;
                        }
                        fishingXPGained += add;
                        fishingXP = totalXP;
                        break;
                    case "Enchanting":
                        lastSkill = "Enchanting";
                        if (ignoreEnchanting) {
                            ignoreEnchanting = false;
                            return;
                        }
                        add = addXP(totalXP, enchantingXP);
                        if (add < 0) {
                            ignoreEnchanting = true;
                            return;
                        }
                        enchantingXPGained += add;
                        enchantingXP = totalXP;
                        break;
                    case "Alchemy":
                        lastSkill = "Alchemy";
                        if (ignoreAlchemy) {
                            ignoreAlchemy = false;
                            return;
                        }
                        add = addXP(totalXP, alchemyXP);
                        if (add < 0) {
                            ignoreAlchemy = true;
                            return;
                        }
                        alchemyXPGained += add;
                        alchemyXP = totalXP;
                        break;
                    default:
                        System.err.println("Unknown skill.");
                }
            }
        }
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (event.gui instanceof GuiChest && skillStopwatch.isStarted() && !skillStopwatch.isSuspended()) {
            skillStopwatch.suspend();
        }
    }

    static double addXP(double totalXP, double skillXP) {
        if (skillXP != 0) {
            if (skillStopwatch.isStarted() && !skillStopwatch.isSuspended()) {
                if (totalXP > skillXP) {
                    return totalXP - skillXP;
                } else {
                    return -1;
                }
            }
        }
        return 0;
    }

    public static double getText(String Macro) {
        double xpToShow = 0;
        switch (Macro) {
            case "Farming":
                xpToShow = farmingXPGained;
                break;
            case "Mining":
                xpToShow = miningXPGained;
                break;
            case "Combat":
                xpToShow = combatXPGained;
                break;
            case "Foraging":
                xpToShow = foragingXPGained;
                break;
            case "Fishing":
                xpToShow = fishingXPGained;
                break;
            case "Enchanting":
                xpToShow = enchantingXPGained;
                break;
            case "Alchemy":
                xpToShow = alchemyXPGained;
                break;
            default:
                System.err.println("Unknown skill in rendering.");
        }
        if (!skillStopwatch.isStarted() || skillStopwatch.isSuspended()) {
            return 0;
        }
        return xpToShow;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.thePlayer;

        tickAmount++;

        if (mc.currentScreen instanceof GuiChest && tickAmount % 5 == 0 && player != null) {
            ContainerChest chest = (ContainerChest) player.openContainer;
            String chestName = chest.getLowerChestInventory().getDisplayName().getUnformattedText().trim();

            if (chestName.equals("Your Skills")) {
                List<Slot> invSlots = ((GuiChest) mc.currentScreen).inventorySlots.inventorySlots;

                Config.Skill_Combat = initializeSkill(invSlots.get(19).getStack());
                Config.Skill_Farming = initializeSkill(invSlots.get(20).getStack());
                Config.Skill_Fishing = initializeSkill(invSlots.get(21).getStack());
                Config.Skill_Mining = initializeSkill(invSlots.get(22).getStack());
                Config.Skill_Foraging = initializeSkill(invSlots.get(23).getStack());
                Config.Skill_Enchanting = initializeSkill(invSlots.get(24).getStack());
                Config.Skill_Alchemy = initializeSkill(invSlots.get(25).getStack());
                Config.Skill_Carpentry = initializeSkill(invSlots.get(29).getStack());

                System.out.println("Updated skill levels.");
            }
        }
    }

    public static String getTimeBetween(double timeOne, double timeTwo) {
        double secondsBetween = Math.floor(timeTwo - timeOne);

        String timeFormatted;
        int days;
        int hours;
        int minutes;
        int seconds;

        if (secondsBetween > 86400) {
            // More than 1d, display #d#h
            days = (int) (secondsBetween / 86400);
            hours = (int) (secondsBetween % 86400 / 3600);
            timeFormatted = days + "d" + hours + "h";
        } else if (secondsBetween > 3600) {
            // More than 1h, display #h#m
            hours = (int) (secondsBetween / 3600);
            minutes = (int) (secondsBetween % 3600 / 60);
            timeFormatted = hours + "h" + minutes + "m";
        } else {
            // Display #m#s
            minutes = (int) (secondsBetween / 60);
            seconds = (int) (secondsBetween % 60);
            timeFormatted = minutes + "m" + seconds + "s";
        }

        return timeFormatted;
    }

    public static String getColour(int index) {
        return "§" + Integer.toHexString(index);
    }

    public static int getWidthFromText(String text) {
        if (text == null) return 0;

        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        String[] splitText = text.split("\n");

        int width = 0;
        for (String line : splitText) {
            int stringLength = fr.getStringWidth(line);
            if (stringLength > width) {
                width = stringLength;
            }
        }

        return width;
    }

    public static int getHeightFromText(String text) {
        if (text == null) return 0;

        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        String[] splitText = text.split("\n");
        return splitText.length * fr.FONT_HEIGHT;
    }

    public static boolean skillsInitialized() {
        return Config.Skill_Mining != -1;
    }

    public static int initializeSkill(ItemStack skillStack) {
        int level = -1;

        if (skillStack != null) {
            String display = skillStack.getDisplayName();
            if (display.startsWith("§a")) {
                if (display.contains(" ")) {
                    level = getIntFromString(display.substring(display.indexOf(" ") + 1), true);
                } else {
                    level = 0;
                }
            }
        }

        return level;
    }

    public static int getIntFromString(String text, boolean romanNumeral) {
        if (text.matches(".*\\d.*")) {
            return Integer.parseInt(StringUtils.stripControlCodes(text).replaceAll("\\D", ""));
        } else if (romanNumeral) {
            int number = 0;

            for (int i = 0; i < text.length(); i++) {
                if (!romanNumerals.containsKey(text.charAt(i))) continue;
                int roman = romanNumerals.get(text.charAt(i));

                if (i != text.length() - 1 && romanNumerals.containsKey(text.charAt(i + 1)) && roman < romanNumerals.get(text.charAt(i + 1))) {
                    number += romanNumerals.get(text.charAt(i + 1)) - roman;
                    i++;
                } else {
                    number += roman;
                }
            }

            return number;
        }

        return -1;
    }

    public static int getPastXpEarned(int currentLevelXp, int limit) {
        if (currentLevelXp == 0) {
            int xpAdded = 0;
            for (int i = 1; i <= limit; i++) {
                xpAdded += skillXPPerLevel[i];
            }
            return xpAdded;
        }
        for (int i = 1, xpAdded = 0; i <= limit; i++) {
            xpAdded += skillXPPerLevel[i - 1];
            if (currentLevelXp == skillXPPerLevel[i]) return xpAdded;
        }
        return 0;
    }

    public static double getTotalXpEarned(int currentLevel, double percentage) {
        double progress = 0;
        if (currentLevel < 60) progress = skillXPPerLevel[currentLevel + 1] * (percentage / 100D);
        double xpAdded = 0;
        for (int i = 1; i <= currentLevel; i++) {
            xpAdded += skillXPPerLevel[i];
        }
        return xpAdded + progress;
    }

    public static void resetSkills() {
        SkillTracker.skillStopwatch = new StopWatch();
        SkillTracker.farmingXPGained = 0;
        SkillTracker.miningXPGained = 0;
        SkillTracker.combatXPGained = 0;
        SkillTracker.foragingXPGained = 0;
        SkillTracker.fishingXPGained = 0;
        SkillTracker.enchantingXPGained = 0;
        SkillTracker.alchemyXPGained = 0;
    }

    public static boolean hitMax(String Skill) {
        if (Skill.equals("Farming") || Skill.equals("Enchanting") || Skill.equals("Mining") || Skill.equals("Combat")) {
            return Config.Skill_Farming + 1 > 60;
        } else {
            return Config.Skill_Fishing + 1 > 50;
        }
    }

    static Map<Character, Integer> romanNumerals = new HashMap<Character, Integer>(){{
        put('I', 1);
        put('V', 5);
        put('X', 10);
        put('L', 50);
        put('C', 100);
        put('D', 500);
        put('M', 1000);
    }};

}
