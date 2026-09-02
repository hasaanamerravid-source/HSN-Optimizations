package hsn.modod.client.compat;

import hsn.modod.client.config.HSNConfigScreen;
import net.minecraft.client.gui.screens.Screen;

public final class SodiumOptionsIntegration {

	private SodiumOptionsIntegration() {
	}

	public static void tryRegister() {
	}

	public static boolean isConfigApiPresent() {
		try {
			Class.forName("net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint");
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	public static void openFullSettings(Screen parent) {
		ClientScreens.open(HSNConfigScreen.create(parent));
	}
}
