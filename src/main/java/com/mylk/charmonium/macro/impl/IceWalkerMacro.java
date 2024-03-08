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
import com.mylk.charmonium.util.charHelpers.npcUtils;
import com.mylk.charmonium.util.helper.Rotation;
import com.mylk.charmonium.util.helper.RotationConfiguration;
import com.mylk.charmonium.util.helper.Timer;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityXPOrb;
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


public class IceWalkerMacro extends AbstractMacro {
   public static State currentState = State.NONE;
   private static int currentWaypoint = 0;
   private final RotationHandler rotation = RotationHandler.getInstance();
   private final Timer closestPointCooldown = new Timer();
   private final Timer noKillTimer = new Timer();
   private final Timer noFindTimer = new Timer();
   private final Timer attackDelay = new Timer();
   public static final Timer diedDelay = new Timer();
   public static Entity target;
   public static Entity targetStand;
   private static List<Entity> ignoreEntity;
   public static boolean hasDied = false;
   private static IceWalkerMacro instance;

   public static IceWalkerMacro getInstance() {
      if (instance == null) {
         instance = new IceWalkerMacro();
      }
      return instance;
   }
   public void onEnable() {
      Config.MacroEnum crop = Config.MacroEnum.ICE_WALKER;
      currentState = State.NONE;
      currentWaypoint = 0;
      rotation.reset();
      ignoreEntity = new ArrayList<>();
      target = null;
      targetStand = null;
      hasDied = false;
      super.setSpawnPos(new Vec3(-49, 200, -122));

      LogUtils.sendDebug("Macro: " + crop);
      MacroHandler.getInstance().setCrop(crop);
      PlayerUtils.getTool();

      if (isNearFarSpawn()) {
         currentState = State.AT_SPAWN;
         Charmonium.sendMessage("Warping to location");
         mc.thePlayer.sendChatMessage(GameStateHandler.islandWarp);
         Charmonium.sendMessage("Walking to mobs [far]");
         MacroHandler.walkTo(routes.wayToIceWalkers, currentWaypoint, new BlockPos(0, 148, -69));
         currentWaypoint++;
      } else if (isNearSpawn()) {
         currentState = State.AT_SPAWN;
         MacroHandler.walkTo(routes.wayToIceWalkers, currentWaypoint);
         Charmonium.sendMessage("Walking to mobs [close]");
         currentWaypoint++;
      } else if (isNearWaypoint()) {
         currentState = State.WALKING;
         MacroHandler.walkTo(routes.iceWalkers, getClosestPoint());
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

      switch (currentState) {
         case NONE:
            target = null;
            targetStand = null;
            currentWaypoint = 0;
            currentState = State.AT_SPAWN;
            break;
         case AT_SPAWN:
            if (currentWaypoint >= routes.wayToIceWalkers.size())
               currentWaypoint = 0;

            if (Charmonium.mc.thePlayer.getPositionVector().distanceTo(new Vec3(0, 128, 155)) < 5) {
               currentWaypoint = 0;
               currentState = State.WALKING;
               Charmonium.sendMessage("Arrived at mobs");
               return;
            }

            if (MacroHandler.walker.isDone()) {
               MacroHandler.walkTo(routes.wayToIceWalkers, currentWaypoint);
               currentWaypoint++;
            }

            break;
         case WALKING:
            if (currentWaypoint >= routes.iceWalkers.size())
               currentWaypoint = 0;

            if (MacroHandler.walker.isDone()) {
               ignoreEntity = new ArrayList<>();
               if (!entities.isEmpty()) {
                  currentState = State.SEARCHING;
                  return;
               }
            }
            break;
         case SEARCHING:
            KeyBindUtils.stopMovement();
            Optional<Entity> optional = entities.stream().min(Comparator.comparingDouble(entity -> entity.getDistanceToEntity(mc.thePlayer)));

            if (optional.isPresent()) {
               targetStand = optional.get();
               target = npcUtils.getEntityCuttingOtherEntity(targetStand, null);
               noKillTimer.schedule();
               MacroHandler.walkTo(target);
               currentState = State.ATTACKING;
            } else {
               if (noFindTimer.hasPassed(5000)) {
                  currentState = State.WALKING;
                  noFindTimer.reset();
               }
            }
            break;
         case ATTACKING:
            if (noKillTimer.hasPassed(6000)) {
               Charmonium.sendMessage("Stuck? Finding new");
               currentState = State.SEARCHING;
               ignoreEntity.add(target);
               target = null;
               targetStand = null;
            }

            if (target == null || targetStand == null || target instanceof EntityXPOrb || ((EntityLivingBase) target).getHealth() <= 1 || Minecraft.getMinecraft().thePlayer.getDistanceToEntity(target) > Config.scanRange || target.isDead) {
               currentState = State.KILLED;
               break;
            }

            boolean visible = PlayerUtils.entityIsVisible(target);

            if (!visible) {
               Charmonium.sendMessage("Something is blocking target, moving on...");
               ignoreEntity.add(target);
               target = null;
                targetStand = null;
               currentState = State.SEARCHING;
            } else {
               boolean targeted = npcUtils.entityIsTargeted(target);
               if (target != null && !targeted) {
                  rotation.easeTo(new RotationConfiguration(new Rotation(rotation.getRotation(target.getPositionVector().add(new Vec3(0, target.height / 2, 0))).getYaw(), 20.f), Config.getRandomRotationTime(), null));
               }
               if (Config.SAttackType == Config.SAttackEnum.LEFT_CLICK.ordinal()) {
                  assert target != null;

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
                     if (!MacroHandler.walker.isDone()) return;

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
               } /*else {

               }*/
            }

            break;
         case KILLED:
            ignoreEntity.add(target);
            target = null;
            entities.clear();
            targetStand = null;
            currentState = State.SEARCHING;

            break;
         case PAUSE:
            break;
      }
   }

   @SubscribeEvent
   public void onMillisecond(MillisecondEvent event) {
      if (!MacroHandler.getInstance().isMacroToggled() || MacroHandler.getInstance().getCrop() != Config.MacroEnum.ICE_WALKER) {
         return;
      }

      if (hasDied) {
         if (!diedDelay.hasPassed(2000)) return;

         if (Charmonium.mc.thePlayer.getPositionVector().distanceTo(new Vec3(0, 149, -69)) > 4) {
            Charmonium.sendMessage("Recovering from death...");
            currentState = State.PAUSE;
            MacroHandler.walker.stop();
            KeyBindUtils.stopMovement();
            target = null;
            targetStand = null;
            mc.thePlayer.sendChatMessage(GameStateHandler.islandWarp);
            diedDelay.schedule();
            return;
         }

         currentWaypoint = 0;
         MacroHandler.walkTo(routes.wayToIceWalkers, currentWaypoint);
         currentWaypoint++;
         diedDelay.reset();
         hasDied = false;
         currentState = State.NONE;
      }
   }

   @SubscribeEvent
   public void onRender(RenderWorldLastEvent event) {
      if (mc.thePlayer == null || mc.theWorld == null) return;

      if (!GameStateHandler.getInstance().atProperIsland() || !MacroHandler.getInstance().isMacroToggled() || MacroHandler.getInstance().getCrop() != Config.MacroEnum.ICE_WALKER) return;

      if (currentState == State.AT_SPAWN) {
         for (AOTVWaypointsStructs.Waypoint value : routes.wayToIceWalkers) {
            BlockPos waypoint = new BlockPos(value.x, value.y, value.z);
            RenderUtils.drawBlockBox(waypoint, Color.PINK);
         }
         return;
      }

      if (target != null) {
         RenderUtils.renderBoundingBox(target, event.partialTicks, new Color(89, 255, 211, 157).getRGB());
      }
   }

   @SubscribeEvent
   public void onChatReceive(ClientChatReceivedEvent event) {
      String message = StringUtils.stripControlCodes(event.message.getUnformattedText());
      if (message.contains("You were killed by") && MacroHandler.getInstance().getCrop() == Config.MacroEnum.ICE_WALKER && MacroHandler.getInstance().isMacroToggled()) {
         Charmonium.sendMessage("Warping to location");
         MacroHandler.walker.stop();
         hasDied = true;
         diedDelay.schedule();
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
                      entity instanceof EntityArmorStand &&
                      (entity.getCustomNameTag().contains("Ice Walker") || entity.getName().contains("Ice Walker")) &&
                      !entity.isDead &&
                      ((EntityLivingBase) entity).getHealth() > 0 &&
                      mc.thePlayer.getPositionEyes(1).distanceTo(entity.getPositionVector()) <= Config.scanRange &&
                      Math.abs(entity.getPositionVector().yCoord - mc.thePlayer.getPositionVector().yCoord) < 3 &&
                      !ignoreEntity.contains(entity))
              .filter(PlayerUtils::entityIsVisible)
              .collect(Collectors.toList());
   }

   private boolean isNearSpawn() {
      return Charmonium.mc.thePlayer.getPositionVector().distanceTo(new Vec3(0, 149, -69)) < 3;
   }

   private boolean isNearFarSpawn() {
      return Charmonium.mc.thePlayer.getPositionVector().distanceTo(new Vec3(-49, 200, -122)) < 3;
   }

   private boolean isNearWaypoint() {
      for (AOTVWaypointsStructs.Waypoint waypoint : routes.iceWalkers) {
         if (Charmonium.mc.thePlayer.getPositionVector().distanceTo(new Vec3(waypoint.x, waypoint.y, waypoint.z)) < 10) {
            return true;
         }
      }
      return false;
   }

   private int getClosestPoint() {
      double distance = 9999;
      int closest = 0;

      if (currentWaypoint >= routes.iceWalkers.size()) {
         currentWaypoint = 0;
      }

      if (!closestPointCooldown.hasPassed(250)) return currentWaypoint;

      int nextWaypoint = findNextWaypoint();

      // Iterate forward
      for (int i = nextWaypoint; i < routes.iceWalkers.size(); i++) {
         double currentDistance = mc.thePlayer.getPositionVector().distanceTo(
                 new Vec3(routes.iceWalkers.get(i).x, routes.iceWalkers.get(i).y, routes.iceWalkers.get(i).z)
         );

         if (currentDistance < distance) {
            distance = currentDistance;
            closest = i;
         }
      }

      for (int i = nextWaypoint - 1; i >= 0; i--) {
         double currentDistance = mc.thePlayer.getPositionVector().distanceTo(
                 new Vec3(routes.iceWalkers.get(i).x, routes.iceWalkers.get(i).y, routes.iceWalkers.get(i).z)
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

      if (nextWaypoint >= routes.iceWalkers.size()) {
         nextWaypoint = 0;
      }

      return nextWaypoint;
   }
   
   public static String[] drawInfo() {
      if (SkillTracker.skillsInitialized()) {
         double xpToShow = SkillTracker.getText("Combat");
         int xpPerHour = (int) Math.round(xpToShow / ((SkillTracker.skillStopwatch.getTime() + 1) / 3600000d));
         int nxtLvl = Config.Skill_Combat + 1;

         return new String[]{
                 "§r§lStats:",
                 "§rMacro: §fIce Walkers",
                 "§rTime: §f" + (Scheduler.getInstance().isRunning() ? Scheduler.getInstance().getStatusString() : "Macroing"),
                 "§rXP Earned: §f" + NumberFormat.getNumberInstance(Locale.US).format(xpToShow) + " [" + NumberFormat.getNumberInstance(Locale.US).format(xpPerHour) + "/hr]",
                 "§rTime til' Lvl. " + nxtLvl + ": §f" + SkillTracker.getTimeBetween(0, SkillTracker.xpLeft / (xpPerHour / 3600D)),
                 "§rState: §f" + currentState.name(),
                 "§r§lTarget:",
                 "§rName: §f" + targetStand.getCustomNameTag(),
                 "§rDistance: §f" + (target != null ? (String.format("%.2f", Math.floor(Minecraft.getMinecraft().thePlayer.getDistanceToEntity(target))) + "m") : "No target"),
                 "§rVisible: §f" + (target != null ? (PlayerUtils.entityIsVisible(target) ? "Yes" : "No") : "No target"),
         };
      }

      return new String[]{
              "§r§lStats:",
              "§rMacro: §fIce Walkers",
              "§rTime: §f" + (Scheduler.getInstance().isRunning() ? Scheduler.getInstance().getStatusString() : "Macroing"),
              "§r Open '/skills' to track xp",
              "§rState: §f" + currentState.name(),
              "§r§lTarget:",
              "§rName: §f" + targetStand.getCustomNameTag(),
              "§rDistance: §f" + (target != null ? (String.format("%.2f", Math.floor(Minecraft.getMinecraft().thePlayer.getDistanceToEntity(target))) + "m") : "No target"),
              "§rHealth: §f" + (target != null ? ((EntityLivingBase) target).getHealth() : "No target"),
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
