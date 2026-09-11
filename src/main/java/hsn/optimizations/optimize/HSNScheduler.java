package hsn.optimizations.optimize;

import hsn.optimizations.HSNOptimizations;
import hsn.optimizations.config.HSNConfig;

/**
 * Frame-first thread policy.
 * <p>
 * The previous implementation called {@code Thread.getAllStackTraces()} from
 * the client tick. That dumps every thread stack and is itself a hitch.
 * This version enumerates threads without stacks, applies priorities at most
 * once per session, and never raises the render thread to MAX_PRIORITY.
 */
public final class HSNScheduler {

	private static volatile boolean logged;
	private static volatile boolean applied;
	private static volatile long lastFrameNs = 8_000_000L;
	private static volatile long ewmaFrameNs = 8_000_000L;
	private static volatile long ewmaJitterNs = 0L;

	private HSNScheduler() {
	}

	public static boolean enabled() {
		HSNConfig cfg = HSNConfig.get();
		return cfg != null && cfg.modEnabled && cfg.schedulerEnabled;
	}

	public static void noteFrameNanos(long ns) {
		if (ns <= 0L || ns >= 250_000_000L) {
			return;
		}
		lastFrameNs = ns;
		long prev = ewmaFrameNs;
		ewmaFrameNs = prev + ((ns - prev) >> 3);
		long delta = Math.abs(ns - prev);
		long prevJ = ewmaJitterNs;
		ewmaJitterNs = prevJ + ((delta - prevJ) >> 3);
	}

	public static long lastFrameNanos() {
		return lastFrameNs;
	}

	public static long ewmaFrameNanos() {
		return ewmaFrameNs;
	}

	public static long ewmaJitterNanos() {
		return ewmaJitterNs;
	}

	/** True when the render loop has spare time and may yield to workers. */
	public static boolean shouldYield() {
		if (!enabled()) {
			return false;
		}
		HSNConfig cfg = HSNConfig.get();
		if (cfg == null || !cfg.smartYieldEnabled) {
			return false;
		}
		// Only yield when the last frame finished with headroom under 60 FPS.
		return lastFrameNs < 12_000_000L && ewmaJitterNs < 4_000_000L;
	}

	/** Call from world-join / client setup, not from every tick. */
	public static void applyOnce() {
		if (applied) {
			return;
		}
		apply();
	}

	public static void apply() {
		if (!enabled()) {
			return;
		}
		int cores = CpuTopology.visibleCores();
		int bg = CpuTopology.backgroundParallelism();
		int changed = 0;
		int workers = 0;
		Thread[] threads = snapshotThreads();
		for (Thread thread : threads) {
			if (thread == null || thread.getName() == null) {
				continue;
			}
			String name = thread.getName();
			try {
				if (isRender(name)) {
					thread.setPriority(Thread.NORM_PRIORITY + 1);
					changed++;
				} else if (isServer(name)) {
					thread.setPriority(Thread.NORM_PRIORITY);
					changed++;
				} else if (isIo(name)) {
					thread.setPriority(Thread.NORM_PRIORITY);
					changed++;
				} else if (isChunkWorker(name) && !name.contains("ForkJoinPool")) {
					workers++;
					thread.setPriority(Thread.NORM_PRIORITY);
					changed++;
				}
			} catch (SecurityException ignored) {
			}
		}
		applied = true;
		if (!logged) {
			HSNOptimizations.LOGGER.info(
					"HSN scheduler: cores={} background={} retuned={} zgc={} (no stack-dump scan)",
					cores, bg, changed, HSNGc.zgcActive());
			logged = true;
		}
	}

	private static Thread[] snapshotThreads() {
		ThreadGroup root = Thread.currentThread().getThreadGroup();
		while (root.getParent() != null) {
			root = root.getParent();
		}
		int guess = Math.max(64, root.activeCount() * 2);
		Thread[] buf = new Thread[guess];
		int n = root.enumerate(buf, true);
		if (n == buf.length) {
			buf = new Thread[n + Math.max(32, n)];
			root.enumerate(buf, true);
		}
		return buf;
	}

	private static boolean isRender(String n) {
		return "Render thread".equals(n) || "Client thread".equals(n)
				|| n.startsWith("Render thread");
	}

	private static boolean isServer(String n) {
		return "Server thread".equals(n) || n.startsWith("Server thread")
				|| n.startsWith("Integrated Server");
	}

	private static boolean isIo(String n) {
		return n.startsWith("IO-Worker-") || n.startsWith("Download-")
				|| n.startsWith("Netty") || n.startsWith("HttpClient");
	}

	private static boolean isChunkWorker(String n) {
		return n.startsWith("Worker-") || n.contains("Worker-Main")
				|| n.startsWith("C2ME") || n.contains("ForkJoinPool");
	}
}
