package hsn.modod.client.compat;

import hsn.modod.HSNOptimizations;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Minimal Sodium presence check. No invasive hooks.
 */
public final class SodiumCompat {

	private static boolean present;

	private SodiumCompat() {
	}

	public static void init() {
		present = FabricLoader.getInstance().isModLoaded("sodium");
		if (present) {
			HSNOptimizations.LOGGER.info("Sodium detected — HSN will complement terrain rendering");
		}
	}

	public static boolean isPresent() {
		return present;
	}
}
