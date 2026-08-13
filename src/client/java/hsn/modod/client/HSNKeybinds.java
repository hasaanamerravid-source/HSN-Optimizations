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
		if (client == null) return;
		long handle = resolveHandle(client);
		if (handle == 0L) return;

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

	private static long resolveHandle(Minecraft client) {
		try {
			Object win = client.getWindow();
			if (win == null) return 0L;
			try {
				return (Long) win.getClass().getMethod("getWindow").invoke(win);
			} catch (Throwable t) {
				try {
					return (Long) win.getClass().getMethod("getHandle").invoke(win);
				} catch (Throwable t2) {
					return 0L;
				}
			}
		} catch (Throwable t) {
			return 0L;
		}
	}

	private static void msg(Minecraft client, String text) {
		HSNOptimizations.LOGGER.info(text);
		Object player = client.player;
		if (player == null) return;
		Component component = Component.literal(text);
		try {
			player.getClass()
					.getMethod("displayClientMessage", Component.class, boolean.class)
					.invoke(player, component, true);
			return;
		} catch (Throwable ignored) {
		}
		try {
			player.getClass()
					.getMethod("sendSystemMessage", Component.class)
					.invoke(player, component);
		} catch (Throwable ignored) {
		}
	}
}
