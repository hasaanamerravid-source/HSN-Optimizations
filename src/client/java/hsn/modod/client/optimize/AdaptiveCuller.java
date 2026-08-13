package hsn.modod.client.optimize;

import hsn.modod.config.HSNConfig;
import net.minecraft.client.Minecraft;

/**
 * Scales entity render distances based on live FPS.
 * Below target FPS -> eases distances down toward minAdaptiveScale.
 * At/above target FPS -> eases back up toward 1.0 (full distance).
 */
public final class AdaptiveCuller {

	private static double smoothedFps = 60.0;
	private static double scale = 1.0;

	private AdaptiveCuller() {
	}

	public static void tick() {
		HSNConfig cfg = HSNConfig.get();

		int fps = 0;
		try {
			fps = Minecraft.getInstance().getFps();
		} catch (Throwable ignored) {
		}
		if (fps > 0) {
			smoothedFps += (fps - smoothedFps) * 0.1;
		}

		if (cfg.performanceModeEnabled) {
			scale = cfg.minAdaptiveScale;
			return;
		}
		if (!cfg.adaptiveCullingEnabled) {
			scale = 1.0;
			return;
		}

		double target = Math.max(10, cfg.targetFps);
		double desired = clamp(smoothedFps / target, cfg.minAdaptiveScale, 1.0);

		// Ease toward the desired scale so distances don't flicker every tick.
		scale += (desired - scale) * 0.08;
		scale = clamp(scale, cfg.minAdaptiveScale, 1.0);
	}

	public static double getScale() {
		return scale;
	}

	public static double getSmoothedFps() {
		return smoothedFps;
	}

	private static double clamp(double v, double min, double max) {
		return Math.max(min, Math.min(max, v));
	}
}
