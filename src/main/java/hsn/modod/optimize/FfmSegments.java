package hsn.modod.optimize;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * FFM MemorySegment lifecycle.
 * <ul>
 *   <li>Library lookups stay on {@link Arena#global()} (process lifetime).</li>
 *   <li>Heap arrays use {@link MemorySegment#ofArray} — zero copy into native
 *       when {@code Linker.Option.critical(true)} is accepted.</li>
 *   <li>Off-heap scratch lives on a thread-local confined arena, grown only,
 *       64-byte aligned. The render thread reuses one session; it is not
 *       closed per call.</li>
 * </ul>
 */
public final class FfmSegments {

	public static final ValueLayout.OfFloat F32 = ValueLayout.JAVA_FLOAT;
	public static final ValueLayout.OfDouble F64 = ValueLayout.JAVA_DOUBLE;
	public static final ValueLayout.OfByte U8 = ValueLayout.JAVA_BYTE;

	private static final ThreadLocal<Scratch> TL = ThreadLocal.withInitial(Scratch::new);

	private FfmSegments() {
	}

	public static MemorySegment heap(float[] a) {
		return MemorySegment.ofArray(a);
	}

	public static MemorySegment heap(double[] a) {
		return MemorySegment.ofArray(a);
	}

	public static MemorySegment heap(byte[] a) {
		return MemorySegment.ofArray(a);
	}

	public static Scratch scratch() {
		return TL.get();
	}

	/** Close the calling thread's confined arena (tests / shutdown). */
	public static void releaseThread() {
		Scratch s = TL.get();
		s.close();
		TL.remove();
	}

	public static final class Scratch implements AutoCloseable {
		private Arena arena;
		private MemorySegment buf;
		private boolean open = true;

		private Scratch() {
			arena = Arena.ofConfined();
			buf = arena.allocate(4096, 64);
		}

		public MemorySegment ensure(long bytes) {
			if (!open) {
				arena = Arena.ofConfined();
				buf = arena.allocate(Math.max(4096, bytes), 64);
				open = true;
			}
			if (buf.byteSize() < bytes) {
				long next = Math.max(bytes + (bytes >> 1), 4096);
				buf = arena.allocate(next, 64);
			}
			return buf.asSlice(0, bytes);
		}

		public MemorySegment floats(int n) {
			return ensure((long) n * Float.BYTES);
		}

		public MemorySegment bytes(int n) {
			return ensure(n);
		}

		/**
		 * One confined allocation split into plane / payload / mask slices.
		 * Alignment is 64 bytes between slices so AVX loads stay legal.
		 */
		public Layout layout(int payloadFloats, int maskBytes) {
			long planes = 24L * Float.BYTES;
			long payload = (long) payloadFloats * Float.BYTES;
			long pad = 64;
			long bytes = planes + pad + payload + pad + maskBytes;
			MemorySegment all = ensure(bytes);
			MemorySegment planeSeg = all.asSlice(0, planes);
			MemorySegment payloadSeg = all.asSlice(planes + pad, payload);
			MemorySegment maskSeg = all.asSlice(planes + pad + payload + pad, maskBytes);
			return new Layout(planeSeg, payloadSeg, maskSeg);
		}

		public void copyIn(float[] src, int n, MemorySegment dest) {
			MemorySegment.copy(MemorySegment.ofArray(src), F32, 0, dest, F32, 0, n);
		}

		public void copyOut(MemorySegment src, byte[] dest, int n) {
			MemorySegment.copy(src, U8, 0, MemorySegment.ofArray(dest), U8, 0, n);
		}

		public record Layout(MemorySegment planes, MemorySegment payload, MemorySegment mask) {
		}

		@Override
		public void close() {
			if (open) {
				arena.close();
				open = false;
			}
		}
	}
}
