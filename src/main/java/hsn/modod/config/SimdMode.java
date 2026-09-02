package hsn.modod.config;

/**
 * SIMD policy for the optional native hotpath.
 * Unsupported choices never crash — the runtime falls back to a kernel
 * the CPU actually has, then to scalar Java.
 */
public enum SimdMode {
	AUTO("Auto (best available)"),
	AVX512("AVX-512"),
	AVX2("AVX2"),
	SCALAR("Off / scalar");

	private final String displayName;

	SimdMode(String displayName) {
		this.displayName = displayName;
	}

	public String displayName() {
		return displayName;
	}

	@Override
	public String toString() {
		return displayName;
	}

	public int nativeCode() {
		return switch (this) {
			case SCALAR -> 1;
			case AVX2 -> 2;
			case AVX512 -> 3;
			default -> 0;
		};
	}

	public static SimdMode fromOrdinal(int ordinal) {
		SimdMode[] values = values();
		if (ordinal < 0) {
			return AUTO;
		}
		if (ordinal >= values.length) {
			return values[values.length - 1];
		}
		return values[ordinal];
	}
}
