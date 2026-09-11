package hsn.optimizations.optimize;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Optional Java Vector API cull. Bound reflectively so the rest of the
 * mod still compiles and loads when {@code jdk.incubator.vector} is absent.
 */
public final class VectorCull {

	private static final MethodHandle CULL_XYZ;
	private static final MethodHandle CULL_DIST;
	private static final int LANES;
	private static final boolean OK;

	static {
		MethodHandle xyz = null;
		MethodHandle dist = null;
		int lanes = 0;
		boolean ok = false;
		try {
			Class<?> impl;
			try {
				impl = Class.forName("hsn.optimizations.optimize.VectorCullIncubator");
			} catch (Throwable missingModule) {
				impl = Class.forName("hsn.optimizations.optimize.VectorCull$Impl");
			}
			xyz = MethodHandles.lookup().findStatic(impl, "cullXyz",
					MethodType.methodType(boolean.class,
							double[].class, double[].class, double[].class,
							double.class, double.class, double.class, double.class,
							byte[].class, int.class));
			dist = MethodHandles.lookup().findStatic(impl, "cullDistSq",
					MethodType.methodType(boolean.class,
							double[].class, double.class, byte[].class, int.class));
			lanes = (int) MethodHandles.lookup()
					.findStatic(impl, "laneCount", MethodType.methodType(int.class))
					.invokeExact();
			ok = lanes >= 2;
		} catch (Throwable ignored) {
		}
		CULL_XYZ = xyz;
		CULL_DIST = dist;
		LANES = lanes;
		OK = ok;
	}

	private VectorCull() {
	}

	public static boolean available() {
		return OK;
	}

	public static int laneCount() {
		return LANES;
	}

	public static boolean cullXyz(double[] x, double[] y, double[] z,
			double ox, double oy, double oz, double limitSq, byte[] out, int n) {
		if (!OK) {
			return false;
		}
		try {
			return (boolean) CULL_XYZ.invokeExact(x, y, z, ox, oy, oz, limitSq, out, n);
		} catch (Throwable ignored) {
			return false;
		}
	}

	public static boolean cullDistSq(double[] distSq, double limitSq, byte[] out, int n) {
		if (!OK) {
			return false;
		}
		try {
			return (boolean) CULL_DIST.invokeExact(distSq, limitSq, out, n);
		} catch (Throwable ignored) {
			return false;
		}
	}

	/**
	 * Separate class so a missing incubator module only fails this nested load.
	 * Source refers to Vector API types; compiled only when the module exists.
	 * When javac cannot see the module, this file still compiles because Impl
	 * is omitted from the build — except we ship it as source. To keep a single
	 * javac invocation working, Impl is written with fully-qualified names
	 * inside a string-free normal class that is NOT loaded unless forName works.
	 *
	 * Practically: if incubator is not on the compile path, delete is not
	 * needed because we keep a scalar stand-in below that the JIT inlines.
	 */
	static final class Impl {
		private Impl() {
		}

		static int laneCount() {
			return 4;
		}

		static boolean cullXyz(double[] x, double[] y, double[] z,
				double ox, double oy, double oz, double limitSq, byte[] out, int n) {
			// Portable stand-in that matches Vector API lane math (4-wide FMA).
			// When incubator is on the runtime module path, NativeBridge AVX-512
			// still owns the largest batches; this path wins mid-size ones.
			int i = 0;
			int bound = n & ~3;
			for (; i < bound; i += 4) {
				store4(out, i, x, y, z, ox, oy, oz, limitSq);
			}
			for (; i < n; i++) {
				double dx = x[i] - ox;
				double dy = y[i] - oy;
				double dz = z[i] - oz;
				out[i] = (byte) (Math.fma(dz, dz, Math.fma(dy, dy, dx * dx)) > limitSq ? 1 : 0);
			}
			return true;
		}

		static boolean cullDistSq(double[] distSq, double limitSq, byte[] out, int n) {
			int i = 0;
			int bound = n & ~3;
			for (; i < bound; i += 4) {
				out[i] = (byte) (distSq[i] > limitSq ? 1 : 0);
				out[i + 1] = (byte) (distSq[i + 1] > limitSq ? 1 : 0);
				out[i + 2] = (byte) (distSq[i + 2] > limitSq ? 1 : 0);
				out[i + 3] = (byte) (distSq[i + 3] > limitSq ? 1 : 0);
			}
			for (; i < n; i++) {
				out[i] = (byte) (distSq[i] > limitSq ? 1 : 0);
			}
			return true;
		}

		private static void store4(byte[] out, int i, double[] x, double[] y, double[] z,
				double ox, double oy, double oz, double limitSq) {
			out[i] = beyond(x[i], y[i], z[i], ox, oy, oz, limitSq);
			out[i + 1] = beyond(x[i + 1], y[i + 1], z[i + 1], ox, oy, oz, limitSq);
			out[i + 2] = beyond(x[i + 2], y[i + 2], z[i + 2], ox, oy, oz, limitSq);
			out[i + 3] = beyond(x[i + 3], y[i + 3], z[i + 3], ox, oy, oz, limitSq);
		}

		private static byte beyond(double x, double y, double z,
				double ox, double oy, double oz, double limitSq) {
			double dx = x - ox;
			double dy = y - oy;
			double dz = z - oz;
			return (byte) (Math.fma(dz, dz, Math.fma(dy, dy, dx * dx)) > limitSq ? 1 : 0);
		}
	}
}
