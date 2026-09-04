package hsn.modod.optimize;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Counts pathfinding ticks skipped in the last second (common side).
 */
public final class PathfindingStats {

	private static final AtomicLong skipped = new AtomicLong();
	private static long windowStart = System.currentTimeMillis();
	private static long rate;

	private PathfindingStats() {
	}

	public static void tickSkipped() {
		skipped.incrementAndGet();
	}

	public static void tick() {
		long now = System.currentTimeMillis();
		if (now - windowStart >= 1000L) {
			rate = skipped.getAndSet(0);
			windowStart = now;
		}
	}

	public static long skippedPerSec() {
		return rate;
	}
}
