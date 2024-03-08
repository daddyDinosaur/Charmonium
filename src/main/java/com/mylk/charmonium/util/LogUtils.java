package com.mylk.charmonium.util;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.mylk.charmonium.config.Config;
import com.mylk.charmonium.config.struct.DiscordWebhook;
import com.mylk.charmonium.handler.GameStateHandler;
import com.mylk.charmonium.handler.MacroHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.StringUtils;
import net.minecraft.util.Tuple;

import java.awt.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class LogUtils {
    private static final long logMsgTime = 1000;
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static String lastDebugMessage;
    private static String lastWebhook;
    private static long statusMsgTime = -1;

    public synchronized static void sendLog(ChatComponentText chat) {
        if (mc.thePlayer != null && !Config.hideLogs)
            mc.thePlayer.addChatMessage(chat);
        else if (mc.thePlayer == null)
            System.out.println("[Charmonium] " + chat.getUnformattedText());
    }

    public static void sendSuccess(String message) {
        sendLog(new ChatComponentText("§2§lCharmonium §8» §a" + message));
    }

    public static void sendWarning(String message) {
        sendLog(new ChatComponentText("§6§lCharmonium §8» §e" + message));
    }

    public static void sendError(String message) {
        sendLog(new ChatComponentText("§4§lCharmonium §8» §c" + message));
    }

    public static void sendDebug(String message) {
        if (lastDebugMessage != null && lastDebugMessage.equals(message)) return;
        if (Config.debugMode && mc.thePlayer != null)
            sendLog(new ChatComponentText("§3§lCharmonium §8» §7" + message));
        else
            System.out.println("[Charmonium] " + message);
        lastDebugMessage = message;
    }

    public static void sendDebugRotation(String message) {
        if (Config.showRotationDebugMessages)
            sendDebug(message);
    }

    public static void sendFailsafeMessage(String message) {
        sendFailsafeMessage(message, false);
    }

    public static void sendFailsafeMessage(String message, boolean pingAll) {
        sendLog(new ChatComponentText("§5§lCharmonium §8» §d" + message));
        webhookLog(StringUtils.stripControlCodes(message), pingAll);
    }

    public static String getRuntimeFormat() {
        if (!MacroHandler.getInstance().getMacroingTimer().isScheduled())
            return "0h 0m 0s";
        long millis = MacroHandler.getInstance().getMacroingTimer().getElapsedTime();
        return formatTime(millis) + (MacroHandler.getInstance().getMacroingTimer().paused ? " (Paused)" : "");
    }

    public static String formatTime(long millis) {

        if (TimeUnit.MILLISECONDS.toHours(millis) > 0) {
            return String.format("%dh %dm %ds",
                    TimeUnit.MILLISECONDS.toHours(millis),
                    TimeUnit.MILLISECONDS.toMinutes(millis) -
                            TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                    TimeUnit.MILLISECONDS.toSeconds(millis) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
            );
        } else if (TimeUnit.MILLISECONDS.toMinutes(millis) > 0) {
            return String.format("%dm %ds",
                    TimeUnit.MILLISECONDS.toMinutes(millis),
                    TimeUnit.MILLISECONDS.toSeconds(millis) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
            );
        } else {
            return TimeUnit.MILLISECONDS.toSeconds(millis) + "." + (TimeUnit.MILLISECONDS.toMillis(millis) -
                    TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(millis))) / 100 + "s";
        }
    }

    public static String capitalize(String message) {
        String[] words = message.split("_|\\s");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            sb.append(word.substring(0, 1).toUpperCase()).append(word.substring(1).toLowerCase()).append(" ");
        }
        return sb.toString().trim();
    }

    public static void webhookStatus() {
        if (!Config.enableWebHook) return;

        if (statusMsgTime == -1) {
            statusMsgTime = System.currentTimeMillis();
        }
        long timeDiff = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - statusMsgTime);
        if (timeDiff >= Config.statusUpdateInterval && Config.sendStatusUpdates) {
            DiscordWebhook webhook = new DiscordWebhook(Config.webHookURL.replace(" ", "").replace("\n", "").trim());
            String randomColor = String.format("#%06x", (int) (Math.random() * 0xFFFFFF));
            webhook.setUsername("Charmonium");
            webhook.setAvatarUrl("https://cdn.discordapp.com/attachments/1152966451406327858/1160577992876109884/icon.png");
            webhook.addEmbed(new DiscordWebhook.EmbedObject()
                    .setTitle("Charmonium")
                    .setAuthor("Instance name -> " + mc.getSession().getUsername(), "https://crafatar.com/avatars/" + mc.getSession().getPlayerID(), "https://crafatar.com/avatars/" + mc.getSession().getPlayerID())
                    .setDescription("## I'm still alive!")
                    .setColor(Color.decode(randomColor))
                    .setFooter("Charmonium Webhook Status", "https://cdn.discordapp.com/attachments/861700235890130986/1144673641951395982/icon.png")
                    .setThumbnail("https://crafatar.com/renders/body/" + mc.getSession().getPlayerID())
                    .addField("Username", mc.getSession().getUsername(), false)
                    .addField("Runtime", getRuntimeFormat(), false)
                    .addField("Crop Type", capitalize(String.valueOf(MacroHandler.getInstance().getCrop())), false)
                    .addField("Location", capitalize(GameStateHandler.getInstance().getLocation().getName()), false)
            );
            Multithreading.schedule(() -> {
                try {
                    webhook.execute();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, 0, TimeUnit.MILLISECONDS);
            statusMsgTime = System.currentTimeMillis();
        }
    }

    public static void webhookLog(String message) {
        webhookLog(message, false);
    }

    @SafeVarargs
    public static void webhookLog(String message, boolean mentionAll, Tuple<String, String>... fields) {
        if (!Config.enableWebHook) return;
        if (!Config.sendLogs) return;

        DiscordWebhook webhook = new DiscordWebhook(Config.webHookURL.replace(" ", "").replace("\n", "").trim());
        webhook.setUsername("Charmonium");
        webhook.setAvatarUrl("https://cdn.discordapp.com/attachments/1152966451406327858/1160577992876109884/icon.png");
        String randomColor = String.format("#%06x", (int) (Math.random() * 0xFFFFFF));
        if (mentionAll) {
            webhook.setContent("@everyone");
        }
        DiscordWebhook.EmbedObject embedObject = new DiscordWebhook.EmbedObject()
                .setTitle("Charmonium")
                .setThumbnail("https://crafatar.com/renders/body/" + mc.getSession().getPlayerID())
                .setDescription("### " + message)
                .setColor(Color.decode(randomColor))
                .setAuthor("Instance name -> " + mc.getSession().getUsername(), "https://crafatar.com/avatars/" + mc.getSession().getPlayerID(), "https://crafatar.com/avatars/" + mc.getSession().getPlayerID())
                .setFooter("Charmonium Webhook Status", "https://cdn.discordapp.com/attachments/861700235890130986/1144673641951395982/icon.png");
        for (Tuple<String, String> field : fields) {
            embedObject.addField(field.getFirst(), field.getSecond(), false);
        }
        webhook.addEmbed(embedObject);
        Multithreading.schedule(() -> {
            try {
                webhook.execute();
            } catch (IOException e) {
                LogUtils.sendError("[Webhook Log] Error: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }, 0, TimeUnit.MILLISECONDS);
    }
}
