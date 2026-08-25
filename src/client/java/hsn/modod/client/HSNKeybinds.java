package hsn.modod.client;

import hsn.modod.HSNOptimizations;
import hsn.modod.config.HSNConfig;
import hsn.modod.config.HSNPresets;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * F6 = Performance Mode toggle
 * F7 = FPS overlay toggle
 * F8 = ULTRA_LOW preset
 * F9 = SAFE preset
 */
public final class HSNKeybinds {

	private static boolean f6Down;
	private static boolean f7Down;
	private static boolean f8Down;
	private static boolean f9Down;

	private HSNKeybinds() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(HSNKeybinds::onTick);
		HSNOptimizations.LOGGER.info("HSN keys: F6=Performance Mode, F7=FPS overlay, F8=ULTRA_LOW, F9=SAFE");
	}

	private static void onTick(Minecraft client) {
		if (client == null || client.getWindow() == null) {
			return;
		}
		if (client.gui != null && client.gui.screen() != null) {
			return;
		}
		long handle = client.getWindow().handle();
		if (handle == 0L) {
			return;
		}

		f6Down = edge(handle, GLFW.GLFW_KEY_F6, f6Down, () -> {
			HSNConfig cfg = HSNConfig.get();
			cfg.performanceModeEnabled = !cfg.performanceModeEnabled;
			cfg.save();
			msg(client, "HSN Performance Mode: " + (cfg.performanceModeEnabled ? "ON" : "OFF"));
		});

		f7Down = edge(handle, GLFW.GLFW_KEY_F7, f7Down, () -> {
			HSNConfig cfg = HSNConfig.get();
			cfg.fpsOverlayEnabled = !cfg.fpsOverlayEnabled;
			cfg.save();
			msg(client, "HSN FPS overlay: " + (cfg.fpsOverlayEnabled ? "ON" : "OFF"));
		});

		f8Down = edge(handle, GLFW.GLFW_KEY_F8, f8Down, () -> {
			HSNConfig cfg = HSNConfig.get();
			HSNPresets.apply(cfg, HSNConfig.Preset.ULTRA_LOW);
			cfg.lastAppliedPreset = HSNConfig.Preset.ULTRA_LOW;
			cfg.save();
			msg(client, "HSN: applied ULTRA_LOW preset");
		});

		f9Down = edge(handle, GLFW.GLFW_KEY_F9, f9Down, () -> {
			HSNConfig cfg = HSNConfig.get();
			HSNPresets.apply(cfg, HSNConfig.Preset.SAFE);
			cfg.lastAppliedPreset = HSNConfig.Preset.SAFE;
			cfg.save();
			msg(client, "HSN: applied SAFE preset");
		});
	}

	private static boolean edge(long handle, int key, boolean wasDown, Runnable action) {
		boolean down = GLFW.glfwGetKey(handle, key) == GLFW.GLFW_PRESS;
		if (down && !wasDown) {
			action.run();
		}
		return down;
	}

	private static void msg(Minecraft client, String text) {
		HSNOptimizations.LOGGER.info(text);
		if (client.player == null) {
			return;
		}
		client.player.sendSystemMessage(Component.literal(text));
	}
}
