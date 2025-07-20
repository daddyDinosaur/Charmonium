package com.charmonium.module;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;

import com.charmonium.CharmoniumClient;
import com.charmonium.interfaces.IClientPlayerInteractionManager;
import com.charmonium.managers.SettingManager;
import com.charmonium.mixin.interfaces.IMinecraftClient;
import com.charmonium.settings.Setting;
import com.charmonium.settings.types.BooleanSetting;
import com.charmonium.settings.types.KeybindSetting;
import com.charmonium.utils.FindItemResult;
import org.lwjgl.glfw.GLFW;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.InputUtil.Key;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public abstract class Module {
    private String name;
    private String description;
    private Category category;

    public final BooleanSetting state;
    public final KeybindSetting keyBind;
    private final List<Setting<?>> settings = new ArrayList<Setting<?>>();

    protected static final MinecraftClient MC = CharmoniumClient.mc;
    protected final IMinecraftClient IMC = CharmoniumClient.IMC;

    public Module(String name) {
        this(name, InputUtil.fromKeyCode(GLFW.GLFW_KEY_UNKNOWN, 0));
    }

    public Module(String name, Key keybind) {
        this.name = name;
        keyBind = KeybindSetting.builder().id("key." + name.toLowerCase()).displayName(name + " Key")
                .defaultValue(keybind).build();

        state = BooleanSetting.builder().id("state." + name.toLowerCase()).displayName(name + " State")
                .defaultValue(false).onUpdate(s -> {
                    if (s)
                        onEnable();
                    else
                        onDisable();
                    onToggle();
                }).build();

        addSetting(keyBind);
        addSetting(state);

        SettingManager.registerSetting(keyBind);
        SettingManager.registerSetting(state);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public KeybindSetting getBind() {
        return keyBind;
    }

    public void addSetting(Setting<?> setting) {
        settings.add(setting);
    }

    public void addSettings(Setting<?>... settings) {
        for (Setting<?> setting : settings) {
            addSetting(setting);
        }
    }

    public List<Setting<?>> getSettings() {
        return settings;
    }

    public boolean hasSettings() {
        return !settings.isEmpty();
    }

    public abstract void onDisable();

    public abstract void onEnable();

    public abstract void onToggle();

    public boolean isKeyPressed(int button) {
        if (button == -1)
            return false;

        if (button < 10) // check
            return false;

        return InputUtil.isKeyPressed(MC.getWindow().getHandle(), button);
    }

    public void toggle() {
        state.setValue(!state.getValue());
    }

    public String getStatus() {
        return state.getValue() ? "Enabled" : "Disabled";
    }

    public String getKeyBindDisplayName() {
        return keyBind.displayName;
    }

    public void resetSettings() {
        for (Setting<?> setting : settings) {
            setting.resetToDefault();
        }
    }

    public final boolean isCategory(Category category) {
        return category.equals(this.category);
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface ModInfo {
        String name();

        String description();

        String category();

        int bind();
    }

    public static int previousSlot = -1;

    public static FindItemResult findInHotbar(Item... items) {
        return findInHotbar(itemStack -> {
            for (Item item : items) {
                if (itemStack.getItem() == item)
                    return true;
            }
            return false;
        });
    }

    public static FindItemResult findInHotbar(Predicate<ItemStack> isGood) {
        if (testInOffHand(isGood)) {
            return new FindItemResult(45, MC.player.getOffHandStack().getCount());
        }

        if (testInMainHand(isGood)) {
            return new FindItemResult(MC.player.getInventory().getSelectedSlot(),
                    MC.player.getMainHandStack().getCount());
        }

        return find(isGood, 0, 8);
    }

    public static FindItemResult find(Predicate<ItemStack> isGood) {
        if (MC.player == null) {
            return new FindItemResult(0, 0);
        }

        return find(isGood, 0, MC.player.getInventory().size());
    }

    public static FindItemResult find(Predicate<ItemStack> isGood, int start, int end) {
        if (MC.player == null) {
            return new FindItemResult(0, 0);
        }

        int slot = -1;
        int count = 0;

        for (int i = start; i <= end; i++) {
            ItemStack stack = MC.player.getInventory().getStack(i);

            if (isGood.test(stack)) {
                if (slot == -1) {
                    slot = i;
                }
                count += stack.getCount();
            }
        }

        return new FindItemResult(slot, count);
    }

    public static FindItemResult findFastestTool(BlockState state) {
        float bestScore = 1;
        int slot = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = MC.player.getInventory().getStack(i);

            if (stack.isSuitableFor(state)) {
                float score = stack.getMiningSpeedMultiplier(state);

                if (score > bestScore) {
                    bestScore = score;
                    slot = i;
                }
            }
        }

        return new FindItemResult(slot, 1);
    }

    public static boolean testInMainHand(Predicate<ItemStack> predicate) {
        return predicate.test(MC.player.getMainHandStack());
    }

    public static boolean testInOffHand(Predicate<ItemStack> predicate) {
        return predicate.test(MC.player.getOffHandStack());
    }

    public static boolean swap(int slot, boolean swapBack) {
        if (slot == 45) {
            return true;
        }

        if (slot < 0 || slot > 8) {
            return false;
        }

        if (swapBack) {
            if (previousSlot == -1) {
                previousSlot = MC.player.getInventory().getSelectedSlot();
            }
        } else {
            previousSlot = -1;
        }

        MC.player.getInventory().setSelectedSlot(slot);
        ((IClientPlayerInteractionManager) MC.interactionManager).char$syncSelected();
        return true;
    }

    public static boolean swapBack() {
        if (previousSlot == -1) {
            return false;
        }

        boolean result = swap(previousSlot, false);
        previousSlot = -1;
        return result;
    }

    public static FindItemResult find(Item... items) {
        return find(itemStack -> {
            for (Item item : items) {
                if (itemStack.getItem() == item)
                    return true;
            }
            return false;
        });
    }

    public static void rotatePitch(float degrees) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;

        if (player != null) {
            float currentPitch = player.getPitch();
            float newPitch = currentPitch + degrees;

            newPitch = Math.max(-90.0F, Math.min(90.0F, newPitch));

            player.setPitch(newPitch);

            client.getNetworkHandler().sendPacket(
                    new PlayerMoveC2SPacket.LookAndOnGround(player.getYaw(), newPitch, player.isOnGround(), false));
        }
    }
}
