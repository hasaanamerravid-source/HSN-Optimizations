package hsn.optimizations.client.compat;

import hsn.optimizations.HSNOptimizations;
import hsn.optimizations.client.config.HSNConfigScreen;
import net.minecraft.client.gui.screens.Screen;

/**
 * Shared entry point for opening the HSN config UI.
 * Cloth Config first (working sliders on 26.3), then YACL, then built-in.
 */
public final class HSNConfigEntry {

	private HSNConfigEntry() {
	}

	public static void open(Screen parent) {
		ClientScreens.open(createScreen(parent));
	}

	public static Screen createScreen(Screen parent) {
		if (HSNClothCompat.isPresent()) {
			try {
				Screen cloth = HSNClothCompat.createScreen(parent);
				if (cloth != null) {
					return cloth;
				}
			} catch (Throwable t) {
				HSNOptimizations.LOGGER.warn("Cloth Config screen failed, trying next UI: {}", t.toString());
			}
		}
		if (HSNYaclCompat.isPresent()) {
			try {
				Screen yacl = HSNYaclCompat.createScreen(parent);
				if (yacl != null) {
					return yacl;
				}
			} catch (Throwable t) {
				HSNOptimizations.LOGGER.warn("YACL config screen failed, falling back to built-in: {}", t.toString());
			}
		}
		return HSNConfigScreen.create(parent);
	}
}
