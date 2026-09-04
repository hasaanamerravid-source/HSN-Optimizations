package hsn.modod.client;

import com.mojang.blaze3d.platform.InputConstants;
import hsn.modod.HSNOptimizations;
import hsn.modod.config.HSNConfig;
import hsn.modod.config.HSNPresets;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Remappable keys (Controls menu). Defaults: F6 perf, F7 overlay, F8 ULTRA_LOW, F9 SAFE.
 */
public final class HSNKeybinds {

	private static KeyMapping perfKey;
	private static KeyMapping overlayKey;
	private static KeyMapping ultraKey;
	private static KeyMapping safeKey;

	private HSNKeybinds() {
	}

	public static void register() {
		KeyMapping.Category category = KeyMapping.Category.register(
				Identifier.fromNamespaceAndPath(HSNOptimizations.MOD_ID, "keys"));

		perfKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.hsn-optimizations.performance",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_F6,
				category));
		overlayKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.hsn-optimizations.overlay",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_F7,
				category));
		ultraKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.hsn-optimizations.ultra",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_F8,
				category));
		safeKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.hsn-optimizations.safe",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_F9,
				category));

		ClientTickEvents.END_CLIENT_TICK.register(HSNKeybinds::onTick);
		HSNOptimizations.LOGGER.info("HSN keys registered (remappable): F6=Performance, F7=FPS overlay, F8=ULTRA_LOW, F9=SAFE");
	}

	private static void onTick(Minecraft client) {
		if (client == null) {
			return;
		}
		while (perfKey.consumeClick()) {
			HSNConfig cfg = HSNConfig.get();
			cfg.performanceModeEnabled = !cfg.performanceModeEnabled;
			cfg.save();
			msg(client, "HSN Performance Mode: " + (cfg.performanceModeEnabled ? "ON" : "OFF"));
		}
		while (overlayKey.consumeClick()) {
			HSNConfig cfg = HSNConfig.get();
			cfg.fpsOverlayEnabled = !cfg.fpsOverlayEnabled;
			cfg.save();
			msg(client, "HSN FPS overlay: " + (cfg.fpsOverlayEnabled ? "ON" : "OFF"));
		}
		while (ultraKey.consumeClick()) {
			HSNConfig cfg = HSNConfig.get();
			HSNPresets.apply(cfg, HSNConfig.Preset.ULTRA_LOW);
			cfg.lastAppliedPreset = HSNConfig.Preset.ULTRA_LOW;
			cfg.save();
			msg(client, "HSN: applied ULTRA_LOW preset");
		}
		while (safeKey.consumeClick()) {
			HSNConfig cfg = HSNConfig.get();
			HSNPresets.apply(cfg, HSNConfig.Preset.SAFE);
			cfg.lastAppliedPreset = HSNConfig.Preset.SAFE;
			cfg.save();
			msg(client, "HSN: applied SAFE preset");
		}
	}

	private static void msg(Minecraft client, String text) {
		HSNOptimizations.LOGGER.info(text);
		if (client.player == null) {
			return;
		}
		client.player.sendSystemMessage(Component.literal(text));
	}
}
