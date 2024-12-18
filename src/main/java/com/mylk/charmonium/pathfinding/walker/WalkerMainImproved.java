package com.mylk.charmonium.pathfinding.walker;

import com.mylk.charmonium.Charmonium;
import com.mylk.charmonium.config.Config;
import com.mylk.charmonium.handler.RotationHandler;
import com.mylk.charmonium.util.BlockUtils;
import com.mylk.charmonium.util.KeyBindUtils;
import com.mylk.charmonium.util.helper.Rotation;
import com.mylk.charmonium.util.helper.RotationConfiguration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;

public class WalkerMainImproved {
  private final RotationHandler rotation = RotationHandler.getInstance();
  private boolean state = false;
  private Deque<Vec3> curPath = new ArrayDeque<>();
  private Vec3 curVec = null;
  private Vec3 prev = null;
  private double distToShift = 0;
  private boolean isShift;
  private Vec3 endBlock;
  private boolean isShifting = false;
  private final Map<KeyBinding, Boolean> prevKeybinds = new HashMap<>();
  private WalkerState currentState = WalkerState.STOPPED;
  private Rotation currentRotation = new Rotation(0, 0);
  private static final float ROTATION_SPEED = 5f;
  private static final int LOOK_AHEAD_DISTANCE = 3;

  private enum WalkerState {
    WALKING, JUMPING, FALLING, SHIFTING, STOPPED
  }

  public void run(List<Vec3> path, boolean walkState, boolean isShiftClose, double distToShift) {
    MinecraftForge.EVENT_BUS.register(this);
    state = walkState;
    curPath = new ArrayDeque<>(path);
    this.endBlock = path.get(path.size() - 1);
    curVec = BlockUtils.getCenteredVec(curPath.removeFirst());
    prev = null;
    this.distToShift = distToShift;
    isShift = isShiftClose;
    smoothPath();
  }

  @SubscribeEvent
  public void onTick(TickEvent.ClientTickEvent event) {
    if (!state) return;
    currentRotation = new Rotation(Charmonium.mc.thePlayer.rotationYaw, Charmonium.mc.thePlayer.rotationPitch);

    updateState();
    handleRotation();
    //smoothRotation(rotation.getRotation(curVec));
    handleMovement();

    double curDist = BlockUtils.distanceFromToXZ(Charmonium.mc.thePlayer.getPositionVector(), curVec);

    if (curDist < 1 && Charmonium.mc.thePlayer.posY + 0.5 >= curVec.yCoord) {
      nextBlock();
    } else if (currentState == WalkerState.FALLING) {
      updatePathDuringFall();
    }

    updateShiftingState();
  }

  private void updateState() {
    if (!state) {
      currentState = WalkerState.STOPPED;
    } else if (isShifting) {
      currentState = WalkerState.SHIFTING;
    } else if (!Charmonium.mc.thePlayer.onGround) {
      currentState = WalkerState.FALLING;
    } else if (isCloseToJump()) {
      currentState = WalkerState.JUMPING;
    } else {
      currentState = WalkerState.WALKING;
    }
  }

  private void handleRotation() {
    Vec3 lookTarget = getLookAheadTarget();
    if (lookTarget != null) {
      Rotation needed = rotation.getRotation(lookTarget);
      needed.setPitch(calculatePitch(lookTarget));
      if (Math.abs(rotation.getServerSideYaw() - needed.getYaw()) > 1.0F ||
              Math.abs(rotation.getServerSidePitch() - needed.getPitch()) > 1.0F) {
        rotation.easeTo(new RotationConfiguration(needed, Config.getRandomRotationTime(), null));
      }
    }
  }

//  private void smoothRotation(Rotation target) {
//    float yawDifference = target.getYaw() - currentRotation.getYaw();
//    float pitchDifference = target.getPitch() - currentRotation.getPitch();
//
//    while (yawDifference > 180) yawDifference -= 360;
//    while (yawDifference < -180) yawDifference += 360;
//
//    currentRotation.setYaw(currentRotation.getYaw() + yawDifference * ROTATION_SPEED * 0.05f);
//    currentRotation.setPitch(currentRotation.getPitch() + pitchDifference * ROTATION_SPEED * 0.05f);
//
//    Charmonium.mc.thePlayer.rotationYaw = currentRotation.getYaw();
//    Charmonium.mc.thePlayer.rotationPitch = currentRotation.getPitch();
//  }

  private Vec3 getLookAheadTarget() {
    if (curPath.isEmpty()) return curVec;

    Vec3 playerPos = Charmonium.mc.thePlayer.getPositionVector();
    Vec3 target = curVec;
    double distanceSum = 0;

    for (Vec3 point : curPath) {
      distanceSum += point.distanceTo(target);
      if (distanceSum > LOOK_AHEAD_DISTANCE) break;
      target = point;
    }

    return target;
  }

  private float calculatePitch(Vec3 target) {
    Vec3 playerPos = Charmonium.mc.thePlayer.getPositionVector();
    double dx = target.xCoord - playerPos.xCoord;
    double dy = target.yCoord - (playerPos.yCoord + Charmonium.mc.thePlayer.getEyeHeight());
    double dz = target.zCoord - playerPos.zCoord;
    double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
    return (float) -Math.toDegrees(Math.atan2(dy, horizontalDistance));
  }

  private void handleMovement() {
    HashSet<KeyBinding> neededKeyPresses = KeyBindUtils.getNeededKeyPressesHash(
            Charmonium.mc.thePlayer.getPositionVector(),
            this.curVec
    );

    for (KeyBinding k : KeyBindUtils.getListKeybinds()) {
      KeyBindUtils.setKeyBindState(k, neededKeyPresses.contains(k));
    }

    Charmonium.mc.thePlayer.setSprinting(true);
    KeyBindUtils.setKeyBindState(Charmonium.mc.gameSettings.keyBindJump, currentState == WalkerState.JUMPING);
    KeyBindUtils.setKeyBindState(Charmonium.mc.gameSettings.keyBindSneak, currentState == WalkerState.SHIFTING);
  }

  private void nextBlock() {
    if (curPath == null || curPath.isEmpty()) {
      stop();
      return;
    }

    prev = curVec;
    curVec = BlockUtils.getCenteredVec(curPath.removeFirst());
  }

  private boolean isCloseToJump() {
    if (prev == null) return false;

    boolean prevBlockNotSlab = !BlockUtils.getBlockType(BlockUtils.fromVecToBP(prev.addVector(0, -1, 0))).getRegistryName().contains("slab");
    boolean curBlockNotSlab = !BlockUtils.getBlockType(BlockUtils.fromVecToBP(curVec.addVector(0, -1, 0))).getRegistryName().contains("slab");

    return Charmonium.mc.thePlayer.posY + 0.5 < curVec.yCoord &&
            curBlockNotSlab &&
            Charmonium.mc.thePlayer.onGround &&
            BlockUtils.distanceFromToXZ(Charmonium.mc.thePlayer.getPositionVector(), curVec) < 3 &&
            prevBlockNotSlab;
  }

  private void updatePathDuringFall() {
    if (!curPath.isEmpty()) {
      Vec3 newClosest = BlockUtils.getClosest(new ArrayList<>(curPath), Charmonium.mc.thePlayer.getPositionVector());
      if (newClosest != null && !newClosest.equals(curVec)) {
        while (!curPath.isEmpty() && !curPath.peekFirst().equals(newClosest)) {
          curPath.removeFirst();
        }
        curVec = newClosest;
      }
    }
  }

  private void updateShiftingState() {
    if (isShift) {
      isShifting = BlockUtils.distanceFromTo(Charmonium.mc.thePlayer.getPositionVector(), endBlock) < distToShift &&
              Math.abs(Charmonium.mc.thePlayer.getPositionVector().yCoord - curVec.yCoord) < 0.1;
    }
  }

  private void smoothPath() {
    if (curPath.size() < 3) return;
    List<Vec3> smoothed = new ArrayList<>(curPath);
    for (int i = 0; i < smoothed.size() - 2; ) {
      Vec3 current = smoothed.get(i);
      Vec3 next = smoothed.get(i + 2);
      if (hasLineOfSight(current, next)) {
        smoothed.remove(i + 1);
      } else {
        i++;
      }
    }
    curPath = new ArrayDeque<>(smoothed);
  }

  private boolean hasLineOfSight(Vec3 start, Vec3 end) {
    Vec3 dir = end.subtract(start).normalize();
    double distance = start.distanceTo(end);
    for (double d = 0; d < distance; d += 0.5) {
      BlockPos pos = new BlockPos(start.addVector(dir.xCoord * d, dir.yCoord * d, dir.zCoord * d));
      if (BlockUtils.isBlockSolid(pos)) {
        return false;
      }
    }
    return true;
  }

  public void pause() {
    for (KeyBinding keyBind : KeyBindUtils.getListKeybinds()) {
      prevKeybinds.put(keyBind, keyBind.isKeyDown());
    }
    KeyBindUtils.stopMovement();
    this.state = false;
  }

  public void unpause() {
    prevKeybinds.forEach(KeyBindUtils::setKeyBindState);
    this.state = true;
  }

  public void stop() {
    KeyBindUtils.stopMovement();
    this.state = false;
    this.curPath = null;
    MinecraftForge.EVENT_BUS.unregister(this);
  }

  public boolean isDone() {
    return curPath == null || curPath.isEmpty();
  }
}