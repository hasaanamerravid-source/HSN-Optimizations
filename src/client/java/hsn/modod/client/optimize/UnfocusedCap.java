package hsn.modod.client.optimize;

import hsn.modod.config.HSNConfig;

/**
 * When the window is in the background there is no reason to run the world
 * at 400+ FPS. Sleep the leftover budget so the GPU and the laptop fan rest.
 */
public final class UnfocusedCap {

	private static long lastNs = System.nanoTime();

	private UnfocusedCap() {
	}

	public static void apply() {
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.unfocusedFpsCapEnabled) {
			return;
		}
		if (CameraSnapshot.windowActive()) {
			lastNs = System.nanoTime();
			return;
		}
		int cap = Math.max(5, cfg.unfocusedFpsCap);
		long frameNs = 1_000_000_000L / cap;
		long now = System.nanoTime();
		long sleep = frameNs - (now - lastNs);
		if (sleep > 1_000_000L) {
			try {
				Thread.sleep(sleep / 1_000_000L, (int) (sleep % 1_000_000L));
			} catch (InterruptedException ignored) {
				Thread.currentThread().interrupt();
			}
		}
		lastNs = System.nanoTime();
	}
}
