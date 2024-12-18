package com.mylk.charmonium.handler;

import com.mylk.charmonium.event.MotionUpdateEvent;
import com.mylk.charmonium.macro.impl.GemstoneMacro;
import com.mylk.charmonium.macro.impl.TunnelsMacro;
import com.mylk.charmonium.mixin.client.MinecraftAccessor;
import com.mylk.charmonium.util.AngleUtils;
import com.mylk.charmonium.util.BlockUtils;
import com.mylk.charmonium.util.LogUtils;
import com.mylk.charmonium.util.helper.Clock;
import com.mylk.charmonium.util.helper.Rotation;
import com.mylk.charmonium.util.helper.RotationConfiguration;
import com.mylk.charmonium.util.helper.Target;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Optional;
import java.util.Random;
import java.util.function.Function;

import static cc.polyfrost.oneconfig.libs.universal.UMath.wrapAngleTo180;

public class RotationHandler {
    private static RotationHandler instance;
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final Rotation startRotation = new Rotation(0f, 0f);
    private final Rotation targetRotation = new Rotation(0f, 0f);
    private final Clock dontRotate = new Clock();
    @Getter
    private boolean rotating;
    private long startTime;
    private double previousTime;
    private float phase;
    private long endTime;
    @Getter
    private float clientSideYaw = 0;
    @Getter
    private float clientSidePitch = 0;
    @Getter
    private float serverSideYaw = 0;
    @Getter
    private float serverSidePitch = 0;
    @Getter
    private static RotationConfiguration configuration;

    private static final Random random = new Random();

    public static RotationHandler getInstance() {
        if (instance == null) {
            instance = new RotationHandler();
        }
        return instance;
    }

    public void easeTo(RotationConfiguration configuration) {
        this.configuration = configuration;
        easingModifier = (random.nextFloat() * 0.5f - 0.25f);
        dontRotate.reset();
        startTime = System.currentTimeMillis();
        startRotation.setRotation(configuration.getFrom());
        Rotation neededChange;
        randomAddition = (Math.random() * 0.3 - 0.15);
        if (configuration.getTarget().isPresent() && configuration.getTarget().get().getTarget().isPresent()) {
            neededChange = getNeededChange(startRotation, configuration.getTarget().get().getTarget().get());
        } else if (configuration.getTo().isPresent()) {
            neededChange = getNeededChange(startRotation, configuration.getTo().get());
        } else {
            throw new IllegalArgumentException("No target or rotation specified!");
        }

        if ((neededChange.getPitch() > 360 || neededChange.getPitch() < -360) && GemstoneMacro.targetGem != null) {
            GemstoneMacro.targetGem = null;
            return;
        }

        if ((neededChange.getYaw() > 360 || neededChange.getYaw() < -360) && GemstoneMacro.targetGem != null) {
            GemstoneMacro.targetGem = null;
            return;
        }

        if ((neededChange.getPitch() > 360 || neededChange.getPitch() < -360) && TunnelsMacro.targetGem != null) {
            TunnelsMacro.targetGem = null;
            return;
        }

        if ((neededChange.getYaw() > 360 || neededChange.getYaw() < -360) && TunnelsMacro.targetGem != null) {
            TunnelsMacro.targetGem = null;
            return;
        }

        targetRotation.setYaw(startRotation.getYaw() + neededChange.getYaw());
        targetRotation.setPitch(startRotation.getPitch() + neededChange.getPitch());

        //LogUtils.sendDebug("[Rotation] Needed change: " + neededChange.getYaw() + " " + neededChange.getPitch());

        float absYaw = Math.max(Math.abs(neededChange.getYaw()), 1);
        float absPitch = Math.max(Math.abs(neededChange.getPitch()), 1);
        float pythagoras = pythagoras(absYaw, absPitch);
        float time = getTime(pythagoras, configuration.getTime());
        endTime = (long) (System.currentTimeMillis() + Math.max(time, 50 + Math.random() * 100));
        rotating = true;
    }

    public void updateInEllipse(float semi_minor_axis, float semi_major_axis, int blocksInFront, float rotationYawAxis, int rate){
        if(rotating) return;

        double deltaTime = (System.currentTimeMillis() - previousTime) / 1000.0f;
        previousTime = System.currentTimeMillis();

        if(deltaTime > 0.1)
            return;

        phase += ((rate + Math.random() * rate / 2.0f) * deltaTime);
        phase %= (2 * Math.PI);

        float x = (float) (semi_major_axis * Math.sin(phase));
        float y = (float) (semi_minor_axis * Math.cos(phase)) - 0.5f;

        float yaw = getYawFromParametricEquation(x, blocksInFront, rotationYawAxis);
        float pitch = getPitchFromParametricEquation(x, y, blocksInFront, rotationYawAxis);

        if(AngleUtils.getAngleDifference(yaw, AngleUtils.getActualRotationYaw()) > 15 || Math.abs(pitch - mc.thePlayer.rotationPitch) > 15){
            easeTo(new RotationConfiguration(new Rotation(yaw, pitch), Math.max(AngleUtils.getYawRotationTime(yaw, 45, 200, 300), AngleUtils.getPitchRotationTime(pitch, 30, 200, 300)), null));
            return;
        }

        rotateInstantlyTo(yaw, pitch);
    }

    public void rotateInstantlyTo(float yaw, float pitch){
        float prevYaw = mc.thePlayer.rotationYaw;
        if(AngleUtils.shouldRotateClockwise(prevYaw, yaw)){
            mc.thePlayer.rotationYaw += AngleUtils.smallestAngleDifference(prevYaw, yaw);
        } else {
            mc.thePlayer.rotationYaw -= AngleUtils.smallestAngleDifference(prevYaw, yaw);
        }
        mc.thePlayer.rotationPitch = pitch;

    }

    public float getYawFromParametricEquation(float x, int blocksInFront, float rotationYawAxis) {
        return rotationYawAxis % 180 == 0 ?
                AngleUtils.getRequiredYaw(x, blocksInFront * BlockUtils.getUnitZ(rotationYawAxis)) :
                AngleUtils.getRequiredYaw(blocksInFront * BlockUtils.getUnitX(rotationYawAxis), x);
    }

    public float getPitchFromParametricEquation(float x, float y, int blocksInFront, float rotationYawAxis){
        return rotationYawAxis % 180 == 0 ?
                AngleUtils.getRequiredPitch(x, y, blocksInFront * BlockUtils.getUnitZ(rotationYawAxis)) :
                AngleUtils.getRequiredPitch(blocksInFront * BlockUtils.getUnitX(rotationYawAxis), y, x);
    }

    public void updateInLimacon(float radius, int blocksInFront, float rotationYawAxis, int rate){
        if(rotating) return;

        double deltaTime = (System.currentTimeMillis() - previousTime) / 1000.0f;
        previousTime = System.currentTimeMillis();

        if(deltaTime > 0.1)
            return;

        phase += (rate / 2.0f * deltaTime);
        phase %= (2 * Math.PI);

        float x, y;

        float a = 1;
        float b = 0.35f;
        float k = radius + 0.5f; // scale factor which scales up the whole loop (+0.5f is just an approximation)
        float c = -1.35f; // translates the whole graph c units right (negative -> left)

        x = (float) (k * (a / 2.0 + b * Math.cos(phase) + a / 2.0 * Math.cos(2 * phase)) + c);
        y = (float) (k * (b * Math.sin(phase) + a / 2.0 * Math.sin(2 * phase)));

        //just need to input the corresponding parametric equations :)

        float yaw = getYawFromParametricEquation(x, blocksInFront, rotationYawAxis);
        float pitch = getPitchFromParametricEquation(x, y, blocksInFront, rotationYawAxis);

        if(AngleUtils.getAngleDifference(yaw, AngleUtils.getActualRotationYaw()) > 15 || Math.abs(pitch - mc.thePlayer.rotationPitch) > 15){
            easeTo(new RotationConfiguration(new Rotation(yaw, pitch), Math.max(AngleUtils.getYawRotationTime(yaw, 45, 200, 300), AngleUtils.getPitchRotationTime(pitch, 30, 200, 300)), null));
            return;
        }
        rotateInstantlyTo(yaw, pitch);


    }

    private float getTime(float pythagoras, float time) {
        if (pythagoras < 25) {
            //LogUtils.sendDebug("[Rotation] Very close rotation, speeding up by 0.65");
            return (long) (time * 0.65);
        }
        if (pythagoras < 45) {
            //LogUtils.sendDebug("[Rotation] Close rotation, speeding up by 0.77");
            return (long) (time * 0.77);
        }
        if (pythagoras < 80) {
            //LogUtils.sendDebug("[Rotation] Not so close, but not that far rotation, speeding up by 0.9");
            return (long) (time * 0.9);
        }
        if (pythagoras > 100) {
            //LogUtils.sendDebug("[Rotation] Far rotation, slowing down by 1.1");
            return (long) (time * 1.1);
        }
        //LogUtils.sendDebug("[Rotation] Normal rotation");
        return (long) (time * 1.0);
    }

    public void easeBackFromServerRotation() {
        if (configuration == null) return;
        LogUtils.sendDebug("[Rotation] Easing back from server rotation");
        configuration.goingBackToClientSide(true);
        startTime = System.currentTimeMillis();
        configuration.setTarget(Optional.empty());
        startRotation.setRotation(new Rotation(serverSideYaw, serverSidePitch));
        Rotation neededChange = getNeededChange(startRotation, new Rotation(clientSideYaw, clientSidePitch));
        targetRotation.setYaw(startRotation.getYaw() + neededChange.getYaw());
        targetRotation.setPitch(startRotation.getPitch() + neededChange.getPitch());

        //LogUtils.sendDebug("[Rotation] Needed change: " + neededChange.getYaw() + " " + neededChange.getPitch());

        float time = configuration.getTime();
        endTime = System.currentTimeMillis() + Math.max((long) time, 50);
        configuration.setCallback(Optional.of(this::reset));
        rotating = true;
    }

    private float pythagoras(float a, float b) {
        return (float) Math.sqrt(a * a + b * b);
    }

    public Rotation getNeededChange(Rotation target) {
        if (configuration != null && configuration.getRotationType() == RotationConfiguration.RotationType.SERVER) {
            return getNeededChange(new Rotation(serverSideYaw, serverSidePitch), target);
        } else {
            return getNeededChange(new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch), target);
        }
    }

    public Rotation getNextPossibleRotation(Entity entity) {
        double xDelta = (entity.posX - entity.lastTickPosX) * 0.4d;
        double zDelta = (entity.posZ - entity.lastTickPosZ) * 0.4d;
        double d = mc.thePlayer.getDistanceToEntity(entity);
        d -= d % 0.8d;
        double xMulti = d / 0.8 * xDelta;
        double zMulti = d / 0.8 * zDelta;
        double x = entity.posX + xMulti - mc.thePlayer.posX;
        double z = entity.posZ + zMulti - mc.thePlayer.posZ;
        double y = mc.thePlayer.posY + mc.thePlayer.getEyeHeight() - Math.min((entity.posY + (entity.height * 0.85) + randomAddition), (entity.posY + entity.height - 0.05));
        float yaw = (float) Math.toDegrees(Math.atan2(z, x)) - 90f;
        double d2 = Math.sqrt(x * x + z * z);
        float pitch = (float) (-(Math.atan2(y, d2) * 180.0 / Math.PI));
        return new Rotation(yaw, -pitch);
    }

    public Rotation getNeededChange(Rotation startRot, Vec3 target) {
        Rotation targetRot;
        if (configuration != null && random.nextGaussian() > 0.8) {
            targetRot = getRotation(target, configuration.randomness());
        } else {
            targetRot = getRotation(target);
        }
        return getNeededChange(startRot, targetRot);
    }

    public Rotation getNeededChange(Rotation startRot, Rotation endRot) {
        float yawDiff = (float) (wrapAngleTo180(endRot.getYaw()) - wrapAngleTo180(startRot.getYaw()));

        yawDiff = AngleUtils.normalizeAngle(yawDiff);

        return new Rotation(yawDiff, endRot.getPitch() - startRot.getPitch());
    }

    private double randomAddition = (Math.random() * 0.3 - 0.15);

    public static Rotation getRotation(Vec3 to) {
        return getRotation(mc.thePlayer.getPositionEyes(((MinecraftAccessor) mc).getTimer().renderPartialTicks), to, false);
    }

    public Rotation getRotation(Vec3 to, boolean randomness) {
        return getRotation(mc.thePlayer.getPositionEyes(((MinecraftAccessor) mc).getTimer().renderPartialTicks), to, randomness);
    }

    public Rotation getRotation(Entity to) {
        return getRotation(mc.thePlayer.getPositionEyes(((MinecraftAccessor) mc).getTimer().renderPartialTicks), to.getPositionVector().addVector(0, Math.min(((to.height * 0.85) + randomAddition), (to.height - 0.05)), 0), false);
    }

    public Rotation getRotation(Entity to, boolean randomness) {
        return getRotation(mc.thePlayer.getPositionEyes(((MinecraftAccessor) mc).getTimer().renderPartialTicks), to.getPositionVector().addVector(0, Math.min(((to.height * 0.85) + randomAddition), (to.height - 0.05)), 0), randomness);
    }

    public static Rotation getRotation(Vec3 from, Vec3 to) {
        if (configuration != null && random.nextGaussian() > 0.8) {
            return getRotation(from, to, configuration.randomness());
        }
        return getRotation(from, to, false);
    }

    public Rotation getRotation(BlockPos pos) {
        if (configuration != null && random.nextGaussian() > 0.8) {
            return getRotation(mc.thePlayer.getPositionEyes(((MinecraftAccessor) mc).getTimer().renderPartialTicks), new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), configuration.randomness());
        }
        return getRotation(mc.thePlayer.getPositionEyes(((MinecraftAccessor) mc).getTimer().renderPartialTicks), new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), false);
    }

    public Rotation getRotation(BlockPos pos, boolean randomness) {
        return getRotation(mc.thePlayer.getPositionEyes(((MinecraftAccessor) mc).getTimer().renderPartialTicks), new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), randomness);
    }

    public static Rotation getRotation(Vec3 from, Vec3 to, boolean randomness) {
        double xDiff = to.xCoord - from.xCoord;
        double yDiff = to.yCoord - from.yCoord;
        double zDiff = to.zCoord - from.zCoord;

        double dist = Math.sqrt(xDiff * xDiff + zDiff * zDiff);

        float yaw = (float) Math.toDegrees(Math.atan2(zDiff, xDiff)) - 90F;
        float pitch = (float) -Math.toDegrees(Math.atan2(yDiff, dist));

        if (randomness) {
            yaw += (float) ((Math.random() - 1) * 4);
            pitch += (float) ((Math.random() - 1) * 4);
        }

        return new Rotation(yaw, pitch);
    }

    public void reset() {
        LogUtils.sendDebug("[Rotation] Resetting");
        rotating = false;
        configuration = null;
        startTime = 0;
        endTime = 0;
    }

    private float interpolate(float start, float end, Function<Float, Float> function) {
        float t = (float) (System.currentTimeMillis() - startTime) / (endTime - startTime);
        return (end - start) * function.apply(t) + start;
    }

    private float easingModifier = 0;

    private float easeOutQuart(float x) {
        return (float) (1 - Math.pow(1 - x, 4));
    }

    private float easeOutExpo(float x) {
        return x == 1 ? 1 : 1 - (float) Math.pow(2, -10 * x);
    }

    private float easeOutBack(float x) {
        float c1 = 1.70158f + easingModifier;
        float c3 = c1 + 1;
        return 1 + c3 * (float) Math.pow(x - 1, 3) + c1 * (float) Math.pow(x - 1, 2);
    }

    private final Clock delayBetweenTargetFollow = new Clock();

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        if (!rotating || configuration == null || configuration.getRotationType() != RotationConfiguration.RotationType.CLIENT)
            return;

        if (mc.currentScreen != null || dontRotate.isScheduled() && !dontRotate.passed()) {
            endTime = System.currentTimeMillis() + configuration.getTime();
            return;
        }

        if (System.currentTimeMillis() >= endTime) {
            // finish
            if (configuration.getCallback().isPresent()) {
                configuration.getCallback().get().run();
            } else { // No callback, just reset
                if (Math.abs(mc.thePlayer.rotationYaw - targetRotation.getYaw()) < 0.1 && Math.abs(mc.thePlayer.rotationPitch - targetRotation.getPitch()) < 0.1) {
                    mc.thePlayer.rotationYaw = targetRotation.getYaw();
                    mc.thePlayer.rotationPitch = targetRotation.getPitch();
                }
                reset();
                return;
            }
            if (configuration == null || !configuration.goingBackToClientSide()) { // Reset was called in callback
                return;
            }
            return;
        }

        if (configuration.followTarget() && configuration.getTarget().isPresent() && delayBetweenTargetFollow.passed()) {
            adjustTargetRotation();
        }
        mc.thePlayer.rotationYaw = interpolate(startRotation.getYaw(), targetRotation.getYaw(), configuration.easeOutBack() ? this::easeOutBack : this::easeOutExpo);
        mc.thePlayer.rotationPitch = interpolate(startRotation.getPitch(), targetRotation.getPitch(), configuration.easeOutBack() ? this::easeOutBack : this::easeOutQuart);
    }

    @SubscribeEvent(receiveCanceled = true)
    public void onUpdatePre(MotionUpdateEvent.Pre event) {
        if (!rotating || configuration == null || configuration.getRotationType() != RotationConfiguration.RotationType.SERVER)
            return;

        if (System.currentTimeMillis() >= endTime) {
            // finish
            if (configuration.getCallback().isPresent()) {
                configuration.getCallback().get().run();
            } else { // No callback, just reset
                reset();
                return;
            }
            if (configuration == null || !configuration.goingBackToClientSide()) { // Reset was called in callback
                return;
            }
        }
        clientSidePitch = mc.thePlayer.rotationPitch;
        clientSideYaw = mc.thePlayer.rotationYaw;
        // rotating
        if (configuration.followTarget() && configuration.getTarget().isPresent() && !configuration.goingBackToClientSide() && delayBetweenTargetFollow.passed()) {
            adjustTargetRotation();
        }
        if (configuration.goingBackToClientSide()) {
            LogUtils.sendDebug("Going back to client side");
            targetRotation.setYaw(clientSideYaw);
            targetRotation.setPitch(clientSidePitch);
        }
        if (mc.currentScreen != null || dontRotate.isScheduled() && !dontRotate.passed()) {
            event.yaw = serverSideYaw;
            event.pitch = serverSidePitch;
            endTime = System.currentTimeMillis() + configuration.getTime();
        } else {
            float interX = interpolate(startRotation.getYaw(), targetRotation.getYaw(), configuration.easeOutBack() ? this::easeOutBack : this::easeOutExpo);
            float interY = interpolate(startRotation.getPitch(), targetRotation.getPitch(), configuration.easeOutBack() ? this::easeOutBack : this::easeOutExpo);
            float absDiffX = Math.abs(interX - targetRotation.getYaw());
            float absDiffY = Math.abs(interY - targetRotation.getPitch());
            event.yaw = absDiffX < 0.1 ? targetRotation.getYaw() : interX;
            event.pitch = absDiffY < 0.1 ? targetRotation.getPitch() : interY;
        }
        serverSidePitch = event.pitch;
        serverSideYaw = event.yaw;
        mc.thePlayer.rotationYaw = serverSideYaw;
        mc.thePlayer.rotationPitch = serverSidePitch;
    }

    private void adjustTargetRotation() {
        Target target = configuration.getTarget().get();
        Rotation rot;
        if (target.getEntity() != null) {
            rot = getNextPossibleRotation(target.getEntity());
        } else if (target.getBlockPos() != null) {
            rot = getRotation(target.getBlockPos());
        } else if (target.getTarget().isPresent()) {
            rot = getRotation(target.getTarget().get());
        } else {
            throw new IllegalArgumentException("No target specified!");
        }
        Rotation neededChange = getNeededChange(startRotation, rot);
        targetRotation.setYaw(startRotation.getYaw() + neededChange.getYaw());
        targetRotation.setPitch(startRotation.getPitch() + neededChange.getPitch());
        delayBetweenTargetFollow.schedule(180 + Math.random() * 90);
    }

    @SubscribeEvent(receiveCanceled = true)
    public void onUpdatePost(MotionUpdateEvent.Post event) {
        if (!rotating) return;
        if (configuration == null || configuration.getRotationType() != RotationConfiguration.RotationType.SERVER)
            return;

        mc.thePlayer.rotationYaw = clientSideYaw;
        mc.thePlayer.rotationPitch = clientSidePitch;
    }
}
