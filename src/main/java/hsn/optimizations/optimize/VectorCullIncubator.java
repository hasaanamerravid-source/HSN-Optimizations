package hsn.optimizations.optimize;

import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

/**
 * Real Java Vector API kernels. Compiled with
 * {@code --add-modules jdk.incubator.vector}. {@link VectorCull} loads this
 * first and falls back to the portable FMA loop if the module is missing.
 */
public final class VectorCullIncubator {

	private static final VectorSpecies<Double> SPEC = DoubleVector.SPECIES_PREFERRED;

	private VectorCullIncubator() {
	}

	public static int laneCount() {
		return SPEC.length();
	}

	public static boolean cullXyz(double[] x, double[] y, double[] z,
			double ox, double oy, double oz, double limitSq, byte[] out, int n) {
		if (x == null || y == null || z == null || out == null || n <= 0) {
			return false;
		}
		final int bound = SPEC.loopBound(n);
		final DoubleVector vx0 = DoubleVector.broadcast(SPEC, ox);
		final DoubleVector vy0 = DoubleVector.broadcast(SPEC, oy);
		final DoubleVector vz0 = DoubleVector.broadcast(SPEC, oz);
		final DoubleVector vlim = DoubleVector.broadcast(SPEC, limitSq);
		int i = 0;
		for (; i < bound; i += SPEC.length()) {
			DoubleVector dx = DoubleVector.fromArray(SPEC, x, i).sub(vx0);
			DoubleVector dy = DoubleVector.fromArray(SPEC, y, i).sub(vy0);
			DoubleVector dz = DoubleVector.fromArray(SPEC, z, i).sub(vz0);
			VectorMask<Double> drop = dx.mul(dx).add(dy.mul(dy)).add(dz.mul(dz))
					.compare(VectorOperators.GT, vlim);
			for (int lane = 0; lane < SPEC.length(); lane++) {
				out[i + lane] = (byte) (drop.laneIsSet(lane) ? 1 : 0);
			}
		}
		for (; i < n; i++) {
			double dx = x[i] - ox;
			double dy = y[i] - oy;
			double dz = z[i] - oz;
			out[i] = (byte) (Math.fma(dz, dz, Math.fma(dy, dy, dx * dx)) > limitSq ? 1 : 0);
		}
		return true;
	}

	public static boolean cullDistSq(double[] distSq, double limitSq, byte[] out, int n) {
		if (distSq == null || out == null || n <= 0) {
			return false;
		}
		final int bound = SPEC.loopBound(n);
		final DoubleVector vlim = DoubleVector.broadcast(SPEC, limitSq);
		int i = 0;
		for (; i < bound; i += SPEC.length()) {
			VectorMask<Double> drop = DoubleVector.fromArray(SPEC, distSq, i)
					.compare(VectorOperators.GT, vlim);
			for (int lane = 0; lane < SPEC.length(); lane++) {
				out[i + lane] = (byte) (drop.laneIsSet(lane) ? 1 : 0);
			}
		}
		for (; i < n; i++) {
			out[i] = (byte) (distSq[i] > limitSq ? 1 : 0);
		}
		return true;
	}
}
