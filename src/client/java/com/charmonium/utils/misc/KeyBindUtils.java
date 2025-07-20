package com.charmonium.utils.misc;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import java.util.*;

public class KeyBindUtils {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static final List<KeyBinding> movementKeys = List.of(
            mc.options.forwardKey,
            mc.options.backKey,
            mc.options.leftKey,
            mc.options.rightKey,
            mc.options.jumpKey,
            mc.options.sneakKey,
            mc.options.sprintKey,
            mc.options.attackKey,
            mc.options.useKey
    );

    public static void setKeyBindState(KeyBinding key, boolean pressed) {
        if (key == null) return;

        if (pressed && mc.currentScreen != null) {
            key.setPressed(false);
            return;
        }
        key.setPressed(pressed);
    }

    public static void stopMovement() {
        stopMovement(false);
    }

    public static void stopMovement(boolean ignoreAttack) {
        movementKeys.forEach(key -> {
            if (key == mc.options.attackKey && ignoreAttack) return;
            setKeyBindState(key, false);
        });
    }

    public static void holdThese(KeyBinding... keys) {
        releaseAllExcept(keys);
        Arrays.stream(keys).forEach(key -> setKeyBindState(key, true));
    }

    public static void releaseAllExcept(KeyBinding... keys) {
        movementKeys.forEach(key -> {
            if (!Arrays.asList(keys).contains(key)) {
                setKeyBindState(key, false);
            }
        });
    }

    public static boolean areMovementKeysReleased() {
        return movementKeys.stream()
                .noneMatch(KeyBinding::isPressed);
    }

    private static final Map<Integer, KeyBinding> DIRECTION_MAP = ImmutableMap.of(
            0, mc.options.forwardKey,
            90, mc.options.leftKey,
            180, mc.options.backKey,
            -90, mc.options.rightKey
    );

    public static Set<KeyBinding> getMovementDirections(Vec3d from, Vec3d to) {
        Set<KeyBinding> keys = new HashSet<>();
        double dx = to.x - from.x;
        double dz = to.z - from.z;

        float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float yawDiff = MathHelper.wrapDegrees(targetYaw - mc.player.getYaw());

        DIRECTION_MAP.forEach((angle, key) -> {
            if (Math.abs(angle - yawDiff) < 67.5 ||
                    Math.abs(angle - (yawDiff + 360)) < 67.5) {
                keys.add(key);
            }
        });

        return keys;
    }

    public static Set<KeyBinding> getPathfindingControls() {
        return Set.of(
                mc.options.forwardKey,
                mc.options.backKey,
                mc.options.leftKey,
                mc.options.rightKey,
                mc.options.jumpKey,
                mc.options.sneakKey
        );
    }
}
