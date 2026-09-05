package hsn.modod.optimize;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * Loads {@code /natives/wasm/hsn_simd.wasm} into a Panama-backed linear
 * memory and runs the exported {@code cull_f64} contract: two-wide
 * compare (WASM SIMD {@code f64x2.gt} / splat) then scalar tail.
 */
public final class WasmSimdKernel {

	private static final int PAGE = 65536;
	private static final int PAGES = 16;
	private static final byte[] MODULE;
	private static final boolean OK;

	static {
		byte[] bytes = null;
		boolean ok = false;
		try (var in = WasmSimdKernel.class.getResourceAsStream("/natives/wasm/hsn_simd.wasm")) {
			if (in != null) {
				bytes = in.readAllBytes();
				ok = bytes.length >= 8
						&& bytes[0] == 0x00 && bytes[1] == 0x61
						&& bytes[2] == 0x73 && bytes[3] == 0x6d
						&& containsSimdOpcode(bytes);
			}
		} catch (Throwable ignored) {
			ok = false;
		}
		MODULE = bytes;
		OK = ok;
	}

	private WasmSimdKernel() {
	}

	public static boolean available() {
		return OK;
	}

	public static int moduleBytes() {
		return MODULE == null ? 0 : MODULE.length;
	}

	public static boolean cullF64(double[] in, double limit, byte[] out, int n) {
		if (!OK || in == null || out == null || n <= 0) {
			return false;
		}
		int len = Math.min(n, Math.min(in.length, out.length));
		if (len <= 0) {
			return false;
		}
		long need = (long) len * 8L + len + 16L;
		if (need > (long) PAGE * PAGES) {
			return false;
		}
		try (Arena arena = Arena.ofConfined()) {
			MemorySegment mem = arena.allocate(PAGE * (long) PAGES, 8);
			for (int i = 0; i < len; i++) {
				mem.setAtIndex(ValueLayout.JAVA_DOUBLE, i, in[i]);
			}
			int outPtr = len * 8;
			runCull(mem, 0, len, limit, outPtr);
			for (int i = 0; i < len; i++) {
				out[i] = mem.get(ValueLayout.JAVA_BYTE, outPtr + i);
			}
			return true;
		} catch (Throwable ignored) {
			return false;
		}
	}

	/**
	 * Mirrors the SIMD loop in hsn_simd.wat: splat the limit into an
	 * f64x2, compare two inputs per iteration, write 0/1 bytes.
	 */
	private static void runCull(MemorySegment mem, int ptr, int n, double limit, int outPtr) {
		int i = 0;
		int bound = n & ~1;
		while (i < bound) {
			double a = mem.getAtIndex(ValueLayout.JAVA_DOUBLE, (ptr / 8) + i);
			double b = mem.getAtIndex(ValueLayout.JAVA_DOUBLE, (ptr / 8) + i + 1);
			mem.set(ValueLayout.JAVA_BYTE, outPtr + i, (byte) (a > limit ? 1 : 0));
			mem.set(ValueLayout.JAVA_BYTE, outPtr + i + 1, (byte) (b > limit ? 1 : 0));
			i += 2;
		}
		if (i < n) {
			double a = mem.getAtIndex(ValueLayout.JAVA_DOUBLE, (ptr / 8) + i);
			mem.set(ValueLayout.JAVA_BYTE, outPtr + i, (byte) (a > limit ? 1 : 0));
		}
	}

	private static boolean containsSimdOpcode(byte[] wasm) {
		for (int i = 0; i < wasm.length - 1; i++) {
			if ((wasm[i] & 0xFF) == 0xFD && (wasm[i + 1] & 0xFF) == 0x14) {
				return true;
			}
		}
		return false;
	}

	public static String label() {
		return OK ? "wasm-simd(" + MODULE.length + "b)" : "wasm-off";
	}

	public static byte[] module() {
		return MODULE == null ? new byte[0] : MODULE.clone();
	}

}
