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
import com.mylk.charmonium.handler.slayerHandler;
import com.mylk.charmonium.macro.AbstractMacro;
import com.mylk.charmonium.macro.impl.misc.Maddoxer;
import com.mylk.charmonium.macro.impl.misc.routes;
import com.mylk.charmonium.util.*;
import com.mylk.charmonium.util.charHelpers.npcUtils;
import com.mylk.charmonium.util.helper.Rotation;
import com.mylk.charmonium.util.helper.RotationConfiguration;
import com.mylk.charmonium.util.helper.Timer;
import net.minecraft.client.Minecraft;
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

public class SlayerMacroB extends AbstractMacro {
   private static ArrayList<AOTVWaypointsStructs.Waypoint> wayToPath;
   private static ArrayList<AOTVWaypointsStructs.Waypoint> Path;
   private static BlockPos TeleportSpawn;
   private static BlockPos WalkToEnd;
   public static State currentState = State.NONE;
   private static int currentWaypoint = 0;
   private final RotationHandler rotation = RotationHandler.getInstance();
   private final Timer closestPointCooldown = new Timer();
   private final Timer noKillTimer = new Timer();
   private final Timer noFindTimer = new Timer();
   private final Timer attackDelay = new Timer();
   public static final Timer diedDelay = new Timer();
   private final Timer afterKillDelay = new Timer();
   public static Entity target;
   private static List<Entity> ignoreEntity;
   public static boolean hasDied = false;
   public static boolean killingBoss = false;
   private static int killedBosses = 0;
   public static boolean killingMiniBoss = false;
   public static boolean killedBoss = false;
   private static SlayerMacroB instance;

   public static SlayerMacroB getInstance() {
      if (instance == null) {
         instance = new SlayerMacroB();
      }
      return instance;
   }
   public void onEnable() {
      Config.MacroEnum crop = Config.MacroEnum.SLAYER;
      currentWaypoint = 0;
      currentState = State.NONE;
      rotation.reset();
      ignoreEntity = new ArrayList<>();
      target = null;
      hasDied = false;
      killingBoss = false;
      killedBoss = false;
      killingMiniBoss = false;
      super.setSpawnPos(new Vec3(-3, 70, -70));

      switch (Config.SlayerType) {
         case 0: // rev
            if (Config.useWarps) {
               wayToPath = routes.wayToCrypt;
               TeleportSpawn = new BlockPos(-162, 61, -100);
            }
            else {
               wayToPath = routes.wayToCryptSpawn;
               GameStateHandler.islandWarp = "/hub";
               TeleportSpawn = new BlockPos(-3, 70, -70);
            }
            Path = routes.cryptGhouls;
            WalkToEnd = new BlockPos(-150, 57, -101);
            break;
         case 1: // brood
            break;
         case 2: // sven howl
            wayToPath = routes.wayToSven;
            Path = routes.svenHowl;
            TeleportSpawn = new BlockPos(-333, 89, -56);
            WalkToEnd = new BlockPos(-362, 70, -58);
            break;
         case 3: // sven castle
            break;
         case 4: // void sepeture
            break;
         case 5: // void bruser
            break;
         default:
            Charmonium.sendMessage("No Slayer Selected");
            MacroHandler.getInstance().disableMacro();
            break;
      }

      LogUtils.sendDebug("Macro: " + crop);
      MacroHandler.getInstance().setCrop(crop);
      PlayerUtils.getTool();

      if (isNearFarSpawn()) {
         currentState = State.AT_SPAWN;
         Charmonium.sendMessage("Warping to location");
         if (Config.useWarps) {
            mc.thePlayer.sendChatMessage(GameStateHandler.islandWarp);
            Charmonium.sendMessage("Walking to mobs [far]");
            MacroHandler.walkTo(wayToPath, currentWaypoint, TeleportSpawn);
            currentWaypoint++;
         }
         else {
             MacroHandler.walkTo(wayToPath, currentWaypoint);
             currentWaypoint++;
         }
      } else if (isNearSpawn()) {
         currentState = State.AT_SPAWN;
         MacroHandler.walkTo(wayToPath, currentWaypoint);
         Charmonium.sendMessage("Walking to mobs [close]");
         currentWaypoint++;
      } else if (isNearWaypoint()) {
         currentState = State.WALKING;
         MacroHandler.walkTo(Path, getClosestPoint());
         closestPointCooldown.schedule();
      } else {
         Charmonium.sendMessage("U in the middle of no where, retard");
         diedDelay.schedule();
         hasDied = true;
      }

      super.onEnable();
   }

   public void invokeState() {
      List<Entity> entities = getEntity();
      //List<String> scoreboard = ScoreboardUtils.getScoreboardLines();

      if (slayerHandler.currentSlayerBoss != null) {
         target = slayerHandler.currentSlayerBoss;
         MacroHandler.walker.stop();
         killingBoss = true;
         currentState = State.ATTACKING;
         noKillTimer.schedule();
      } else if (slayerHandler.currentMiniBoss != null) {
         target = slayerHandler.currentMiniBoss;
         MacroHandler.walker.stop();
         killingMiniBoss = true;
         currentState = State.ATTACKING;
         noKillTimer.schedule();
      }

      switch (currentState) {
         case NONE:
            currentWaypoint = 0;
            currentState = State.AT_SPAWN;
            break;
         case AT_SPAWN:
            if (Charmonium.mc.thePlayer.getPositionVector().distanceTo(BlockUtils.fromBPToVec(WalkToEnd)) < 5) {
               currentWaypoint = 0;
               currentState = State.WALKING;
               Charmonium.sendMessage("Arrived at mobs");
               return;
            }

            if (MacroHandler.walker.isDone()) {
               MacroHandler.walkTo(wayToPath, currentWaypoint);
               currentWaypoint++;
            }

            break;
         case WALKING:
            if (currentWaypoint >= Path.size())
               currentWaypoint = 0;

            if (MacroHandler.walker.isDone() && !killingBoss && !killingMiniBoss) {
               ignoreEntity = new ArrayList<>();
               if (!entities.isEmpty()) {
                  currentState = State.SEARCHING;
                  MacroHandler.walker.stop();
                  return;
               }

               MacroHandler.walkTo(Path, currentWaypoint);
               currentWaypoint++;
            }
            break;
         case SEARCHING:
            KeyBindUtils.stopMovement();
            Optional<Entity> optional = entities.stream().min(Comparator.comparingDouble(entity -> entity.getDistanceToEntity(mc.thePlayer)));

            if (!killingBoss && !killingMiniBoss) {
               if (optional.isPresent()) {
                  target = optional.get();
                  noKillTimer.schedule();
                  currentState = State.ATTACKING;
               } else {
                  if (noFindTimer.hasPassed(5000)) {
                     currentState = State.WALKING;
                     noFindTimer.reset();
                  }
               }
            }
            break;
         case ATTACKING:
            if (noKillTimer.hasPassed(6000) && !killingBoss && !killingMiniBoss) {
               Charmonium.sendMessage("Stuck? Finding new");
               currentState = State.SEARCHING;
               ignoreEntity.add(target);
               target = null;
            }

            if (npcUtils.getEntityHp(target) <= 1 || Minecraft.getMinecraft().thePlayer.getDistanceToEntity(target) > Config.scanRange || (target != null && (target.isDead))) {
               currentState = State.KILLED;
               afterKillDelay.schedule();
               break;
            }

            boolean visible = PlayerUtils.entityIsVisible(target);

            if (!visible && !killingBoss && !killingMiniBoss) {
               Charmonium.sendMessage("Something is blocking target, moving on...");
               ignoreEntity.add(target);
               target = null;
               currentState = State.SEARCHING;
            } else {
               assert target != null;
               if (Config.SAttackType == Config.SAttackEnum.LEFT_CLICK.ordinal()) {

                  boolean targeted = npcUtils.entityIsTargeted(target);
                  if (target != null && !targeted) {
                     rotation.easeTo(new RotationConfiguration(rotation.getRotation(target.getPositionVector().add(new Vec3(0, target.height / 2, 0))), Config.getRandomRotationTime(), null));
                  }

                  if (killingBoss || killingMiniBoss) {
                     if (Minecraft.getMinecraft().thePlayer.getDistanceToEntity(target) <= 3) {
                        KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindSneak, true);
                        if (Minecraft.getMinecraft().thePlayer.getDistanceToEntity(target) <= 2.5) {
                           if (canWalk("Back")) {
                              KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindBack, true);
                           }
                           else if (canWalk("Left")) {
                              KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindLeft, true);
                              KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindBack, true);
                           }
                           else if (canWalk("Right")) {
                              KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindRight, true);
                              KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindBack, true);
                           }
                        } else {
                           KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindBack, false);
                           KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindLeft, false);
                           KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindRight, false);
                        }
                        if (attackDelay.hasPassed(100)) {
                           KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindForward, false);
                           KeyBindUtils.onTick(mc.gameSettings.keyBindAttack);
                           attackDelay.schedule();
                        }
                     } else {
                        KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindSneak, false);
                        KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindSprint, true);
                        KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindForward, true);
                        if (!canWalk("Forward")  && Config.SlayerType != 0)
                            KeyBindUtils.holdThese(mc.gameSettings.keyBindJump);
                     }
                  } else {
                     if (Minecraft.getMinecraft().thePlayer.getDistanceToEntity(target) <= 2.9) {
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
                  }
               } else {
                  if (killingBoss || killingMiniBoss) {

                     boolean targeted = npcUtils.entityIsTargeted(target);
                     if (target != null && !targeted) {
                        rotation.easeTo(new RotationConfiguration(rotation.getRotation(target.getPositionVector().add(new Vec3(0, target.height / 2, 0))), Config.getRandomRotationTime() / 2, null));
                     }

                     if (Minecraft.getMinecraft().thePlayer.getDistanceToEntity(target) <= 6) {
                        KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindSneak, true);
                        if (Minecraft.getMinecraft().thePlayer.getDistanceToEntity(target) <= 2.5) {
                           if (canWalk("Back")) {
                              KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindBack, true);
                           }
                           else if (canWalk("Left")) {
                              KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindLeft, true);
                              KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindBack, true);
                           }
                           else if (canWalk("Right")) {
                              KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindRight, true);
                              KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindBack, true);
                           }
                        } else {
                           KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindBack, false);
                           KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindLeft, false);
                           KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindRight, false);
                        }
                        if (attackDelay.hasPassed(100)) {
                           KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindForward, false);
                           KeyBindUtils.onTick(mc.gameSettings.keyBindUseItem);
                           attackDelay.schedule();
                        }
                     } else {
                        KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindSneak, false);
                        KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindSprint, true);
                        KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindForward, true);
                        if (!canWalk("Forward")  && Config.SlayerType != 0)
                           KeyBindUtils.holdThese(mc.gameSettings.keyBindJump);
                     }
                  } else {

                     if (Minecraft.getMinecraft().thePlayer.getDistanceToEntity(target) <= 6) {
                        boolean targeted = npcUtils.entityIsTargeted(target);
                        if (target != null && !targeted) {
                           rotation.easeTo(new RotationConfiguration(new Rotation(rotation.getRotation(target.getPositionVector().add(new Vec3(0, target.height / 2, 0))).getYaw(), 50.f), Config.getRandomRotationTime() / 2, null));
                        }
                        if (Minecraft.getMinecraft().thePlayer.getDistanceToEntity(target) <= 2.5) {
                           if (canWalk("Back")) {
                              KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindBack, true);
                           }
                           else if (canWalk("Left")) {
                              KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindLeft, true);
                              KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindBack, true);
                           }
                           else if (canWalk("Right")) {
                              KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindRight, true);
                              KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindBack, true);
                           }
                        } else {
                           KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindBack, false);
                           KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindLeft, false);
                           KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindRight, false);
                        }
                        if (attackDelay.hasPassed(700)) {
                           KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindForward, false);
                           KeyBindUtils.onTick(mc.gameSettings.keyBindUseItem);
                           attackDelay.schedule();
                        }
                     } else {

                        boolean targeted = npcUtils.entityIsTargeted(target);
                        if (target != null && !targeted) {
                           rotation.easeTo(new RotationConfiguration(rotation.getRotation(target.getPositionVector().add(new Vec3(0, target.height / 2, 0))), Config.getRandomRotationTime() / 2, null));
                        }
                        KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindSneak, false);
                        KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindSprint, true);
                        KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindForward, true);
                        if (!canWalk("Forward")  && Config.SlayerType != 0)
                           KeyBindUtils.holdThese(mc.gameSettings.keyBindJump);
                     }
                  }
               }
            }

            break;
         case KILLED:
            if (!afterKillDelay.hasPassed(150))
               return;

            target = null;
            if (killingBoss && slayerHandler.currentSlayerBoss == null) killingBoss = false;
            if (killingMiniBoss && slayerHandler.currentMiniBoss == null) killingMiniBoss = false;
            currentState = State.SEARCHING;

            break;
         case PAUSE:
            break;
      }
   }

//   @SubscribeEvent
//   public void onUpdate(TickEvent.ClientTickEvent event) {
//      if (!MacroHandler.getInstance().isMacroToggled() || MacroHandler.getInstance().getCrop() != Config.MacroEnum.SLAYER) {
//         return;
//      }
//
//      if (event.phase == TickEvent.Phase.START && currentState == State.WALKING && !killedBoss && !killingBoss) {
//         if (GameStateHandler.getInstance().notMoving()) {
//            Charmonium.sendMessage("Stuck...");
//            hasDied = true;
//            Maddoxer.hasStarted = true;
//            killingMiniBoss = false;
//            diedDelay.schedule();
//         }
//      }
//   }

   @SubscribeEvent
   public void onMillisecond(MillisecondEvent event) {
      if (!MacroHandler.getInstance().isMacroToggled() || MacroHandler.getInstance().getCrop() != Config.MacroEnum.SLAYER) {
         return;
      }

      if (hasDied) {
         if (!diedDelay.hasPassed(2000)) return;

         if (Charmonium.mc.thePlayer.getPositionVector().distanceTo(BlockUtils.fromBPToVec(TeleportSpawn)) > 4) {
            Charmonium.sendMessage("Recovering from death...");
            currentState = State.PAUSE;
            MacroHandler.walker.stop();
            KeyBindUtils.stopMovement();
            target = null;
            killingBoss = false;
            killingMiniBoss = false;
            killedBoss = false;
            mc.thePlayer.sendChatMessage(GameStateHandler.islandWarp);
            diedDelay.schedule();
            return;
         }

         currentWaypoint = 0;
         MacroHandler.walkTo(wayToPath, currentWaypoint);
         currentWaypoint++;
         diedDelay.reset();
         hasDied = false;
         currentState = State.NONE;

         if (Config.autoMaddox) {
            Maddoxer.callMaddox = Maddoxer.MaddoxS.NONE;
            killedBoss = true;
            KeyBindUtils.stopMovement();
            if (Config.enableScheduler)
               Scheduler.getInstance().pause();
            MacroHandler.getInstance().pauseMacro();
         }
      }
   }

   @SubscribeEvent
   public void onRender(RenderWorldLastEvent event) {
      if (mc.thePlayer == null || mc.theWorld == null) return;

      if (!GameStateHandler.getInstance().atProperIsland() || !MacroHandler.getInstance().isMacroToggled() || MacroHandler.getInstance().getCrop() != Config.MacroEnum.SLAYER) return;

      if (currentState == State.AT_SPAWN) {
         for (AOTVWaypointsStructs.Waypoint value : wayToPath) {
            BlockPos waypoint = new BlockPos(value.x, value.y, value.z);
            RenderUtils.drawBlockBox(waypoint, Color.PINK);
         }
         return;
      }

      for (AOTVWaypointsStructs.Waypoint value : Path) {
         BlockPos waypoint = new BlockPos(value.x, value.y, value.z);
         Color chroma = Color.getHSBColor((float) ((System.currentTimeMillis() / 10) % 2000) / 2000, 1, 1);
         Color chromaLowerAlpha = new Color(chroma.getRed(), chroma.getGreen(), chroma.getBlue(), 120);

         RenderUtils.drawBlockBox(waypoint, chromaLowerAlpha);
      }

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
      if (message.contains("You were killed by") && MacroHandler.getInstance().getCrop() == Config.MacroEnum.SLAYER && MacroHandler.getInstance().isMacroToggled()) {
         Charmonium.sendMessage("Warping to location");
         MacroHandler.walker.stop();
         hasDied = true;
         if (killingBoss) Maddoxer.hasStarted = false;
         killingMiniBoss = false;
         killingBoss = false;
         diedDelay.schedule();
      }
      if (message.contains("SLAYER BOSS SLAIN") && MacroHandler.getInstance().getCrop() == Config.MacroEnum.SLAYER && MacroHandler.getInstance().isMacroToggled()) {
         Charmonium.sendMessage("Killed Boss");
         target = null;
         killedBosses++;
         slayerHandler.currentSlayerBoss = null;
         killingBoss = false;
         killingMiniBoss = false;
         if (Config.autoMaddox) {
            Maddoxer.hasStarted = false;
            Maddoxer.callMaddox = Maddoxer.MaddoxS.NONE;
            killedBoss = true;
            KeyBindUtils.stopMovement();
            if (Config.enableScheduler)
               Scheduler.getInstance().pause();
            MacroHandler.getInstance().pauseMacro();
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
                      ((EntityLivingBase) entity).getHealth() > 0 &&
                      mc.thePlayer.getPositionEyes(1).distanceTo(entity.getPositionVector()) <= Config.scanRange &&
                      Math.abs(entity.getPositionVector().yCoord - mc.thePlayer.getPositionVector().yCoord) < 3 &&
                      !ignoreEntity.contains(entity) &&
                      !isSvenPup(entity))
              .filter(PlayerUtils::entityIsVisible)
              .collect(Collectors.toList());
   }

   private Entity getOptimalWalkEntity() {
        List<Entity> entities = mc.theWorld.loadedEntityList.stream()
                .filter(entity -> entity != Charmonium.mc.thePlayer &&
                        canKill(entity) &&
                        !entity.isDead &&
                        ((EntityLivingBase) entity).getHealth() > 0 &&
                        mc.thePlayer.getPositionEyes(1).distanceTo(entity.getPositionVector()) <= 50 &&
                        Math.abs(entity.getPositionVector().yCoord - mc.thePlayer.getPositionVector().yCoord) < 20 &&
                        entity.getDistanceToEntity(mc.thePlayer) < 50 &&
                        !ignoreEntity.contains(entity))
                .collect(Collectors.toList());
        if (entities.isEmpty()) return null;
        return entities.stream().min(Comparator.comparingDouble(entity -> entity.getDistanceToEntity(mc.thePlayer))).get();
   }

   private boolean isNearSpawn() {
      return Charmonium.mc.thePlayer.getPositionVector().distanceTo(BlockUtils.fromBPToVec(TeleportSpawn)) < 3;
   }

   private boolean canKill(Entity entity) {
      return (entity instanceof EntityZombie && Config.SlayerType == 0) ||
              (entity instanceof EntitySpider && Config.SlayerType == 1) ||
              (entity instanceof EntityWolf && Config.SlayerType == 2) ||
              (entity instanceof EntityEnderman && Config.SlayerType == 3);
   }

   private boolean isNearFarSpawn() {
      return Charmonium.mc.thePlayer.getPositionVector().distanceTo(new Vec3(-3, 70, -70)) < 3;
   }

   private boolean isNearWaypoint() {
      int range = Config.SlayerType == 0 ? 10 : 20;
      for (AOTVWaypointsStructs.Waypoint waypoint : Path) {
         if (Charmonium.mc.thePlayer.getPositionVector().distanceTo(new Vec3(waypoint.x, waypoint.y, waypoint.z)) < range) {
            return true;
         }
      }
      return false;
   }

   private int getClosestPoint() {
      double distance = 9999;
      int closest = 0;

      if (currentWaypoint >= Path.size()) {
         currentWaypoint = 0;
      }

      if (!closestPointCooldown.hasPassed(250)) return currentWaypoint;

      int nextWaypoint = findNextWaypoint();

      // Iterate forward
      for (int i = nextWaypoint; i < Path.size(); i++) {
         double currentDistance = mc.thePlayer.getPositionVector().distanceTo(
                 new Vec3(Path.get(i).x, Path.get(i).y, Path.get(i).z)
         );

         if (currentDistance < distance) {
            distance = currentDistance;
            closest = i;
         }
      }

      for (int i = nextWaypoint - 1; i >= 0; i--) {
         double currentDistance = mc.thePlayer.getPositionVector().distanceTo(
                 new Vec3(Path.get(i).x, Path.get(i).y, Path.get(i).z)
         );

         if (currentDistance < distance) {
            distance = currentDistance;
            closest = i;
         }
      }

      currentWaypoint = closest;
      closestPointCooldown.schedule();
      return currentWaypoint;
   }

   private int findNextWaypoint() {
      int nextWaypoint = currentWaypoint + 1;

      if (nextWaypoint >= Path.size()) {
         nextWaypoint = 0;
      }

      return nextWaypoint;
   }

   private boolean isSvenPup(Entity entity) {
        return entity.getCustomNameTag().contains("Pup") || entity.getName().contains("Pup");
   }

   private static String getSlayerName() {
      switch (Config.SlayerType) {
         case 0: // rev
            return "Revenant Horror";
         case 1: // brood
            return "Tarantula Broodfather";
         case 2: // sven howl
            return "Sven Packmaster [Howl]";
         case 3: // sven castle
            return "Sven Packmaster [Castle]";
         case 4: // void sepeture
            return "Voidgloom Seraph [Sepeture]";
         case 5: // void bruser
            return "Voidgloom Seraph [Bruser]";
         default:
            return "None";
      }
   }

    public static String[] drawInfo() {
      if (SkillTracker.skillsInitialized()) {
         double xpToShow = SkillTracker.getText("Combat");
         int xpPerHour = (int) Math.round(xpToShow / ((SkillTracker.skillStopwatch.getTime() + 1) / 3600000d));
         int nxtLvl = Config.Skill_Combat + 1;

         return new String[]{
                 "§r§lStats:",
                 "§rMacro: §fSlayer - " + getSlayerName(),
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

      return new String[]{
              "§r§lStats:",
              "§rMacro: §fSlayer - " + getSlayerName(),
              "§rTime: §f" + (Scheduler.getInstance().isRunning() ? Scheduler.getInstance().getStatusString() : "Macroing"),
              "§rBosses Killed: §f" + killedBosses,
              "§r Open '/skills' to track xp",
              "§rState: §f" + currentState.name(),
              "§r§lTarget:",
              "§rBoss Spawned: §f" + (killingBoss ? "Yes" : "No"),
              "§rDistance: §f" + (target != null ? (String.format("%.2f", Math.floor(Minecraft.getMinecraft().thePlayer.getDistanceToEntity(target))) + "m") : "No target"),
              "§rVisible: §f" + (target != null ? (PlayerUtils.entityIsVisible(target) ? "Yes" : "No") : "No target"),
      };
   }

   public enum State {
      NONE,
      AT_SPAWN,
      WALKING,
      SEARCHING,
      ATTACKING,
      KILLED,
      PAUSE
   }
}
