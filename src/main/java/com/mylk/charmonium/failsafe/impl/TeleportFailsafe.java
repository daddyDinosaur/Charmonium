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
import com.mylk.charmonium.macro.impl.GemstoneMacro;
import com.mylk.charmonium.macro.impl.IceWalkerMacro;
import com.mylk.charmonium.macro.impl.SlayerMacro;
import com.mylk.charmonium.util.AngleUtils;
import com.mylk.charmonium.util.BlockUtils;
import com.mylk.charmonium.util.LogUtils;
import com.mylk.charmonium.util.PlayerUtils;
import com.mylk.charmonium.util.helper.Rotation;
import com.mylk.charmonium.util.helper.RotationConfiguration;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;

import java.util.Objects;

public class TeleportFailsafe extends Failsafe {
    private static TeleportFailsafe instance;
    public static TeleportFailsafe getInstance() {
        if (instance == null) {
            instance = new TeleportFailsafe();
        }
        return instance;
    }

    @Override
    public int getPriority() {
        return 5;
    }

    @Override
    public FailsafeManager.EmergencyType getType() {
        return FailsafeManager.EmergencyType.TELEPORT_CHECK;
    }

    @Override
    public boolean shouldSendNotification() {
        return FailsafeNotificationsPage.notifyOnTeleportationFailsafe;
    }

    @Override
    public boolean shouldPlaySound() {
        return FailsafeNotificationsPage.alertOnTeleportationFailsafe;
    }

    @Override
    public boolean shouldTagEveryone() {
        return FailsafeNotificationsPage.tagEveryoneOnTeleportationFailsafe;
    }

    @Override
    public boolean shouldAltTab() {
        return FailsafeNotificationsPage.autoAltTabOnTeleportationFailsafe;
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
                FishingMacro.stuckInWater ||
                SlayerMacro.killingBoss ||
                GemstoneMacro.stopChecks ||
                Objects.requireNonNull(BlockUtils.getBlockState(blockBehindPlayer)).getBlock() == Blocks.wooden_slab ||
                Objects.requireNonNull(BlockUtils.getBlockState(blockUnderPlayer)).getBlock() == Blocks.wooden_slab) &&
                Config.getFailsafeExceptions().contains(Config.FailsafeException.ROTATION_CHECK)) {
            LogUtils.sendWarning("[Failsafe] Got teleport packet while a whitelisted scenario ! Ignoring that one.");
            return;
        }

        S08PacketPlayerPosLook packet = (S08PacketPlayerPosLook) event.packet;
        Vec3 currentPlayerPos = mc.thePlayer.getPositionVector();
        Vec3 packetPlayerPos = new Vec3(packet.getX(), packet.getY(), packet.getZ());

        if (packet.getY() >= 90 || BlockUtils.bedrockCount() > 2) {
            LogUtils.sendDebug("[Failsafe] Most likely a bedrock check! Will check in a moment to be sure.");
            return;
        }

        rotationBeforeReacting = new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        double distance = currentPlayerPos.distanceTo(packetPlayerPos);
        LogUtils.sendDebug("[Failsafe] Teleport 2 detected! Distance: " + distance);
        if (distance >= Config.teleportCheckSensitivity) {
            LogUtils.sendDebug("[Failsafe] Teleport detected! Distance: " + distance);
            final double lastReceivedPacketDistance = currentPlayerPos.distanceTo(LagDetector.getInstance().getLastPacketPosition());
            // blocks per tick
            final double playerMovementSpeed = mc.thePlayer.getAttributeMap().getAttributeInstanceByName("generic.movementSpeed").getAttributeValue();
            final int ticksSinceLastPacket = (int) Math.ceil(LagDetector.getInstance().getTimeSinceLastTick() / 50D);
            final double estimatedMovement = playerMovementSpeed * ticksSinceLastPacket;
            if (lastReceivedPacketDistance > 7.5D && Math.abs(lastReceivedPacketDistance - estimatedMovement) < Config.teleportCheckLagSensitivity)
                return;
            FailsafeManager.getInstance().possibleDetection(this);
        }
    }

    @Override
    public void duringFailsafeTrigger() {
        if (mc.currentScreen != null) {
            PlayerUtils.closeScreen();
            // just in case something in the hand keeps opening the screen
            if (FailsafeManager.getInstance().swapItemDuringRecording && mc.thePlayer.inventory.currentItem > 1)
                FailsafeManager.getInstance().selectNextItemSlot();
            return;
        }
        switch (teleportCheckState) {
            case NONE:
                teleportCheckState = TeleportCheckState.WAIT_BEFORE_START;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 500);
                break;
            case WAIT_BEFORE_START:
                MacroHandler.getInstance().pauseMacro();
                if (rotationBeforeReacting == null)
                    rotationBeforeReacting = new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
                if (positionBeforeReacting == null)
                    positionBeforeReacting = mc.thePlayer.getPosition();
                MovRecPlayer.setYawDifference(AngleUtils.getClosest(rotationBeforeReacting.getYaw()));
                teleportCheckState = TeleportCheckState.LOOK_AROUND;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 500);
                break;
            case LOOK_AROUND:
                MovRecPlayer.getInstance().playRandomRecording("TELEPORT_CHECK_Start_");
                teleportCheckState = TeleportCheckState.WAIT_BEFORE_SENDING_MESSAGE_1;
                FailsafeManager.getInstance().scheduleRandomDelay(2000, 3000);
                break;
            case WAIT_BEFORE_SENDING_MESSAGE_1:
                if (MovRecPlayer.getInstance().isRunning())
                    break;
                if (!Config.sendFailsafeMessage) {
                    teleportCheckState = TeleportCheckState.ROTATE_TO_POS_BEFORE;
                    FailsafeManager.getInstance().scheduleRandomDelay(300, 600);
                    break;
                }
                if (CustomFailsafeMessagesPage.customTeleportationMessages.isEmpty()) {
                    randomMessage = FailsafeManager.getRandomMessage();
                } else {
                    String[] customMessages = CustomFailsafeMessagesPage.customTeleportationMessages.split("\\|");
                    randomMessage = FailsafeManager.getRandomMessage(customMessages);
                }
                teleportCheckState = TeleportCheckState.SEND_MESSAGE;
                FailsafeManager.getInstance().scheduleRandomDelay(2000, 3000);
                break;
            case SEND_MESSAGE:
                LogUtils.sendDebug("[Failsafe] Chosen message: " + randomMessage);
                mc.thePlayer.sendChatMessage("/ac " + randomMessage);
                teleportCheckState = TeleportCheckState.ROTATE_TO_POS_BEFORE;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                break;
            case ROTATE_TO_POS_BEFORE:
                rotation.easeTo(new RotationConfiguration(new Rotation(rotationBeforeReacting.getYaw(), rotationBeforeReacting.getPitch()),
                        500, null));
                teleportCheckState = TeleportCheckState.LOOK_AROUND_2;
                FailsafeManager.getInstance().swapItemDuringRecording = Math.random() < 0.2;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                break;
            case LOOK_AROUND_2:
                if (rotation.isRotating())
                    break;
                if (Math.random() < 0.2) {
                    if (Math.random() > 0.4)
                        teleportCheckState = TeleportCheckState.WAIT_BEFORE_SENDING_MESSAGE_2;
                    else
                        teleportCheckState = TeleportCheckState.GO_BACK_START;
                    FailsafeManager.getInstance().scheduleRandomDelay(2000, 3000);
                    break;
                } else if (mc.thePlayer.getActivePotionEffects() != null
                        && mc.thePlayer.getActivePotionEffects().stream().anyMatch(potionEffect -> potionEffect.getPotionID() == 8)
                        && Math.random() < 0.2) {
                    MovRecPlayer.getInstance().playRandomRecording("TELEPORT_CHECK_JumpBoost_");
                } else if (mc.thePlayer.capabilities.allowFlying && BlockUtils.isAboveHeadClear() && Math.random() < 0.3) {
                    MovRecPlayer.getInstance().playRandomRecording("TELEPORT_CHECK_Fly_");
                } else {
                    MovRecPlayer.getInstance().playRandomRecording("TELEPORT_CHECK_OnGround_");
                }
                FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                break;
            case WAIT_BEFORE_SENDING_MESSAGE_2:
                if (MovRecPlayer.getInstance().isRunning())
                    break;
                if (!Config.sendFailsafeMessage) {
                    teleportCheckState = TeleportCheckState.GO_BACK_START;
                    FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                    break;
                }
                if (CustomFailsafeMessagesPage.customContinueMessages.isEmpty()) {
                    randomContinueMessage = FailsafeManager.getRandomContinueMessage();
                } else {
                    String[] customContinueMessages = CustomFailsafeMessagesPage.customContinueMessages.split("\\|");
                    randomContinueMessage = FailsafeManager.getRandomMessage(customContinueMessages);
                }
                teleportCheckState = TeleportCheckState.SEND_MESSAGE_2;
                FailsafeManager.getInstance().scheduleRandomDelay(3500, 2500);
                break;
            case SEND_MESSAGE_2:
                LogUtils.sendDebug("[Failsafe] Chosen message: " + randomContinueMessage);
                mc.thePlayer.sendChatMessage("/ac " + randomContinueMessage);
                teleportCheckState = TeleportCheckState.GO_BACK_START;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                break;
            case GO_BACK_START:
                if (MovRecPlayer.getInstance().isRunning())
                    break;
                if (FailsafeManager.getInstance().swapItemDuringRecording)
                    FailsafeManager.getInstance().swapItemDuringRecording = false;
                if (mc.thePlayer.getPosition().distanceSq(positionBeforeReacting) < 2) {
                    teleportCheckState = TeleportCheckState.ROTATE_TO_POS_BEFORE_2;
                    break;
                }
                if (mc.thePlayer.getPosition().distanceSq(positionBeforeReacting) < 10) {
                    BaritoneHandler.walkToBlockPos(positionBeforeReacting);
                    teleportCheckState = TeleportCheckState.GO_BACK_END;
                } else {
                    teleportCheckState = TeleportCheckState.WARP_GARDEN;
                }
                break;
            case GO_BACK_END:
                if (BaritoneHandler.hasFailed() || !BaritoneHandler.isWalkingToGoalBlock()) {
                    teleportCheckState = TeleportCheckState.ROTATE_TO_POS_BEFORE_2;
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
                teleportCheckState = TeleportCheckState.END;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                break;
            case WARP_GARDEN:
                if (mc.thePlayer.getPosition().distanceSq(new BlockPos(PlayerUtils.getSpawnLocation())) < 3) {
                    teleportCheckState = TeleportCheckState.ROTATE_TO_POS_BEFORE_2;
                    break;
                }
                MacroHandler.getInstance().getCurrentMacro().ifPresent(cm -> cm.triggerWarpGarden(true));
                FailsafeManager.getInstance().scheduleRandomDelay(3000, 1000);
                break;
            case END:
                this.endOfFailsafeTrigger();
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
        teleportCheckState = TeleportCheckState.NONE;
        rotationBeforeReacting = null;
        positionBeforeReacting = null;
        randomMessage = null;
        randomContinueMessage = null;
        rotation.reset();
    }

    private TeleportCheckState teleportCheckState = TeleportCheckState.NONE;
    private final RotationHandler rotation = RotationHandler.getInstance();
    private BlockPos positionBeforeReacting = null;
    private Rotation rotationBeforeReacting = null;
    String randomMessage;
    String randomContinueMessage;

    enum TeleportCheckState {
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
        WARP_GARDEN,
        END
    }
}
