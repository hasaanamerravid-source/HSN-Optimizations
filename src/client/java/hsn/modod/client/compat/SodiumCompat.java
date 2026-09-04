package hsn.modod.client.compat;

import hsn.modod.client.config.HSNConfigScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;

public final class SodiumCompat {

	private static boolean present;

	private SodiumCompat() {
	}

	public static void init() {
		present = FabricLoader.getInstance().isModLoaded("sodium");
	}

	public static boolean isPresent() {
		return present;
	}

	public static void openHSNConfig(Screen parent) {
		Screen p = parent != null ? parent : ClientScreens.current();
		ClientScreens.open(HSNConfigScreen.create(p));
	}
}
