package com.mylk.charmonium.failsafe.impl;

import com.mylk.charmonium.Charmonium;
import com.mylk.charmonium.config.Config;
import com.mylk.charmonium.config.page.CustomFailsafeMessagesPage;
import com.mylk.charmonium.config.page.FailsafeNotificationsPage;
import com.mylk.charmonium.event.ReceivePacketEvent;
import com.mylk.charmonium.failsafe.Failsafe;
import com.mylk.charmonium.failsafe.FailsafeManager;
import com.mylk.charmonium.feature.impl.LagDetector;
import com.mylk.charmonium.feature.impl.MovRecPlayer;
import com.mylk.charmonium.handler.BaritoneHandler;
import com.mylk.charmonium.handler.GameStateHandler;
import com.mylk.charmonium.handler.MacroHandler;
import com.mylk.charmonium.handler.RotationHandler;
import com.mylk.charmonium.macro.impl.FishingMacro;
import com.mylk.charmonium.macro.impl.IceWalkerMacro;
import com.mylk.charmonium.macro.impl.SlayerMacro;
import com.mylk.charmonium.macro.impl.GemstoneMacro;
import com.mylk.charmonium.util.AngleUtils;
import com.mylk.charmonium.util.BlockUtils;
import com.mylk.charmonium.util.LogUtils;
import com.mylk.charmonium.util.helper.Rotation;
import com.mylk.charmonium.util.helper.RotationConfiguration;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.BlockPos;

import java.util.Objects;

public class RotationFailsafe extends Failsafe {
    private static RotationFailsafe instance;
    public static RotationFailsafe getInstance() {
        if (instance == null) {
            instance = new RotationFailsafe();
        }
        return instance;
    }

    @Override
    public int getPriority() {
        return 4;
    }

    @Override
    public FailsafeManager.EmergencyType getType() {
        return FailsafeManager.EmergencyType.ROTATION_CHECK;
    }

    @Override
    public boolean shouldSendNotification() {
        return FailsafeNotificationsPage.notifyOnRotationFailsafe;
    }

    @Override
    public boolean shouldPlaySound() {
        return FailsafeNotificationsPage.alertOnRotationFailsafe;
    }

    @Override
    public boolean shouldTagEveryone() {
        return FailsafeNotificationsPage.tagEveryoneOnRotationFailsafe;
    }

    @Override
    public boolean shouldAltTab() {
        return FailsafeNotificationsPage.autoAltTabOnRotationFailsafe;
    }

    @Override
    public void onReceivedPacketDetection(ReceivePacketEvent event) {
        if (MacroHandler.getInstance().isTeleporting())
            return;
        if (!(event.packet instanceof S08PacketPlayerPosLook)) {
            return;
        }
        if (LagDetector.getInstance().isLagging() || LagDetector.getInstance().wasJustLagging()) {
            LogUtils.sendWarning("[Failsafe] Got rotation packet while lagging! Ignoring that one.");
            return;
        }

        BlockPos blockUnderPlayer = mc.thePlayer.getPosition().down();
        BlockPos blockBehindPlayer = mc.thePlayer.getPosition().down().offset(mc.thePlayer.getHorizontalFacing().getOpposite());
        if ((!MacroHandler.walker.isDone() ||
                SlayerMacro.hasDied ||
                IceWalkerMacro.hasDied ||
                FishingMacro.hasDied ||
                FishingMacro.target != null ||
                FishingMacro.stuckInWater ||
                SlayerMacro.killingBoss ||
                GemstoneMacro.stopChecks ||
                Objects.requireNonNull(BlockUtils.getBlockState(blockBehindPlayer)).getBlock() == Blocks.wooden_slab ||
                Objects.requireNonNull(BlockUtils.getBlockState(blockUnderPlayer)).getBlock() == Blocks.wooden_slab) &&
                Config.getFailsafeExceptions().contains(Config.FailsafeException.ROTATION_CHECK)) {
            LogUtils.sendWarning("[Failsafe] Got rotation packet while a whitelisted scenario ! Ignoring that one.");
            return;
        }

        S08PacketPlayerPosLook packet = (S08PacketPlayerPosLook) event.packet;
        double packetYaw = packet.getYaw();
        double packetPitch = packet.getPitch();
        double playerYaw = mc.thePlayer.rotationYaw;
        double playerPitch = mc.thePlayer.rotationPitch;
        double yawDiff = Math.abs(packetYaw - playerYaw);
        double pitchDiff = Math.abs(packetPitch - playerPitch);
        double threshold = Config.rotationCheckSensitivity;
        if (yawDiff == 360 && pitchDiff == 0) // prevents false checks
            return;
        if (yawDiff >= threshold || pitchDiff >= threshold) {
            rotationBeforeReacting = new Rotation((float) playerYaw, (float) playerPitch);
            LogUtils.sendDebug("[Failsafe] Rotation detected! Yaw diff: " + yawDiff + ", Pitch diff: " + pitchDiff);
            FailsafeManager.getInstance().possibleDetection(this);
        }
    }

    @Override
    public void duringFailsafeTrigger() {

        switch (rotationCheckState) {
            case NONE:
                rotationCheckState = RotationCheckState.WAIT_BEFORE_START;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 500);
                break;
            case WAIT_BEFORE_START:
                MacroHandler.getInstance().pauseMacro();
                if (rotationBeforeReacting == null)
                    rotationBeforeReacting = new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
                MovRecPlayer.setYawDifference(AngleUtils.getClosest(rotationBeforeReacting.getYaw()));
                positionBeforeReacting = mc.thePlayer.getPosition();
                rotationCheckState = RotationCheckState.LOOK_AROUND;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 500);
                break;
            case LOOK_AROUND:
                MovRecPlayer.getInstance().playRandomRecording("ROTATION_CHECK_Start_");
                rotationCheckState = RotationCheckState.WAIT_BEFORE_SENDING_MESSAGE_1;
                FailsafeManager.getInstance().scheduleRandomDelay(2000, 3000);
                break;
            case WAIT_BEFORE_SENDING_MESSAGE_1:
                if (MovRecPlayer.getInstance().isRunning())
                    break;
                if (!Config.sendFailsafeMessage) {
                    rotationCheckState = RotationCheckState.ROTATE_TO_POS_BEFORE;
                    FailsafeManager.getInstance().scheduleRandomDelay(300, 600);
                    break;
                }
                if (CustomFailsafeMessagesPage.customRotationMessages.isEmpty()) {
                    randomMessage = FailsafeManager.getRandomMessage();
                } else {
                    String[] customMessages = CustomFailsafeMessagesPage.customRotationMessages.split("\\|");
                    randomMessage = FailsafeManager.getRandomMessage(customMessages);
                }
                rotationCheckState = RotationCheckState.SEND_MESSAGE;
                FailsafeManager.getInstance().scheduleRandomDelay(randomMessage.length() * 150L, 1000);
                break;
            case SEND_MESSAGE:
                LogUtils.sendDebug("[Failsafe] Chosen message: " + randomMessage);
                mc.thePlayer.sendChatMessage("/ac " + randomMessage);
                rotationCheckState = RotationCheckState.ROTATE_TO_POS_BEFORE;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                break;
            case ROTATE_TO_POS_BEFORE:
                rotation.easeTo(new RotationConfiguration(new Rotation(rotationBeforeReacting.getYaw(), rotationBeforeReacting.getPitch()),
                        500, null));
                rotationCheckState = RotationCheckState.LOOK_AROUND_2;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                break;
            case LOOK_AROUND_2:
                if (rotation.isRotating())
                    break;
                MovRecPlayer.getInstance().playRandomRecording("ROTATION_CHECK_Continue_");
                rotationCheckState = RotationCheckState.WAIT_BEFORE_SENDING_MESSAGE_2;
                FailsafeManager.getInstance().scheduleRandomDelay(2000, 3000);
                break;
            case WAIT_BEFORE_SENDING_MESSAGE_2:
                if (MovRecPlayer.getInstance().isRunning())
                    break;
                if (!Config.sendFailsafeMessage || Math.random() < 0.3) {
                    rotationCheckState = RotationCheckState.GO_BACK_START;
                    FailsafeManager.getInstance().scheduleRandomDelay(300, 600);
                }
                if (CustomFailsafeMessagesPage.customContinueMessages.isEmpty()) {
                    randomContinueMessage = FailsafeManager.getRandomContinueMessage();
                } else {
                    String[] customContinueMessages = CustomFailsafeMessagesPage.customContinueMessages.split("\\|");
                    randomContinueMessage = FailsafeManager.getRandomMessage(customContinueMessages);
                }
                rotationCheckState = RotationCheckState.SEND_MESSAGE_2;
                FailsafeManager.getInstance().scheduleRandomDelay(randomContinueMessage.length() * 150L, 1000);
                break;
            case SEND_MESSAGE_2:
                if (MovRecPlayer.getInstance().isRunning())
                    break;
                LogUtils.sendDebug("[Failsafe] Chosen message: " + randomContinueMessage);
                mc.thePlayer.sendChatMessage("/ac " + randomContinueMessage);
                rotationCheckState = RotationCheckState.GO_BACK_START;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                break;
            case GO_BACK_START:
                if (MovRecPlayer.getInstance().isRunning())
                    break;
                if (mc.thePlayer.getPosition().distanceSq(positionBeforeReacting) < 2) {
                    rotationCheckState = RotationCheckState.ROTATE_TO_POS_BEFORE_2;
                    break;
                }
                BaritoneHandler.walkToBlockPos(positionBeforeReacting);
                rotationCheckState = RotationCheckState.GO_BACK_END;
                break;
            case GO_BACK_END:
                if (BaritoneHandler.hasFailed() || !BaritoneHandler.isWalkingToGoalBlock()) {
                    rotationCheckState = RotationCheckState.ROTATE_TO_POS_BEFORE_2;
                    FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                    break;
                }
                LogUtils.sendDebug("Distance difference: " + mc.thePlayer.getPosition().distanceSq(positionBeforeReacting));
                FailsafeManager.getInstance().scheduleDelay(200);
                break;
            case ROTATE_TO_POS_BEFORE_2:
                if (rotation.isRotating()) break;
                rotation.easeTo(new RotationConfiguration(new Rotation(rotationBeforeReacting.getYaw(), rotationBeforeReacting.getPitch()),
                        500, null));
                this.endOfFailsafeTrigger();
                FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                break;
        }
    }

    @Override
    public void endOfFailsafeTrigger() {
        FailsafeManager.getInstance().stopFailsafes();
        if (mc.thePlayer.getPosition().getY() < 100 && GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.GARDEN)
            MacroHandler.getInstance().resumeMacro();
    }

    @Override
    public void resetStates() {
        rotationCheckState = RotationCheckState.NONE;
        rotationBeforeReacting = null;
        positionBeforeReacting = null;
        randomMessage = null;
        randomContinueMessage = null;
        rotation.reset();
    }

    private RotationCheckState rotationCheckState = RotationCheckState.NONE;
    private final RotationHandler rotation = RotationHandler.getInstance();
    private BlockPos positionBeforeReacting = null;
    private Rotation rotationBeforeReacting = null;
    String randomMessage;
    String randomContinueMessage;

    enum RotationCheckState {
        NONE,
        WAIT_BEFORE_START,
        LOOK_AROUND,
        WAIT_BEFORE_SENDING_MESSAGE_1,
        SEND_MESSAGE,
        ROTATE_TO_POS_BEFORE,
        LOOK_AROUND_2,
        WAIT_BEFORE_SENDING_MESSAGE_2,
        SEND_MESSAGE_2,
        ROTATE_TO_POS_BEFORE_2,
        GO_BACK_START,
        GO_BACK_END,
    }
}