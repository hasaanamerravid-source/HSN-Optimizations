package hsn.modod.client.optimize;

import hsn.modod.HSNOptimizations;
import hsn.modod.config.HSNConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

/**
 * Simple FPS overlay (toggle with F7). Shows live FPS, the active preset,
 * the current adaptive cull scale, and whether Performance Mode is on.
 *
 * Registers itself against the HUD render event via reflection so the mod
 * still compiles even if that event's package/signature moves between
 * Minecraft/Fabric API versions.
 */
public final class FpsOverlay {

	private FpsOverlay() {
	}

	public static void register() {
		try {
			Class<?> eventClass = Class.forName("net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback");
			Field eventField = eventClass.getField("EVENT");
			Object event = eventField.get(null);

			Object proxy = Proxy.newProxyInstance(
					eventClass.getClassLoader(),
					new Class<?>[]{eventClass},
					(proxyObj, method, args) -> {
						if (args != null && args.length > 0) {
							tryDraw(args[0]);
						}
						return null;
					});

			event.getClass().getMethod("register", eventClass).invoke(event, proxy);
		} catch (Throwable t) {
			HSNOptimizations.LOGGER.warn("HSN FPS overlay: HUD render hook unavailable on this API, overlay disabled ({})", t.toString());
		}
	}

	public static void tryDraw(Object guiGraphics) {
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.fpsOverlayEnabled) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc == null || isGuiHidden(mc)) {
			return;
		}

		int fps = 0;
		try {
			fps = mc.getFps();
		} catch (Throwable ignored) {
			try {
				Object f = mc.getClass().getMethod("getFps").invoke(mc);
				if (f instanceof Number n) {
					fps = n.intValue();
				}
			} catch (Throwable ignored2) {
			}
		}

		String line = "HSN " + fps + " FPS | " + cfg.lastAppliedPreset
				+ " | cull " + Math.round(AdaptiveCuller.getScale() * 100) + "%"
				+ (cfg.performanceModeEnabled ? " | PERF MODE" : "");
		try {
			Font font = mc.font;
			int x = cfg.fpsOverlayX;
			int y = cfg.fpsOverlayY;
			int color = 0xE0E0E0;
			try {
				guiGraphics.getClass()
						.getMethod("drawString", Font.class, String.class, int.class, int.class, int.class, boolean.class)
						.invoke(guiGraphics, font, line, x, y, color, true);
			} catch (Throwable t1) {
				try {
					guiGraphics.getClass()
							.getMethod("drawString", Font.class, String.class, int.class, int.class, int.class)
							.invoke(guiGraphics, font, line, x, y, color);
				} catch (Throwable ignored) {
				}
			}
		} catch (Throwable ignored) {
		}
	}

	private static boolean isGuiHidden(Minecraft mc) {
		try {
			Object options = mc.options;
			if (options == null) return false;
			try {
				return (boolean) options.getClass().getField("hideGui").get(options);
			} catch (Throwable t) {
				try {
					return (boolean) options.getClass().getMethod("hideGui").invoke(options);
				} catch (Throwable t2) {
					return false;
				}
			}
		} catch (Throwable ignored) {
			return false;
		}
	}
}
