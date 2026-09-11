package hsn.optimizations;

import hsn.optimizations.optimize.CpuTopology;
import hsn.optimizations.optimize.HSNGc;

/**
 * Runs as early as the loader allows. Safe place to size the common
 * ForkJoin pool from real CPU quota. Cannot enable ZGC here — the
 * collector is already chosen — but it can write the launcher hint.
 */
public final class HSNEarly {

	private static volatile boolean ran;

	private HSNEarly() {
	}

	public static void run() {
		if (ran) {
			return;
		}
		ran = true;
		try {
			int p = CpuTopology.backgroundParallelism();
			System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", Integer.toString(p));
		} catch (Throwable ignored) {
			// Pre-launch must never abort the game.
		}
		try {
			HSNGc.publish();
		} catch (Throwable ignored) {
			// Collector hint is optional.
		}
	}
}
