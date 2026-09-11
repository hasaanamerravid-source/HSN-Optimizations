package hsn.optimizations.client.compat;

import hsn.optimizations.client.config.HSNConfigScreen;
import hsn.optimizations.platform.HSNPlatform;
import net.minecraft.client.gui.screens.Screen;

public final class SodiumCompat {

	private static boolean present;

	private SodiumCompat() {
	}

	public static void init() {
		present = HSNPlatform.modPresent("sodium");
	}

	public static boolean isPresent() {
		return present;
	}

	public static void openHSNConfig(Screen parent) {
		Screen p = parent != null ? parent : ClientScreens.current();
		ClientScreens.open(HSNConfigScreen.create(p));
	}
}
