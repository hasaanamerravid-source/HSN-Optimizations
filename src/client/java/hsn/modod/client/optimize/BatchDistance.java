package hsn.modod.client.optimize;

import hsn.modod.optimize.NativeBridge;

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
		if (n >= 16 && NativeBridge.cullXyz(x, y, z, ox, oy, oz, limitSq, out, n)) {
			return;
		}
		cullXyzJava(x, y, z, n, ox, oy, oz, limitSq, out);
	}

	public static void cullXyzJava(double[] x, double[] y, double[] z, int n,
			double ox, double oy, double oz, double limitSq, byte[] out) {
		int i = 0;
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
