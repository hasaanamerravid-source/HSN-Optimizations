package hsn.modod.client.optimize;

/**
 * Occupancy set for Sodium graph nodes visited this frame.
 * Entity draws fail-open when the set is empty or Sodium is absent.
 */
public final class SodiumVisibleSections {

	private static final int CAP = 8192;
	private static final long EMPTY = Long.MIN_VALUE;
	private static final long[] keys = new long[CAP];
	private static final int[] gen = new int[CAP];
	private static int epoch = 1;
	private static int size;
	private static boolean usable;

	private SodiumVisibleSections() {
	}

	public static void beginFrame() {
		epoch++;
		if (epoch == Integer.MAX_VALUE) {
			epoch = 1;
			java.util.Arrays.fill(gen, 0);
		}
		size = 0;
		usable = false;
	}

	public static void endFrame() {
		usable = size > 8;
	}

	public static boolean isUsable() {
		return usable;
	}

	public static void markBlockOrigin(int originX, int originY, int originZ) {
		mark(originX >> 4, originY >> 4, originZ >> 4);
	}

	public static void mark(int sx, int sy, int sz) {
		if (size >= (CAP >> 1)) {
			usable = false;
			return;
		}
		long key = pack(sx, sy, sz);
		int i = mix(key) & (CAP - 1);
		for (int n = 0; n < 16; n++) {
			int slot = (i + n) & (CAP - 1);
			if (gen[slot] != epoch) {
				keys[slot] = key;
				gen[slot] = epoch;
				size++;
				return;
			}
			if (keys[slot] == key) {
				return;
			}
		}
	}

	public static boolean containsBlock(double x, double y, double z) {
		if (!usable) {
			return true;
		}
		return contains((int) Math.floor(x) >> 4, (int) Math.floor(y) >> 4, (int) Math.floor(z) >> 4);
	}

	public static boolean contains(int sx, int sy, int sz) {
		if (!usable) {
			return true;
		}
		long key = pack(sx, sy, sz);
		int i = mix(key) & (CAP - 1);
		for (int n = 0; n < 16; n++) {
			int slot = (i + n) & (CAP - 1);
			if (gen[slot] != epoch) {
				return false;
			}
			if (keys[slot] == key) {
				return true;
			}
		}
		return true;
	}

	public static int size() {
		return size;
	}

	private static long pack(int sx, int sy, int sz) {
		return ((long) sx & 0x3FFFFFFL) << 38
				| ((long) sz & 0x3FFFFFFL) << 12
				| ((long) sy & 0xFFFL);
	}

	private static int mix(long key) {
		key ^= key >>> 33;
		key *= 0xff51afd7ed558ccdL;
		key ^= key >>> 33;
		return (int) key;
	}
}
