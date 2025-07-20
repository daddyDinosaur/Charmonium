package com.charmonium.utils.pathfinding;

import com.charmonium.Charmonium;
import com.charmonium.CharmoniumClient;
import com.charmonium.event.events.Render3DEvent;
import com.charmonium.event.events.TickEvent;
import com.charmonium.event.listeners.Render3DListener;
import com.charmonium.event.listeners.TickListener;
import com.charmonium.managers.RotationManager;
import com.charmonium.utils.blocks.BlockUtils;
import com.charmonium.utils.misc.KeyBindUtils;
import com.charmonium.utils.rotation.Rotation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.stream.Collectors;

public class Walker implements TickListener, Render3DListener {
    private final RotationManager rotation = Charmonium.getInstance().rotationManager;
    private boolean state = false;
    private List<Vec3d> curPath = new ArrayList<>();
    public static List<BlockPos> currentPath = new ArrayList<>();
    private Vec3d curVec = null;
    private Vec3d prev = null;
    private double distToShift = 0;
    private boolean isShift;
    private Vec3d endBlock;
    private boolean isShifting = false;
    private Map<KeyBinding, Boolean> prevKeybinds = new HashMap<>();
    private long lastPathRecalculationTime = 0;
    private static final long RECALCULATION_COOLDOWN = 5000;
    private List<Vec3d> recentPositions = new LinkedList<>();
    private static final int MAX_RECENT_POSITIONS = 30;
    private long lastRotationTime = 0;
    private static final long ROTATION_COOLDOWN = 400;

    public static float rotationTime = 500f;
    public static float rotationTimeRandomness = 300;

    public static long getRandomRotationTime() {
        return (long) (rotationTime + (float) Math.random() * rotationTimeRandomness);
    }

    public void run(List<Vec3d> path, boolean walkState) {
        Charmonium.getInstance().eventManager.AddListener(TickListener.class, this);
        Charmonium.getInstance().eventManager.AddListener(Render3DListener.class, this);
        state = walkState;
        curPath = new ArrayList<>(path);
        curVec = BlockUtils.getCenteredVec(curPath.getFirst());
        curPath.remove(0);
        prev = null;
        lastRotationTime = 0;
    }

    public void run(List<Vec3d> path, boolean walkState, boolean isShiftClose, double distToToShift) {
        Charmonium.getInstance().eventManager.AddListener(TickListener.class, this);
        Charmonium.getInstance().eventManager.AddListener(Render3DListener.class, this);
        state = walkState;
        curPath = new ArrayList<>(path);
        endBlock = curPath.getLast();
        curVec = BlockUtils.getCenteredVec(curPath.getFirst());
        curPath.remove(0);
        prev = null;
        distToShift = distToToShift;
        isShift = isShiftClose;
        lastRotationTime = 0;
    }

    @Override
    public void onTick(TickEvent.Pre event) {}

    @Override
    public void onTick(TickEvent.Post event) {
        if (!state) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        assert mc.player != null;
        Vec3d currentPos = mc.player.getPos();

        if (recentPositions.size() >= MAX_RECENT_POSITIONS) {
            recentPositions.removeFirst();
        }
        recentPositions.add(currentPos);

        if (isOscillatingInBox()) {
            handleStuckSituation();
            return;
        }

        if (curVec == null || isAtLastBlock(currentPos)) {
            stop();
            return;
        }

        double curDist = BlockUtils.distanceFromToXZ(currentPos, curVec);

        if (curDist < 1 && mc.player.getY() + 0.5 >= curVec.y) {
            nextBlock();
            return;
        }

        if (!mc.player.isOnGround()) {
            if (!curPath.isEmpty()) {
                Vec3d newClosest = BlockUtils.getClosest(curPath, mc.player.getPos());
                if (newClosest == null || !newClosest.equals(curVec)) {
                    removeUntil(newClosest);
                    curVec = newClosest;
                }
            }
        }

        handleRotation(currentPos);
        handleMovement(currentPos);
        handleSpecialActions();
    }

    private void handleRotation(Vec3d currentPos) {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (!mc.player.isOnGround() || rotation.isRotating()) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastRotationTime < ROTATION_COOLDOWN) return;

        Vec3d rotationTarget = getOptimalRotationTarget(currentPos);
        if (rotationTarget == null) return;

        Rotation currentPlayerRotation = new Rotation(mc.player.getYaw(), mc.player.getPitch());
        Rotation targetRotation = RotationManager.getRotation(rotationTarget);

        float yawDiff = Math.abs(rotation.getNeededChange(currentPlayerRotation, targetRotation).getYaw());

        if (yawDiff > 8) {
            targetRotation.setPitch(Math.max(targetRotation.getPitch(), -15));
            rotation.easeTo(targetRotation, getRandomRotationTime());
            lastRotationTime = currentTime;
        }
    }

    private Vec3d getOptimalRotationTarget(Vec3d currentPos) {
        if (endBlock == null) return curVec;

        double distanceToEnd = BlockUtils.distanceFromTo(currentPos, endBlock);

        if (distanceToEnd <= 3) {
            return endBlock.add(0, 0.3, 0);
        }

        Vec3d lookAheadTarget = getLookAheadTarget();
        if (lookAheadTarget != null) {
            return lookAheadTarget.add(0, 0.2, 0);
        }

        return curVec.add(0, 0.1, 0);
    }

    private void handleMovement(Vec3d currentPos) {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (curPath.size() > 2 &&
                BlockUtils.distanceFromToXZ(currentPos, curPath.get(1)) < 1.5 &&
                isPathClear(currentPos, curPath.get(2))) {
            removeUntil(curPath.get(2));
            curVec = curPath.get(0);
        }

        Set<KeyBinding> neededKeyPresses = KeyBindUtils.getMovementDirections(currentPos, curVec);
        KeyBindUtils.getPathfindingControls().forEach(k ->
                KeyBindUtils.setKeyBindState(k, neededKeyPresses.contains(k)));

        mc.player.setSprinting(true);
        KeyBindUtils.setKeyBindState(mc.options.jumpKey, isCloseToJump());
    }

    private void handleSpecialActions() {
        if (isShift) {
            MinecraftClient mc = MinecraftClient.getInstance();
            isShifting = BlockUtils.distanceFromTo(mc.player.getPos(), endBlock) < distToShift
                    && mc.player.getPos().y == curVec.y;
        }
        KeyBindUtils.setKeyBindState(MinecraftClient.getInstance().options.sneakKey, isShifting);
    }

    private void nextBlock() {
        if (curPath == null || curPath.isEmpty()) {
            curVec = null;
            stop();
            return;
        }
        prev = curVec;
        curVec = BlockUtils.getCenteredVec(curPath.remove(0));
    }

    private boolean isCloseToJump() {
        MinecraftClient mc = MinecraftClient.getInstance();
        assert mc.player != null;

        if (prev != null &&
                !BlockUtils.getBlockType(BlockUtils.fromVecToBP(curVec.add(0, -1, 0))).getDefaultState().isIn(BlockTags.SLABS)) {
            return (mc.player.getY() + 0.5 < curVec.y &&
                    !BlockUtils.getBlockType(BlockUtils.fromVecToBP(curVec.add(0, -1, 0))).getDefaultState().isIn(BlockTags.SLABS) &&
                    mc.player.isOnGround() &&
                    BlockUtils.distanceFromToXZ(mc.player.getPos(), curVec) < 3);
        }

        return (mc.player.getY() + 0.5 < curVec.y &&
                !BlockUtils.getBlockType(BlockUtils.fromVecToBP(curVec.add(0, -1, 0))).getDefaultState().isIn(BlockTags.SLABS) &&
                mc.player.isOnGround() &&
                BlockUtils.distanceFromToXZ(mc.player.getPos(), curVec) < 3);
    }

    private void removeUntil(Vec3d vec) {
        while (!curPath.isEmpty()) {
            if (curPath.getFirst().equals(vec)) return;
            curPath.removeFirst();
        }
    }

    private boolean isOscillatingInBox() {
        if (recentPositions.size() < MAX_RECENT_POSITIONS) return false;

        double totalMovement = 0;
        Vec3d previous = recentPositions.get(0);
        for (Vec3d current : recentPositions) {
            totalMovement += previous.distanceTo(current);
            previous = current;
        }
        return totalMovement < 2.0;
    }

    private boolean isAtLastBlock(Vec3d currentPos) {
        if (endBlock == null) return false;
        double dist = BlockUtils.distanceFromToXZ(currentPos, endBlock);
        double verticalDiff = Math.abs(currentPos.y - endBlock.y);
        return dist < 0.7 && verticalDiff < 0.7;
    }

    private boolean isPathClear(Vec3d start, Vec3d end) {
        Vec3d direction = end.subtract(start).normalize();
        double distance = start.distanceTo(end);

        for (double d = 0; d < distance; d += 0.5) {
            Vec3d checkPos = start.add(direction.multiply(d));
            BlockPos blockPos = new BlockPos((int)checkPos.x, (int)checkPos.y, (int)checkPos.z);
            if (BlockUtils.isBlockSolid(blockPos) &&
                    !BlockUtils.canWalkThrough(blockPos)) {
                return false;
            }
        }
        return true;
    }

    private void handleStuckSituation() {
        KeyBindUtils.setKeyBindState(MinecraftClient.getInstance().options.jumpKey, true);

        MinecraftClient.getInstance().execute(() -> {
            if (System.currentTimeMillis() - lastPathRecalculationTime < RECALCULATION_COOLDOWN) return;

            assert MinecraftClient.getInstance().player != null;
            BlockPos newStart = new BlockPos(
                    (int) Math.floor(MinecraftClient.getInstance().player.getX()),
                    (int) Math.floor(MinecraftClient.getInstance().player.getY()),
                    (int) Math.floor(MinecraftClient.getInstance().player.getZ())
            );
            BlockPos endPos = BlockUtils.fromVecToBP(endBlock);

            PathFinderConfig config = new PathFinderConfig(10000, newStart, endPos);
            AStarPathFinder pathFinder = new AStarPathFinder(config);
            List<BlockNodeClass> newNodes = pathFinder.findPath();

            if (newNodes == null || newNodes.isEmpty()) {
                this.curPath = Collections.singletonList(endBlock);
                this.curVec = endBlock;
                return;
            }

            List<Vec3d> newPath = pathFinder.fromClassToVec(newNodes);

            stop();
            if (newPath != null) {
                CharmoniumClient.sendMessage("Unstuck Path found! Length: " + newNodes.size() + " blocks");
                currentPath = newNodes.stream()
                        .map(BlockNodeClass::getBlockPos)
                        .collect(Collectors.toList());
                Walker walker = new Walker();
                walker.run(newPath, true, false, 2);

            } else {
                CharmoniumClient.sendMessage("Unable to get unstuck");
                currentPath.clear();
            }

            this.lastPathRecalculationTime = System.currentTimeMillis();
            this.recentPositions.clear();
        });
    }

    private Vec3d getLookAheadTarget() {
        int lookAhead = Math.min(3, curPath.size());
        if (lookAhead > 0) {
            Vec3d target = curPath.get(lookAhead - 1);
            double progress = BlockUtils.distanceFromTo(MinecraftClient.getInstance().player.getPos(), curVec) / 2.0;
            return target.add(0, 0.2 * progress, 0);
        }
        return endBlock;
    }

    public void pause() {
        KeyBindUtils.getPathfindingControls().forEach(key ->
                prevKeybinds.put(key, key.isPressed()));

        KeyBindUtils.stopMovement();
        state = false;
    }

    public void unpause() {
        prevKeybinds.forEach(KeyBindUtils::setKeyBindState);
        state = true;
    }

    public void stop() {
        KeyBindUtils.stopMovement();
        state = false;
        curPath = null;
        curVec = null;
        recentPositions.clear();
        lastRotationTime = 0;
        Charmonium.getInstance().eventManager.RemoveListener(TickListener.class, this);
        Charmonium.getInstance().eventManager.RemoveListener(Render3DListener.class, this);
    }

    public boolean isDone() {
        return curPath == null;
    }

    @Override
    public void onRender(Render3DEvent event) {}
}
