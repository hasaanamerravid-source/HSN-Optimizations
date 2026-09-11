package hsn.optimizations.client.optimize;

/**
 * Fabric client init runs inside {@code Minecraft.<init>} before GLFW.
 * Any {@code glfw*} / {@code GL.getCapabilities()} / {@code VK.getCapabilities()}
 * call at that point leaves error {@code 0x10001} in GLFW's queue. Vanilla
 * {@code GLX._initGlfw} then aborts: "GLFW error before init".
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
