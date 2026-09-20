package hsn.optimizations.client.optimize;

import hsn.optimizations.HSNOptimizations;
import hsn.optimizations.config.HSNConfig;
import hsn.optimizations.config.WorldRenderShape;

import java.util.Locale;

/**
 * Session-level safety for old / integrated GPUs. Config auto-tune only
 * runs once; this still turns off the 26.3 blit path every launch when
 * the live GPU string looks like Intel HD 2000–4000 or a software rasterizer.
 */
public final class IntegratedGpuGuard {

	private static volatile boolean applied;

	private IntegratedGpuGuard() {
	}

	public static void applyIfNeeded() {
		if (applied) {
			return;
		}
		if (!GpuProbe.resolved() && (GpuProbe.renderer() == null || GpuProbe.renderer().isBlank())) {
			return;
		}
		String renderer = GpuProbe.renderer();
		if (renderer == null || renderer.isBlank()) {
			renderer = LowEndTuner.gpuLabel();
		}
		String r = renderer == null ? "" : renderer.toLowerCase(Locale.ROOT);
		boolean fragile = r.contains("hd graphics 2000")
				|| r.contains("hd graphics 3000")
				|| r.contains("hd graphics 4000")
				|| r.contains("2nd generation core")
				|| r.contains("sandybridge")
				|| r.contains("llvmpipe")
				|| r.contains("softpipe")
				|| r.contains("software rasterizer");
		boolean integrated = fragile || LowEndTuner.integratedHint();
		if (!integrated) {
			applied = true;
			return;
		}

		HSNConfig cfg = HSNConfig.get();
		boolean changed = false;
		if (cfg.renderScaleEnabled || Math.abs(cfg.renderScale - 1.0) >= 0.01) {
			cfg.renderScaleEnabled = false;
			cfg.renderScale = 1.0;
			changed = true;
		}
		if (fragile && cfg.circularRenderingEnabled) {
			cfg.circularRenderingEnabled = false;
			cfg.worldRenderShape = WorldRenderShape.OFF;
			changed = true;
		}
		if (fragile && cfg.alwaysKeepChunks < 3) {
			cfg.alwaysKeepChunks = 3;
			changed = true;
		}
		applied = true;
		if (changed) {
			HSNOptimizations.LOGGER.warn(
					"HSN: GPU '{}' is integrated/old. RenderScale{} disabled for this session.",
					renderer,
					fragile ? " and circular terrain" : "");
			cfg.saveSoon();
		}
	}
}
