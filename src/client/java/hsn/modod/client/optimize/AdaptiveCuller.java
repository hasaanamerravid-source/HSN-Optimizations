package hsn.modod.client.optimize;

import hsn.modod.config.HSNConfig;
import net.minecraft.client.Minecraft;

/**
 * Scales entity render distances based on live FPS.
 * Below target FPS -> eases distances down toward minAdaptiveScale.
 * At/above target FPS -> eases back up toward 1.0 (full distance).
 *
 * Also drives the "Weak GPU" auto layer: when smoothed FPS stays low
 * for a sustained period, extra-aggressive rules become active.
 */
public final class AdaptiveCuller {

	private static double smoothedFps = 60.0;
	private static double scale = 1.0;
	private static boolean weakGpuActive = false;
	private static int lowFpsStreak = 0;

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

		// Weak-GPU auto detection
		if (cfg.weakGpuAutoEnabled) {
			if (smoothedFps < cfg.weakGpuFpsThreshold) {
				lowFpsStreak = Math.min(200, lowFpsStreak + 1);
			} else {
				lowFpsStreak = Math.max(0, lowFpsStreak - 2);
			}
			// Activate after ~3 seconds of sustained low FPS, deactivate after recovery
			weakGpuActive = lowFpsStreak > 60;
		} else {
			weakGpuActive = false;
			lowFpsStreak = 0;
		}

		if (cfg.performanceModeEnabled) {
			scale = Math.max(0.4, cfg.minAdaptiveScale);
			return;
		}
		if (!cfg.adaptiveCullingEnabled) {
			scale = 1.0;
			return;
		}

		double target = Math.max(10, cfg.targetFps);
		double desired = clamp(smoothedFps / target, cfg.minAdaptiveScale, 1.0);

		// When weak-GPU layer is active, bias the scale slightly lower
		if (weakGpuActive) {
			desired = Math.min(desired, Math.max(cfg.minAdaptiveScale, desired * 0.85));
		}

		// Ease toward the desired scale so distances don't flicker every tick.
		scale += (desired - scale) * 0.08;
		scale = clamp(scale, Math.max(0.4, cfg.minAdaptiveScale), 1.0);
	}

	public static double getScale() {
		return scale;
	}

	public static double getSmoothedFps() {
		return smoothedFps;
	}

	/** True when the auto weak-GPU layer has engaged. */
	public static boolean isWeakGpuActive() {
		return weakGpuActive;
	}

	private static double clamp(double v, double min, double max) {
		return Math.max(min, Math.min(max, v));
	}
}
