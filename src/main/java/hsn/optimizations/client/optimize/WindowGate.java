package hsn.optimizations.client.optimize;

/**
 * Fabric client init runs inside {@code Minecraft.<init>} before the
 * window backend is ready. Minecraft 26.3 uses SDL3 (not GLFW).
 * Calling {@code GL.getCapabilities()} / {@code VK.getCapabilities()}
 * or any native context query before {@link #markReady()} can abort
 * window init. Wait until the first client tick sees a live window.
 */
public final class WindowGate {

	private static volatile boolean ready;

	private WindowGate() {
	}

	public static boolean ready() {
		return ready;
	}

	public static void markReady() {
		if (ready) {
			return;
		}
		ready = true;
		try {
			GraphicsBackend.probe();
			GpuProbe.tick();
		} catch (Throwable ignored) {
		}
	}
}
