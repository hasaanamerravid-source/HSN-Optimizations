package hsn.optimizations.client.optimize;

/** Per-second counters for the new 3.8.7 R passes. */
public final class HighEndCounters {

	private static long interp;
	private static long clientTick;
	private static long anim;
	private static long light;
	private static long ambient;
	private static long windowStart = System.currentTimeMillis();
	private static long interpRate, tickRate, animRate, lightRate, ambientRate;

	private HighEndCounters() {
	}

	public static void interpSkip() {
		interp++;
	}

	public static void tickSkip() {
		clientTick++;
	}

	public static void animSkip() {
		anim++;
	}

	public static void lightSkip() {
		light++;
	}

	public static void ambientSkip() {
		ambient++;
	}

	public static void tick() {
		long now = System.currentTimeMillis();
		if (now - windowStart >= 1000L) {
			interpRate = interp;
			tickRate = clientTick;
			animRate = anim;
			lightRate = light;
			ambientRate = ambient;
			interp = clientTick = anim = light = ambient = 0;
			windowStart = now;
		}
	}

	public static long interpPerSec() {
		return interpRate;
	}

	public static long tickPerSec() {
		return tickRate;
	}

	public static long animPerSec() {
		return animRate;
	}

	public static long lightPerSec() {
		return lightRate;
	}

	public static long ambientPerSec() {
		return ambientRate;
	}
}
