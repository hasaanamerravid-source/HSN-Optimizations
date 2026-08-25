package hsn.modod.client.compat;

import hsn.modod.HSNOptimizations;
import hsn.modod.client.config.HSNConfigScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;

/**
 * Sodium presence detection + Video Settings integration.
 *
 * When Sodium is installed the official Config API entrypoint
 * ({@code sodium:config_api_user} → {@link HSNSodiumConfigEntryPoint})
 * registers a dedicated "HSN Optimizations" tab in Video Settings.
 * Reese's Sodium Options shows the same page in its sidebar automatically.
 *
 * A soft mixin button remains as a fallback that opens the full Cloth UI.
 */
public final class SodiumCompat {

	private static boolean present;

	private SodiumCompat() {
	}

	public static void init() {
		present = FabricLoader.getInstance().isModLoaded("sodium");
		if (present) {
			HSNOptimizations.LOGGER.info(
					"Sodium detected — HSN settings appear as a Video Settings tab " +
					"and via the 'HSN Optimizations…' button. Full UI also in Mod Menu.");
			SodiumOptionsIntegration.tryRegister();
		} else {
			HSNOptimizations.LOGGER.info("Sodium not present — HSN runs in standalone mode. Use Mod Menu for settings.");
		}
	}

	public static boolean isPresent() {
		return present;
	}

	/** Opens the HSN config screen as a child of the current screen. */
	public static void openHSNConfig(Screen parent) {
		Screen p = parent != null ? parent : ClientScreens.current();
		ClientScreens.open(HSNConfigScreen.create(p));
	}
}
