package hsn.modod.optimize;

import java.lang.foreign.AddressLayout;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Confined-arena Panama driver for the extra pipeline libraries.
 * Mixins keep using {@link NativeBridge}; this class is the explicit
 * off-heap test / batch entry.
 */
public final class NativePipeline {

	private static final ValueLayout.OfFloat F32 = ValueLayout.JAVA_FLOAT;
	private static final ValueLayout.OfByte U8 = ValueLayout.JAVA_BYTE;
	private static final ValueLayout.OfLong U64 = ValueLayout.JAVA_LONG;
	private static final AddressLayout ADDR = ValueLayout.ADDRESS;

	private static final Linker LINKER = Linker.nativeLinker();

	private NativePipeline() {
	}

	public static void main(String[] args) throws Throwable {
		Path dir = resolveNativeDir(args);
		System.out.println("native dir = " + dir.toAbsolutePath());

		try (Arena arena = Arena.ofConfined()) {
			Optional<SymbolLookup> pipe = load(dir.resolve("libhsn_pipeline.so"), arena);
			Optional<SymbolLookup> lod = load(dir.resolve("libhsn_lod.so"), arena);
			Optional<SymbolLookup> zig = load(dir.resolve("libhsn_zig.so"), arena);

			runRsqrt(arena, pipe);
			runAabb(arena, pipe);
			runAlign(arena, pipe);
			runLod(arena, lod);
			runZig(arena, zig);
		}
	}

	private static Path resolveNativeDir(String[] args) {
		if (args != null && args.length > 0) {
			return Path.of(args[0]);
		}
		Path here = Path.of("src/main/resources/natives/linux-x86_64");
		if (Files.isDirectory(here)) {
			return here;
		}
		return Path.of("natives/linux-x86_64");
	}

	private static Optional<SymbolLookup> load(Path so, Arena arena) {
		if (!Files.isRegularFile(so)) {
			System.out.println("skip missing " + so.getFileName());
			return Optional.empty();
		}
		try {
			return Optional.of(SymbolLookup.libraryLookup(so, arena));
		} catch (Throwable t) {
			System.out.println("skip " + so.getFileName() + ": " + t);
			return Optional.empty();
		}
	}

	private static MethodHandle down(SymbolLookup lookup, String name, FunctionDescriptor desc) {
		MemorySegment sym = lookup.find(name).orElseThrow(() -> new IllegalStateException(name));
		return LINKER.downcallHandle(sym, desc);
	}

	private static void runRsqrt(Arena arena, Optional<SymbolLookup> pipe) throws Throwable {
		if (pipe.isEmpty()) {
			return;
		}
		MethodHandle fn = down(pipe.get(), "hsn_engine_rsqrt_f32",
				FunctionDescriptor.ofVoid(ADDR, ADDR, U64));
		int n = 16;
		MemorySegment in = arena.allocate(F32, n);
		MemorySegment out = arena.allocate(F32, n);
		float[] src = {1f, 4f, 16f, 0.25f, 9f, 100f, 36f, 0.01f, 2f, 8f, 0f, -1f, 49f, 81f, 25f, 64f};
		for (int i = 0; i < n; i++) {
			in.setAtIndex(F32, i, src[i]);
		}
		fn.invokeExact(in, out, (long) n);
		System.out.print("rsqrt:");
		for (int i = 0; i < n; i++) {
			System.out.printf(" %.5f", out.getAtIndex(F32, i));
		}
		System.out.println();
	}

	private static void runAabb(Arena arena, Optional<SymbolLookup> pipe) throws Throwable {
		if (pipe.isEmpty()) {
			return;
		}
		MethodHandle fn = down(pipe.get(), "hsn_engine_cull_aabb",
				FunctionDescriptor.ofVoid(ADDR, ADDR, ADDR, U64));
		MemorySegment planes = arena.allocate(F32, 24);
		// unit box frustum: ±X ±Y ±Z at 10
		float[] p = {
				1, 0, 0, 10, -1, 0, 0, 10,
				0, 1, 0, 10, 0, -1, 0, 10,
				0, 0, 1, 10, 0, 0, -1, 10
		};
		for (int i = 0; i < 24; i++) {
			planes.setAtIndex(F32, i, p[i]);
		}
		int n = 2;
		MemorySegment aabb = arena.allocate(F32, n * 6L);
		float[] boxes = {
				-1, -1, -1, 1, 1, 1,
				40, 40, 40, 42, 42, 42
		};
		for (int i = 0; i < boxes.length; i++) {
			aabb.setAtIndex(F32, i, boxes[i]);
		}
		MemorySegment mask = arena.allocate(U8, n);
		fn.invokeExact(planes, aabb, mask, (long) n);
		System.out.println("aabb cull: inside=" + mask.getAtIndex(U8, 0)
				+ " outside=" + mask.getAtIndex(U8, 1)
				+ " (expect 0 then 1)");
	}

	private static void runAlign(Arena arena, Optional<SymbolLookup> pipe) throws Throwable {
		if (pipe.isEmpty()) {
			return;
		}
		MethodHandle fn = down(pipe.get(), "hsn_cpp_cacheline_aligned",
				FunctionDescriptor.of(ValueLayout.JAVA_INT, ADDR));
		MemorySegment buf = arena.allocate(256, 64);
		int ok = (int) fn.invokeExact(buf);
		System.out.println("cacheline aligned=" + ok);
	}

	private static void runLod(Arena arena, Optional<SymbolLookup> lod) throws Throwable {
		if (lod.isEmpty()) {
			return;
		}
		MethodHandle fn = down(lod.get(), "hsn_lod_thresholds",
				FunctionDescriptor.ofVoid(
						ADDR, ADDR, ADDR,
						F32, F32, F32,
						F32, F32, F32,
						ADDR, U64));
		int n = 4;
		MemorySegment x = arena.allocate(F32, n);
		MemorySegment y = arena.allocate(F32, n);
		MemorySegment z = arena.allocate(F32, n);
		MemorySegment out = arena.allocate(U8, n);
		float[] xs = {0, 1, 3, 8};
		for (int i = 0; i < n; i++) {
			x.setAtIndex(F32, i, xs[i]);
			y.setAtIndex(F32, i, 0f);
			z.setAtIndex(F32, i, 0f);
		}
		fn.invokeExact(x, y, z, 0f, 0f, 0f, 4f, 16f, 64f, out, (long) n);
		System.out.print("lod:");
		for (int i = 0; i < n; i++) {
			System.out.print(" " + out.getAtIndex(U8, i));
		}
		System.out.println(" (expect 0 0 1 2)");
	}

	private static void runZig(Arena arena, Optional<SymbolLookup> zig) throws Throwable {
		if (zig.isEmpty()) {
			return;
		}
		MethodHandle mul = down(zig.get(), "hsn_zig_mul_mat4",
				FunctionDescriptor.ofVoid(ADDR, ADDR, ADDR));
		MethodHandle hash = down(zig.get(), "hsn_zig_spatial_hash",
				FunctionDescriptor.of(U64, F32, F32, F32, F32));
		MemorySegment a = arena.allocate(F32, 16);
		MemorySegment b = arena.allocate(F32, 16);
		MemorySegment o = arena.allocate(F32, 16);
		for (int i = 0; i < 16; i++) {
			float v = (i % 5 == 0) ? 1f : 0f;
			a.setAtIndex(F32, i, v);
			b.setAtIndex(F32, i, v);
		}
		mul.invokeExact(a, b, o);
		System.out.printf("zig m00=%.1f m55=%.1f%n", o.getAtIndex(F32, 0), o.getAtIndex(F32, 5));
		long h = (long) hash.invokeExact(8f, 0f, 8f, 16f);
		System.out.println("zig hash=" + Long.toUnsignedString(h, 16));
	}
}
