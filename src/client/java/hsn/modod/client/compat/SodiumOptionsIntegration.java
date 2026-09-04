package hsn.modod.client.compat;

import hsn.modod.HSNOptimizations;
import hsn.modod.client.config.HSNConfigScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;

/**
 * Fallback when the official {@code sodium:config_api_user} entrypoint is
 * missing from a custom Sodium build. The real page is still registered by
 * {@link HSNSodiumConfigEntryPoint} when the Config API is present.
 */
public final class SodiumOptionsIntegration {

	private static volatile boolean registered;

	private SodiumOptionsIntegration() {
	}

	public static void tryRegister() {
		if (registered) {
			return;
		}
		registered = true;

		boolean sodium = FabricLoader.getInstance().isModLoaded("sodium");
		boolean api = isConfigApiPresent();
		if (!sodium) {
			HSNOptimizations.LOGGER.info("Sodium not loaded; HSN settings stay on Mod Menu / YACL.");
			return;
		}
		if (api) {
			HSNOptimizations.LOGGER.info(
					"Sodium Config API present; HSN tab is registered via sodium:config_api_user.");
			return;
		}
		HSNOptimizations.LOGGER.info(
				"Sodium loaded without Config API; use Mod Menu or Video Settings button for HSN.");
	}

	public static boolean isConfigApiPresent() {
		try {
			Class.forName("net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint");
			return true;
		} catch (Throwable t) {
			return false;
		}
	}

	public static void openFullSettings(Screen parent) {
		Screen p = parent != null ? parent : ClientScreens.current();
		ClientScreens.open(HSNConfigScreen.create(p));
	}
}
