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

public class WalkerMain {
  private final RotationHandler rotation = RotationHandler.getInstance();
  boolean state = false;
  List<Vec3> curPath = new ArrayList<>();
  Vec3 curVec = null;
  Vec3 prev = null;
  double distToShift = 0;
  boolean isShift;
  Vec3 endBlock;
  boolean isShifting = false;

  Map<KeyBinding, Boolean> prevKeybinds = new HashMap<>();
  List<KeyBinding> prevPressedKeybinds = new ArrayList<>();

  public void run(List<Vec3> path, boolean walkState) {
    MinecraftForge.EVENT_BUS.register(this);
    state = walkState;
    curPath = path;
    curVec = BlockUtils.getCenteredVec(path.get(0));
    path.remove(0);
    prev = null;
  }

  public void run(List<Vec3> path, boolean walkState, boolean isShiftClose, double distToToShift) {
    MinecraftForge.EVENT_BUS.register(this);
    state = walkState;
    curPath = path;

    this.endBlock = path.get(path.size() - 1);

    curVec = BlockUtils.getCenteredVec(path.get(0));
    path.remove(0);
    prev = null;

    this.distToShift = distToToShift;
    isShift = isShiftClose;
  }

  @SubscribeEvent
  public void onTick(TickEvent.ClientTickEvent event) {
    if (!state) return;

    double curDist = BlockUtils.distanceFromToXZ(Charmonium.mc.thePlayer.getPositionVector(), curVec);

    if (curDist < 1 && Charmonium.mc.thePlayer.posY + 0.5 >= curVec.yCoord) {
      nextBlock();
      return;
    }

    if (!Charmonium.mc.thePlayer.onGround) {
      if (!this.curPath.isEmpty()) {
        Vec3 newClosest = BlockUtils.getClosest(this.curPath, Charmonium.mc.thePlayer.getPositionVector());

        if (newClosest == null || !newClosest.equals(this.curVec)) {
          removeUntil(newClosest);
          this.curVec = newClosest;
        }
      }
    }

    //pathingRotations.Rotation needed = pathingRotations.getRotation(curVec);
    //needed.pitch = 0.0F;

    if (Charmonium.mc.thePlayer.onGround) {
      rotation.easeTo(new RotationConfiguration(new Rotation(rotation.getRotation(curVec).getYaw(), 0), Config.getRandomRotationTime() * 2, null));
      //pathingRotations.smoothLook(needed, 300);
    }

    HashSet<KeyBinding> neededKeyPresses = KeyBindUtils.getNeededKeyPressesHash(
      Charmonium.mc.thePlayer.getPositionVector(),
      this.curVec
    );

    for (KeyBinding k : KeyBindUtils.getListKeybinds()) {
      KeyBinding.setKeyBindState(k.getKeyCode(), neededKeyPresses.contains(k));
    }

    Charmonium.mc.thePlayer.setSprinting(true);

    KeyBindUtils.setKeyBindState(Charmonium.mc.gameSettings.keyBindJump, isCloseToJump());

    if (this.isShift) {
      isShifting =
        BlockUtils.distanceFromTo(Charmonium.mc.thePlayer.getPositionVector(), endBlock) < this.distToShift &&
        Charmonium.mc.thePlayer.getPositionVector().yCoord == this.curVec.yCoord;
    }

    KeyBindUtils.setKeyBindState(Charmonium.mc.gameSettings.keyBindSneak, isShifting);

    if (BlockUtils.distanceFromTo(Minecraft.getMinecraft().thePlayer.getPositionVector(), endBlock) > 1)
      rotation.easeTo(new RotationConfiguration(new Rotation(rotation.getRotation(new BlockPos(this.endBlock.xCoord + 0.5, this.endBlock.yCoord +1, this.endBlock.zCoord + 0.5)).getYaw(), 0), Config.getRandomRotationTime() * 2, null));
  }

  void nextBlock() {
    if (curPath == null || curPath.isEmpty()) {
      stop();
      return;
    }

    prev = curVec;
    curVec = BlockUtils.getCenteredVec(curPath.remove(0));
  }

  boolean isCloseToJump() {
    if (
      prev != null &&
      !BlockUtils.getBlockType(BlockUtils.fromVecToBP(prev.addVector(0, -1, 0))).getRegistryName().contains("slab")
    ) {
      return (
        Charmonium.mc.thePlayer.posY + 0.5 < curVec.yCoord &&
        !BlockUtils
          .getBlockType(BlockUtils.fromVecToBP(curVec.addVector(0, -1, 0)))
          .getRegistryName()
          .contains("slab") &&
        Charmonium.mc.thePlayer.onGround &&
        BlockUtils.distanceFromToXZ(Charmonium.mc.thePlayer.getPositionVector(), curVec) < 3
      );
    }

    return (
      Charmonium.mc.thePlayer.posY + 0.5 < curVec.yCoord &&
      !BlockUtils.getBlockType(BlockUtils.fromVecToBP(curVec.addVector(0, -1, 0))).getRegistryName().contains("slab") &&
      Charmonium.mc.thePlayer.onGround &&
      BlockUtils.distanceFromToXZ(Charmonium.mc.thePlayer.getPositionVector(), curVec) < 3
    );
  }

  void removeUntil(Vec3 vec) {
    while (!this.curPath.isEmpty()) {
      if (this.curPath.get(0).equals(vec)) return;
      this.curPath.remove(0);
    }
  }

  public void pause() {
    prevKeybinds.put(KeyBindUtils.keybindA, KeyBindUtils.keybindA.isKeyDown());
    prevKeybinds.put(KeyBindUtils.keybindD, KeyBindUtils.keybindD.isKeyDown());
    prevKeybinds.put(KeyBindUtils.keybindS, KeyBindUtils.keybindS.isKeyDown());
    prevKeybinds.put(KeyBindUtils.keybindW, KeyBindUtils.keybindW.isKeyDown());

    prevKeybinds.put(KeyBindUtils.keyBindJump, KeyBindUtils.keyBindJump.isKeyDown());
    prevKeybinds.put(KeyBindUtils.keyBindShift, KeyBindUtils.keyBindShift.isKeyDown());

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
    return curPath == null;
  }
}
