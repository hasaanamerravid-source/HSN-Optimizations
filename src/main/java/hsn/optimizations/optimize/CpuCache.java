package hsn.optimizations.optimize;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads the last-level cache size from sysfs (Linux) so batch thresholds
 * can stay inside L3 instead of bouncing through DRAM.
 */
public final class CpuCache {

	private static final long L3_BYTES = detectL3();
	private static final int LINE = detectLine();

	private CpuCache() {
	}

	public static long l3Bytes() {
		return L3_BYTES;
	}

	public static int lineSize() {
		return LINE;
	}

	public static int l3Kib() {
		return (int) Math.max(1L, L3_BYTES / 1024L);
	}

	/**
	 * Minimum batch length before crossing into native / Vector API.
	 * Small L3 → stay in Java longer (FFI + huge SoA hurts).
	 * Big L3 → native earlier.
	 */
	public static int nativeBatchFloor() {
		long mb = L3_BYTES / (1024L * 1024L);
		if (mb >= 24) {
			return 12;
		}
		if (mb >= 12) {
			return 16;
		}
		if (mb >= 6) {
			return 24;
		}
		return 32;
	}

	private static long detectL3() {
		Path base = Path.of("/sys/devices/system/cpu/cpu0/cache");
		long best = 0L;
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(base, "index*")) {
			for (Path index : stream) {
				Path level = index.resolve("level");
				Path size = index.resolve("size");
				if (!Files.isRegularFile(level) || !Files.isRegularFile(size)) {
					continue;
				}
				int lv = Integer.parseInt(Files.readString(level).trim());
				if (lv < 3) {
					continue;
				}
				best = Math.max(best, parseSize(Files.readString(size).trim()));
			}
		} catch (Exception ignored) {
		}
		if (best <= 0L) {
			best = 8L * 1024L * 1024L;
		}
		return best;
	}

	private static int detectLine() {
		try {
			Path p = Path.of("/sys/devices/system/cpu/cpu0/cache/index3/coherency_line_size");
			if (Files.isRegularFile(p)) {
				int n = Integer.parseInt(Files.readString(p).trim());
				if (n >= 32 && n <= 256) {
					return n;
				}
			}
		} catch (Exception ignored) {
		}
		return 64;
	}

	static long parseSize(String raw) {
		if (raw == null || raw.isEmpty()) {
			return 0L;
		}
		String s = raw.trim().toUpperCase();
		long mul = 1L;
		if (s.endsWith("K")) {
			mul = 1024L;
			s = s.substring(0, s.length() - 1);
		} else if (s.endsWith("M")) {
			mul = 1024L * 1024L;
			s = s.substring(0, s.length() - 1);
		} else if (s.endsWith("G")) {
			mul = 1024L * 1024L * 1024L;
			s = s.substring(0, s.length() - 1);
		}
		try {
			return Long.parseLong(s.trim()) * mul;
		} catch (NumberFormatException ignored) {
			return 0L;
		}
	}
}
