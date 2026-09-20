package hsn.optimizations.optimize;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Visible CPUs plus cgroup quota so containers and shared hosts do not
 * oversubscribe workers.
 */
public final class CpuTopology {

	private static final int CACHED = detect();

	private CpuTopology() {
	}

	public static int visibleCores() {
		return CACHED;
	}

	/**
	 * Background parallelism that leaves headroom for the render thread,
	 * the integrated server, and the GPU driver.
	 */
	public static int backgroundParallelism() {
		int cores = visibleCores();
		if (cores <= 2) {
			return 1;
		}
		if (cores <= 4) {
			return 2;
		}
		int reserve = cores <= 8 ? 1 : (cores <= 16 ? 2 : 3);
		int workers = Math.max(2, cores - reserve);
		// Huge L3 can feed more workers; tiny L3 should not oversubscribe.
		long mb = CpuCache.l3Bytes() / (1024L * 1024L);
		if (mb < 8 && workers > 2) {
			workers = Math.max(2, workers - 1);
		}
		return workers;
	}

	public static int l3Kib() {
		return CpuCache.l3Kib();
	}

	private static int detect() {
		int jvm = Math.max(1, Runtime.getRuntime().availableProcessors());
		int quota = cgroupQuota();
		if (quota > 0 && quota < jvm) {
			return quota;
		}
		return jvm;
	}

	private static int cgroupQuota() {
		int v2 = parseQuota(Path.of("/sys/fs/cgroup/cpu.max"));
		if (v2 > 0) {
			return v2;
		}
		return parseCfs(
				Path.of("/sys/fs/cgroup/cpu/cpu.cfs_quota_us"),
				Path.of("/sys/fs/cgroup/cpu/cpu.cfs_period_us"));
	}

	private static int parseQuota(Path file) {
		try {
			if (!Files.isRegularFile(file)) {
				return -1;
			}
			String line = Files.readString(file).trim();
			String[] p = line.split("\\s+");
			if (p.length >= 1 && !"max".equals(p[0])) {
				long quota = Long.parseLong(p[0]);
				long period = p.length > 1 ? Long.parseLong(p[1]) : 100_000L;
				if (quota > 0 && period > 0) {
					return (int) Math.max(1L, (quota + period - 1) / period);
				}
			}
		} catch (Exception ignored) {
		}
		return -1;
	}

	private static int parseCfs(Path quotaFile, Path periodFile) {
		try {
			if (!Files.isRegularFile(quotaFile) || !Files.isRegularFile(periodFile)) {
				return -1;
			}
			long quota = Long.parseLong(Files.readString(quotaFile).trim());
			long period = Long.parseLong(Files.readString(periodFile).trim());
			if (quota > 0 && period > 0) {
				return (int) Math.max(1L, (quota + period - 1) / period);
			}
		} catch (Exception ignored) {
		}
		return -1;
	}
}
