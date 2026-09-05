package hsn.modod.client;

import com.mojang.blaze3d.platform.InputConstants;
import hsn.modod.HSNOptimizations;
import hsn.modod.client.compat.ClientScreens;
import hsn.modod.client.config.HSNConfigScreen;
import hsn.modod.config.HSNConfig;
import hsn.modod.optimize.HotPath;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * F6 — master kill switch (every HSN pass off / on).
 * F7 — open the HSN settings screen.
 */
public final class HSNKeybinds {

	private static KeyMapping killSwitchKey;
	private static KeyMapping settingsKey;

	private HSNKeybinds() {
	}

	public static void register() {
		KeyMapping.Category category = KeyMapping.Category.register(
				Identifier.fromNamespaceAndPath(HSNOptimizations.MOD_ID, "keys"));

		killSwitchKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.hsn-optimizations.killswitch",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_F6,
				category));
		settingsKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.hsn-optimizations.settings",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_F7,
				category));

		ClientTickEvents.END_CLIENT_TICK.register(HSNKeybinds::onTick);
		HSNOptimizations.LOGGER.info("HSN keys: F6 = master kill switch, F7 = open settings");
	}

	private static void onTick(Minecraft client) {
		if (client == null) {
			return;
		}
		while (killSwitchKey.consumeClick()) {
			HSNConfig cfg = HSNConfig.get();
			cfg.modEnabled = !cfg.modEnabled;
			cfg.save();
			HotPath.rebuild(cfg);
			String state = cfg.modEnabled ? "ON" : "OFF (vanilla rendering)";
			HSNOptimizations.LOGGER.info("HSN master switch {}", state);
			if (client.player != null) {
				client.player.sendSystemMessage(Component.literal("HSN Optimizations: " + state));
			}
		}
		while (settingsKey.consumeClick()) {
			if (ClientScreens.current() == null) {
				ClientScreens.open(HSNConfigScreen.create(null));
			}
		}
	}
}
