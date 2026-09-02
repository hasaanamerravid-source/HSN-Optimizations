package hsn.modod.client.optimize;

import hsn.modod.HSNOptimizations;
import hsn.modod.config.HSNConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class FpsOverlay {

	private FpsOverlay() {
	}

	public static void register() {
		Identifier id = Identifier.fromNamespaceAndPath("hsn-optimizations", "fps_overlay");
		try {
			HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, id, FpsOverlay::extract);
		} catch (Throwable first) {
			try {
				HudElementRegistry.addLast(id, FpsOverlay::extract);
			} catch (Throwable second) {
				HSNOptimizations.LOGGER.warn("FPS overlay not registered: {}", second.toString());
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

		StringBuilder line = new StringBuilder(64);
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
		graphics.text(mc.font, line.toString(), cfg.fpsOverlayX, cfg.fpsOverlayY, 0xFFE0E0E0, true);
	}
}
