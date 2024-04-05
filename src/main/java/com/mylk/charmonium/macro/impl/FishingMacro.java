package com.mylk.charmonium.macro.impl;

import com.mylk.charmonium.Charmonium;
import com.mylk.charmonium.config.Config;
import com.mylk.charmonium.config.aotv.AOTVWaypointsStructs;
import com.mylk.charmonium.event.MillisecondEvent;
import com.mylk.charmonium.event.SpawnParticleEvent;
import com.mylk.charmonium.feature.impl.Scheduler;
import com.mylk.charmonium.feature.impl.SkillTracker;
import com.mylk.charmonium.handler.GameStateHandler;
import com.mylk.charmonium.handler.MacroHandler;
import com.mylk.charmonium.handler.RotationHandler;
import com.mylk.charmonium.macro.AbstractMacro;
import com.mylk.charmonium.macro.impl.misc.routes;
import com.mylk.charmonium.util.*;
import com.mylk.charmonium.util.charHelpers.npcUtils;
import com.mylk.charmonium.util.helper.*;
import com.mylk.charmonium.util.helper.Timer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.entity.projectile.EntityPotion;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class FishingMacro extends AbstractMacro {
   private static State currentState = State.NONE;
   private static AntiAfkState currentAFKState = AntiAfkState.AWAY;
   private static int currentWaypoint = 0;
   private final Timer closestPointCooldown = new Timer();
   private static final List<String> fishingMobs = new ArrayList<String>() {
      {
         add("Squid");
         add("Sea Walker");
         add("Night Squid");
         add("Sea Guardian");
         add("Sea Witch");
         add("Sea Archer");
         add("Rider Of The Deep");
         add("Catfish");
         add("Carrot King");
         add("Sea Leech");
         add("Guardian Defender");
         add("Deep Sea Protector");
         add("Water Hydra");
         add("The Sea Emperor");
         add("Agarimoo");
         add("Magma Slug");
         add("Moogma");
         add("Lava Leech");
         add("Pyroclastic Worm");
         add("Lava Flame");
         add("Fire Eel");
         add("Taurus");
         add("Thunder");
         add("Lord Jawbus");
         add("Oasis Rabbit");
         add("Oasis Sheep");
         add("Water Worm");
         add("Poisoned Water Worm");
         add("Zombie Miner");
         add("Scarecrow");
         add("Nightmare");
         add("Werewolf");
         add("Phantom Fisherman");
         add("Grim Reaper");
         add("Plhlegblast");
         add("Flaming Worm");
         add("Lava Blaze");
         add("Lava Pigman");
         add("Blue Shark");
         add("Great White Shark");
         add("Nurse Shark");
         add("Rider of the Deep");
         add("Sea Emperor");
         add("Tiger Shark");
      }
   };
   private static final List<BlockPos> fishingBlockList = new ArrayList<BlockPos>() {
      {
         add(new BlockPos(-63, 69, 21));
         add(new BlockPos(-65, 69, 20));
         add(new BlockPos(-68, 69, 19));
         add(new BlockPos(-71, 69, 18));
         add(new BlockPos(-61, 69, 21));
      }
   };
   public static String currentTool = "";
   private final Timer throwTimer = new Timer();
   private final Timer inWaterTimer = new Timer();
   private final Timer stuckWaterTimer = new Timer();
   private static int fishFished = 0;
   private static int monstersKilled = 0;
   private final Timer noKillTimer = new Timer();
   private final Timer recastDelay = new Timer();
   private final Timer antiAfkTimer = new Timer();
   public static boolean hasDied = false;
   private final Timer diedDelay = new Timer();
   private static List<Entity> ignoreEntity;
   private double oldBobberPosY = 0.0D;
   public static Entity target;
   public static Entity targetStand;
   private final Timer attackDelay = new Timer();
   private static Rotation startRotation;
   private static Rotation inWaterRotation;
   private static BlockPos FishingBlock;
   public static boolean stuckInWater = false;
   private static Rotation randomRotation;
   private static final CopyOnWriteArrayList<ParticleEntry> particles = new CopyOnWriteArrayList<>();
   private final RotationHandler rotation = RotationHandler.getInstance();
   private static FishingMacro instance;

   public static FishingMacro getInstance() {
      if (instance == null) {
         instance = new FishingMacro();
      }
      return instance;
   }
   public void onEnable() {
      Config.MacroEnum crop = Config.MacroEnum.FISHING;
      currentWaypoint = 0;
      currentTool = Config.fishingRodName;
      closestPointCooldown.reset();
      target = null;
      targetStand = null;
      hasDied = false;
      startRotation = null;
      currentState = State.NONE;
      stuckInWater = false;
      recastDelay.schedule();
      antiAfkTimer.schedule();
      throwTimer.schedule();
      ignoreEntity = new ArrayList<>();
      inWaterTimer.schedule();
      stuckWaterTimer.reset();
      inWaterRotation = null;
      Random random = new Random();
      randomRotation = new Rotation(randomLook(17, 28), randomLook(0, 24));
      FishingBlock = fishingBlockList.get(random.nextInt(fishingBlockList.size()));

      LogUtils.sendDebug("Macro: " + crop);
      MacroHandler.getInstance().setCrop(crop);
      PlayerUtils.getTool();

      if (isNearSpawn()) {
         currentState = State.AT_SPAWN;
         MacroHandler.walkTo(routes.wayToFishing, currentWaypoint);
         Charmonium.sendMessage("Walking to fishing [far]");
         currentWaypoint++;
      } else if (isNearWaypoint()) {
         Charmonium.sendMessage("Walking to fishing [close]");
         currentState = State.THROWING;
         MacroHandler.walkTo(FishingBlock);
      } else {
         mc.thePlayer.sendChatMessage("/hub");
         currentState = State.AT_SPAWN;
      }

      super.onEnable();
   }

   public void invokeState() {
      particles.removeIf(p -> (System.currentTimeMillis() - p.timeAdded) > 1000);
      List<Entity> entities = getEntity();
      ItemStack heldItem;

      if (!entities.isEmpty() && target == null) {
         Optional<Entity> optional = entities.stream().min(Comparator.comparingDouble(entity -> entity.getDistanceToEntity(mc.thePlayer)));

         targetStand = optional.get();
         target = npcUtils.getEntityCuttingOtherEntity(targetStand, null);
         noKillTimer.schedule();
         currentState = State.KILL;
      }

      if (mc.thePlayer.isInWater()) {
         stuckInWater = true;
         if (!stuckWaterTimer.isScheduled()) stuckWaterTimer.schedule();
         if (!MacroHandler.walker.isDone()) MacroHandler.walker.stop();
         currentTool = "Aspect of the";
         if (inWaterRotation == null) inWaterRotation = rotation.getRotation(new Vec3(-67, 74, 21));
         if (AngleUtils.smallestAngleDifference(mc.thePlayer.rotationYaw, inWaterRotation.getYaw() ) > 2 ||
                 AngleUtils.smallestAngleDifference(mc.thePlayer.rotationPitch, inWaterRotation.getPitch()) > 2) {
            rotation.easeTo(new RotationConfiguration(inWaterRotation, Config.getRandomRotationTime(), null));
         }
         if (!stuckWaterTimer.hasPassed(356)) return;
         stuckWaterTimer.schedule();
         KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindSneak, false);
         KeyBindUtils.rightClick();
         Charmonium.sendMessage("Stuck in water, using AOT");
         return;
      } else if (stuckInWater && !mc.thePlayer.isInWater()) {
         stuckInWater = false;
         stuckWaterTimer.reset();
         inWaterRotation = null;
         Charmonium.sendMessage("No longer stuck in water");
         resetMacroState();
      }

      if (antiAfkTimer.hasPassed(3000 + new Random().nextInt(1500)) && currentState != State.KILL) {
         antiAfkTimer.schedule();

         if (!rotation.isRotating() && startRotation != null) {
            switch (currentAFKState) {
               case AWAY: {
                  Rotation randomRotation = new Rotation(startRotation.getYaw() + (-2 + new Random().nextInt(4)), startRotation.getPitch() + (-2 + new Random().nextInt(4)));
                  rotation.easeTo(
                          new RotationConfiguration(randomRotation, Config.getRandomRotationTime(), null));
                  currentAFKState = AntiAfkState.BACK;
                  break;
               }
               case BACK: {
                  rotation.easeTo(
                          new RotationConfiguration(startRotation, Config.getRandomRotationTime(), null));
                  currentAFKState = AntiAfkState.AWAY;
                  break;
               }
            }
         }
      }

      switch (currentState) {
         case NONE:
            KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindSneak, false);
            currentWaypoint = 0;
            currentState = State.AT_SPAWN;
            break;
         case AT_SPAWN:
            if (currentWaypoint >= routes.wayToFishing.size()) {
               currentWaypoint = 0;
               return;
            }

            KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindSneak, false);
            if (Charmonium.mc.thePlayer.getPositionVector().distanceTo(BlockUtils.fromBPToVec(new BlockPos(-68, 68, 17))) < 5) {
               currentWaypoint = 0;
               currentState = State.THROWING;
               Charmonium.sendMessage("Arrived at fishing spot");
               return;
            }

            if (MacroHandler.walker.isDone()) {
               MacroHandler.walkTo(routes.wayToFishing, currentWaypoint);
               currentWaypoint++;
            }

            break;
         case THROWING:
            if (Charmonium.mc.thePlayer.getPositionVector().distanceTo(BlockUtils.fromBPToVec(FishingBlock)) < 1.5) {
               currentTool = Config.fishingRodName;
               KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindSneak, true);

               if (rotation.isRotating()) break;

               if (AngleUtils.smallestAngleDifference(mc.thePlayer.rotationYaw, randomRotation.getYaw() ) > 4 ||
                       AngleUtils.smallestAngleDifference(mc.thePlayer.rotationPitch, randomRotation.getPitch()) > 4) {
                  startRotation = new Rotation(randomRotation.getYaw() + (float) Math.random(), randomRotation.getPitch() + (float) Math.random());
                  rotation.easeTo(new RotationConfiguration(startRotation, (long)(Config.getRandomRotationTime() * 1.5), null));
                  break;
               }

               if (mc.thePlayer.fishEntity == null && throwTimer.hasPassed(250)) {
                  mc.thePlayer.inventory.currentItem = PlayerUtils.getFarmingTool(MacroHandler.getInstance().getCrop());
                  mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem());
                  throwTimer.schedule();
                  inWaterTimer.schedule();
                  currentState = State.IN_WATER;
                  break;
               }
               if (throwTimer.hasPassed(2500) && mc.thePlayer.fishEntity != null) {
                  currentState = State.FISH_BITE;
               }
            } else {
               if (MacroHandler.walker.isDone()) {
                  MacroHandler.walkTo(FishingBlock);
               }
            }
            break;
         case IN_WATER:
            heldItem = mc.thePlayer.getHeldItem();
            if (heldItem != null && heldItem.getItem() == Items.fishing_rod) {
               if (throwTimer.hasPassed(500) && mc.thePlayer.fishEntity != null) {
                  if (mc.thePlayer.fishEntity.isInWater() || mc.thePlayer.fishEntity.isInLava()) {
                     EntityFishHook bobber = mc.thePlayer.fishEntity;
                     if (inWaterTimer.hasPassed(2500) && Math.abs(bobber.motionX) < 0.01 && Math.abs(bobber.motionZ) < 0.01) {
                        double movement = bobber.posY - oldBobberPosY;
                        oldBobberPosY = bobber.posY;
                        if ((movement < -0.04 && isBobberNearParticles(bobber)) || bobber.caughtEntity != null) {
                           currentState = State.FISH_BITE;
                           fishFished++;
                        }
                     }
                     break;
                  }
                  if (inWaterTimer.hasPassed(2500)) {
                     currentState = State.FISH_BITE;
                  }
                  break;
               }
               if (throwTimer.hasPassed(2500) && mc.thePlayer.fishEntity == null) {
                  throwTimer.schedule();
                  currentState = State.THROWING;
               }
               break;
            }
            break;
         case FISH_BITE:
            if (mc.thePlayer.fishEntity != null && recastDelay.hasPassed(500)) {
               mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem());
               recastDelay.schedule();
            }

            if (recastDelay.hasPassed((long)(Config.recastDelay * 1.5))) {
               currentState = State.THROWING;
            }
            break;
         case KILL:
            if (noKillTimer.hasPassed(6000)) {
               Charmonium.sendMessage("Stuck? Finding new");
               ignoreEntity.add(target);
               resetMacroState();
            }
            if (target instanceof EntityPotion) return;

            if (target == null || targetStand == null  || target instanceof EntityItem || target instanceof EntityXPOrb || ((EntityLivingBase) target).getHealth() <= 1 || target.isDead) {
               monstersKilled++;
               resetMacroState();
               break;
            }

            boolean visible = PlayerUtils.entityIsVisible(target);

            if (!visible) {
               Charmonium.sendMessage("Something is blocking target, moving on...");
               ignoreEntity.add(target);
               resetMacroState();
            } else {
               boolean targeted = npcUtils.entityIsTargeted(target);
               if (target != null && !targeted) {
                  if (Config.SFAttackType == Config.SFAttackEnum.HYPERION.ordinal())
                     rotation.easeTo(new RotationConfiguration(new Rotation(mc.thePlayer.rotationYaw, 90.0F), Config.getRandomRotationTime(), null));
                  else
                     rotation.easeTo(new RotationConfiguration(rotation.getRotation(target.getPositionVector().add(new Vec3(0, target.height / 2, 0))), Config.getRandomRotationTime(), null));
               }

               assert target != null;
               currentTool = Config.fishingWeaponName;

               if (Config.SFAttackType == Config.SFAttackEnum.LEFT_CLICK.ordinal()) {
                  if (Minecraft.getMinecraft().thePlayer.getDistanceToEntity(target) <= mc.playerController.getBlockReachDistance()) {
                     if (Config.shiftWhenKill)
                        KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindSneak, true);
                     if (attackDelay.hasPassed(100)) {
                        KeyBindUtils.stopMovement();
                        KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindJump, false);
                        KeyBindUtils.onTick(mc.gameSettings.keyBindAttack);
                        attackDelay.schedule();
                     }
                  } else {
                     KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindForward, true);
                     KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindSprint, true);
                     KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindSneak, false);
                     if (!canWalk("Forward") && Config.SlayerType != 0) {
                        KeyBindUtils.holdThese(mc.gameSettings.keyBindJump);
                        return;
                     }
                     if (Config.jumpWhenWalking)
                        KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindJump, true);
                  }
               } else if (Config.SFAttackType == Config.SFAttackEnum.RANGED.ordinal()){
                  if (Minecraft.getMinecraft().thePlayer.getDistanceToEntity(target) <= 8) {
                     if (Config.shiftWhenKill)
                        KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindSneak, true);
                     if (attackDelay.hasPassed(100)) {
                        KeyBindUtils.stopMovement();
                        KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindJump, false);
                        KeyBindUtils.onTick(mc.gameSettings.keyBindUseItem);
                        attackDelay.schedule();
                     }
                  }
               } else {
                  if (AngleUtils.getAngleDifference(mc.thePlayer.rotationPitch, 90) >= 10) return;
                  if (Minecraft.getMinecraft().thePlayer.getDistanceToEntity(target) <= 5) {
                     if (Config.shiftWhenKill)
                        KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindSneak, true);
                     if (attackDelay.hasPassed(100)) {
                        KeyBindUtils.stopMovement();
                        KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindJump, false);
                        KeyBindUtils.onTick(mc.gameSettings.keyBindUseItem);
                        attackDelay.schedule();
                     }
                  }
               }
            }

            break;
      }
   }

   @SubscribeEvent
   public void onRender(RenderWorldLastEvent event) {
      assert mc.thePlayer != null;
      assert mc.theWorld != null;

      if (!GameStateHandler.getInstance().atProperIsland() || !MacroHandler.getInstance().isMacroToggled() || MacroHandler.getInstance().getCrop() != Config.MacroEnum.FISHING) return;

      if (currentState == State.AT_SPAWN) {
         for (AOTVWaypointsStructs.Waypoint value : routes.wayToFishing) {
            BlockPos waypoint = new BlockPos(value.x, value.y, value.z);
            RenderUtils.drawBlockBox(waypoint, Color.PINK);
         }
         return;
      }

      if (target != null) {
         RenderUtils.renderBoundingBox(target, event.partialTicks, new Color(255, 89, 128, 157).getRGB());
      }
   }

   @SubscribeEvent
   public void onChatReceive(ClientChatReceivedEvent event) {
      assert mc.thePlayer != null;
      assert mc.theWorld != null;

      if (!GameStateHandler.getInstance().atProperIsland() || !MacroHandler.getInstance().isMacroToggled() || MacroHandler.getInstance().getCrop() != Config.MacroEnum.FISHING) return;

      String message = StringUtils.stripControlCodes(event.message.getUnformattedText());
      if (message.contains("You were killed by")) {
         hasDied = true;
         Charmonium.sendMessage("Warping to location");
         MacroHandler.walker.stop();
         recastDelay.schedule();
         antiAfkTimer.schedule();
         target = null;
         targetStand = null;
         currentAFKState = AntiAfkState.AWAY;
         diedDelay.schedule();
      }
   }

   @SubscribeEvent
   public void handleParticles(SpawnParticleEvent packet) {
      assert mc.thePlayer != null;
      assert mc.theWorld != null;

      if (!GameStateHandler.getInstance().atProperIsland() || !MacroHandler.getInstance().isMacroToggled() || MacroHandler.getInstance().getCrop() != Config.MacroEnum.FISHING) return;

      if (packet.getParticleTypes() == EnumParticleTypes.WATER_WAKE || packet.getParticleTypes() == EnumParticleTypes.SMOKE_NORMAL || packet.getParticleTypes() == EnumParticleTypes.FLAME) {
         particles.add(new ParticleEntry(new Vec3(packet.getXCoord(), packet.getYCoord(), packet.getZCoord()), System.currentTimeMillis()));
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
      return mc.theWorld.getEntitiesInAABBexcluding(mc.thePlayer,
                      mc.thePlayer.getEntityBoundingBox().expand(Config.seaCeatureRange, (Config.seaCeatureRange >> 1), Config.seaCeatureRange),
                      e -> e instanceof EntityArmorStand).stream()
              .filter((v) -> v.getDistanceToEntity(mc.thePlayer) < Config.seaCeatureRange &&
              !v.getName().contains(mc.thePlayer.getName()) &&
              !v.isDead &&
              ((EntityLivingBase) v).getHealth() > 0 &&
              fishingMobs.stream().anyMatch((a) -> v.getCustomNameTag().contains(a)) &&
              !v.getCustomNameTag().toLowerCase().contains("squid"))
              .filter(PlayerUtils::entityIsVisible)
              .collect(Collectors.toList());
   }

   private void resetMacroState() {
      throwTimer.schedule();
      recastDelay.schedule();
      antiAfkTimer.schedule();
      stuckWaterTimer.reset();
      currentState = State.THROWING;
      target = null;
      targetStand = null;
      startRotation = null;
      inWaterTimer.schedule();
   }

   private boolean isNearSpawn() {
      Vec3 spawn = new Vec3(-3, 70, -70);
      return Charmonium.mc.thePlayer.getPositionVector().distanceTo(spawn) < 3;
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
              "§rMacro: §fFishing",
              "",
              "§rFish Fished: §f" + fishFished,
              "§rMonsters Killed: §f" + monstersKilled,
              "§rTime: §f" + (Scheduler.getInstance().isRunning() ? Scheduler.getInstance().getStatusString() : "Macroing"),
              "§rMAX SKILL",
              "§rState: §f" + currentState.name(),
      };
   }

   private static String[] drawSkillInfo() {
      double xpToShow = SkillTracker.getText("Fishing");
      int xpPerHour = (int) Math.round(xpToShow / ((SkillTracker.skillStopwatch.getTime() + 1) / 3600000d));
      int nxtLvl = Config.Skill_Fishing + 1;

      return new String[]{
              "§r§lStats:",
              "§rMacro: §fFishing",
              "",
              "§rFish Fished: §f" + fishFished,
              "§rMonsters Killed: §f" + monstersKilled,
              "§rTime: §f" + (Scheduler.getInstance().isRunning() ? Scheduler.getInstance().getStatusString() : "Macroing"),
              "§rXP Earned: §f" + NumberFormat.getNumberInstance(Locale.US).format(xpToShow) + " [" + NumberFormat.getNumberInstance(Locale.US).format(xpPerHour) + "/h]",
              "§rTime til' Lvl. " + nxtLvl + ": §f" + SkillTracker.getTimeBetween(0, SkillTracker.xpLeft / (xpPerHour / 3600D)),
              "§rState: §f" + currentState.name(),
      };
   }

   private static String[] drawDefaultInfo() {
      return new String[]{
              "§r§lStats:",
              "§rMacro: §fFishing",
              "",
              "§rFish Fished: §f" + fishFished,
              "§rMonsters Killed: §f" + monstersKilled,
              "§rTime: §f" + (Scheduler.getInstance().isRunning() ? Scheduler.getInstance().getStatusString() : "Macroing"),
              "§rOpen '/skills' to track xp",
              "§rState: §f" + currentState.name(),
      };
   }

   private boolean isNearWaypoint() {
      return Charmonium.mc.thePlayer.getPositionVector().distanceTo(BlockUtils.fromBPToVec(FishingBlock)) < 15;
   }

   public double getHorizontalDistance(Vec3 vec1, Vec3 vec2) {
      double d0 = vec1.xCoord - vec2.xCoord;
      double d2 = vec1.zCoord - vec2.zCoord;
      return MathHelper.sqrt_double(d0 * d0 + d2 * d2);
   }

   private boolean isBobberNearParticles(EntityFishHook bobber) {
      return particles.stream().anyMatch(v -> (getHorizontalDistance(bobber.getPositionVector(), v.position) < 0.2D));
   }

   float randomLook(double min, double max) {
      Random r = new Random();
      return (float) ((r.nextInt((int)((max-min)*10+1))+min*10) / 10.0);
   }

   private static class ParticleEntry {
      public Vec3 position;

      public long timeAdded;

      public ParticleEntry(Vec3 position, long timeAdded) {
         this.position = position;
         this.timeAdded = timeAdded;
      }
   }

   public enum State {
      NONE,
      AT_SPAWN,
      THROWING,
      IN_WATER,
      FISH_BITE,
      KILL,
   }

   public enum AntiAfkState {
      AWAY,
      BACK,
   }
}