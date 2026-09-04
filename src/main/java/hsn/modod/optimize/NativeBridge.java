package hsn.modod.optimize;

import hsn.modod.HSNOptimizations;
import hsn.modod.config.HSNConfig;
import hsn.modod.config.SimdMode;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;

/**
 * Optional Panama downcalls into {@code libhsn_hotpath}.
 * Mixins keep using {@link HotPath} on the per-entity path.
 * Batch helpers pick AVX-512, AVX2, or scalar from config + CPUID.
 * Hardware that lacks a requested extension always falls back.
 */
public final class NativeBridge {

	private static final boolean AVAILABLE;
	private static final boolean CPU_AVX;
	private static final boolean CPU_AVX2;
	private static final boolean CPU_AVX512;
	private static final MethodHandle CULL_MASK;
	private static final MethodHandle QUALITY_BATCH;
	private static final MethodHandle CPU_FLAGS;
	private static final MethodHandle SET_MODE;
	private static final MethodHandle ACTIVE_SIMD;
	private static final MethodHandle CULL_XYZ;

	private static volatile boolean useNative = true;
	private static volatile SimdMode requested = SimdMode.AUTO;

	static {
		MethodHandle cull = null;
		MethodHandle quality = null;
		MethodHandle flags = null;
		MethodHandle setMode = null;
		MethodHandle active = null;
		MethodHandle xyz = null;
		boolean ok = false;
		boolean avx = false;
		boolean avx2 = false;
		boolean avx512 = false;
		try {
			Path lib = extractLibrary();
			if (lib != null) {
				SymbolLookup lookup = SymbolLookup.libraryLookup(lib, Arena.global());
				Linker linker = Linker.nativeLinker();
				Linker.Option heap = criticalHeap();
				cull = downcall(linker, lookup, "hsn_cull_mask",
						FunctionDescriptor.ofVoid(
								ValueLayout.ADDRESS,
								ValueLayout.JAVA_DOUBLE,
								ValueLayout.ADDRESS,
								ValueLayout.JAVA_LONG),
						heap);
				quality = downcall(linker, lookup, "hsn_quality_batch",
						FunctionDescriptor.ofVoid(
								ValueLayout.ADDRESS,
								ValueLayout.JAVA_DOUBLE,
								ValueLayout.JAVA_DOUBLE,
								ValueLayout.JAVA_DOUBLE,
								ValueLayout.ADDRESS,
								ValueLayout.JAVA_LONG),
						heap);
				flags = optionalDowncall(linker, lookup, "hsn_cpu_flags",
						FunctionDescriptor.of(ValueLayout.JAVA_INT), heap);
				setMode = optionalDowncall(linker, lookup, "hsn_set_simd_mode",
						FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT), heap);
				active = optionalDowncall(linker, lookup, "hsn_active_simd",
						FunctionDescriptor.of(ValueLayout.JAVA_INT), heap);
				xyz = optionalDowncall(linker, lookup, "hsn_cull_xyz",
						FunctionDescriptor.ofVoid(
								ValueLayout.ADDRESS,
								ValueLayout.ADDRESS,
								ValueLayout.ADDRESS,
								ValueLayout.JAVA_DOUBLE,
								ValueLayout.JAVA_DOUBLE,
								ValueLayout.JAVA_DOUBLE,
								ValueLayout.JAVA_DOUBLE,
								ValueLayout.ADDRESS,
								ValueLayout.JAVA_LONG),
						heap);
				ok = cull != null && quality != null;
				if (ok && flags != null) {
					int bits = (int) flags.invokeExact();
					avx = (bits & 2) != 0;
					avx2 = (bits & 4) != 0;
					avx512 = (bits & 16) != 0;
				}
			}
		} catch (Throwable t) {
			HSNOptimizations.LOGGER.debug("Native bridge unused: {}", t.toString());
			ok = false;
		}
		CULL_MASK = cull;
		QUALITY_BATCH = quality;
		CPU_FLAGS = flags;
		SET_MODE = setMode;
		ACTIVE_SIMD = active;
		CULL_XYZ = xyz;
		AVAILABLE = ok;
		CPU_AVX = avx;
		CPU_AVX2 = avx2;
		CPU_AVX512 = avx512;
	}

	private NativeBridge() {
	}

	public static boolean available() {
		return AVAILABLE;
	}

	public static boolean avx() {
		return CPU_AVX;
	}

	public static boolean avx2() {
		return CPU_AVX2;
	}

	public static boolean avx512() {
		return CPU_AVX512;
	}

	public static boolean nativeEnabled() {
		return AVAILABLE && useNative;
	}

	public static SimdMode requestedMode() {
		return requested;
	}

	public static String activeLabel() {
		if (!AVAILABLE || !useNative) {
			return "java-scalar";
		}
		int code = activeCode();
		return switch (code) {
			case 3 -> "avx512";
			case 2 -> "avx2";
			default -> "scalar";
		};
	}

	public static int cpuFlags() {
		if (CPU_FLAGS == null) {
			return AVAILABLE ? 1 : 0;
		}
		try {
			return (int) CPU_FLAGS.invokeExact();
		} catch (Throwable t) {
			return AVAILABLE ? 1 : 0;
		}
	}

	public static void applyConfig(HSNConfig cfg) {
		if (cfg == null) {
			cfg = new HSNConfig();
		}
		useNative = cfg.nativeHotpathEnabled;
		requested = cfg.simdMode == null ? SimdMode.AUTO : cfg.simdMode;
		if (SET_MODE == null) {
			return;
		}
		try {
			int code = useNative ? requested.nativeCode() : 1;
			SET_MODE.invokeExact(code);
		} catch (Throwable ignored) {
		}
	}

	/**
	 * Writes 1 into {@code out[i]} when {@code distSq[i] > limitSq}.
	 * Uses the configured native kernel when allowed, otherwise Java.
	 */
	public static boolean cullMask(double[] distSq, double limitSq, byte[] out, int n) {
		if (distSq == null || out == null || n <= 0) {
			return false;
		}
		int len = Math.min(n, Math.min(distSq.length, out.length));
		if (len <= 0) {
			return false;
		}
		if (nativeEnabled() && len >= 16 && invokeCull(distSq, limitSq, out, len)) {
			return true;
		}
		cullMaskJava(distSq, limitSq, out, len);
		return true;
	}

	public static boolean qualityBatch(double[] distSq, double maxDist, double startFactor,
									  double minQ, double[] out, int n) {
		if (distSq == null || out == null || n <= 0) {
			return false;
		}
		int len = Math.min(n, Math.min(distSq.length, out.length));
		if (len <= 0) {
			return false;
		}
		if (nativeEnabled() && len >= 16 && invokeQuality(distSq, maxDist, startFactor, minQ, out, len)) {
			return true;
		}
		qualityBatchJava(distSq, maxDist, startFactor, minQ, out, len);
		return true;
	}

	private static int activeCode() {
		if (ACTIVE_SIMD == null) {
			if (useNative && requested == SimdMode.AVX512 && CPU_AVX512) {
				return 3;
			}
			if (useNative && (requested == SimdMode.AVX2 || requested == SimdMode.AVX512 || requested == SimdMode.AUTO) && CPU_AVX2) {
				return CPU_AVX512 && requested != SimdMode.AVX2 ? 3 : 2;
			}
			return 1;
		}
		try {
			return (int) ACTIVE_SIMD.invokeExact();
		} catch (Throwable t) {
			return 1;
		}
	}

	private static boolean invokeCull(double[] distSq, double limitSq, byte[] out, int len) {
		try {
			MemorySegment in = MemorySegment.ofArray(distSq);
			MemorySegment mask = MemorySegment.ofArray(out);
			CULL_MASK.invokeExact(in, limitSq, mask, (long) len);
			return true;
		} catch (Throwable heapDenied) {
			try (Arena arena = Arena.ofConfined()) {
				MemorySegment in = arena.allocate(ValueLayout.JAVA_DOUBLE, len);
				MemorySegment mask = arena.allocate(ValueLayout.JAVA_BYTE, len);
				MemorySegment.copy(MemorySegment.ofArray(distSq), ValueLayout.JAVA_DOUBLE, 0,
						in, ValueLayout.JAVA_DOUBLE, 0, len);
				CULL_MASK.invokeExact(in, limitSq, mask, (long) len);
				MemorySegment.copy(mask, ValueLayout.JAVA_BYTE, 0,
						MemorySegment.ofArray(out), ValueLayout.JAVA_BYTE, 0, len);
				return true;
			} catch (Throwable t) {
				return false;
			}
		}
	}

	private static boolean invokeQuality(double[] distSq, double maxDist, double startFactor,
										 double minQ, double[] out, int len) {
		try {
			MemorySegment in = MemorySegment.ofArray(distSq);
			MemorySegment dest = MemorySegment.ofArray(out);
			QUALITY_BATCH.invokeExact(in, maxDist, startFactor, minQ, dest, (long) len);
			return true;
		} catch (Throwable heapDenied) {
			try (Arena arena = Arena.ofConfined()) {
				MemorySegment in = arena.allocate(ValueLayout.JAVA_DOUBLE, len);
				MemorySegment dest = arena.allocate(ValueLayout.JAVA_DOUBLE, len);
				MemorySegment.copy(MemorySegment.ofArray(distSq), ValueLayout.JAVA_DOUBLE, 0,
						in, ValueLayout.JAVA_DOUBLE, 0, len);
				QUALITY_BATCH.invokeExact(in, maxDist, startFactor, minQ, dest, (long) len);
				MemorySegment.copy(dest, ValueLayout.JAVA_DOUBLE, 0,
						MemorySegment.ofArray(out), ValueLayout.JAVA_DOUBLE, 0, len);
				return true;
			} catch (Throwable t) {
				return false;
			}
		}
	}


	public static boolean cullXyz(double[] x, double[] y, double[] z,
			double ox, double oy, double oz, double limitSq, byte[] out, int n) {
		if (!nativeEnabled() || CULL_XYZ == null || x == null || y == null || z == null || out == null) {
			return false;
		}
		int len = Math.min(n, Math.min(x.length, Math.min(y.length, Math.min(z.length, out.length))));
		if (len < 16) {
			return false;
		}
		try {
			CULL_XYZ.invokeExact(
					MemorySegment.ofArray(x),
					MemorySegment.ofArray(y),
					MemorySegment.ofArray(z),
					ox, oy, oz, limitSq,
					MemorySegment.ofArray(out),
					(long) len);
			return true;
		} catch (Throwable ignored) {
			return false;
		}
	}

	static void cullMaskJava(double[] distSq, double limitSq, byte[] out, int len) {
		int i = 0;
		int bound = len - 7;
		while (i < bound) {
			out[i] = (byte) (distSq[i] > limitSq ? 1 : 0);
			out[i + 1] = (byte) (distSq[i + 1] > limitSq ? 1 : 0);
			out[i + 2] = (byte) (distSq[i + 2] > limitSq ? 1 : 0);
			out[i + 3] = (byte) (distSq[i + 3] > limitSq ? 1 : 0);
			out[i + 4] = (byte) (distSq[i + 4] > limitSq ? 1 : 0);
			out[i + 5] = (byte) (distSq[i + 5] > limitSq ? 1 : 0);
			out[i + 6] = (byte) (distSq[i + 6] > limitSq ? 1 : 0);
			out[i + 7] = (byte) (distSq[i + 7] > limitSq ? 1 : 0);
			i += 8;
		}
		while (i < len) {
			out[i] = (byte) (distSq[i] > limitSq ? 1 : 0);
			i++;
		}
	}

	static void qualityBatchJava(double[] distSq, double maxDist, double startFactor,
								 double minQ, double[] out, int len) {
		if (!(maxDist > 0.0)) {
			for (int i = 0; i < len; i++) {
				out[i] = 1.0;
			}
			return;
		}
		if (startFactor < 0.15) startFactor = 0.15;
		else if (startFactor > 0.95) startFactor = 0.95;
		if (minQ < 0.05) minQ = 0.05;
		else if (minQ > 1.0) minQ = 1.0;
		double start = maxDist * startFactor;
		double startSq = start * start;
		double maxSq = maxDist * maxDist;
		double span = maxSq - startSq;
		for (int i = 0; i < len; i++) {
			double d = distSq[i];
			if (!(d > 0.0) || d <= startSq) {
				out[i] = 1.0;
				continue;
			}
			if (d >= maxSq || span <= 0.0001) {
				out[i] = minQ;
				continue;
			}
			double t = (d - startSq) / span;
			if (t < 0.0) t = 0.0;
			else if (t > 1.0) t = 1.0;
			t = t * t * (3.0 - 2.0 * t);
			double q = 1.0 - t * (1.0 - minQ);
			if (q < minQ) q = minQ;
			else if (q > 1.0) q = 1.0;
			out[i] = q;
		}
	}

	private static MethodHandle optionalDowncall(Linker linker, SymbolLookup lookup, String name,
												 FunctionDescriptor desc, Linker.Option heap) {
		try {
			return downcall(linker, lookup, name, desc, heap);
		} catch (Throwable ignored) {
			return null;
		}
	}

	private static MethodHandle downcall(Linker linker, SymbolLookup lookup, String name,
										 FunctionDescriptor desc, Linker.Option heap) throws Throwable {
		var symbol = lookup.find(name).orElseThrow();
		if (heap != null) {
			try {
				return linker.downcallHandle(symbol, desc, heap);
			} catch (Throwable ignored) {
				return linker.downcallHandle(symbol, desc);
			}
		}
		return linker.downcallHandle(symbol, desc);
	}

	private static Linker.Option criticalHeap() {
		try {
			return Linker.Option.critical(true);
		} catch (Throwable ignored) {
			return null;
		}
	}

	private static Path extractLibrary() throws Exception {
		String os = System.getProperty("os.name", "").toLowerCase();
		String arch = System.getProperty("os.arch", "").toLowerCase();
		String resource;
		if (os.contains("linux") && (arch.contains("amd64") || arch.contains("x86_64"))) {
			resource = "/natives/linux-x86_64/libhsn_hotpath.so";
		} else {
			return null;
		}
		try (var in = NativeBridge.class.getResourceAsStream(resource)) {
			if (in == null) {
				return null;
			}
			Path dir = Path.of(System.getProperty("java.io.tmpdir"), "hsn-optimizations");
			Files.createDirectories(dir);
			Path out = dir.resolve("libhsn_hotpath-3.8.6.so");
			long incoming = in.available();
			boolean stale = !Files.isRegularFile(out) || Files.size(out) == 0L
					|| (incoming > 0 && Files.size(out) != incoming);
			if (stale) {
				Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
			}
			try {
				Files.setPosixFilePermissions(out, EnumSet.of(
						PosixFilePermission.OWNER_READ,
						PosixFilePermission.OWNER_WRITE,
						PosixFilePermission.OWNER_EXECUTE,
						PosixFilePermission.GROUP_READ,
						PosixFilePermission.GROUP_EXECUTE,
						PosixFilePermission.OTHERS_READ,
						PosixFilePermission.OTHERS_EXECUTE));
			} catch (UnsupportedOperationException ignored) {
			}
			return out;
		}
	}
}
