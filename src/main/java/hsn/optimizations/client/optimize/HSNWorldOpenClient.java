package hsn.optimizations.client.optimize;

import hsn.optimizations.client.compat.ClientScreens;
import hsn.optimizations.config.HSNConfig;
import hsn.optimizations.optimize.HSNWorldOpen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Client-side companion to {@link HSNWorldOpen}.
 *
 * <p>Instant-open is a <em>singleplayer</em> helper. On a remote server the
 * {@code ReceivingLevelScreen} ("Joining world…") is the handshake: vanilla
 * dismisses it after enough chunks arrive. Cancelling {@code setScreen} once
 * the local player exists leaves that overlay up forever with the world
 * already rendered behind it. Kill-switch / {@code modEnabled=false} made
 * the same join work because this path stopped running.
 */
public final class HSNWorldOpenClient {

	private HSNWorldOpenClient() {
	}

	/**
	 * True when an incoming joining overlay should not replace the current
	 * screen. Never true on remote multiplayer.
	 */
	public static boolean shouldAdmitFrame(Minecraft mc, Screen incoming) {
		if (!readyToAdmit(mc) || incoming == null) {
			return false;
		}
		if (isRemoteMultiplayer(mc)) {
			return false;
		}
		return HSNWorldOpen.isAdmissionOverlay(incoming.getClass().getName());
	}

	/**
	 * If a joining overlay is still up after the local player exists in
	 * singleplayer, close it. No-op on remote servers.
	 */
	public static void tick(Minecraft mc) {
		if (!readyToAdmit(mc) || isRemoteMultiplayer(mc)) {
			return;
		}
		Screen cur = ClientScreens.current();
		if (cur == null) {
			return;
		}
		if (HSNWorldOpen.isAdmissionOverlay(cur.getClass().getName())) {
			ClientScreens.close();
		}
	}

	private static boolean readyToAdmit(Minecraft mc) {
		if (!HSNWorldOpen.enabled() || mc == null) {
			return false;
		}
		HSNConfig cfg = HSNConfig.get();
		if (cfg == null || !cfg.admitPlayWhenReady) {
			return false;
		}
		if (mc.level == null || mc.player == null) {
			return false;
		}
		return mc.player.isAlive();
	}

	/**
	 * Integrated / LAN host → false. Dedicated / remote server → true.
	 * Unknown method names fail closed (treat as remote) so a 26.2 rename
	 * cannot re-break multiplayer joining.
	 */
	static boolean isRemoteMultiplayer(Minecraft mc) {
		if (mc == null) {
			return true;
		}
		if (boolMethod(mc, "hasSingleplayerServer") || boolMethod(mc, "isLocalServer")) {
			return false;
		}
		if (methodValue(mc, "getSingleplayerServer") != null) {
			return false;
		}
		if (methodValue(mc, "getCurrentServer") != null) {
			return true;
		}
		if (methodValue(mc, "getConnection") != null) {
			return true;
		}
		return false;
	}

	private static boolean boolMethod(Minecraft mc, String name) {
		try {
			Object v = mc.getClass().getMethod(name).invoke(mc);
			return v instanceof Boolean b && b;
		} catch (Throwable ignored) {
			return false;
		}
	}

	private static Object methodValue(Minecraft mc, String name) {
		try {
			return mc.getClass().getMethod(name).invoke(mc);
		} catch (Throwable ignored) {
			return null;
		}
	}
}
