package hsn.modod.optimize;

/**
 * Graduated tick-interval helper: entities close to a player tick every tick,
 * entities far from every player tick less often — but never freeze outright,
 * so physics/despawn/merge logic keeps progressing, just at a lower rate.
 */
public final class ThrottleUtil {

	private ThrottleUtil() {
	}

	/** Linearly scales the tick interval between 1 (at/below startDistance) and maxInterval (at/beyond maxDistance). */
	public static int intervalForDistance(double distance, double startDistance, double maxDistance, int maxInterval) {
		if (distance <= startDistance) {
			return 1;
		}
		if (distance >= maxDistance || maxDistance <= startDistance) {
			return Math.max(1, maxInterval);
		}
		double t = (distance - startDistance) / (maxDistance - startDistance);
		return (int) Math.round(1 + t * (maxInterval - 1));
	}

	/**
	 * Spreads throttled updates across ticks using a per-entity salt (e.g. entity id)
	 * so throttled entities don't all recompute on the exact same world tick.
	 */
	public static boolean shouldTick(long gameTime, int salt, int interval) {
		if (interval <= 1) {
			return true;
		}
		return Math.floorMod(gameTime + salt, interval) == 0;
	}
}
