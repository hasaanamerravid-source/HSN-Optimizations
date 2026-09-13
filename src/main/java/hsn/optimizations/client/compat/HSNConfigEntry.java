package hsn.optimizations.client.compat;

import hsn.optimizations.HSNOptimizations;
import hsn.optimizations.client.config.HSNConfigScreen;
import net.minecraft.client.gui.screens.Screen;

/**
 * Shared entry point for opening the HSN config UI.
 * Prefers YACL when the mod is present; falls back to the built-in vanilla
 * {@link HSNConfigScreen}. Matches the project's optional-compat pattern
 * (Sodium / VoxelSniper): never a hard dependency.
 */
public final class HSNConfigEntry {

	private HSNConfigEntry() {
	}

	/**
	 * Open the best available config screen for the given parent.
	 */
	public static void open(Screen parent) {
		ClientScreens.open(createScreen(parent));
	}

	/**
	 * Build a new config {@link Screen}. Prefer YACL when installed.
	 */
	public static Screen createScreen(Screen parent) {
		if (HSNYaclCompat.isPresent()) {
			try {
				Screen yacl = HSNYaclCompat.createScreen(parent);
				if (yacl != null) {
					return yacl;
				}
			} catch (Throwable t) {
				HSNOptimizations.LOGGER.warn(
						"YACL config screen failed, falling back to built-in: {}", t.toString());
			}
		}
		return HSNConfigScreen.create(parent);
	}
}
