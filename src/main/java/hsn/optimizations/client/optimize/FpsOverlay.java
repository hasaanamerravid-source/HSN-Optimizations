package hsn.optimizations.client.optimize;

import hsn.optimizations.HSNOptimizations;
import hsn.optimizations.config.HSNConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;

/**
 * FPS line on the HUD. Fabric API 26.2 removed {@code HudRenderCallback};
 * register through {@link HudElementRegistry} instead.
 */
public final class FpsOverlay {

	private static final StringBuilder LINE = new StringBuilder(96);

	private FpsOverlay() {
	}

	public static void register() {
		try {
			HudElementRegistry.addLast(HSNOptimizations.id("fps-overlay"), (graphics, delta) -> draw(graphics));
		} catch (Throwable t) {
			HSNOptimizations.LOGGER.warn("FPS overlay not registered: {}", t.toString());
		}
	}

	private static void draw(Object graphics) {
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.fpsOverlayEnabled) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc == null || mc.font == null) {
			return;
		}
		StringBuilder line = LINE;
		line.setLength(0);
		line.append("HSN ").append(mc.getFps()).append(" FPS | ")
				.append(cfg.lastAppliedPreset != null ? cfg.lastAppliedPreset : "CUSTOM")
				.append(" | drop ").append(CullStats.particlesPerSec()).append("p/")
				.append(CullStats.entitiesPerSec()).append("e | scale ")
				.append(Math.round(AdaptiveCuller.getScale() * 100.0)).append('%');
		if (cfg.performanceModeEnabled) {
			line.append(" | PERF");
		}
		if (AdaptiveCuller.isWeakGpuActive()) {
			line.append(" | WEAK-GPU");
		}
		String text = line.toString();
		Object font = mc.font;
		int x = cfg.fpsOverlayX;
		int y = cfg.fpsOverlayY;
		int color = 0xFFE0E0E0;
		String[] names = {"text", "drawString", "drawText", "drawShadowedString"};
		for (String name : names) {
			try {
				graphics.getClass().getMethod(name, font.getClass(), String.class, int.class, int.class, int.class, boolean.class)
						.invoke(graphics, font, text, x, y, color, true);
				return;
			} catch (Throwable ignored) {
			}
			try {
				graphics.getClass().getMethod(name, font.getClass(), String.class, int.class, int.class, int.class)
						.invoke(graphics, font, text, x, y, color);
				return;
			} catch (Throwable ignored) {
			}
		}
	}
}
