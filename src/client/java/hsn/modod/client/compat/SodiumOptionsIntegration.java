package hsn.modod.client.compat;

import hsn.modod.HSNOptimizations;
import hsn.modod.client.config.HSNConfigScreen;
import net.minecraft.client.gui.screens.Screen;

/**
 * Probes whether Sodium's official Config API is on the classpath.
 * Actual page registration happens through {@link HSNSodiumConfigEntryPoint}
 * via the {@code sodium:config_api_user} Fabric entrypoint — no Mixins required.
 */
public final class SodiumOptionsIntegration {

	private SodiumOptionsIntegration() {
	}

	public static void tryRegister() {
		if (!SodiumCompat.isPresent()) return;

		if (isConfigApiPresent()) {
			HSNOptimizations.LOGGER.info(
					"Sodium Config API present — open Video Settings → HSN Optimizations");
		} else {
			HSNOptimizations.LOGGER.info(
					"Sodium Config API classes not found; using button in Sodium options screen + Mod Menu");
		}
	}

	private static boolean isConfigApiPresent() {
		try {
			Class.forName("net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint");
			Class.forName("net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder");
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		} catch (Throwable t) {
			HSNOptimizations.LOGGER.debug("Sodium Config API probe failed: {}", t.toString());
			return false;
		}
	}

	public static void openFullSettings(Screen parent) {
		ClientScreens.open(HSNConfigScreen.create(parent));
	}
}
