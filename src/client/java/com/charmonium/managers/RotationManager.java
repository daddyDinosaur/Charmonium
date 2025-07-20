package com.charmonium.managers;

import com.charmonium.Charmonium;
import com.charmonium.utils.misc.Clock;
import com.charmonium.utils.rotation.AngleUtils;
import com.charmonium.utils.rotation.Rotation;
import com.charmonium.event.events.Render3DEvent;
import com.charmonium.event.events.SendMovementPacketEvent;
import com.charmonium.event.events.SendPacketEvent;
import com.charmonium.event.events.TickEvent;
import com.charmonium.event.listeners.Render3DListener;
import com.charmonium.event.listeners.SendMovementPacketListener;
import com.charmonium.event.listeners.SendPacketListener;
import com.charmonium.event.listeners.TickListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Random;
import java.util.function.Function;

public class RotationManager implements TickListener, Render3DListener, SendPacketListener, SendMovementPacketListener {
    private static final MinecraftClient MC = MinecraftClient.getInstance();

    private final Rotation startRotation = new Rotation(0f, 0f);
    private final Rotation targetRotation = new Rotation(0f, 0f);
    private final Rotation currentRotation = new Rotation(0f, 0f);
    private final Clock dontRotate = new Clock();

    private boolean rotating;
    private long startTime;
    private long endTime;
    private boolean smoothRotation = true;
    private final Random random = new Random();

    public RotationManager() {
        Charmonium.getInstance().eventManager.AddListener(TickListener.class, this);
        Charmonium.getInstance().eventManager.AddListener(Render3DListener.class, this);
        Charmonium.getInstance().eventManager.AddListener(SendPacketListener.class, this);
        Charmonium.getInstance().eventManager.AddListener(SendMovementPacketListener.class, this);
    }

    public void easeTo(Rotation to, long time) {
        this.dontRotate.reset();
        this.startTime = System.currentTimeMillis();

        this.startRotation.setYaw(MC.player.getYaw());
        this.startRotation.setPitch(MC.player.getPitch());

        Rotation neededChange = getNeededChange(startRotation, to);
        this.targetRotation.setYaw(startRotation.getYaw() + neededChange.getYaw());
        this.targetRotation.setPitch(startRotation.getPitch() + neededChange.getPitch());

        float absYaw = Math.max(Math.abs(neededChange.getYaw()), 1);
        float absPitch = Math.max(Math.abs(neededChange.getPitch()), 1);
        float pythagoras = (float) Math.sqrt(absYaw * absYaw + absPitch * absPitch);
        float adjustedTime = getTime(pythagoras, time);

        this.endTime = System.currentTimeMillis() + (long) Math.max(adjustedTime, 50 + Math.random() * 100);
        this.rotating = true;
    }

    public void setInstant(Rotation rotation) {
        MC.player.setYaw(rotation.getYaw());
        MC.player.setPitch(rotation.getPitch());
        this.currentRotation.setRotation(rotation);
        this.rotating = false;
    }

    @Override
    public void onTick(TickEvent.Pre event) {}

    @Override
    public void onTick(TickEvent.Post event) {
        if (!rotating) return;

        if (MC.currentScreen != null || (dontRotate.isScheduled() && !dontRotate.passed())) {
            endTime = System.currentTimeMillis() + (endTime - System.currentTimeMillis());
            return;
        }

        if (System.currentTimeMillis() >= endTime) {
            this.currentRotation.setYaw(targetRotation.getYaw());
            this.currentRotation.setPitch(targetRotation.getPitch());
            this.rotating = false;
            return;
        }

        float progress = (System.currentTimeMillis() - startTime) / (float) (endTime - startTime);
        progress = MathHelper.clamp(progress, 0, 1);

        this.currentRotation.setYaw(interpolate(startRotation.getYaw(), targetRotation.getYaw(), progress, this::easeOutExpo));
        this.currentRotation.setPitch(interpolate(startRotation.getPitch(), targetRotation.getPitch(), progress, this::easeOutQuart));
    }

    @Override
    public void onRender(Render3DEvent event) {
        if (rotating && smoothRotation) {
            assert MC.player != null;
            MC.player.setYaw(currentRotation.getYaw());
            MC.player.setPitch(currentRotation.getPitch());
        }
    }

    @Override
    public void onSendMovementPacket(SendMovementPacketEvent.Pre event) {
        if (!rotating) return;

        Vec3d playerPos = MC.player.getPos();
        boolean onGround = MC.player.isOnGround();
        boolean horizontalCollision = MC.player.horizontalCollision;

        event.cancel();

        MC.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(
                playerPos.x,
                playerPos.y,
                playerPos.z,
                currentRotation.getYaw(),
                currentRotation.getPitch(),
                onGround,
                horizontalCollision
        ));
    }

    @Override
    public void onSendMovementPacket(SendMovementPacketEvent.Post event) {}

    @Override
    public void onSendPacket(SendPacketEvent event) {}

    public static Rotation getRotation(Vec3d target) {
        MinecraftClient mc = MinecraftClient.getInstance();
        assert mc.player != null;
        Vec3d playerPos = mc.player.getEyePos();

        double deltaX = target.x - playerPos.x;
        double deltaY = target.y - playerPos.y;
        double deltaZ = target.z - playerPos.z;

        float yaw = (float) (Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0);
        float distance = (float) Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        float pitch = (float) (-Math.toDegrees(Math.atan2(deltaY, distance)));

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

    public void setSmoothRotation(boolean smooth) {
        this.smoothRotation = smooth;
    }

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
        startTime = 0;
        endTime = 0;
    }
}
