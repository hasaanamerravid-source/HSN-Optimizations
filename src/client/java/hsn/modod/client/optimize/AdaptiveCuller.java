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

    private static volatile int instantFps = 60;
    private static volatile double smoothedFps = 60.0;
    private static volatile double scale = 1.0;
    private static volatile boolean weakGpuActive = false;
    
    private static int lowFpsStreak = 0;
    private static int lastReportedVanillaFps = -1;

    private AdaptiveCuller() {
    }

    public static void tick() {
        HSNConfig cfg = HSNConfig.get();

        int fps = -1;
        try {
            Minecraft client = Minecraft.getInstance();
            if (client != null) {
                fps = client.getFps();
            }
        } catch (Throwable ignored) {
        }

        // Only update exponential moving average when Minecraft actually updates its internal FPS counter
        if (fps > 0) {
            instantFps = fps;
            if (fps != lastReportedVanillaFps) {
                lastReportedVanillaFps = fps;
                // Exponential moving average over frame updates (alpha = 0.2)
                smoothedFps += (fps - smoothedFps) * 0.2;
            }
        }

        // Sanitize configuration inputs to avoid clamp inversion errors (min > max)
        double minScale = clamp(cfg.minAdaptiveScale, 0.05, 1.0);
        double targetFps = Math.max(10.0, cfg.targetFps);

        // Weak-GPU auto detection
        if (cfg.weakGpuAutoEnabled) {
            if (smoothedFps < cfg.weakGpuFpsThreshold) {
                lowFpsStreak = Math.min(200, lowFpsStreak + 1);
            } else {
                lowFpsStreak = Math.max(0, lowFpsStreak - 2);
            }
            // Activate after ~3 seconds (60 ticks) of sustained low FPS
            weakGpuActive = lowFpsStreak > 60;
        } else {
            weakGpuActive = false;
            lowFpsStreak = 0;
        }

        // Absolute performance preset override
        if (cfg.performanceModeEnabled) {
            scale = minScale;
            hsn.modod.optimize.HotPath.publishScale(scale);
            LowEndTuner.tick();
            return;
        }

        if (!cfg.adaptiveCullingEnabled) {
            scale = 1.0;
            hsn.modod.optimize.HotPath.publishScale(scale);
            LowEndTuner.tick();
            return;
        }

        double desired = clamp(smoothedFps / targetFps, minScale, 1.0);

        // When weak-GPU layer is active, bias the target scale down by 15%
        if (weakGpuActive) {
            desired = Math.min(desired, Math.max(minScale, desired * 0.85));
        }

        // Smoothly ease scale toward target value to prevent frame-to-frame popping
        double currentScale = scale;
        currentScale += (desired - currentScale) * 0.08;
        scale = clamp(currentScale, minScale, 1.0);
        hsn.modod.optimize.HotPath.publishScale(scale);
        LowEndTuner.tick();
    }

    public static double getScale() {
        return scale;
    }

    public static double getSmoothedFps() {
        return smoothedFps;
    }

    public static int getInstantFps() {
        return instantFps;
    }

    /** True when the auto weak-GPU layer has engaged. */
    public static boolean isWeakGpuActive() {
        return weakGpuActive;
    }

    private static double clamp(double v, double min, double max) {
        if (min > max) {
            return max;
        }
        return Math.max(min, Math.min(max, v));
    }
}