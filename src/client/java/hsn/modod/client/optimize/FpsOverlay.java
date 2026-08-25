package hsn.modod.client.optimize;

import hsn.modod.HSNOptimizations;
import hsn.modod.config.HSNConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/**
 * FPS overlay (F7). Uses Fabric 26.2 HudElementRegistry + GuiGraphicsExtractor.text.
 * Shows live FPS, active preset, adaptive cull scale, performance-mode and weak-GPU flags.
 */
public final class FpsOverlay {

	private FpsOverlay() {
	}

	public static void register() {
		try {
			HudElementRegistry.attachElementBefore(
					VanillaHudElements.CHAT,
					Identifier.fromNamespaceAndPath("hsn-optimizations", "fps_overlay"),
					FpsOverlay::extract);
			HSNOptimizations.LOGGER.info("HSN FPS overlay registered on HUD (before chat)");
		} catch (Throwable t) {
			try {
				HudElementRegistry.addLast(
						Identifier.fromNamespaceAndPath("hsn-optimizations", "fps_overlay"),
						FpsOverlay::extract);
				HSNOptimizations.LOGGER.info("HSN FPS overlay registered on HUD (addLast fallback)");
			} catch (Throwable t2) {
				HSNOptimizations.LOGGER.warn("HSN FPS overlay failed to register: {}", t2.toString());
			}
		}
	}

	private static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.fpsOverlayEnabled) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		if (mc == null || mc.font == null) {
			return;
		}
		// Respect F1 / hide GUI
		if (mc.options != null) {
			try {
				// Try showGui first (newer), fallback to hideGui (older)
				boolean hidden = false;
				try {
					hidden = !mc.options.getClass().getField("showGui").getBoolean(mc.options);
				} catch (NoSuchFieldException e) {
					hidden = mc.options.getClass().getField("hideGui").getBoolean(mc.options);
				}
				if (hidden) return;
			} catch (Throwable t) {
				// If we can't determine, just continue
			}
		}

		int fps = mc.getFps();
		int cullPct = (int) Math.round(AdaptiveCuller.getScale() * 100.0);

		// Build once per frame with StringBuilder for fewer temporary objects
		StringBuilder sb = new StringBuilder(64);
		sb.append("HSN ").append(fps).append(" FPS | ")
				.append(cfg.lastAppliedPreset != null ? cfg.lastAppliedPreset : "CUSTOM")
				.append(" | cull ").append(cullPct).append('%');
		if (cfg.performanceModeEnabled) {
			sb.append(" | PERF MODE");
		}
		if (AdaptiveCuller.isWeakGpuActive()) {
			sb.append(" | WEAK-GPU");
		}

		graphics.text(mc.font, sb.toString(), cfg.fpsOverlayX, cfg.fpsOverlayY, 0xFFE0E0E0, true);
	}
}
