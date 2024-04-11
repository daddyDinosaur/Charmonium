package com.mylk.charmonium.feature.impl.charMods;

import com.mojang.realmsclient.gui.ChatFormatting;
import com.mylk.charmonium.Charmonium;
import com.mylk.charmonium.config.Config;
import com.mylk.charmonium.handler.GameStateHandler;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class replaceDiorite {
    private static boolean shouldDiorite;
    private static replaceDiorite instance;
    public static replaceDiorite getInstance() {
        if (instance == null) {
            instance = new replaceDiorite();
        }
        return instance;
    }
    private final List<List<Integer>> pillars = new ArrayList<>();
    private final List<BlockPos> coordinates = new ArrayList<>();

    public void dioriteSetup() {
        pillars.add(Arrays.asList(46, 169, 41));
        pillars.add(Arrays.asList(46, 169, 65));
        pillars.add(Arrays.asList(100, 169, 65));
        pillars.add(Arrays.asList(100, 179, 41));

        for (List<Integer> pillar : pillars) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dy = 0; dy <= 37; dy++) {
                    for (int dz = -3; dz <= 3; dz++) {
                        coordinates.add(new BlockPos(pillar.get(0) + dx, pillar.get(1) + dy, pillar.get(2) + dz));
                    }
                }
            }
        }
    }

    public void dioriteReplacer() {
        for (BlockPos pos : coordinates) {
            if (isDiorite(pos)) {
                setGlass(pos);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onMessage(ClientChatReceivedEvent event) {
        if (!Config.replaceDiorite) {
            return;
        }

        String message = ChatFormatting.stripFormatting(event.message.getUnformattedText());
        if (message.equals("[BOSS] Storm: Pathetic Maxor, just like expected.")) {
            Charmonium.sendMessage("Replacing Diorite");
            dioriteSetup();
            shouldDiorite = true;
        }

        if (message.equals("[BOSS] Storm: At least my son died by your hands")) {
            Charmonium.sendMessage("Stopping Diorite");
            shouldDiorite = false;
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!Config.replaceDiorite && shouldDiorite) return;
        if (event.phase == TickEvent.Phase.START) return;
        if (Charmonium.mc.thePlayer == null || Charmonium.mc.theWorld == null) return;

        if (GameStateHandler.dungeonFloor != 7) {
            shouldDiorite = false;
            return;
        }

        dioriteReplacer();
    }

//    @SubscribeEvent(priority = EventPriority.HIGHEST)
//    public void onChatPacket(ReceivePacketEvent event) {
//        if (!Config.replaceDiorite || !(event.packet instanceof S02PacketChat)) {
//            return;
//        }
//
//        S02PacketChat daPacket = (S02PacketChat) event.packet;
//        assert daPacket != null;
//        if (daPacket.getChatComponent().getUnformattedText().contains(":") || daPacket.getChatComponent().getUnformattedText().contains(">")) return;
//
//        String message = ChatFormatting.stripFormatting(event.message.getUnformattedText());
//        if (message.equals("[BOSS] Storm: Pathetic Maxor, just like expected.")) {
//            Charmonium.sendMessage("Replacing Diorite");
//            dioriteReplacer();
//        }
//    }

    private void setGlass(BlockPos pos) {
        Charmonium.mc.theWorld.setBlockState(pos, Blocks.glass.getDefaultState(), 3);
    }

    private boolean isDiorite(BlockPos pos) {
        return Charmonium.mc.theWorld.getChunkProvider().provideChunk(pos.getX() >> 4, pos.getZ() >> 4).getBlockState(pos).getBlock() == Blocks.stone;
    }
}
