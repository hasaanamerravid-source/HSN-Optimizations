package hsn.optimizations.client.compat;

import hsn.optimizations.HSNOptimizations;
import hsn.optimizations.platform.HSNPlatform;
import net.minecraft.client.gui.screens.Screen;

/**
 * Optional YetAnotherConfigLib (YACL) bridge.
 * Presence is checked at runtime; no classes from YACL are referenced
 * until {@link #createScreen} runs, so the mod stays loadable without YACL.
 */
public final class HSNYaclCompat {

	private static boolean present;
	private static boolean initialized;

	private HSNYaclCompat() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		present = HSNPlatform.modPresent("yet_another_config_lib_v3")
				|| HSNPlatform.modPresent("yacl")
				|| HSNPlatform.modPresent("yet-another-config-lib");
		if (present) {
			HSNOptimizations.LOGGER.info("YACL detected; HSN config will use YACL UI when opened.");
		}
	}

	public static boolean isPresent() {
		if (!initialized) {
			init();
		}
		return present;
	}

	/**
	 * Build a YACL screen. Returns null on any failure so callers can fall back.
	 */
	public static Screen createScreen(Screen parent) {
		if (!isPresent()) {
			return null;
		}
		try {
			return hsn.optimizations.client.config.HSNYaclConfigScreen.create(parent);
		} catch (NoClassDefFoundError | ExceptionInInitializerError e) {
			HSNOptimizations.LOGGER.warn("YACL classes missing at runtime: {}", e.toString());
			present = false;
			return null;
		} catch (Throwable t) {
			HSNOptimizations.LOGGER.warn("YACL screen construction failed: {}", t.toString());
			return null;
		}
	}
}
