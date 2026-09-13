package hsn.optimizations.client.optimize;

import hsn.optimizations.optimize.HotPath;

import java.util.concurrent.atomic.AtomicInteger;

/** Live particle count for the hard cap. Spawn path refuses extras. */
public final class ParticleLive {

	private static final AtomicInteger LIVE = new AtomicInteger();

	private ParticleLive() {
	}

	public static boolean acceptSpawn() {
		if (!HotPath.flag(HotPath.HARD_PARTICLE_CAP)) {
			return true;
		}
		int budget = HotPath.particleBudget();
		for (;;) {
			int n = LIVE.get();
			if (n >= budget) {
				return false;
			}
			if (LIVE.compareAndSet(n, n + 1)) {
				return true;
			}
		}
	}

	public static void released() {
		LIVE.updateAndGet(n -> n > 0 ? n - 1 : 0);
	}

	public static int count() {
		return LIVE.get();
	}
}
