package hsn.optimizations.client.optimize;

import hsn.optimizations.optimize.HotPath;

/**
 * Per-item frustum tests against the planes captured this frame.
 * Batch work still goes through assembly via {@code NativeBridge.cullAabb}.
 * This class is for particles / sounds / beacons — no FFI per call.
 */
public final class FrustumCull {

	private FrustumCull() {
	}

	private static boolean rejects(float[] p, int i, float x, float y, float z, float radius) {
		return p[i] * x + p[i + 1] * y + p[i + 2] * z + p[i + 3] < -radius;
	}

	public static boolean enabled() {
		return HotPath.flag(HotPath.NATIVE_FRUSTUM) && CameraSnapshot.frustumReady();
	}

	/** True if the point is outside the view frustum. */
	public static boolean outsidePoint(double x, double y, double z) {
		if (!enabled()) {
			return false;
		}
		float[] p = CameraSnapshot.planes();
		float fx = (float) x, fy = (float) y, fz = (float) z;
		return rejects(p, 0, fx, fy, fz, 0f)
				|| rejects(p, 4, fx, fy, fz, 0f)
				|| rejects(p, 8, fx, fy, fz, 0f)
				|| rejects(p, 12, fx, fy, fz, 0f)
				|| rejects(p, 16, fx, fy, fz, 0f)
				|| rejects(p, 20, fx, fy, fz, 0f);
	}

	/** True if the sphere is fully outside. */
	public static boolean outsideSphere(double x, double y, double z, double radius) {
		if (!enabled()) {
			return false;
		}
		float[] p = CameraSnapshot.planes();
		float fx = (float) x, fy = (float) y, fz = (float) z, r = (float) radius;
		return rejects(p, 0, fx, fy, fz, r)
				|| rejects(p, 4, fx, fy, fz, r)
				|| rejects(p, 8, fx, fy, fz, r)
				|| rejects(p, 12, fx, fy, fz, r)
				|| rejects(p, 16, fx, fy, fz, r)
				|| rejects(p, 20, fx, fy, fz, r);
	}
}
