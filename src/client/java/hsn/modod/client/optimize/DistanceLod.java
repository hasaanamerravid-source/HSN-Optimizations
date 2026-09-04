package hsn.modod.client.optimize;

import hsn.modod.config.HSNConfig;

public final class DistanceLod {

	private DistanceLod() {
	}

	public static double quality(double distSq, double maxDist) {
		return quality(distSq, maxDist, HSNConfig.get());
	}

	public static double quality(double distSq, double maxDist, HSNConfig cfg) {
		if (cfg == null || !cfg.progressiveLodEnabled || maxDist <= 0.0 || Double.isNaN(distSq) || distSq < 0.0) {
			return 1.0;
		}
		return qualityAt(distSq, maxDist, cfg.progressiveLodStart, cfg.progressiveLodMinQuality);
	}

	public static double particleKeepMultiplier(double distSq, double maxDist, boolean highPriority) {
		return particleKeepMultiplier(distSq, maxDist, highPriority, HSNConfig.get());
	}

	public static double particleKeepMultiplier(double distSq, double maxDist, boolean highPriority, HSNConfig cfg) {
		if (cfg == null || !cfg.particleQualityCurveEnabled) {
			return 1.0;
		}
		double q = quality(distSq, maxDist, cfg);
		return highPriority ? (0.55 + 0.45 * q) : q;
	}

	public static boolean qualityBatch(double[] distSq, double maxDist, double startFactor,
									  double minQ, double[] out, int n) {
		return hsn.modod.optimize.NativeBridge.qualityBatch(distSq, maxDist, startFactor, minQ, out, n);
	}

	static double qualityAt(double distSq, double maxDist, double startFactor, double minQ) {
		if (maxDist <= 0.0) {
			return 1.0;
		}
		if (startFactor < 0.15) startFactor = 0.15;
		else if (startFactor > 0.95) startFactor = 0.95;
		if (minQ < 0.05) minQ = 0.05;
		else if (minQ > 1.0) minQ = 1.0;
		if (distSq <= 0.0) {
			return 1.0;
		}
		double start = maxDist * startFactor;
		double startSq = start * start;
		if (distSq <= startSq) {
			return 1.0;
		}
		double maxSq = maxDist * maxDist;
		if (distSq >= maxSq) {
			return minQ;
		}
		double span = maxSq - startSq;
		if (span <= 0.0001) {
			return minQ;
		}
		double t = (distSq - startSq) / span;
		if (t < 0.0) t = 0.0;
		else if (t > 1.0) t = 1.0;
		t = t * t * (3.0 - 2.0 * t);
		double q = 1.0 - t * (1.0 - minQ);
		if (q < minQ) return minQ;
		if (q > 1.0) return 1.0;
		return q;
	}
}
