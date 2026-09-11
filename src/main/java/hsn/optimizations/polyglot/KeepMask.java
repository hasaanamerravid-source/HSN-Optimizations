package hsn.optimizations.polyglot;

/**
 * In-process keep/drop mask. Replaces the Scala sidecar so a missing
 * polyglot jar cannot break particle / sound keep-rates.
 */
public final class KeepMask {

	private KeepMask() {
	}

	public static int apply(double[] hashes, double keep, byte[] out, int n) {
		if (hashes == null || out == null || n <= 0) {
			return 0;
		}
		int len = Math.min(n, Math.min(hashes.length, out.length));
		int i = 0;
		int bound8 = len - 7;
		while (i < bound8) {
			out[i] = (byte) (hashes[i] > keep ? 1 : 0);
			out[i + 1] = (byte) (hashes[i + 1] > keep ? 1 : 0);
			out[i + 2] = (byte) (hashes[i + 2] > keep ? 1 : 0);
			out[i + 3] = (byte) (hashes[i + 3] > keep ? 1 : 0);
			out[i + 4] = (byte) (hashes[i + 4] > keep ? 1 : 0);
			out[i + 5] = (byte) (hashes[i + 5] > keep ? 1 : 0);
			out[i + 6] = (byte) (hashes[i + 6] > keep ? 1 : 0);
			out[i + 7] = (byte) (hashes[i + 7] > keep ? 1 : 0);
			i += 8;
		}
		int bound = len - 3;
		while (i < bound) {
			out[i] = (byte) (hashes[i] > keep ? 1 : 0);
			out[i + 1] = (byte) (hashes[i + 1] > keep ? 1 : 0);
			out[i + 2] = (byte) (hashes[i + 2] > keep ? 1 : 0);
			out[i + 3] = (byte) (hashes[i + 3] > keep ? 1 : 0);
			i += 4;
		}
		while (i < len) {
			out[i] = (byte) (hashes[i] > keep ? 1 : 0);
			i++;
		}
		return len;
	}
}
