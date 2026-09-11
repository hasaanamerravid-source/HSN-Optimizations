package hsn.optimizations.client.optimize;

import hsn.optimizations.optimize.CpuCache;
import hsn.optimizations.optimize.NativeBridge;
import hsn.optimizations.optimize.VectorCull;

/**
 * Structure-of-arrays squared-distance tests. Native path is used only for
 * batches of 16+; smaller counts stay in Java.
 */
public final class BatchDistance {

	private BatchDistance() {
	}

	public static void cullXyz(double[] x, double[] y, double[] z, int n,
			double ox, double oy, double oz, double limitSq, byte[] out) {
		if (x == null || y == null || z == null || out == null || n <= 0) {
			return;
		}
		n = Math.min(n, Math.min(x.length, Math.min(y.length, Math.min(z.length, out.length))));
		if (n <= 0) {
			return;
		}
		int floor = CpuCache.nativeBatchFloor();
		if (n >= floor && NativeBridge.cullXyz(x, y, z, ox, oy, oz, limitSq, out, n)) {
			return;
		}
		if (n >= 8 && VectorCull.cullXyz(x, y, z, ox, oy, oz, limitSq, out, n)) {
			return;
		}
		cullXyzJava(x, y, z, n, ox, oy, oz, limitSq, out);
	}

	public static void cullXyzJava(double[] x, double[] y, double[] z, int n,
			double ox, double oy, double oz, double limitSq, byte[] out) {
		int i = 0;
		int bound8 = n - 7;
		while (i < bound8) {
			out[i] = beyond(x[i], y[i], z[i], ox, oy, oz, limitSq);
			out[i + 1] = beyond(x[i + 1], y[i + 1], z[i + 1], ox, oy, oz, limitSq);
			out[i + 2] = beyond(x[i + 2], y[i + 2], z[i + 2], ox, oy, oz, limitSq);
			out[i + 3] = beyond(x[i + 3], y[i + 3], z[i + 3], ox, oy, oz, limitSq);
			out[i + 4] = beyond(x[i + 4], y[i + 4], z[i + 4], ox, oy, oz, limitSq);
			out[i + 5] = beyond(x[i + 5], y[i + 5], z[i + 5], ox, oy, oz, limitSq);
			out[i + 6] = beyond(x[i + 6], y[i + 6], z[i + 6], ox, oy, oz, limitSq);
			out[i + 7] = beyond(x[i + 7], y[i + 7], z[i + 7], ox, oy, oz, limitSq);
			i += 8;
		}
		int bound = n - 3;
		while (i < bound) {
			out[i] = beyond(x[i], y[i], z[i], ox, oy, oz, limitSq);
			out[i + 1] = beyond(x[i + 1], y[i + 1], z[i + 1], ox, oy, oz, limitSq);
			out[i + 2] = beyond(x[i + 2], y[i + 2], z[i + 2], ox, oy, oz, limitSq);
			out[i + 3] = beyond(x[i + 3], y[i + 3], z[i + 3], ox, oy, oz, limitSq);
			i += 4;
		}
		while (i < n) {
			out[i] = beyond(x[i], y[i], z[i], ox, oy, oz, limitSq);
			i++;
		}
	}

	private static byte beyond(double x, double y, double z, double ox, double oy, double oz, double limitSq) {
		double dx = x - ox;
		double dy = y - oy;
		double dz = z - oz;
		return (byte) (dx * dx + dy * dy + dz * dz > limitSq ? 1 : 0);
	}
}
