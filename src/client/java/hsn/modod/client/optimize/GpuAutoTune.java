package hsn.modod.client.optimize;

import hsn.modod.HSNOptimizations;
import hsn.modod.config.HSNConfig;
import hsn.modod.config.HSNPresets;

import java.util.Locale;

/**
 * Runs once — the very first time a brand-new HSN config sees a real GPU
 * string — and picks a sane starting preset so both weak integrated GPUs
 * (old Intel HD, laptop iGPUs, software renderers) and capable dedicated
 * GPUs (GeForce RTX/GTX, Radeon RX, Arc) get reasonable defaults without the
 * player needing to find the presets screen first.
 * <p>
 * Never touches a config that has already been auto-tiered or that existed
 * before this feature shipped — see {@link HSNConfig#autoHardwareTierApplied}.
 */
public final class GpuAutoTune {

	private GpuAutoTune() {
	}

	public static void maybeApplyOnce(String renderer, boolean integratedHint) {
		HSNConfig cfg = HSNConfig.get();
		if (cfg.autoHardwareTierApplied) {
			return;
		}
		cfg.autoHardwareTierApplied = true;

		HSNConfig.Preset tier = pickTier(renderer, integratedHint);
		HSNPresets.apply(cfg, tier);
		cfg.lastAppliedPreset = tier;
		HSNOptimizations.LOGGER.info(
				"HSN auto-detected GPU '{}' -> applying {} preset (change anytime in HSN Extra settings or ModMenu)",
				renderer == null ? "unknown" : renderer, tier);
		cfg.save();
	}

	private static HSNConfig.Preset pickTier(String renderer, boolean integratedHint) {
		String r = renderer == null ? "" : renderer.toLowerCase(Locale.ROOT);

		// Very old / software-rasterized integrated GPUs need the most help:
		// Sandy/Ivy/Ivy-Bridge-era "HD Graphics 2000/3000/4000", ancient GMA
		// parts, and any software fallback (llvmpipe / softpipe / Mesa's
		// generic "software rasterizer").
		boolean veryWeak = r.contains("llvmpipe") || r.contains("softpipe")
				|| r.contains("software rasterizer") || r.contains("gma")
				|| r.contains("hd graphics 2000") || r.contains("hd graphics 3000")
				|| r.contains("hd graphics 4000");
		if (veryWeak) {
			return HSNConfig.Preset.ULTRA_LOW;
		}

		// Any other integrated GPU (newer Intel UHD/Iris, AMD Vega / "Radeon
		// Graphics" APUs) — still worth trimming, just not as aggressively.
		if (integratedHint) {
			return HSNConfig.Preset.SAFE;
		}

		// Confidently dedicated, modern GPU (GeForce RTX/GTX, Radeon RX, Arc
		// A/B) — leave HSN's BALANCED defaults in place. They keep the
		// CPU-side throttles (pathfinding, item/XP tick skip, LOD) that cut
		// stutter without touching visuals, which is what high-end hardware
		// benefits from most since Sodium already handles the GPU side.
		return HSNConfig.Preset.BALANCED;
	}
}
