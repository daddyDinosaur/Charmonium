package com.mylk.charmonium.macro.impl;

import com.mylk.charmonium.Charmonium;
import com.mylk.charmonium.config.Config;
import com.mylk.charmonium.config.aotv.AOTVWaypointsStructs;
import com.mylk.charmonium.feature.impl.Scheduler;
import com.mylk.charmonium.feature.impl.SkillTracker;
import com.mylk.charmonium.handler.GameStateHandler;
import com.mylk.charmonium.handler.MacroHandler;
import com.mylk.charmonium.handler.RotationHandler;
import com.mylk.charmonium.macro.AbstractMacro;
import com.mylk.charmonium.util.*;
import com.mylk.charmonium.util.helper.RotationConfiguration;
import com.mylk.charmonium.util.helper.Timer;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import com.mylk.charmonium.macro.impl.misc.routes;

import java.awt.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class ForagingMacro extends AbstractMacro {
   private static State currentState = State.NONE;
   private static int currentWaypoint = 0;
   private final Timer closestPointCooldown = new Timer();
   private boolean atSpawn = false;
   private static List<BlockPos> wood;
   private static BlockPos closest;
   private int tickStuck = 0;
   HashSet<BlockPos> broken = new HashSet<>();
   private final RotationHandler rotation = RotationHandler.getInstance();
   private static ForagingMacro instance;

   public static ForagingMacro getInstance() {
      if (instance == null) {
         instance = new ForagingMacro();
      }
      return instance;
   }
   public void onEnable() {
      Config.MacroEnum crop = Config.MacroEnum.FORAGING;
      currentWaypoint = 0;
      closest = null;
      closestPointCooldown.reset();
      currentState = State.NONE;
      super.setSpawnPos(new Vec3(-3, 70, -70));

      LogUtils.sendDebug("Macro: " + crop);
      MacroHandler.getInstance().setCrop(crop);
      PlayerUtils.getTool();

      if (isNearSpawn()) {
         currentState = State.AT_SPAWN;
         atSpawn = true;
         MacroHandler.walkTo(routes.wayToForaging, currentWaypoint);
         Charmonium.sendMessage("Walking to foraging");
         currentWaypoint++;
      } else if (isNearWaypoint()) {
         currentState = State.WALKING;
         MacroHandler.walkTo(routes.hubForaging, getClosestPoint());
      } else {
         mc.thePlayer.sendChatMessage("/hub");
         currentState = State.AT_SPAWN;
      }

      super.onEnable();
   }

   public void invokeState() {
      wood = getWoodAround();

      switch (currentState) {
         case NONE:
            currentState = State.AT_SPAWN;
            break;
         case AT_SPAWN:
            tickStuck = 0;
            if (Charmonium.mc.thePlayer.getPositionVector().distanceTo(new Vec3(-111, 74, -32)) < 5) {
               atSpawn = false;
               currentWaypoint = 0;
               currentState = State.WALKING;
               Charmonium.sendMessage("Arrived at foraging");
            }

            if (MacroHandler.walker.isDone() && atSpawn) {
               MacroHandler.walkTo(routes.wayToForaging, currentWaypoint);
               currentWaypoint++;
            }

            break;
         case WALKING:
            if (currentWaypoint >= routes.hubForaging.size()) {
               currentWaypoint = 0;
            }

            if (MacroHandler.walker.isDone()) {
               if (!wood.isEmpty()) {
                  currentState = State.AT_WOOD;
                  return;
               }

               MacroHandler.walkTo(routes.hubForaging, currentWaypoint);
               currentWaypoint++;
            }

            break;
         case AT_WOOD:
            closest = BlockUtils.getClosest(
                    wood,
                    broken,
                    new BlockPos(mc.thePlayer.getPositionVector())
            );

            if (closest != null && Charmonium.mc.theWorld.getBlockState(closest).getBlock() != Blocks.air && !wood.isEmpty()) {
               currentState = State.MINING;
            } else {
               resetMacroState();
            }

            break;
         case MINING:
            broken.add(closest);

            if (wood.size() == 1) {
               rotation.easeTo(new RotationConfiguration(rotation.getRotation(closest), Config.getRandomRotationTime() * 2, null));
               KeyBindUtils.holdThese(mc.gameSettings.keyBindAttack);
            }

            if (BlockUtils.distanceFromTo(mc.thePlayer.getPositionVector(), BlockUtils.fromBPToVec(closest)) < mc.playerController.getBlockReachDistance() * 0.8) {
               rotation.easeTo(new RotationConfiguration(rotation.getRotation(closest), Config.getRandomRotationTime() * 2, null));
               KeyBindUtils.holdThese(mc.gameSettings.keyBindAttack);
            }

            if (Charmonium.mc.theWorld.getBlockState(closest).getBlock() == Blocks.air || closest == null || wood.isEmpty()) {
               resetMacroState();
            }

            break;
      }
   }

//   @SubscribeEvent
//   public void onMillisecond(MillisecondEvent event) {
//      if (!MacroHandler.getInstance().isMacroToggled() || MacroHandler.getInstance().getCrop() != Config.MacroEnum.FORAGING) {
//         return;
//      }
//
//      MovingObjectPosition ray = mc.thePlayer.rayTrace(5, 1);
//      if (ray != null && !ray.getBlockPos().equals(closest) && currentState == State.MINING) {
//         tickStuck++;
//         Charmonium.sendMessage("Stuck: " + tickStuck);
//         if (tickStuck > 3000) {
//            MacroHandler.walkTo(routes.hubForaging, currentWaypoint);
//            currentWaypoint++;
//            resetMacroState();
//         }
//      } else {
//         tickStuck = 0;
//      }
//   }

   @SubscribeEvent
   public void onRender(RenderWorldLastEvent event) {
      if (mc.thePlayer == null || mc.theWorld == null) return;

      if (!GameStateHandler.getInstance().atProperIsland() || !MacroHandler.getInstance().isMacroToggled() || MacroHandler.getInstance().getCrop() != Config.MacroEnum.FORAGING) return;

      if (currentState == State.AT_SPAWN) {
         for (AOTVWaypointsStructs.Waypoint value : routes.wayToForaging) {
            BlockPos waypoint = new BlockPos(value.x, value.y, value.z);
            RenderUtils.drawBlockBox(waypoint, Color.PINK);
         }
         return;
      }

      for (AOTVWaypointsStructs.Waypoint value : routes.hubForaging) {
         BlockPos waypoint = new BlockPos(value.x, value.y, value.z);
         Color chroma = Color.getHSBColor((float) ((System.currentTimeMillis() / 10) % 2000) / 2000, 1, 1);
         Color chromaLowerAlpha = new Color(chroma.getRed(), chroma.getGreen(), chroma.getBlue(), 120);

         RenderUtils.drawBlockBox(waypoint, chromaLowerAlpha);
      }

      if (wood == null || wood.isEmpty()) return;
      for (BlockPos value : wood) {
         if (value == null) return;
         RenderUtils.drawBlockBox(value, new Color(127, 255, 212, 120));
      }
   }

   private void resetMacroState() {
      tickStuck = 0;
      broken.clear();
      closest = null;
      currentState = State.WALKING;
      wood.clear();
   }

   private boolean isNearSpawn() {
      Vec3 spawn = new Vec3(-3, 70, -70);
      return Charmonium.mc.thePlayer.getPositionVector().distanceTo(spawn) < 3;
   }

   private int getClosestPoint() {
      double distance = 9999;
      int closest = 0;

      if (!closestPointCooldown.hasPassed(250)) return currentWaypoint;

      if (currentWaypoint >= routes.hubForaging.size() && !atSpawn) {
         currentWaypoint = 0;
      }

      for (int i = currentWaypoint + 1; i < routes.hubForaging.size(); i++) {
         double currentDistance = mc.thePlayer.getPositionVector().distanceTo(
                 new Vec3(routes.hubForaging.get(i).x, routes.hubForaging.get(i).y, routes.hubForaging.get(i).z)
         );

         if (currentDistance < distance) {
            distance = currentDistance;
            closest = i;
         }
      }

      currentWaypoint = closest;
      return currentWaypoint;
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
              "§rMacro: §fForaging",
              "",
              "§rWood Nearby: §f" + (!wood.isEmpty() ? wood.size() : "None"),
              "§rDistance: §f" + (closest != null ? Math.floor(BlockUtils.distanceFromTo(mc.thePlayer.getPositionVector(), BlockUtils.fromBPToVec(closest))) + "m" : "0m"),
              "§rTime: §f" + (Scheduler.getInstance().isRunning() ? Scheduler.getInstance().getStatusString() : "Macroing"),
              "§rMAX SKILL",
              "§rState: §f" + currentState.name(),
      };
   }

   private static String[] drawSkillInfo() {
      double xpToShow = SkillTracker.getText("Foraging");
      int xpPerHour = (int) Math.round(xpToShow / ((SkillTracker.skillStopwatch.getTime() + 1) / 3600000d));
      int nxtLvl = Config.Skill_Foraging + 1;

      return new String[]{
              "§r§lStats:",
              "§rMacro: §fForaging",
              "",
              "§rWood Nearby: §f" + (!wood.isEmpty() ? wood.size() : "None"),
              "§rDistance: §f" + (closest != null ? Math.floor(BlockUtils.distanceFromTo(mc.thePlayer.getPositionVector(), BlockUtils.fromBPToVec(closest))) + "m" : "0m"),
              "§rTime: §f" + (Scheduler.getInstance().isRunning() ? Scheduler.getInstance().getStatusString() : "Macroing"),
              "§rXP Earned: §f" + NumberFormat.getNumberInstance(Locale.US).format(xpToShow) + " [" + NumberFormat.getNumberInstance(Locale.US).format(xpPerHour) + "/hr]",
              "§rTime til' Lvl. " + nxtLvl + ": §f" + SkillTracker.getTimeBetween(0, SkillTracker.xpLeft / (xpPerHour / 3600D)),
              "§rState: §f" + currentState.name(),
      };
   }

   private static String[] drawDefaultInfo() {
      return new String[]{
              "§r§lStats:",
              "§rMacro: §fForaging",
              "",
              "§rWood Nearby: §f" + (!wood.isEmpty() ? wood.size() : "None"),
              "§rDistance: §f" + (closest != null ? Math.floor(BlockUtils.distanceFromTo(mc.thePlayer.getPositionVector(), BlockUtils.fromBPToVec(closest))) + "m" : "0m"),
              "§rTime: §f" + (Scheduler.getInstance().isRunning() ? Scheduler.getInstance().getStatusString() : "Macroing"),
              "§rOpen '/skills' to track xp",
              "§rState: §f" + currentState.name(),
      };
   }

   private boolean isNearWaypoint() {
      for (AOTVWaypointsStructs.Waypoint waypoint : routes.hubForaging) {
         if (Charmonium.mc.thePlayer.getPositionVector().distanceTo(new Vec3(waypoint.x, waypoint.y, waypoint.z)) < 5) {
            return true;
         }
      }
      return false;
   }

   List<BlockPos> getWoodAround() {
      double playerReach = mc.playerController.getBlockReachDistance() * 0.8;
      List<BlockPos> returnBlocks = new ArrayList<>();
      Iterable<BlockPos> blocks = BlockPos.getAllInBox(
              BlockUtils.fromVecToBP(Charmonium.mc.thePlayer.getPositionVector()).add(-playerReach, -playerReach, -playerReach),
              BlockUtils.fromVecToBP(Charmonium.mc.thePlayer.getPositionVector()).add(playerReach, playerReach, playerReach)
      );
      blocks.forEach(i -> {
         if (BlockUtils.getBlock(i) == Blocks.log) {
            if (BlockUtils.distanceFromTo(mc.thePlayer.getPositionVector(), BlockUtils.fromBPToVec(i)) < mc.playerController.getBlockReachDistance() * 0.8)
               returnBlocks.add(i);
         }
      });

      return returnBlocks;
   }

   public enum State {
      NONE,
      AT_SPAWN,
      WALKING,
      AT_WOOD,
      MINING,
   }

}
