package hsn.modod.client.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Minecraft 26.2 opens screens through {@code Minecraft.gui.setScreen}.
 */
public final class ClientScreens {

	private ClientScreens() {
	}

	public static void open(Screen screen) {
		Minecraft mc = Minecraft.getInstance();
		if (mc == null || mc.gui == null) {
			return;
		}
		if (mc.isSameThread()) {
			mc.gui.setScreen(screen);
		} else {
			mc.execute(() -> mc.gui.setScreen(screen));
		}
	}

	public static Screen current() {
		Minecraft mc = Minecraft.getInstance();
		if (mc == null || mc.gui == null) {
			return null;
		}
		return mc.gui.screen();
	}
}
