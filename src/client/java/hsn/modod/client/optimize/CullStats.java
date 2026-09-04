package hsn.modod.client.optimize;

/** Client-thread counters for F3 (reset each second). */
public final class CullStats {
	private static long particlesSkipped;
	private static long entitiesSkipped;
	private static long sectionsSkipped;
	private static long windowStart = System.currentTimeMillis();
	private static long partRate, entRate, secRate;

	private CullStats() {}

	public static void particleSkip() { particlesSkipped++; }
	public static void entitySkip() { entitiesSkipped++; }
	public static void sectionSkip() { sectionsSkipped++; }

	public static void tick() {
		long now = System.currentTimeMillis();
		if (now - windowStart >= 1000L) {
			partRate = particlesSkipped;
			entRate = entitiesSkipped;
			secRate = sectionsSkipped;
			particlesSkipped = 0;
			entitiesSkipped = 0;
			sectionsSkipped = 0;
			windowStart = now;
		}
	}

	public static long particlesPerSec() { return partRate; }
	public static long entitiesPerSec() { return entRate; }
	public static long sectionsPerSec() { return secRate; }
}
