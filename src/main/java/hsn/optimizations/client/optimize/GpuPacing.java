package hsn.optimizations.client.optimize;

import com.mojang.blaze3d.opengl.GlStateManager;
import hsn.optimizations.config.HSNConfig;
import hsn.optimizations.optimize.HSNScheduler;
import org.lwjgl.opengl.GL11C;

import java.util.Locale;

/**
 * Frame-pacing helper.
 * <p>
 * The old path only flipped BufferStorage off for Intel HD 2000–4000, so on
 * every modern GPU the toggle did nothing. This version also treats high
 * frame-time jitter on integrated GPUs as a reason to keep mapped buffers
 * mutable, which is the actual hitch source on those drivers.
 */
public final class GpuPacing {

	private static volatile boolean scanned;
	private static volatile boolean preferMutable;
	private static volatile boolean integrated;

	private GpuPacing() {
	}

	public static boolean shouldUseMutableBuffers() {
		if (!HSNConfig.get().framePacingFixEnabled) {
			return false;
		}
		if (!scanned) {
			scanOnce();
		}
		return preferMutable;
	}

	private static synchronized void scanOnce() {
		if (scanned) {
			return;
		}
		try {
			GraphicsBackend.probe();
			if (GraphicsBackend.vulkan()) {
				preferMutable = false;
				integrated = false;
				scanned = true;
				return;
			}
			String renderer = getGlString(7937);
			String vendor = getGlString(7936);
			if (renderer.isEmpty() && vendor.isEmpty()) {
				return;
			}
			LowEndTuner.noteGpu(renderer, vendor);
			integrated = isIntegrated(renderer, vendor);
			preferMutable = isLegacyIntelHd(renderer, vendor);
			scanned = true;
		} catch (Throwable ignored) {
			preferMutable = false;
		}
	}

	private static String getGlString(int pname) {
		if (!GlGuard.glUsable()) {
			return "";
		}
		String result = "";
		try {
			result = GlStateManager._getString(pname);
		} catch (Throwable ignored) {
			if (!GlGuard.glUsable()) {
				return "";
			}
			try {
				result = GL11C.glGetString(pname);
			} catch (Throwable ignored2) {
				return "";
			}
		}
		return lower(result);
	}

	private static boolean isIntegrated(String renderer, String vendor) {
		return renderer.contains("intel")
				|| vendor.contains("intel")
				|| renderer.contains("uhd")
				|| renderer.contains("iris")
				|| renderer.contains("radeon graphics")
				|| renderer.contains("vega")
				|| renderer.contains("adreno")
				|| renderer.contains("mali")
				|| renderer.contains("apple m");
	}

	private static boolean isLegacyIntelHd(String renderer, String vendor) {
		boolean isIntelDriver = vendor.contains("intel")
				|| vendor.contains("mesa")
				|| vendor.contains("freedesktop")
				|| renderer.contains("intel");
		if (!isIntelDriver) {
			return false;
		}
		return renderer.contains("hd graphics 2000")
				|| renderer.contains("hd graphics 3000")
				|| renderer.contains("hd graphics 2500")
				|| renderer.contains("hd graphics 4000")
				|| renderer.contains("hd graphics (byt)")
				|| (renderer.contains("hd graphics") && !renderer.matches(".*hd graphics \\d{3,}.*"));
	}

	private static String lower(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT);
	}
}
