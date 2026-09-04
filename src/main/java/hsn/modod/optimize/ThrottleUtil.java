package hsn.modod.optimize;

public final class ThrottleUtil {

	private ThrottleUtil() {
	}

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

	public static int intervalForDistanceSq(double distSq, double startDistance, double maxDistance, int maxInterval) {
		if (distSq <= startDistance * startDistance) {
			return 1;
		}
		double maxSq = maxDistance * maxDistance;
		if (distSq >= maxSq || maxDistance <= startDistance) {
			return Math.max(1, maxInterval);
		}
		double startSq = startDistance * startDistance;
		double t = (distSq - startSq) / (maxSq - startSq);
		if (t < 0.0) t = 0.0;
		else if (t > 1.0) t = 1.0;
		return (int) Math.round(1 + t * (maxInterval - 1));
	}

	public static boolean shouldTick(long gameTime, int salt, int interval) {
		if (interval <= 1) {
			return true;
		}
		return Math.floorMod(gameTime + salt, interval) == 0;
	}
}
