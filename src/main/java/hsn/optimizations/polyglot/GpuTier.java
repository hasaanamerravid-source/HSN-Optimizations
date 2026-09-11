package hsn.optimizations.polyglot;

import java.util.Locale;

/**
 * Maps a GPU renderer string to a starting preset name.
 * Java copy so the mod does not depend on the optional Kotlin sidecar jar.
 */
public final class GpuTier {

	private GpuTier() {
	}

	public static String pick(String renderer, boolean integratedHint) {
		String r = renderer == null ? "" : renderer.toLowerCase(Locale.ROOT);
		if (containsAny(r,
				"llvmpipe", "softpipe", "software rasterizer", "gma",
				"hd graphics 2000", "hd graphics 3000", "hd graphics 4000")) {
			return "ULTRA_LOW";
		}
		if (containsAny(r,
				"rtx 5090", "rtx 5080", "rtx 5070", "rtx 50",
				"rtx 4090", "rtx 4080", "rtx 4070 ti",
				"rx 7900", "rx 9070", "rx 8090", "arc b580")) {
			return "COMPETITIVE";
		}
		if (integratedHint || containsAny(r, "intel uhd", "iris xe", "radeon graphics", "vega 8", "vega 11")) {
			return "SAFE";
		}
		return "BALANCED";
	}

	private static boolean containsAny(String hay, String... needles) {
		for (String n : needles) {
			if (hay.contains(n)) {
				return true;
			}
		}
		return false;
	}
}
