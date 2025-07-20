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
import net.minecraft.util.math.MathHelper;
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
    private float currentHumanPitch = 0f;
    private int pitchSinceLastUpdate = 0;
    private static final int NODES_PER_PITCH = 3;
    private float lastYawTarget = Float.NaN;
    private float lastPitchTarget = Float.NaN;
    private long lastEaseRequestTime = 0;
    private static final float ROTATION_ANGLE_EPSILON = 2.0f;
    private static final long MIN_ROTATION_INTERVAL_MS = 100;
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
        curVec = BlockUtils.getCenteredVec(curPath.get(0));
        curPath.remove(0);
        prev = null;
        endBlock = curPath.isEmpty() ? curVec : curPath.get(curPath.size() - 1);
        currentHumanPitch = getRandomPitch();
        pitchSinceLastUpdate = 0;
        lastYawTarget = Float.NaN;
        lastPitchTarget = Float.NaN;
        lastEaseRequestTime = 0;
    }

    public void run(List<Vec3d> path, boolean walkState, boolean isShiftClose, double distToToShift) {
        Charmonium.getInstance().eventManager.AddListener(TickListener.class, this);
        Charmonium.getInstance().eventManager.AddListener(Render3DListener.class, this);
        state = walkState;
        curPath = new ArrayList<>(path);
        curVec = BlockUtils.getCenteredVec(curPath.get(0));
        curPath.remove(0);
        prev = null;
        endBlock = curPath.isEmpty() ? curVec : curPath.get(curPath.size() - 1);
        distToShift = distToToShift;
        isShift = isShiftClose;
        currentHumanPitch = getRandomPitch();
        pitchSinceLastUpdate = 0;
        lastYawTarget = Float.NaN;
        lastPitchTarget = Float.NaN;
        lastEaseRequestTime = 0;
    }

    @Override
    public void onTick(TickEvent.Pre event) {}

    @Override
    public void onTick(TickEvent.Post event) {
        if (!state) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        assert mc.player != null;
        Vec3d currentPos = mc.player.getPos();
        //trySkipNodes(currentPos);

        if (recentPositions.size() >= MAX_RECENT_POSITIONS) recentPositions.removeFirst();
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

        if (mc.player.isOnGround()) {
            HumanRotation rot = getHumanLookTargetAndPitch(currentPos);
            float desiredYaw = rot.yaw;
            float desiredPitch = rot.pitch;

            boolean needRotate = false;
            if (Float.isNaN(lastYawTarget) || Float.isNaN(lastPitchTarget)) {
                needRotate = true;
            } else if (Math.abs(MathHelper.wrapDegrees(desiredYaw - lastYawTarget)) > ROTATION_ANGLE_EPSILON
                    || Math.abs(desiredPitch - lastPitchTarget) > ROTATION_ANGLE_EPSILON) {
                needRotate = true;
            } else if (!rotation.isRotating() && System.currentTimeMillis() - lastEaseRequestTime > MIN_ROTATION_INTERVAL_MS) {
                needRotate = true;
            }
            if (needRotate) {
                rotation.easeTo(new Rotation(desiredYaw, desiredPitch), getRandomRotationTime() * 2);
                lastYawTarget = desiredYaw;
                lastPitchTarget = desiredPitch;
                lastEaseRequestTime = System.currentTimeMillis();
            }
        }

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

        if (isShift) {
            isShifting = BlockUtils.distanceFromTo(mc.player.getPos(), endBlock) < distToShift &&
                    mc.player.getPos().y == curVec.y;
        }
        KeyBindUtils.setKeyBindState(mc.options.sneakKey, isShifting);

//        Vec3d lookTarget = getLookAheadTarget();
//        if (BlockUtils.distanceFromTo(currentPos, endBlock) > 1) {
//            Rotation targetRot = RotationManager.getRotation(lookTarget);
//            rotation.followTo(new Rotation(targetRot.getYaw(), 0f), getRandomRotationTime() * 2);
//        }
    }

    private void nextBlock() {
        if (curPath == null || curPath.isEmpty()) {
            curVec = null;
            stop();
            return;
        }
        prev = curVec;
        curVec = BlockUtils.getCenteredVec(curPath.removeFirst());
        pitchSinceLastUpdate++;
        if (pitchSinceLastUpdate >= NODES_PER_PITCH) {
            currentHumanPitch = getRandomPitch();
            pitchSinceLastUpdate = 0;
        }
    }

    private boolean isCloseToJump() {
        MinecraftClient mc = MinecraftClient.getInstance();
        assert mc.player != null;
        if (prev != null &&
                !BlockUtils.getBlockType(BlockUtils.fromVecToBP(prev.add(0, -1, 0))).getDefaultState().isIn(BlockTags.SLABS)) {
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
        Vec3d previous = recentPositions.getFirst();
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
                        .map(BlockNodeClass::getBlockPos).collect(Collectors.toList());
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

    private static Vec3d lerpVec(Vec3d from, Vec3d to, double t) {
        return new Vec3d(
                from.x + (to.x - from.x) * t,
                from.y + (to.y - from.y) * t,
                from.z + (to.z - from.z) * t
        );
    }

    private static class HumanRotation {
        public final float yaw;
        public final float pitch;
        public HumanRotation(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    private HumanRotation getHumanLookTargetAndPitch(Vec3d playerPos) {
        if (curPath == null || curPath.isEmpty()) {
            Rotation rot = RotationManager.getRotation(curVec);
            return new HumanRotation(rot.getYaw(), currentHumanPitch);
        }
        double distToCur = playerPos.distanceTo(curVec);
        Vec3d nextNode = !curPath.isEmpty() ? curPath.get(0) : curVec;
        Vec3d afterNextNode = curPath.size() >= 2 ? curPath.get(1) : nextNode;
        double t = MathHelper.clamp(1.0 - (distToCur / 3.5), 0.0, 1.0);
        Vec3d blend1 = lerpVec(curVec, nextNode, t * 0.6);
        Vec3d blend2 = lerpVec(blend1, afterNextNode, t * 0.25);
        Vec3d gazeTarget = blend2.add(0d, 0.45d, 0d);
        Rotation rot = RotationManager.getRotation(gazeTarget);
        float pitch;
        if (t > 0.5) {
            pitch = (float) (rot.getPitch() * (t - 0.5f) * 2 + currentHumanPitch * (1 - (t - 0.5f) * 2));
            pitch = MathHelper.clamp(pitch, -6f, 15f);
        } else {
            pitch = currentHumanPitch;
        }
        return new HumanRotation(rot.getYaw(), pitch);
    }

    private float getRandomPitch() {
        return (float) (Math.random() * (9.0 - 3.0) + 3.0);
    }

    private void trySkipNodes(Vec3d currentPos) {
        if (curPath == null || curPath.isEmpty()) return;
        int n = curPath.size();

        if (n <= 1) return;

        Vec3d finalDir = curPath.get(n - 1).subtract(currentPos).normalize();

        int skipTo = -1;
        Vec3d node = curPath.getFirst();
        Vec3d toNode = node.subtract(currentPos);
        double forward = toNode.dotProduct(finalDir);

        if (forward < -0.15 && currentPos.distanceTo(node) < 0.75) {
            skipTo = 0;
        }

        if (skipTo >= 0) {
            curPath.subList(0, skipTo + 1).clear();
            if (!curPath.isEmpty()) curVec = curPath.getFirst();
            else curVec = currentPos;
        }
    }

    public void pause() {
        KeyBindUtils.getPathfindingControls().forEach(key ->
                prevKeybinds.put(key, key.isPressed()));
        KeyBindUtils.stopMovement();
        rotation.stopFollow();
        state = false;
    }

    public void unpause() {
        prevKeybinds.forEach(KeyBindUtils::setKeyBindState);
        state = true;
    }

    public void stop() {
        KeyBindUtils.stopMovement();
        rotation.stopFollow();
        state = false;
        curPath = null;
        curVec = null;
        recentPositions.clear();
        lastYawTarget = Float.NaN;
        lastPitchTarget = Float.NaN;
        lastEaseRequestTime = 0;
        Charmonium.getInstance().eventManager.RemoveListener(TickListener.class, this);
        Charmonium.getInstance().eventManager.RemoveListener(Render3DListener.class, this);
    }

    public boolean isDone() {
        return curPath == null;
    }

    @Override
    public void onRender(Render3DEvent event) {}
}
