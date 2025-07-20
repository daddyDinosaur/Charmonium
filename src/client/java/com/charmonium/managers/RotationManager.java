package com.charmonium.managers;

import com.charmonium.Charmonium;
import com.charmonium.utils.rotation.AngleUtils;
import com.charmonium.utils.rotation.Rotation;
import com.charmonium.event.events.Render3DEvent;
import com.charmonium.event.events.TickEvent;
import com.charmonium.event.listeners.Render3DListener;
import com.charmonium.event.listeners.TickListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import java.util.Random;
import java.util.function.Function;

public class RotationManager implements TickListener, Render3DListener {
    private static final MinecraftClient MC = MinecraftClient.getInstance();

    private final Rotation startRotation = new Rotation(0f, 0f);
    private final Rotation targetRotation = new Rotation(0f, 0f);
    private final Rotation currentRotation = new Rotation(0f, 0f);

    private boolean rotating;
    private boolean followTarget;
    private long startTime;
    private long endTime;
    private long lastUpdateTargetTime;
    private static final long FOLLOW_TARGET_UPDATE_MS = 90;
    private static final float FOLLOW_ANGLE_THRESHOLD = 3.0f;
    private final Random random = new Random();

    public RotationManager() {
        Charmonium.getInstance().eventManager.AddListener(TickListener.class, this);
        Charmonium.getInstance().eventManager.AddListener(Render3DListener.class, this);
    }

    public void easeTo(Rotation to, long time) {
        followTarget = false;
        this.startTime = System.currentTimeMillis();
        this.startRotation.setYaw(MC.player.getYaw());
        this.startRotation.setPitch(MC.player.getPitch());
        Rotation neededChange = getNeededChange(startRotation, to);
        this.targetRotation.setYaw(startRotation.getYaw() + neededChange.getYaw());
        this.targetRotation.setPitch(startRotation.getPitch() + neededChange.getPitch());
        float absYaw = Math.max(Math.abs(neededChange.getYaw()), 1);
        float absPitch = Math.max(Math.abs(neededChange.getPitch()), 1);
        float pythag = (float) Math.sqrt(absYaw * absYaw + absPitch * absPitch);
        float adjustedTime = getTime(pythag, time);
        this.endTime = System.currentTimeMillis() + (long) Math.max(adjustedTime, 100 + random.nextDouble() * 80);
        this.rotating = true;
    }

    public void followTo(Rotation to, long time) {
        if (!rotating) {
            easeTo(to, time);
            followTarget = true;
            lastUpdateTargetTime = System.currentTimeMillis();
        } else {
            followTarget = true;
            if (System.currentTimeMillis() - lastUpdateTargetTime >= FOLLOW_TARGET_UPDATE_MS) {
                float newYaw = to.getYaw();
                float newPitch = to.getPitch();
                boolean update = Math.abs(AngleUtils.normalizeAngle(newYaw - targetRotation.getYaw())) > FOLLOW_ANGLE_THRESHOLD ||
                        Math.abs(newPitch - targetRotation.getPitch()) > FOLLOW_ANGLE_THRESHOLD;
                if (update) {
                    // Only update target endpoint, do NOT reset start time or startRotation!
                    targetRotation.setYaw(newYaw);
                    targetRotation.setPitch(newPitch);
                    // Optionally stretch time if target moved a lot (makes curves more "lazy")
                    float toStart = (float) Math.sqrt(
                            Math.pow(AngleUtils.normalizeAngle(targetRotation.getYaw() - startRotation.getYaw()), 2) +
                                    Math.pow(targetRotation.getPitch() - startRotation.getPitch(), 2));
                    float origDur = endTime - startTime;
                    float factor = MathHelper.clamp(toStart / 40f, 1f, 2.1f);
                    long newEnd = System.currentTimeMillis() + (long) ((endTime - System.currentTimeMillis()) * factor);
                    endTime = Math.max(endTime, newEnd);
                }
                lastUpdateTargetTime = System.currentTimeMillis();
            }
        }
    }

    public void stopFollow() {
        followTarget = false;
    }

    @Override
    public void onTick(TickEvent.Pre event) {}
    @Override
    public void onTick(TickEvent.Post event) {}

    // The KEY: smooth interpolation only runs on render, not tick
    @Override
    public void onRender(Render3DEvent event) {
        if (!rotating) return;
        if (System.currentTimeMillis() >= endTime) {
            currentRotation.setYaw(targetRotation.getYaw());
            currentRotation.setPitch(targetRotation.getPitch());
            MC.player.setYaw(currentRotation.getYaw());
            MC.player.setPitch(currentRotation.getPitch());
            rotating = false;
            followTarget = false;
            return;
        }
        float progress = MathHelper.clamp((System.currentTimeMillis() - startTime) / (float) (endTime - startTime), 0, 1);
        float interpYaw = interpolate(startRotation.getYaw(), targetRotation.getYaw(), progress, this::easeOutExpo);
        float interpPitch = interpolate(startRotation.getPitch(), targetRotation.getPitch(), progress, this::easeOutQuart);
        currentRotation.setYaw(interpYaw);
        currentRotation.setPitch(interpPitch);
        MC.player.setYaw(interpYaw);
        MC.player.setPitch(interpPitch);
    }

    public static Rotation getRotation(Vec3d target) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Vec3d playerPos = mc.player.getEyePos();
        double dx = target.x - playerPos.x;
        double dy = target.y - playerPos.y;
        double dz = target.z - playerPos.z;
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float dist = (float) Math.sqrt(dx * dx + dz * dz);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, dist)));
        yaw = MathHelper.wrapDegrees(yaw);
        pitch = MathHelper.wrapDegrees(pitch);
        return new Rotation(yaw, pitch);
    }

    public Rotation getCurrentRotation() {
        return new Rotation(currentRotation.getYaw(), currentRotation.getPitch());
    }
    public boolean isRotating() {
        return rotating;
    }
    public void setSmoothRotation(boolean smooth) {}

    private float interpolate(float start, float end, float progress, Function<Float, Float> easing) {
        return (end - start) * easing.apply(progress) + start;
    }

    private float getTime(float pythagoras, float baseTime) {
        if (pythagoras < 25) return baseTime * 0.65f;
        if (pythagoras < 45) return baseTime * 0.77f;
        if (pythagoras < 80) return baseTime * 0.9f;
        if (pythagoras > 100) return baseTime * 1.1f;
        return baseTime;
    }
    public Rotation getNeededChange(Rotation start, Rotation end) {
        float yawDiff = end.getYaw() - start.getYaw();
        yawDiff = AngleUtils.normalizeAngle(yawDiff);
        return new Rotation(yawDiff, end.getPitch() - start.getPitch());
    }
    private float easeOutExpo(float x) {
        return x == 1 ? 1 : 1 - (float) Math.pow(2, -10 * x);
    }
    private float easeOutQuart(float x) {
        return (float) (1 - Math.pow(1 - x, 4));
    }
    public void reset() {
        rotating = false;
        followTarget = false;
        startTime = 0;
        endTime = 0;
    }
}
