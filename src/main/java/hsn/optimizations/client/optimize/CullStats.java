package hsn.optimizations.client.optimize;

/**
 * Per-second skip counters for F3 / overlay. Cheap counters, no allocations.
 */
public final class CullStats {

	private static long particles;
	private static long entities;
	private static long sections;
	private static long sounds;
	private static long windowStart = System.currentTimeMillis();
	private static long particleRate, entityRate, sectionRate, soundRate;

	private CullStats() {
	}

	public static void particleSkip() {
		particles++;
	}

	public static void entitySkip() {
		entities++;
	}

	public static void sectionSkip() {
		sections++;
	}

	public static void soundSkip() {
		sounds++;
	}

	public static void tick() {
		long now = System.currentTimeMillis();
		if (now - windowStart >= 1000L) {
			particleRate = particles;
			entityRate = entities;
			sectionRate = sections;
			soundRate = sounds;
			particles = entities = sections = sounds = 0L;
			windowStart = now;
		}
	}

	public static long particlesPerSec() {
		return particleRate;
	}

	public static long entitiesPerSec() {
		return entityRate;
	}

	public static long sectionsPerSec() {
		return sectionRate;
	}

	public static long soundsPerSec() {
		return soundRate;
	}
}
