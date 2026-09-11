package hsn.optimizations.client.optimize;

import com.mojang.blaze3d.opengl.GlStateManager;
import hsn.optimizations.config.HSNConfig;
import org.lwjgl.opengl.GL11C;

import java.util.Locale;

/**
 * Original HSN helpers for weak / integrated GPUs: smoother frame times,
 * tighter texture work, and a conservative client work budget under load.
 * Does not replace Sodium terrain meshing or upload internals.
 */
public final class LowEndTuner {

	private static volatile boolean integratedHint;
	private static volatile boolean probed;
	private static volatile double workBudget = 1.0;
	private static volatile String gpuLabel = "unknown";

	private LowEndTuner() {
	}

	public static void noteGpu(String renderer, String vendor) {
		String r = renderer == null ? "" : renderer.toLowerCase(Locale.ROOT);
		String v = vendor == null ? "" : vendor.toLowerCase(Locale.ROOT);
		gpuLabel = (renderer == null || renderer.isBlank()) ? "unknown" : renderer;
		integratedHint = r.contains("intel")
				|| r.contains("uhd")
				|| r.contains("iris")
				|| r.contains("hd graphics")
				|| r.contains("radeon graphics")
				|| r.contains("vega")
				|| (v.contains("intel") && !r.contains("arc"))
				|| r.contains("llvmpipe")
				|| r.contains("softpipe");
	}

	public static void tick() {
		GpuProbe.tick();
		if (GpuProbe.resolved()) {
			probed = true;
		} else if (!probed) {
			probeGpu();
		}
		HSNConfig cfg = HSNConfig.get();
		double scale = AdaptiveCuller.getScale();
		double budget = 1.0;
		if (cfg.adaptiveUploadBudgetEnabled) {
			double reserved = cfg.uploadBudgetFraction;
			if (AdaptiveCuller.isWeakGpuActive() || cfg.performanceModeEnabled) {
				reserved = Math.min(0.40, reserved + 0.08);
			}
			budget = 1.0 - reserved * (1.0 - scale);
		}
		if (cfg.lowEndHardwareTuneEnabled && integratedHint) {
			budget = Math.min(budget, 0.92);
		}
		if (cfg.laptopPowerSaveEnabled && AdaptiveCuller.getSmoothedFps() < cfg.targetFps) {
			budget = Math.min(budget, Math.max(0.55, scale));
		}
		workBudget = Math.max(0.45, Math.min(1.0, budget));
	}

	public static double workBudget() {
		return workBudget;
	}

	public static boolean integratedHint() {
		return integratedHint;
	}

	public static String gpuLabel() {
		return gpuLabel;
	}

	private static void probeGpu() {
		try {
			if (probeVulkanOrDeviceApi()) {
				probed = true;
				try {
					GpuAutoTune.maybeApplyOnce(gpuLabel, integratedHint);
				} catch (Throwable ignored) {
				}
				return;
			}
			if (!GlGuard.glUsable()) {
				return;
			}
			String renderer = gl(7937);
			String vendor = gl(7936);
			if (renderer.isEmpty() && vendor.isEmpty()) {
				return;
			}
			noteGpu(renderer, vendor);
			probed = true;
			try {
				GpuAutoTune.maybeApplyOnce(gpuLabel, integratedHint);
			} catch (Throwable ignored) {
			}
		} catch (Throwable ignored) {
		}
	}

	private static boolean probeVulkanOrDeviceApi() {
		try {
			Class<?> rs = Class.forName("com.mojang.blaze3d.systems.RenderSystem");
			Object device = null;
			for (String name : new String[]{"getDevice", "getGpuDevice", "getGraphicsDevice"}) {
				try {
					device = rs.getMethod(name).invoke(null);
					if (device != null) {
						break;
					}
				} catch (NoSuchMethodException ignored) {
				}
			}
			if (device == null) {
				return false;
			}
			String renderer = "";
			for (String name : new String[]{"getRenderer", "renderer", "getName", "name", "toString"}) {
				try {
					Object v = name.equals("toString")
							? device.toString()
							: device.getClass().getMethod(name).invoke(device);
					if (v != null && !v.toString().isBlank()) {
						renderer = v.toString();
						break;
					}
				} catch (NoSuchMethodException ignored) {
				}
			}
			if (renderer.isBlank()) {
				return false;
			}
			noteGpu(renderer, "");
			return true;
		} catch (Throwable ignored) {
			return false;
		}
	}

	static String glString(int pname) {
		return gl(pname);
	}

	private static String gl(int pname) {
		if (!GlGuard.glUsable()) {
			return "";
		}
		try {
			String s = GlStateManager._getString(pname);
			if (s != null && !s.isBlank()) {
				return s;
			}
		} catch (Throwable ignored) {
		}
		if (!GlGuard.glUsable()) {
			return "";
		}
		try {
			String s = GL11C.glGetString(pname);
			return s == null ? "" : s;
		} catch (Throwable ignored) {
			return "";
		}
	}

	/** Extra texture-interval steps when the work budget is tight. */
	public static int extraTextureSkip() {
		if (workBudget >= 0.90) {
			return 0;
		}
		if (workBudget >= 0.75) {
			return 1;
		}
		return 2;
	}
}
