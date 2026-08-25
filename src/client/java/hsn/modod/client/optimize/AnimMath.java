package hsn.modod.client.optimize;

/**
 * Shared constants and helpers that replace repeated expensive animation math.
 * Inspired by the old FastAnim approach (cache PI/180, avoid recalculating
 * the same values for every limb of every entity every frame).
 */
public final class AnimMath {

	/** Degrees → radians. Cached once instead of being recomputed thousands of times. */
	public static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

	/** Radians → degrees. */
	public static final float RAD_TO_DEG = (float) (180.0 / Math.PI);

	/** 2π – useful for wrapping angles. */
	public static final float TWO_PI = (float) (Math.PI * 2.0);

	private AnimMath() {
	}

	/** Convert degrees to radians using the cached constant. */
	public static float toRad(float degrees) {
		return degrees * DEG_TO_RAD;
	}

	/** Convert radians to degrees. */
	public static float toDeg(float radians) {
		return radians * RAD_TO_DEG;
	}

	/** Cheap wrap into [0, 2π). */
	public static float wrapRad(float angle) {
		angle %= TWO_PI;
		if (angle < 0.0f) {
			angle += TWO_PI;
		}
		return angle;
	}
}
