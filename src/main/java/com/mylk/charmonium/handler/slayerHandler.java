package com.mylk.charmonium.handler;

import com.google.common.base.Predicate;
import com.mylk.charmonium.Charmonium;
import com.mylk.charmonium.config.Config;
import com.mylk.charmonium.event.RenderLivingEntityEvent;
import com.mylk.charmonium.util.ScoreboardUtils;
import com.mylk.charmonium.util.charHelpers.npcUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntityBlaze;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.util.StringUtils;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class slayerHandler {
    public static Entity currentSlayerBoss;
    public static Entity currentMiniBoss;
    public static final HashSet<Entity> checked = new HashSet<>();
    public static final HashSet<Entity> checkedMini = new HashSet<>();
    private static slayerHandler instance;

    public static slayerHandler getInstance() {
        if (instance == null) {
            instance = new slayerHandler();
        }
        return instance;
    }
    private static final HashMap<String, Predicate<? super Entity>> bosses = new HashMap<String, Predicate<? super Entity>>() {{
        put("Revenant Horror", o -> o instanceof EntityZombie);
        put("Tarantula Broodfather", o -> o instanceof EntitySpider);
        put("Sven Packmaster", o -> o instanceof EntityWolf);
        put("Voidgloom Seraph", o -> o instanceof EntityEnderman);
        put("Inferno Demonlord", o -> o instanceof EntityBlaze);
    }};

    private static final HashMap<String, Predicate<? super Entity>> miniBosses = new HashMap<String, Predicate<? super Entity>>() {{
        put("Revenant Champion", o -> o instanceof EntityZombie);
        put("Revenant Sycophant", o -> o instanceof EntityZombie);
        put("Deformed Revenant", o -> o instanceof EntityZombie);
        put("Atoned Champion", o -> o instanceof EntityZombie);
        put("Atoned Revenant", o -> o instanceof EntityZombie);
        put("Tarantula Beast", o -> o instanceof EntitySpider);
        put("Mutant Tarantula", o -> o instanceof EntitySpider);
        put("Tarantula Vermin\t", o -> o instanceof EntitySpider);
        put("Sven Follower", o -> o instanceof EntityWolf);
        put("Sven Alpha", o -> o instanceof EntityWolf);
        put("Pack Enforcer", o -> o instanceof EntityWolf);
        put("Voidling Devotee", o -> o instanceof EntityEnderman);
        put("Voidling Radical", o -> o instanceof EntityEnderman);
        put("Voidcrazed Maniac", o -> o instanceof EntityEnderman);
    }};

    @SubscribeEvent
    public void onRenderEntityLiving(RenderLivingEntityEvent event) {
        if (Charmonium.mc.thePlayer == null) return;
        Entity entity = event.entity;
        if (checked.contains(entity)) return;
        List<String> scoreboard = ScoreboardUtils.getScoreboardLines();
        String name = StringUtils.stripControlCodes(entity.getCustomNameTag());

        if (ScoreboardUtils.scoreboardContains("Slay the boss", scoreboard)) {
            if (entity instanceof EntityArmorStand && entity.hasCustomName()) {
                if (name.equals("Spawned by: " + Charmonium.mc.thePlayer.getName())) {
                    for (Map.Entry<String, Predicate<? super Entity>> boss : bosses.entrySet()) {
                        if (ScoreboardUtils.scoreboardContains(boss.getKey(), scoreboard)) {
                            List<Entity> possibleSlayerBosses = entity.getEntityWorld().getEntitiesInAABBexcluding(entity, entity.getEntityBoundingBox().expand(0, 2, 0), boss.getValue());
                            if (!possibleSlayerBosses.isEmpty()) {
                                currentSlayerBoss = possibleSlayerBosses.get(0);
                                checked.add(currentSlayerBoss);
                            }
                        }
                    }
                }
            }
            checked.add(entity);
        } else {
            currentSlayerBoss = null;
        }

        if (checkedMini.contains(entity)) return;

        if (entity instanceof EntityArmorStand && entity.hasCustomName()) {
            for (Map.Entry<String, Predicate<? super Entity>> miniBoss : miniBosses.entrySet()) {
                if (name.contains(miniBoss.getKey())) {
                    List<Entity> possibleMiniBosses = entity.getEntityWorld().getEntitiesInAABBexcluding(entity, entity.getEntityBoundingBox().expand(0, 2, 0), miniBoss.getValue());
                    if (!possibleMiniBosses.isEmpty()) {
                        currentMiniBoss = possibleMiniBosses.get(0);
                        checkedMini.add(currentMiniBoss);

                    }
                }
            }
        }
        checkedMini.add(entity);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (Charmonium.mc.thePlayer == null) return;
        if (Charmonium.mc.thePlayer.ticksExisted % 20 == 0) {
            checked.clear();
            checkedMini.clear();
        }

        if (npcUtils.getEntityHp(currentMiniBoss) <= 1 || Minecraft.getMinecraft().thePlayer.getDistanceToEntity(currentMiniBoss) > Config.scanRange || (currentMiniBoss != null && (currentMiniBoss.isDead))) {
            currentMiniBoss = null;
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        checked.clear();
        checkedMini.clear();
    }
}
