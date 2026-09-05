package hsn.modod.client.optimize;

import hsn.modod.optimize.HotPath;

/**
 * Per-item frustum tests against the planes captured this frame.
 * Batch work still goes through assembly via {@code NativeBridge.cullAabb}.
 * This class is for particles / sounds / beacons — no FFI per call.
 */
public final class FrustumCull {

	private FrustumCull() {
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
		for (int i = 0; i < 24; i += 4) {
			if (p[i] * fx + p[i + 1] * fy + p[i + 2] * fz + p[i + 3] < 0f) {
				return true;
			}
		}
		return false;
	}

	/** True if the sphere is fully outside. */
	public static boolean outsideSphere(double x, double y, double z, double radius) {
		if (!enabled()) {
			return false;
		}
		float[] p = CameraSnapshot.planes();
		float fx = (float) x, fy = (float) y, fz = (float) z, r = (float) radius;
		for (int i = 0; i < 24; i += 4) {
			float d = p[i] * fx + p[i + 1] * fy + p[i + 2] * fz + p[i + 3];
			if (d < -r) {
				return true;
			}
		}
		return false;
	}
}
