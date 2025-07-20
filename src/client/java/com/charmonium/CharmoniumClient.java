package com.charmonium;

import com.charmonium.managers.*;
import com.charmonium.mixin.interfaces.IMinecraftClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.sql.Driver;

public class CharmoniumClient implements ClientModInitializer {
	public static MinecraftClient mc;
	public static IMinecraftClient IMC;

	public CommandManager commandManager;
	public ModuleManager moduleManager;
	public EventManager eventManager;
	public SettingManager settingManager;
	public RotationManager rotationManager;

	public void Initialize() {
		mc = MinecraftClient.getInstance();
		IMC = (IMinecraftClient) mc;
	}

	public void loadAssets() {
		eventManager = new EventManager();
		moduleManager = new ModuleManager();
		settingManager = new SettingManager();
		commandManager = new CommandManager();
		rotationManager = new RotationManager();
	}

	public static void sendMessage(String message) {
		if (mc.player != null) {
			Text styledMessage = Text.literal("[")
					.styled(style -> style.withColor(Formatting.GRAY))
					.append(Text.literal("Charmonium")
							.styled(style -> style.withColor(Formatting.LIGHT_PURPLE)))
					.append(Text.literal("] ")
							.styled(style -> style.withColor(Formatting.GRAY)))
					.append(Text.literal(message));

			mc.player.sendMessage(styledMessage, false);
		}
	}


	@Override
	public void onInitializeClient() {
		commandManager = new CommandManager();
	}
}