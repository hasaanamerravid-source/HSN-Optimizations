package hsn.modod.client.optimize;

import java.util.concurrent.atomic.AtomicLong;

/** Client-side counters for F3 (reset each second). */
public final class CullStats {
	private static final AtomicLong particlesSkipped = new AtomicLong();
	private static final AtomicLong entitiesSkipped = new AtomicLong();
	private static long windowStart = System.currentTimeMillis();
	private static long partRate, entRate;

	private CullStats() {}

	public static void particleSkip() { particlesSkipped.incrementAndGet(); }
	public static void entitySkip() { entitiesSkipped.incrementAndGet(); }

	public static void tick() {
		long now = System.currentTimeMillis();
		if (now - windowStart >= 1000L) {
			partRate = particlesSkipped.getAndSet(0);
			entRate = entitiesSkipped.getAndSet(0);
			windowStart = now;
		}
	}

	public static long particlesPerSec() { return partRate; }
	public static long entitiesPerSec() { return entRate; }
}
