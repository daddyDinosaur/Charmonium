package com.mylk.charmonium.macro.impl;

import com.mylk.charmonium.Charmonium;
import com.mylk.charmonium.config.Config;
import com.mylk.charmonium.feature.impl.Scheduler;
import com.mylk.charmonium.feature.impl.SkillTracker;
import com.mylk.charmonium.feature.impl.UngrabMouse;
import com.mylk.charmonium.handler.GameStateHandler;
import com.mylk.charmonium.handler.MacroHandler;
import com.mylk.charmonium.handler.RotationHandler;
import com.mylk.charmonium.handler.slayerHandler;
import com.mylk.charmonium.macro.AbstractMacro;
import com.mylk.charmonium.macro.impl.misc.Maddoxer;
import com.mylk.charmonium.util.*;
import com.mylk.charmonium.util.charHelpers.npcUtils;
import com.mylk.charmonium.util.helper.Rotation;
import com.mylk.charmonium.util.helper.RotationConfiguration;
import com.mylk.charmonium.util.helper.Timer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityWolf;
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
import java.util.stream.Collectors;

public class SlayerAura extends AbstractMacro {
    public static State currentState = State.NONE;
    private final RotationHandler rotation = RotationHandler.getInstance();
    private final Timer noKillTimer = new Timer();
    private final Timer attackDelay = new Timer();
    private final Timer afterKillDelay = new Timer();
    public static Entity target;
    private static List<Entity> ignoreEntity;
    public static boolean killingBoss = false;
    private static int killedBosses = 0;
    public static boolean killingMiniBoss = false;
    public static boolean killedBoss = false;
    private static SlayerAura instance;

    public static SlayerAura getInstance() {
        if (instance == null) {
            instance = new SlayerAura();
        }
        return instance;
    }
    public void onEnable() {
        Config.MacroEnum crop = Config.MacroEnum.SLAYER_AURA;
        currentState = State.NONE;
        rotation.reset();
        ignoreEntity = new ArrayList<>();
        target = null;
        killingBoss = false;
        killedBoss = false;
        killingMiniBoss = false;
        super.setSpawnPos(new Vec3(-3, 70, -70));

        switch (Config.SlayerAuraType) {
            case 0: // rev
                GameStateHandler.islandWarp = "/hub";
                break;
            case 1: // brood
                break;
            case 2: // sven
                GameStateHandler.islandWarp = "/park";
                break;
            case 3: // void
                break;
            default:
                Charmonium.sendMessage("No Slayer Selected");
                MacroHandler.getInstance().disableMacro();
                break;
        }

        LogUtils.sendDebug("Macro: " + crop);
        MacroHandler.getInstance().setCrop(crop);
        PlayerUtils.getTool();

        super.onEnable();

        UngrabMouse.getInstance().regrabMouse();
    }

    public void invokeState() {
        List<Entity> entities = getEntity();
        Optional<Entity> optional = Optional.empty();
        if (!entities.isEmpty())
            optional = entities.stream().min(Comparator.comparingDouble(entity -> entity.getDistanceToEntity(mc.thePlayer)));
        else
            ignoreEntity = new ArrayList<>();

        if (slayerHandler.currentSlayerBoss != null || slayerHandler.currentMiniBoss != null) {
            target = slayerHandler.currentSlayerBoss != null ? slayerHandler.currentSlayerBoss : slayerHandler.currentMiniBoss;
            killingBoss = slayerHandler.currentSlayerBoss != null;
            killingMiniBoss = slayerHandler.currentMiniBoss != null;
            currentState = State.ATTACKING;
            noKillTimer.schedule();
        }

        if (optional.isPresent() && target == null) {
            target = optional.get();
            noKillTimer.schedule();
            currentState = State.ATTACKING;
        }

        switch (currentState) {
            case NONE:
                KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindSneak, false);
                if (target == null) {
                    ignoreEntity = new ArrayList<>();
                }
                break;
            case ATTACKING:
                if (noKillTimer.hasPassed(6000) && !killingBoss && !killingMiniBoss) {
                    Charmonium.sendMessage("Stuck? Finding new");
                    currentState = State.NONE;
                    ignoreEntity.add(target);
                    target = null;
                }

                if (npcUtils.getEntityHp(target) <= 1 || Minecraft.getMinecraft().thePlayer.getDistanceToEntity(target) > 5 || (target != null && (target.isDead))) {
                    currentState = State.KILLED;
                    afterKillDelay.schedule();
                    break;
                }

                boolean visible = PlayerUtils.entityIsVisible(target);

                if (!visible && !killingBoss && !killingMiniBoss) {
                    Charmonium.sendMessage("Something is blocking target, moving on...");
                    ignoreEntity.add(target);
                    target = null;
                    currentState = State.NONE;
                } else {
                    assert target != null;
                    if (Config.SA_AttackType == Config.SA_AttackEnum.LEFT_CLICK.ordinal()) {

                        boolean targeted = npcUtils.entityIsTargeted(target);
                        if (target != null && !targeted) {
                            rotation.easeTo(new RotationConfiguration(rotation.getRotation(target.getPositionVector().add(new Vec3(0, target.height / 2, 0))), Config.getRandomRotationTime(), null));
                        }

                        bossMovement(3, mc.gameSettings.keyBindAttack);
                    } else {
                        boolean targeted = npcUtils.entityIsTargeted(target);
                        if (target != null && !targeted) {
                            rotation.easeTo(new RotationConfiguration(new Rotation(rotation.getRotation(target.getPositionVector().add(new Vec3(0, target.height / 2, 0))).getYaw(), 50.f), Config.getRandomRotationTime() / 2, null));
                        }

                        bossMovement(6, mc.gameSettings.keyBindUseItem);
                    }
                }

                break;
            case KILLED:
                if (!afterKillDelay.hasPassed(150))
                    return;

                target = null;
                if (killingBoss && slayerHandler.currentSlayerBoss == null) killingBoss = false;
                if (killingMiniBoss && slayerHandler.currentMiniBoss == null) killingMiniBoss = false;
                KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindSneak, false);
                currentState = State.NONE;

                break;
            case PAUSE:
                break;
        }
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (!GameStateHandler.getInstance().atProperIsland() || !MacroHandler.getInstance().isMacroToggled() || MacroHandler.getInstance().getCrop() != Config.MacroEnum.SLAYER_AURA) return;

        if (slayerHandler.currentSlayerBoss != null) {
            RenderUtils.renderBoundingBox(slayerHandler.currentSlayerBoss, event.partialTicks, new Color(255, 31, 124, 157).getRGB());
            return;
        }

        if (slayerHandler.currentMiniBoss != null) {
            RenderUtils.renderBoundingBox(slayerHandler.currentMiniBoss, event.partialTicks, new Color(255, 192, 31, 157).getRGB());
            return;
        }

        if (target != null) {
            RenderUtils.renderBoundingBox(target, event.partialTicks, new Color(31, 255, 160, 157).getRGB());
        }
    }

    @SubscribeEvent
    public void onChatReceive(ClientChatReceivedEvent event) {
        String message = StringUtils.stripControlCodes(event.message.getUnformattedText());
        if (message.contains("You were killed by") && MacroHandler.getInstance().getCrop() == Config.MacroEnum.SLAYER_AURA && MacroHandler.getInstance().isMacroToggled()) {
            Charmonium.sendMessage("Disabling macro");
            if (killingBoss) Maddoxer.hasStarted = false;
            killingMiniBoss = false;
            killingBoss = false;
            MacroHandler.getInstance().disableMacro();
        }
        if (message.contains("SLAYER BOSS SLAIN") && MacroHandler.getInstance().getCrop() == Config.MacroEnum.SLAYER_AURA && MacroHandler.getInstance().isMacroToggled()) {
            Charmonium.sendMessage("Killed Boss");
            target = null;
            killedBosses++;
            slayerHandler.currentSlayerBoss = null;
            killingBoss = false;
            killingMiniBoss = false;
        }
    }

    private void bossMovement(int distance, KeyBinding Key) {
        if (Minecraft.getMinecraft().thePlayer.getDistanceToEntity(target) <= distance) {
            if (Config.shiftAuraKill)
                KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindSneak, true);
            if (attackDelay.hasPassed(Config.SA_AttackDelay)) {
                KeyBindUtils.onTick(Key);
                attackDelay.schedule();
            }
        }
    }

    private boolean canWalk(String direction) {
        float yaw = mc.thePlayer.rotationYaw;
        BlockPos blockPos = new BlockPos(0, 0, 0);
        switch (direction) {
            case "Back":
                blockPos = BlockUtils.getRelativeBlockPos(0, 0, -1, yaw);
                break;
            case "Left":
                blockPos = BlockUtils.getRelativeBlockPos(-1, 0, 0, yaw);
                break;
            case "Right":
                blockPos = BlockUtils.getRelativeBlockPos(1, 0, 0, yaw);
                break;
            case "Forward":
                blockPos = BlockUtils.getRelativeBlockPos(0, 0, 1, yaw);
                break;
        }

        assert BlockUtils.getBlock(blockPos) != null;
        return BlockUtils.getBlock(blockPos) == Blocks.air || BlockUtils.getBlock(blockPos) == Blocks.wooden_slab || BlockUtils.getBlock(blockPos).getMaterial().isLiquid();
    }

    private List<Entity> getEntity() {
        return mc.theWorld.loadedEntityList.stream()
                .filter(entity -> entity != Charmonium.mc.thePlayer &&
                        canKill(entity) &&
                        !entity.isDead &&
                        isInFOV(entity, 85) &&
                        ((EntityLivingBase) entity).getHealth() > 0 &&
                        mc.thePlayer.getPositionEyes(1).distanceTo(entity.getPositionVector()) <= 5 &&
                        Math.abs(entity.getPositionVector().yCoord - mc.thePlayer.getPositionVector().yCoord) < 3 &&
                        !ignoreEntity.contains(entity) &&
                        !isSvenPup(entity))
                .filter(PlayerUtils::entityIsVisible)
                .collect(Collectors.toList());
    }

    private boolean isInFOV(Entity entity, double fovThreshold) {
        double angle = wrapDegrees(Math.atan2(entity.posZ - mc.thePlayer.posZ, entity.posX - mc.thePlayer.posX) * (180.0 / Math.PI) - mc.thePlayer.rotationYaw);

        return Math.abs(angle) <= fovThreshold / 2.0;
    }

    public static double wrapDegrees(double value) {
        value %= 360.0;
        if (value >= 180.0) {
            value -= 360.0;
        }
        if (value < -180.0) {
            value += 360.0;
        }
        return value;
    }

    public static float wrapDegrees(float value) {
        value %= 360.0f;
        if (value >= 180.0f) {
            value -= 360.0f;
        }
        if (value < -180.0f) {
            value += 360.0f;
        }
        return value;
    }

    private boolean canKill(Entity entity) {
        return (entity instanceof EntityZombie && Config.SlayerAuraType == 0) ||
                (entity instanceof EntitySpider && Config.SlayerAuraType == 1) ||
                (entity instanceof EntityWolf && Config.SlayerAuraType == 2) ||
                (entity instanceof EntityEnderman && Config.SlayerAuraType == 3);
    }

    private boolean isSvenPup(Entity entity) {
        return entity.getCustomNameTag().contains("Pup") || entity.getName().contains("Pup");
    }

    private static String getSlayerName() {
        switch (Config.SlayerAuraType) {
            case 0: // rev
                return "Revenant Horror";
            case 1: // brood
                return "Tarantula Broodfather";
            case 2: // sven
                return "Sven Packmaster";
            case 3: // void
                return "Voidgloom Seraph";
            default:
                return "None";
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
                "§rMacro: §fSlayer Aura - " + getSlayerName(),
                "§rTime: §f" + (Scheduler.getInstance().isRunning() ? Scheduler.getInstance().getStatusString() : "Macroing"),
                "§rBosses Killed: §f" + killedBosses,
                "§rMAX SKILL",
                "§rState: §f" + currentState.name(),
                "§r§lTarget:",
                "§rBoss Spawned: §f" + (killingBoss ? "Yes" : "No"),
                "§rDistance: §f" + (target != null ? (String.format("%.2f", Math.floor(Minecraft.getMinecraft().thePlayer.getDistanceToEntity(target))) + "m") : "No target"),
                "§rVisible: §f" + (target != null ? (PlayerUtils.entityIsVisible(target) ? "Yes" : "No") : "No target"),
        };
    }

    private static String[] drawSkillInfo() {
        double xpToShow = SkillTracker.getText("Combat");
        int xpPerHour = (int) Math.round(xpToShow / ((SkillTracker.skillStopwatch.getTime() + 1) / 3600000d));
        int nxtLvl = Config.Skill_Combat + 1;

        return new String[]{
                "§r§lStats:",
                "§rMacro: §fSlayer Aura - " + getSlayerName(),
                "§rTime: §f" + (Scheduler.getInstance().isRunning() ? Scheduler.getInstance().getStatusString() : "Macroing"),
                "§rBosses Killed: §f" + killedBosses,
                "§rXP Earned: §f" + NumberFormat.getNumberInstance(Locale.US).format(xpToShow) + " [" + NumberFormat.getIntegerInstance(Locale.US).format(xpPerHour) + "/hr]",
                "§rTime til' Lvl. " + nxtLvl + ": §f" + SkillTracker.getTimeBetween(0, SkillTracker.xpLeft / (xpPerHour / 3600D)),
                "§rState: §f" + currentState.name(),
                "§r§lTarget:",
                "§rBoss Spawned: §f" + (killingBoss ? "Yes" : "No"),
                "§rDistance: §f" + (target != null ? (String.format("%.2f", Math.floor(Minecraft.getMinecraft().thePlayer.getDistanceToEntity(target))) + "m") : "No target"),
                "§rVisible: §f" + (target != null ? (PlayerUtils.entityIsVisible(target) ? "Yes" : "No") : "No target"),
        };
    }

    private static String[] drawDefaultInfo() {
        return new String[]{
                "§r§lStats:",
                "§rMacro: §fSlayer Aura - " + getSlayerName(),
                "§rTime: §f" + (Scheduler.getInstance().isRunning() ? Scheduler.getInstance().getStatusString() : "Macroing"),
                "§rBosses Killed: §f" + killedBosses,
                "§rOpen '/skills' to track xp",
                "§rState: §f" + currentState.name(),
                "§r§lTarget:",
                "§rBoss Spawned: §f" + (killingBoss ? "Yes" : "No"),
                "§rDistance: §f" + (target != null ? (String.format("%.2f", Math.floor(Minecraft.getMinecraft().thePlayer.getDistanceToEntity(target))) + "m") : "No target"),
                "§rVisible: §f" + (target != null ? (PlayerUtils.entityIsVisible(target) ? "Yes" : "No") : "No target"),
        };
    }

    public enum State {
        NONE,
        ATTACKING,
        KILLED,
        PAUSE
    }
}
