package com.mylk.charmonium.util.charHelpers;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StringUtils;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class npcUtils {
    private static final Pattern healthPattern = Pattern.compile("(?:§8[§7Lv(\\d+)§8])?\\s*(?:§c)?(.+?)(?:§r)? §[ae]([\\dBMk]+)§c❤");
    private static final Pattern healthPattern2 = Pattern.compile("(?:§8[§7Lv(\\d+)§8])?\\s*(?:§c)?(.+?)(?:§r)? §[ae]([\\dBMk]+)§f/§[ae]([\\dBMk]+)§c❤");
    private static final Ordering<NetworkPlayerInfo> playerOrdering = Ordering.from(new PlayerComparator());
    private static Pattern pattern = Pattern.compile("§([0-9]|[a-z])");

    @SideOnly(Side.CLIENT)
    static class PlayerComparator implements Comparator<NetworkPlayerInfo> {
        private PlayerComparator() {}

        public int compare(NetworkPlayerInfo o1, NetworkPlayerInfo o2) {
            ScorePlayerTeam team1 = o1.getPlayerTeam();
            ScorePlayerTeam team2 = o2.getPlayerTeam();
            return ComparisonChain.start().compareTrueFirst(
                            o1.getGameType() != WorldSettings.GameType.SPECTATOR,
                            o2.getGameType() != WorldSettings.GameType.SPECTATOR
                    )
                    .compare(
                            team1 != null ? team1.getRegisteredName() : "",
                            team2 != null ? team2.getRegisteredName() : ""
                    )
                    .compare(o1.getGameProfile().getName(), o2.getGameProfile().getName()).result();
        }
    }

    public static List<String> getTabListPlayersUnprocessed() {
        List<NetworkPlayerInfo> players =
                playerOrdering.sortedCopy(Minecraft.getMinecraft().thePlayer.sendQueue.getPlayerInfoMap());

        List<String> result = new ArrayList<>();

        for (NetworkPlayerInfo info : players) {
            String name = Minecraft.getMinecraft().ingameGUI.getTabList().getPlayerName(info);
            result.add(name);
        }
        return result;
    }
    public static List<String> getTabListPlayersSkyblock() {
        List<String> tabListPlayersFormatted = getTabListPlayersUnprocessed();
        List<String> playerList = new ArrayList<>();
        tabListPlayersFormatted.remove(0); // remove "Players (x)"
        String firstPlayer = null;
        for(String s : tabListPlayersFormatted) {
            int a = s.indexOf("]");
            if(a == -1) continue;
            if (s.length() < a + 2) continue;

            s = s.substring(a + 2).replaceAll("§([0-9]|[a-z])", "").replace("♲", "").trim();
            if(firstPlayer == null)
                firstPlayer = s;
            else if(s.equals(firstPlayer)) // it returns two copy of the player list for some reason
                break;
            playerList.add(s);
        }
        return playerList;
    }

    public static boolean isNpc(Entity entity) {
        if (!(entity instanceof EntityOtherPlayerMP)) {
            return false;
        }
        return !getTabListPlayersSkyblock().contains(entity.getName());
    }

    public static Entity getEntityCuttingOtherEntity(Entity e, Class<?> entityType) {
        List<Entity> possible = Minecraft.getMinecraft().theWorld.getEntitiesInAABBexcluding(e, e.getEntityBoundingBox().expand(0.3D, 2.0D, 0.3D), a -> {
            boolean flag1 = (!a.isDead && !a.equals(Minecraft.getMinecraft().thePlayer));
            boolean flag2 = !(a instanceof EntityArmorStand);
            boolean flag3 = !(a instanceof net.minecraft.entity.projectile.EntityFireball);
            boolean flag4 = !(a instanceof net.minecraft.entity.projectile.EntityFishHook);
            boolean flag5 = (entityType == null || entityType.isInstance(a));
            return flag1 && flag2 && flag3 && flag4 && flag5;
        });
        if (!possible.isEmpty())
            return Collections.min(possible, Comparator.comparing(e2 -> e2.getDistanceToEntity(e)));
        return null;
    }

    public static Entity getStandOfEntity(Entity e, Class<?> entityType) {
        List<Entity> possible = Minecraft.getMinecraft().theWorld.getEntitiesInAABBexcluding(e, e.getEntityBoundingBox().expand(0.3D, 2.0D, 0.3D), a -> {
            boolean flag1 = (!a.isDead && !a.equals(Minecraft.getMinecraft().thePlayer));
            boolean flag3 = !(a instanceof net.minecraft.entity.projectile.EntityFireball);
            boolean flag4 = !(a instanceof net.minecraft.entity.projectile.EntityFishHook);
            boolean flag5 = (entityType == null || entityType.isInstance(a));
            return flag1 && flag3 && flag4 && flag5;
        });
        if (!possible.isEmpty()) {
            return Collections.min(possible, Comparator.comparing(e2 -> e2.getDistanceToEntity(e)));
        }
        return null;
    }

    public static boolean entityIsTargeted(Entity entity) {
        int reach = 50;
        Minecraft mc = Minecraft.getMinecraft();
        Vec3 eyesPos = mc.thePlayer.getPositionEyes(1.0F);
        Vec3 lookVec = mc.thePlayer.getLook(1.0F);
        List<Entity> entityList = mc.theWorld.getEntitiesInAABBexcluding(mc.thePlayer, mc.thePlayer.getEntityBoundingBox().addCoord(lookVec.xCoord * reach, lookVec.yCoord * reach, lookVec.zCoord * reach).expand(1.0D, 1.0D, 1.0D), Entity::canBeCollidedWith);
        Entity entityMouseOver = null;
        for (Entity e : entityList) {
            AxisAlignedBB entityBoundingBox = e.getEntityBoundingBox().expand(-0.3D, -0.3D, -0.3D);
            MovingObjectPosition movingObjectPosition = entityBoundingBox.calculateIntercept(eyesPos, eyesPos.addVector(lookVec.xCoord * reach, lookVec.yCoord * reach, lookVec.zCoord * reach));
            if (movingObjectPosition != null) {
                double distanceToEntity = eyesPos.distanceTo(movingObjectPosition.hitVec);
                if (distanceToEntity < reach) {
                    entityMouseOver = e;
                    reach = (int) distanceToEntity;
                }
            }
        }
        return entityMouseOver != null && entityMouseOver.equals(entity);
    }

    public static Entity getEntityLookingAt(int reach) {
        Minecraft mc = Minecraft.getMinecraft();
        Vec3 eyesPos = mc.thePlayer.getPositionEyes(1.0F);
        Vec3 lookVec = mc.thePlayer.getLook(1.0F);
        List<Entity> entityList = mc.theWorld.getEntitiesInAABBexcluding(mc.thePlayer,
                mc.thePlayer.getEntityBoundingBox().addCoord(lookVec.xCoord * reach,
                        lookVec.yCoord * reach,
                        lookVec.zCoord * reach).expand(1.0D, 1.0D, 1.0D),
                Entity::canBeCollidedWith);
        Entity entityMouseOver = null;
        for (Entity e : entityList) {
            AxisAlignedBB entityBoundingBox = e.getEntityBoundingBox().expand(0.3D, 0.3D, 0.3D);
            MovingObjectPosition movingObjectPosition = entityBoundingBox.calculateIntercept(eyesPos, eyesPos.addVector(lookVec.xCoord * reach, lookVec.yCoord * reach, lookVec.zCoord * reach));
            if (movingObjectPosition != null) {
                double distanceToEntity = eyesPos.distanceTo(movingObjectPosition.hitVec);
                if (distanceToEntity < reach) {
                    entityMouseOver = e;
                    reach = (int) distanceToEntity;
                }
            }
        }
        return entityMouseOver;
    }

    public static Entity getStandOfEntityLookingAt(int reach) {
        Minecraft mc = Minecraft.getMinecraft();
        Vec3 eyesPos = mc.thePlayer.getPositionEyes(1.0F);
        Vec3 lookVec = mc.thePlayer.getLook(1.0F);
        List<Entity> entityList = mc.theWorld.getEntitiesInAABBexcluding(mc.thePlayer,
                mc.thePlayer.getEntityBoundingBox().addCoord(lookVec.xCoord * reach,
                        lookVec.yCoord * reach,
                        lookVec.zCoord * reach).expand(1.0D, 1.0D, 1.0D),
                Entity::canBeCollidedWith);
        Entity entityMouseOver = null;
        for (Entity e : entityList) {
            AxisAlignedBB entityBoundingBox = e.getEntityBoundingBox().expand(0.3D, 0.3D, 0.3D);
            MovingObjectPosition movingObjectPosition = entityBoundingBox.calculateIntercept(eyesPos, eyesPos.addVector(lookVec.xCoord * reach, lookVec.yCoord * reach, lookVec.zCoord * reach));
            if (movingObjectPosition != null) {
                double distanceToEntity = eyesPos.distanceTo(movingObjectPosition.hitVec);
                if (distanceToEntity < reach) {
                    entityMouseOver = e;
                    reach = (int) distanceToEntity;
                }
            }
        }
        return entityMouseOver != null ? getStandOfEntity(entityMouseOver, EntityArmorStand.class) : null;
    }

    public static String stripString(String s) {
        char[] nonValidatedString = StringUtils.stripControlCodes(s).toCharArray();
        StringBuilder validated = new StringBuilder();
        for (char a : nonValidatedString) {
            if (a < '' && a > '\024')
                validated.append(a);
        }
        return validated.toString();
    }

    public static int getEntityHp(Entity entity) {
        if (entity instanceof EntityArmorStand || entity instanceof EntityOtherPlayerMP) {
            String name = entity.getCustomNameTag();

            // Use a regex pattern to match the health information
            Pattern healthPattern = Pattern.compile("§a(\\d{1,3}(?:,\\d{3})?(?:\\.\\d+)?)§f/§a(\\d{1,3}(?:,\\d{3})?(?:\\.\\d+)?)§c❤");
            Matcher matcher = healthPattern.matcher(name);

            if (matcher.find()) {
                String hpSubstring = matcher.group(1);

                if (!hpSubstring.isEmpty()) {
                    return (int) parseHealthValue(hpSubstring);
                }
            }
        } else if (entity instanceof EntityLivingBase) {
            return (int) ((EntityLivingBase) entity).getHealth();
        }
        return -1;
    }

    private static double parseHealthValue(String hpSubstring) {
        try {
            if (hpSubstring.endsWith(".")) {
                hpSubstring += "0";
            }

            return Double.parseDouble(hpSubstring.replace(",", ""));
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return -1;
        }
    }



//    public static int getEntityHp(Entity entity) {
//        if (entity instanceof EntityArmorStand || entity instanceof EntityOtherPlayerMP) {
//            String name = entity.getCustomNameTag();
//            String nName = entity.getName();
//            Charmonium.sendMessage("C Name: " + name + "N Name: " + nName);
//            Charmonium.sendMessage("C Cont: " + name.contains("❤") + "N Cont: " + nName.contains("❤"));
//            if (name.contains("❤")) {
//                Matcher matcher = healthPattern.matcher(name);
//                Matcher matcher2 = healthPattern2.matcher(name);
//                Charmonium.sendMessage("1 Match: " + matcher.matches() + "2 Match: " + matcher2.matches());
//                if (matcher.matches() || matcher2.matches()) {
//                    String hp = matcher.matches() ? matcher.group(2) : matcher2.group(2);
//                    Charmonium.sendMessage("HP: " + hp);
//                    int modifer = 1;
//                    if (name.contains("k§c❤")) {
//                        modifer = 1000;
//                    } else if (name.contains("M§c❤")) {
//                        modifer = 1000000;
//                    } else if (name.contains("B§c❤")) {
//                        modifer = 1000000000;
//                    }
//                    return (int) (Double.parseDouble(hp.replace("k", "").replace("M", "").replace("B", "")) * modifer);
//                }
//            }
//        } else if (entity instanceof EntityLivingBase) {
//            return (int) ((EntityLivingBase) entity).getHealth();
//        }
//        return -1;
//    }

    @SuppressWarnings("unused")
    private static final String[] npcNames = {
            "Golden Goblin",
            "Goblin",
            "Weakling",
            "Fireslinger",
            "Executive Viper",
            "Grunt",
            "Eliza",
            "Fraiser",
            "Wilson",
            "Ceanna",
            "Carlton",
            "Treasure Hoarder",
            "Team Treasuire",
            "Star Centry"
    };
}
