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
	private static final MethodHandle ASM_CULL;
	private static final MethodHandle CPP_XYZ;
	private static final MethodHandle CPP_CONE;
	private static final MethodHandle GO_KEEP;
	private static final MethodHandle C_CULL;
	private static final MethodHandle C_RSQRT;
	private static final MethodHandle ASM_AABB;
	private static final MethodHandle ASM_SPHERE;
	private static final MethodHandle ENG_AABB;
	private static final MethodHandle LOD_AABB;
	private static final MethodHandle NIM_CULL;
	private static final MethodHandle D_CULL;
	private static final MethodHandle FORTRAN_CULL;

	private static volatile boolean useNative = true;
	private static volatile boolean useFrustum = true;
	private static volatile SimdMode requested = SimdMode.AUTO;

	static {
		MethodHandle cull = null;
		MethodHandle quality = null;
		MethodHandle flags = null;
		MethodHandle setMode = null;
		MethodHandle active = null;
		MethodHandle xyz = null;
		MethodHandle asmCull = null;
		MethodHandle cppXyz = null;
		MethodHandle cppCone = null;
		MethodHandle goKeep = null;
		MethodHandle cCull = null;
		MethodHandle cRsqrt = null;
		MethodHandle asmAabb = null;
		MethodHandle asmSphere = null;
		MethodHandle engAabb = null;
		MethodHandle lodAabb = null;
		MethodHandle nimCull = null;
		MethodHandle dCull = null;
		MethodHandle fortranCull = null;
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
				asmCull = loadOptional(linker, heap, "libhsn_asm.so", "hsn_asm_cull_f64",
						FunctionDescriptor.ofVoid(
								ValueLayout.ADDRESS,
								ValueLayout.JAVA_DOUBLE,
								ValueLayout.ADDRESS,
								ValueLayout.JAVA_LONG));
				cppXyz = loadOptional(linker, heap, "libhsn_cpp.so", "hsn_cpp_cull_xyz",
						FunctionDescriptor.ofVoid(
								ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
								ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE,
								ValueLayout.JAVA_DOUBLE, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
				cppCone = loadOptional(linker, heap, "libhsn_cpp.so", "hsn_cpp_cone_mask",
						FunctionDescriptor.ofVoid(
								ValueLayout.ADDRESS, ValueLayout.ADDRESS,
								ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE,
								ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE,
								ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
				goKeep = loadOptional(linker, heap, "libhsn_go.so", "hsn_go_keep_mask",
						FunctionDescriptor.ofVoid(
								ValueLayout.ADDRESS, ValueLayout.JAVA_DOUBLE,
								ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
				cCull = loadOptional(linker, heap, "libhsn_c.so", "hsn_c_cull_f64",
						FunctionDescriptor.ofVoid(
								ValueLayout.ADDRESS,
								ValueLayout.JAVA_DOUBLE,
								ValueLayout.ADDRESS,
								ValueLayout.JAVA_LONG));
				cRsqrt = loadOptional(linker, heap, "libhsn_c.so", "hsn_c_rsqrt_f32",
						FunctionDescriptor.ofVoid(
								ValueLayout.ADDRESS,
								ValueLayout.ADDRESS,
								ValueLayout.JAVA_LONG));
				asmAabb = loadOptional(linker, heap, "libhsn_asm.so", "hsn_asm_cull_aabb_f32",
						FunctionDescriptor.ofVoid(
								ValueLayout.ADDRESS, ValueLayout.ADDRESS,
								ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
				asmSphere = loadOptional(linker, heap, "libhsn_asm.so", "hsn_asm_cull_sphere_f32",
						FunctionDescriptor.ofVoid(
								ValueLayout.ADDRESS, ValueLayout.ADDRESS,
								ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
				engAabb = loadOptional(linker, heap, "libhsn_pipeline.so", "hsn_engine_cull_aabb",
						FunctionDescriptor.ofVoid(
								ValueLayout.ADDRESS, ValueLayout.ADDRESS,
								ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
				lodAabb = loadOptional(linker, heap, "libhsn_lod.so", "hsn_lod_cull_aabb_f32",
						FunctionDescriptor.ofVoid(
								ValueLayout.ADDRESS, ValueLayout.ADDRESS,
								ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
				nimCull = loadOptional(linker, heap, "libhsn_nim.so", "hsn_nim_cull_f64",
						FunctionDescriptor.ofVoid(
								ValueLayout.ADDRESS,
								ValueLayout.JAVA_DOUBLE,
								ValueLayout.ADDRESS,
								ValueLayout.JAVA_LONG));
				dCull = loadOptional(linker, heap, "libhsn_d.so", "hsn_d_cull_f64",
						FunctionDescriptor.ofVoid(
								ValueLayout.ADDRESS,
								ValueLayout.JAVA_DOUBLE,
								ValueLayout.ADDRESS,
								ValueLayout.JAVA_LONG));
				fortranCull = loadOptional(linker, heap, "libhsn_fortran.so", "hsn_fortran_cull_f64",
						FunctionDescriptor.ofVoid(
								ValueLayout.ADDRESS,
								ValueLayout.JAVA_DOUBLE,
								ValueLayout.ADDRESS,
								ValueLayout.JAVA_LONG));
				if (ok && flags != null) {
					int bits = (int) flags.invokeExact();
					avx = (bits & 2) != 0;
					avx2 = (bits & 4) != 0;
					avx512 = (bits & 16) != 0;
				}
			}
			if (asmCull == null || cppXyz == null || goKeep == null || asmAabb == null || engAabb == null) {
				Linker linker = Linker.nativeLinker();
				Linker.Option heap = criticalHeap();
				if (asmCull == null) {
					asmCull = loadOptional(linker, heap, "libhsn_asm.so", "hsn_asm_cull_f64",
							FunctionDescriptor.ofVoid(
									ValueLayout.ADDRESS,
									ValueLayout.JAVA_DOUBLE,
									ValueLayout.ADDRESS,
									ValueLayout.JAVA_LONG));
				}
				if (cppXyz == null) {
					cppXyz = loadOptional(linker, heap, "libhsn_cpp.so", "hsn_cpp_cull_xyz",
							FunctionDescriptor.ofVoid(
									ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
									ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE,
									ValueLayout.JAVA_DOUBLE, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
				}
				if (cppCone == null) {
					cppCone = loadOptional(linker, heap, "libhsn_cpp.so", "hsn_cpp_cone_mask",
							FunctionDescriptor.ofVoid(
									ValueLayout.ADDRESS, ValueLayout.ADDRESS,
									ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE,
									ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE,
									ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
				}
				if (goKeep == null) {
					goKeep = loadOptional(linker, heap, "libhsn_go.so", "hsn_go_keep_mask",
							FunctionDescriptor.ofVoid(
									ValueLayout.ADDRESS, ValueLayout.JAVA_DOUBLE,
									ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
				}
				if (cCull == null) {
					cCull = loadOptional(linker, heap, "libhsn_c.so", "hsn_c_cull_f64",
							FunctionDescriptor.ofVoid(
									ValueLayout.ADDRESS,
									ValueLayout.JAVA_DOUBLE,
									ValueLayout.ADDRESS,
									ValueLayout.JAVA_LONG));
				}
				if (cRsqrt == null) {
					cRsqrt = loadOptional(linker, heap, "libhsn_c.so", "hsn_c_rsqrt_f32",
							FunctionDescriptor.ofVoid(
									ValueLayout.ADDRESS,
									ValueLayout.ADDRESS,
									ValueLayout.JAVA_LONG));
				}
				if (asmAabb == null) {
					asmAabb = loadOptional(linker, heap, "libhsn_asm.so", "hsn_asm_cull_aabb_f32",
							FunctionDescriptor.ofVoid(
									ValueLayout.ADDRESS, ValueLayout.ADDRESS,
									ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
				}
				if (asmSphere == null) {
					asmSphere = loadOptional(linker, heap, "libhsn_asm.so", "hsn_asm_cull_sphere_f32",
							FunctionDescriptor.ofVoid(
									ValueLayout.ADDRESS, ValueLayout.ADDRESS,
									ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
				}
				if (engAabb == null) {
					engAabb = loadOptional(linker, heap, "libhsn_pipeline.so", "hsn_engine_cull_aabb",
							FunctionDescriptor.ofVoid(
									ValueLayout.ADDRESS, ValueLayout.ADDRESS,
									ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
				}
				if (lodAabb == null) {
					lodAabb = loadOptional(linker, heap, "libhsn_lod.so", "hsn_lod_cull_aabb_f32",
							FunctionDescriptor.ofVoid(
									ValueLayout.ADDRESS, ValueLayout.ADDRESS,
									ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
				}
				if (nimCull == null) {
					nimCull = loadOptional(linker, heap, "libhsn_nim.so", "hsn_nim_cull_f64",
							FunctionDescriptor.ofVoid(
									ValueLayout.ADDRESS,
									ValueLayout.JAVA_DOUBLE,
									ValueLayout.ADDRESS,
									ValueLayout.JAVA_LONG));
				}
				if (dCull == null) {
					dCull = loadOptional(linker, heap, "libhsn_d.so", "hsn_d_cull_f64",
							FunctionDescriptor.ofVoid(
									ValueLayout.ADDRESS,
									ValueLayout.JAVA_DOUBLE,
									ValueLayout.ADDRESS,
									ValueLayout.JAVA_LONG));
				}
				if (fortranCull == null) {
					fortranCull = loadOptional(linker, heap, "libhsn_fortran.so", "hsn_fortran_cull_f64",
							FunctionDescriptor.ofVoid(
									ValueLayout.ADDRESS,
									ValueLayout.JAVA_DOUBLE,
									ValueLayout.ADDRESS,
									ValueLayout.JAVA_LONG));
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
		ASM_CULL = asmCull;
		CPP_XYZ = cppXyz;
		CPP_CONE = cppCone;
		GO_KEEP = goKeep;
		C_CULL = cCull;
		C_RSQRT = cRsqrt;
		ASM_AABB = asmAabb;
		ASM_SPHERE = asmSphere;
		ENG_AABB = engAabb;
		LOD_AABB = lodAabb;
		NIM_CULL = nimCull;
		D_CULL = dCull;
		FORTRAN_CULL = fortranCull;
		AVAILABLE = ok || asmCull != null || cppXyz != null || cCull != null
				|| asmAabb != null || engAabb != null;
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
		String extra = "";
		if (ASM_CULL != null) extra += "+asm";
		if (CPP_XYZ != null) extra += "+cpp";
		if (GO_KEEP != null) extra += "+go";
		if (C_CULL != null) extra += "+c";
		if (NIM_CULL != null) extra += "+nim";
		if (D_CULL != null) extra += "+d";
		if (FORTRAN_CULL != null) extra += "+fortran";
		if (ASM_AABB != null || ENG_AABB != null || LOD_AABB != null) extra += "+frustum";
		int code = activeCode();
		String core = switch (code) {
			case 3 -> "avx512";
			case 2 -> "avx2";
			default -> "scalar";
		};
		return core + extra + (WasmSimdKernel.available() ? "+wasm" : "");
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
		useFrustum = cfg.nativeHotpathEnabled && cfg.nativeFrustumCullingEnabled;
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
		if (nativeEnabled() && len >= 16 && invokeAsmCull(distSq, limitSq, out, len)) {
			return true;
		}
		if (nativeEnabled() && len >= 16 && invokeCCull(distSq, limitSq, out, len)) {
			return true;
		}
		if (nativeEnabled() && len >= 16 && invokeNamedCull(NIM_CULL, distSq, limitSq, out, len)) {
			return true;
		}
		if (nativeEnabled() && len >= 16 && invokeNamedCull(D_CULL, distSq, limitSq, out, len)) {
			return true;
		}
		if (nativeEnabled() && len >= 16 && invokeNamedCull(FORTRAN_CULL, distSq, limitSq, out, len)) {
			return true;
		}
		if (nativeEnabled() && len >= 16 && WasmSimdKernel.cullF64(distSq, limitSq, out, len)) {
			return true;
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
		if (!nativeEnabled() || x == null || y == null || z == null || out == null) {
			return false;
		}
		int len = Math.min(n, Math.min(x.length, Math.min(y.length, Math.min(z.length, out.length))));
		if (len < 16) {
			return false;
		}
		if (invokeCppXyz(x, y, z, ox, oy, oz, limitSq, out, len)) {
			return true;
		}
		if (CULL_XYZ == null) {
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


	public static boolean keepMask(double[] hashes, double keep, byte[] out, int n) {
		if (hashes == null || out == null || n <= 0) {
			return false;
		}
		int len = Math.min(n, Math.min(hashes.length, out.length));
		if (len <= 0) {
			return false;
		}
		if (nativeEnabled() && GO_KEEP != null && len >= 16 && invokeGoKeep(hashes, keep, out, len)) {
			return true;
		}
		if (len >= 8) {
			try {
				hsn.modod.polyglot.KeepMask.apply(hashes, keep, out, len);
				return true;
			} catch (Throwable ignored) {
			}
		}
		for (int i = 0; i < len; i++) {
			out[i] = (byte) (hashes[i] > keep ? 1 : 0);
		}
		return true;
	}

	public static boolean coneMask(double[] x, double[] z, double ox, double oz,
			double fx, double fz, byte[] out, int n) {
		if (!nativeEnabled() || CPP_CONE == null || x == null || z == null || out == null) {
			return false;
		}
		int len = Math.min(n, Math.min(x.length, Math.min(z.length, out.length)));
		if (len < 16) {
			return false;
		}
		try {
			CPP_CONE.invokeExact(
					MemorySegment.ofArray(x),
					MemorySegment.ofArray(z),
					ox, oz, fx, fz,
					MemorySegment.ofArray(out),
					(long) len);
			return true;
		} catch (Throwable ignored) {
			return false;
		}
	}


	private static boolean invokeNamedCull(MethodHandle handle, double[] distSq, double limitSq, byte[] out, int len) {
		if (handle == null) {
			return false;
		}
		try {
			handle.invokeExact(
					MemorySegment.ofArray(distSq),
					limitSq,
					MemorySegment.ofArray(out),
					(long) len);
			return true;
		} catch (Throwable ignored) {
			return false;
		}
	}

	private static boolean invokeCCull(double[] distSq, double limitSq, byte[] out, int len) {
		if (C_CULL == null) {
			return false;
		}
		try {
			C_CULL.invokeExact(
					MemorySegment.ofArray(distSq),
					limitSq,
					MemorySegment.ofArray(out),
					(long) len);
			return true;
		} catch (Throwable ignored) {
			return false;
		}
	}

	public static boolean rsqrtBatch(float[] in, float[] out, int n) {
		if (!nativeEnabled() || C_RSQRT == null || in == null || out == null || n <= 0) {
			return false;
		}
		int len = Math.min(n, Math.min(in.length, out.length));
		if (len <= 0) {
			return false;
		}
		try {
			C_RSQRT.invokeExact(
					MemorySegment.ofArray(in),
					MemorySegment.ofArray(out),
					(long) len);
			return true;
		} catch (Throwable ignored) {
			return false;
		}
	}

	private static boolean invokeAsmCull(double[] distSq, double limitSq, byte[] out, int len) {
		if (ASM_CULL == null) {
			return false;
		}
		try {
			ASM_CULL.invokeExact(
					MemorySegment.ofArray(distSq),
					limitSq,
					MemorySegment.ofArray(out),
					(long) len);
			return true;
		} catch (Throwable ignored) {
			return false;
		}
	}

	private static boolean invokeCppXyz(double[] x, double[] y, double[] z,
			double ox, double oy, double oz, double limitSq, byte[] out, int len) {
		if (CPP_XYZ == null) {
			return false;
		}
		try {
			CPP_XYZ.invokeExact(
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

	private static boolean invokeGoKeep(double[] hashes, double keep, byte[] out, int len) {
		try {
			GO_KEEP.invokeExact(
					MemorySegment.ofArray(hashes),
					keep,
					MemorySegment.ofArray(out),
					(long) len);
			return true;
		} catch (Throwable ignored) {
			return false;
		}
	}

	private static MethodHandle loadOptional(Linker linker, Linker.Option heap,
			String fileName, String symbol, FunctionDescriptor desc) {
		try {
			Path lib = extractNamed(fileName);
			if (lib == null) {
				return null;
			}
			SymbolLookup lookup = SymbolLookup.libraryLookup(lib, Arena.global());
			return downcall(linker, lookup, symbol, desc, heap);
		} catch (Throwable ignored) {
			return null;
		}
	}


	public static boolean frustumEnabled() {
		return nativeEnabled() && useFrustum;
	}

	/**
	 * Packed AABB frustum test. planes24 = 6*(a,b,c,d), aabb = n*6 floats.
	 * Tries assembly first, then C engine, then Rust, then a confined-arena copy.
	 */
	public static boolean cullAabb(float[] planes24, float[] aabb, byte[] out, int n) {
		if (!frustumEnabled() || planes24 == null || aabb == null || out == null || n <= 0) {
			return false;
		}
		if (planes24.length < 24) {
			return false;
		}
		int len = Math.min(n, Math.min(aabb.length / 6, out.length));
		if (len <= 0) {
			return false;
		}
		if (invokeAabb(ASM_AABB, planes24, aabb, out, len)) {
			return true;
		}
		if (invokeAabb(ENG_AABB, planes24, aabb, out, len)) {
			return true;
		}
		if (invokeAabb(LOD_AABB, planes24, aabb, out, len)) {
			return true;
		}
		cullAabbJava(planes24, aabb, out, len);
		return true;
	}

	public static boolean cullSphere(float[] planes24, float[] xyzr, byte[] out, int n) {
		if (!frustumEnabled() || planes24 == null || xyzr == null || out == null || n <= 0) {
			return false;
		}
		int len = Math.min(n, Math.min(xyzr.length / 4, out.length));
		if (len <= 0 || planes24.length < 24) {
			return false;
		}
		if (ASM_SPHERE != null) {
			try {
				ASM_SPHERE.invokeExact(
						FfmSegments.heap(planes24),
						FfmSegments.heap(xyzr),
						FfmSegments.heap(out),
						(long) len);
				return true;
			} catch (Throwable ignored) {
			}
		}
		cullSphereJava(planes24, xyzr, out, len);
		return true;
	}

	static void cullAabbJava(float[] planes, float[] aabb, byte[] out, int n) {
		for (int i = 0; i < n; i++) {
			int b = i * 6;
			float minx = aabb[b], miny = aabb[b + 1], minz = aabb[b + 2];
			float maxx = aabb[b + 3], maxy = aabb[b + 4], maxz = aabb[b + 5];
			byte drop = 0;
			for (int p = 0; p < 6; p++) {
				int o = p * 4;
				float px = planes[o] >= 0f ? maxx : minx;
				float py = planes[o + 1] >= 0f ? maxy : miny;
				float pz = planes[o + 2] >= 0f ? maxz : minz;
				if (planes[o] * px + planes[o + 1] * py + planes[o + 2] * pz + planes[o + 3] < 0f) {
					drop = 1;
					break;
				}
			}
			out[i] = drop;
		}
	}

	static void cullSphereJava(float[] planes, float[] xyzr, byte[] out, int n) {
		for (int i = 0; i < n; i++) {
			int b = i * 4;
			float x = xyzr[b], y = xyzr[b + 1], z = xyzr[b + 2], r = xyzr[b + 3];
			byte drop = 0;
			for (int p = 0; p < 6; p++) {
				int o = p * 4;
				float d = planes[o] * x + planes[o + 1] * y + planes[o + 2] * z + planes[o + 3];
				if (d < -r) {
					drop = 1;
					break;
				}
			}
			out[i] = drop;
		}
	}

	private static boolean invokeAabb(MethodHandle fn, float[] planes, float[] aabb, byte[] out, int len) {
		if (fn == null) {
			return false;
		}
		try {
			fn.invokeExact(
					FfmSegments.heap(planes),
					FfmSegments.heap(aabb),
					FfmSegments.heap(out),
					(long) len);
			return true;
		} catch (Throwable heapDenied) {
			FfmSegments.Scratch scratch = FfmSegments.scratch();
			try {
				FfmSegments.Scratch.Layout lay = scratch.layout(len * 6, len);
				scratch.copyIn(planes, 24, lay.planes());
				MemorySegment.copy(MemorySegment.ofArray(aabb), FfmSegments.F32, 0,
						lay.payload(), FfmSegments.F32, 0, len * 6);
				fn.invokeExact(lay.planes(), lay.payload(), lay.mask(), (long) len);
				scratch.copyOut(lay.mask(), out, len);
				return true;
			} catch (Throwable t) {
				return false;
			}
		}
	}

	private static Path extractLibrary() throws Exception {
		return extractNamed("libhsn_hotpath.so");
	}

	private static Path extractNamed(String fileName) throws Exception {
		String os = System.getProperty("os.name", "").toLowerCase();
		String arch = System.getProperty("os.arch", "").toLowerCase();
		if (!(os.contains("linux") && (arch.contains("amd64") || arch.contains("x86_64")))) {
			return null;
		}
		String resource = "/natives/linux-x86_64/" + fileName;
		try (var in = NativeBridge.class.getResourceAsStream(resource)) {
			if (in == null) {
				return null;
			}
			Path dir = Path.of(System.getProperty("java.io.tmpdir"), "hsn-optimizations");
			Files.createDirectories(dir);
			Path out = dir.resolve(fileName);
			Path marker = dir.resolve("hsn-native.version");
			String want = "3.8.7-R-ffm";
			boolean versionOk = Files.isRegularFile(marker) && Files.readString(marker).trim().equals(want);
			if (!versionOk) {
				try (var stream = Files.list(dir)) {
					stream.filter(p -> {
						String n = p.getFileName().toString();
						return n.startsWith("libhsn_") && n.endsWith(".so");
					}).forEach(p -> {
						try {
							Files.deleteIfExists(p);
						} catch (Exception ignored) {
						}
					});
				} catch (Exception ignored) {
				}
			}
			boolean stale = !versionOk || !Files.isRegularFile(out) || Files.size(out) == 0L;
			if (stale) {
				Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
				Files.writeString(marker, want);
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
