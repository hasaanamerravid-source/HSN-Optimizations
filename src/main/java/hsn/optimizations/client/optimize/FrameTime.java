package hsn.optimizations.client.optimize;

/**
 * Per-frame timing for 26.3. Vanilla {@code Minecraft.getFps()} only moves
 * about once a second, which made adaptive culling feel sticky. Mixins mark
 * the start/end of {@code runTick} and AdaptiveCuller reads the EMA.
 */
public final class FrameTime {

	private static final double ALPHA = 0.12;
	private static volatile long startNs;
	private static volatile double emaMs = 16.6;
	private static volatile double lastMs = 16.6;
	private static volatile int instantFps = 60;

	private FrameTime() {
	}

	public static void begin() {
		startNs = System.nanoTime();
	}

	public static void end() {
		long start = startNs;
		if (start == 0L) {
			return;
		}
		long dt = System.nanoTime() - start;
		if (dt <= 0L || dt > 250_000_000L) {
			return;
		}
		double ms = dt / 1_000_000.0;
		lastMs = ms;
		emaMs += (ms - emaMs) * ALPHA;
		int fps = (int) Math.round(1000.0 / Math.max(1.0, emaMs));
		if (fps < 1) {
			fps = 1;
		}
		if (fps > 1000) {
			fps = 1000;
		}
		instantFps = fps;
	}

	public static double emaMs() {
		return emaMs;
	}

	public static double lastMs() {
		return lastMs;
	}

	public static int fps() {
		return instantFps;
	}
}
