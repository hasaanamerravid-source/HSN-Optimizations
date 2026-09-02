package hsn.modod.optimize;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Common-side counter (safe on dedicated servers, unlike the client-only CullStats)
 * for how many item/XP-orb ticks the distance throttle skipped in the last second.
 */
public final class ThrottleStats {

	private static final AtomicLong ticksSkipped = new AtomicLong();
	private static long windowStart = System.currentTimeMillis();
	private static long rate;

	private ThrottleStats() {
	}

	public static void tickSkipped() {
		ticksSkipped.incrementAndGet();
	}

	public static void tick() {
		long now = System.currentTimeMillis();
		if (now - windowStart >= 1000L) {
			rate = ticksSkipped.getAndSet(0);
			windowStart = now;
		}
	}

	public static long skippedPerSec() {
		return rate;
	}
}
