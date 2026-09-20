package hsn.optimizations.client.optimize;

import hsn.optimizations.optimize.HotPath;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Live particle count for the hard cap. Spawn path refuses extras.
 * 26.3 order-independent transparency makes translucent particles much
 * more expensive, so the live budget is scaled down when adaptive culling
 * has already dropped resolution.
 */
public final class ParticleLive {

	private static final AtomicInteger LIVE = new AtomicInteger();

	private ParticleLive() {
	}

	public static boolean acceptSpawn() {
		if (!HotPath.flag(HotPath.HARD_PARTICLE_CAP)) {
			return true;
		}
		int budget = effectiveBudget();
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

	private static int effectiveBudget() {
		int budget = HotPath.particleBudget();
		if (budget <= 0) {
			return 0;
		}
		double scale = AdaptiveCuller.getScale();
		if (scale >= 0.95) {
			return budget;
		}
		// OIT composite cost grows with overlapping translucent particles.
		int tightened = (int) Math.round(budget * (0.45 + 0.55 * scale));
		return Math.max(32, tightened);
	}
}
