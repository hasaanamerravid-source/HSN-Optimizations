package hsn.modod.client.optimize;

import hsn.modod.config.HSNConfig;

/**
 * Progressive quality falloff near the edge of the player's current render
 * distance (or a configured max distance). Quality stays 1.0 until the start
 * factor, then smoothly drops toward minQuality. Does not change vanilla
 * render distance — only degrades quality of objects already inside it.
 */
public final class DistanceLod {

	private DistanceLod() {
	}

	/**
	 * @param distSq     squared distance from camera/player
	 * @param maxDist    the hard limit distance (e.g. maxEntityRenderDistance)
	 * @return quality in [minQuality .. 1.0]
	 */
	public static double quality(double distSq, double maxDist) {
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.progressiveLodEnabled || maxDist <= 0.0) {
			return 1.0;
		}
		double dist = Math.sqrt(Math.max(0.0, distSq));
		double start = maxDist * clamp(cfg.progressiveLodStart, 0.15, 0.95);
		if (dist <= start) {
			return 1.0;
		}
		if (dist >= maxDist) {
			return cfg.progressiveLodMinQuality;
		}
		// Smoothstep-ish falloff from start → maxDist
		double t = (dist - start) / (maxDist - start);
		t = t * t * (3.0 - 2.0 * t); // smoothstep
		double q = 1.0 - t * (1.0 - cfg.progressiveLodMinQuality);
		return clamp(q, cfg.progressiveLodMinQuality, 1.0);
	}

	/** Convenience when you already have a linear distance. */
	public static double qualityFromDist(double dist, double maxDist) {
		return quality(dist * dist, maxDist);
	}

	/**
	 * Entity LOD stage derived from quality:
	 * 0 = full, 1 = mild (anim reduce), 2 = heavy (more skips), 3 = near-cull.
	 */
	public static int entityStage(double distSq, double maxDist) {
		if (!HSNConfig.get().entityLodStagesEnabled) {
			return 0;
		}
		double q = quality(distSq, maxDist);
		if (q >= 0.85) return 0;
		if (q >= 0.55) return 1;
		if (q >= 0.30) return 2;
		return 3;
	}

	/**
	 * Particle keep multiplier from the quality curve.
	 * High-priority particles get a softer curve so combat stays visible.
	 */
	public static double particleKeepMultiplier(double distSq, double maxDist, boolean highPriority) {
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.particleQualityCurveEnabled) {
			return 1.0;
		}
		double q = quality(distSq, maxDist);
		if (highPriority) {
			// High priority never drops below ~0.55 keep multiplier
			return 0.55 + 0.45 * q;
		}
		return q;
	}

	private static double clamp(double v, double min, double max) {
		return Math.max(min, Math.min(max, v));
	}
}
