package com.mylk.charmonium.feature.impl.charMods;

import com.mylk.charmonium.Charmonium;
import com.mylk.charmonium.handler.GameStateHandler;
import com.mylk.charmonium.util.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class autoCrystal {
    private static autoCrystal instance;
    public static autoCrystal getInstance() {
        if (instance == null) {
            instance = new autoCrystal();
        }
        return instance;
    }
    private final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!Objects.equals(GameStateHandler.getPhase(), "P1")) return;
        List<Entity> armorStands = mc.theWorld != null ? mc.theWorld.loadedEntityList.stream()
                .filter(EntityArmorStand.class::isInstance)
                .map(EntityArmorStand.class::cast)
                .filter(it -> {
                    ItemStack itemStack = it.getInventory()[4];
                    LogUtils.sendDebug("Inventory: " + Arrays.toString(it.getInventory()));
                    return itemStack != null && itemStack.getDisplayName().contains("Crystal");
                })
                .collect(Collectors.toList()) : Collections.emptyList();

        for (Entity armorStand : armorStands) {
            if (mc.thePlayer.getDistanceToEntity(armorStand) > 4) continue;
            interactWithEntity(armorStand);
        }
    }

    private void interactWithEntity(Entity entity) {
        MovingObjectPosition objectMouseOver = mc.objectMouseOver;
        if (objectMouseOver != null) {
            double dx = objectMouseOver.hitVec.xCoord - entity.posX;
            double dy = objectMouseOver.hitVec.yCoord - entity.posY;
            double dz = objectMouseOver.hitVec.zCoord - entity.posZ;
            mc.getNetHandler().addToSendQueue(new C02PacketUseEntity(entity, new Vec3(dx, dy, dz)));
        }
    }

}
