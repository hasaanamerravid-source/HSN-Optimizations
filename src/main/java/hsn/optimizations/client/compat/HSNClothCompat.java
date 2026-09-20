package hsn.optimizations.client.compat;

import hsn.optimizations.HSNOptimizations;
import hsn.optimizations.platform.HSNPlatform;
import net.minecraft.client.gui.screens.Screen;

/**
 * Optional Cloth Config bridge. Presence is checked at runtime so the
 * mod still loads when Cloth is not installed.
 */
public final class HSNClothCompat {

	private static boolean present;
	private static boolean initialized;

	private HSNClothCompat() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		present = HSNPlatform.modPresent("cloth-config")
				|| HSNPlatform.modPresent("cloth_config")
				|| HSNPlatform.modPresent("cloth-config2");
		if (present) {
			HSNOptimizations.LOGGER.info("Cloth Config detected; HSN settings will use Cloth sliders.");
		}
	}

	public static boolean isPresent() {
		if (!initialized) {
			init();
		}
		return present;
	}

	public static Screen createScreen(Screen parent) {
		if (!isPresent()) {
			return null;
		}
		try {
			return hsn.optimizations.client.config.HSNClothConfigScreen.create(parent);
		} catch (NoClassDefFoundError | ExceptionInInitializerError e) {
			HSNOptimizations.LOGGER.warn("Cloth Config classes missing at runtime: {}", e.toString());
			present = false;
			return null;
		} catch (Throwable t) {
			HSNOptimizations.LOGGER.warn("Cloth Config screen failed: {}", t.toString());
			return null;
		}
	}
}
