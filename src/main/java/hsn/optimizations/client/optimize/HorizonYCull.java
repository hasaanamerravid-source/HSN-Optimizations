package hsn.optimizations.client.optimize;

import hsn.optimizations.config.HSNConfig;
import hsn.optimizations.optimize.HotPath;
import hsn.optimizations.optimize.NativeBridge;
import hsn.optimizations.optimize.WasmSimdKernel;
import net.minecraft.client.Minecraft;

/**
 * Lowest visible world Y from pitch + real FOV + view distance, plus a
 * packed AABB test against the bottom frustum plane.
 */
public final class HorizonYCull {

	private static final double WORLD_FLOOR = -2048.0;
	private static volatile double minVisibleY = WORLD_FLOOR;
	private static volatile double camX;
	private static volatile double camY;
	private static volatile double camZ;
	private static volatile double slope;
	private static volatile double keep;
	private static volatile boolean active;

	private HorizonYCull() {
	}

	public static void update(double x, double y, double z, double lookY) {
		HSNConfig cfg = HSNConfig.get();
		if (cfg == null || !cfg.modEnabled || !cfg.horizonYCullEnabled
				|| hsn.optimizations.client.compat.IrisCompat.shadersOn()
				|| hsn.optimizations.client.compat.VoxelSniperCompat.pauseHorizon()) {
			active = false;
			minVisibleY = WORLD_FLOOR;
			return;
		}

		double keepBand = cfg.horizonYKeepBelow;
		if (keepBand < 8.0) {
			keepBand = 8.0;
		} else if (keepBand > 96.0) {
			keepBand = 96.0;
		}

		double far = 192.0;
		double halfV = Math.toRadians(35.0);
		try {
			Minecraft mc = Minecraft.getInstance();
			if (mc != null && mc.options != null) {
				int vd = mc.options.renderDistance().get();
				if (vd < 2) {
					vd = 2;
				}
				far = vd * 16.0;
				halfV = CameraSnapshot.halfVerticalFov();
			}
		} catch (Throwable ignored) {
		}

		double ly = lookY;
		if (ly > 1.0) {
			ly = 1.0;
		} else if (ly < -1.0) {
			ly = -1.0;
		}
		double elev = Math.asin(ly);
		double downElev = elev - halfV;

		double raySlope;
		double rayFloor;
		if (downElev <= -Math.PI * 0.5 + 0.03) {
			raySlope = -1.0;
			rayFloor = WORLD_FLOOR;
		} else {
			raySlope = Math.sin(downElev);
			rayFloor = y + far * raySlope;
		}

		double floor = Math.min(y - keepBand, rayFloor) - 8.0;
		if (floor < WORLD_FLOOR) {
			floor = WORLD_FLOOR;
		}
		camX = x;
		camY = y;
		camZ = z;
		keep = keepBand;
		slope = raySlope;
		minVisibleY = floor;
		active = true;
	}

	public static boolean active() {
		return active && HotPath.masterOn();
	}

	public static double minVisibleY() {
		return minVisibleY;
	}

	public static boolean below(double y) {
		return active && y < minVisibleY;
	}

	public static boolean below(double x, double y, double z) {
		if (!active) {
			return false;
		}
		double dx = x - camX;
		double dz = z - camZ;
		double dist = Math.sqrt(dx * dx + dz * dz);
		double floor = Math.min(camY - keep, camY + dist * slope) - 8.0;
		return y < floor;
	}

	public static boolean boxBelow(double maxY) {
		return active && maxY < minVisibleY;
	}

	public static boolean boxBelow(double x, double z, double maxY) {
		if (!active) {
			return false;
		}
		double dx = x - camX;
		double dz = z - camZ;
		double dist = Math.sqrt(dx * dx + dz * dz);
		double floor = Math.min(camY - keep, camY + dist * slope) - 8.0;
		return maxY < floor;
	}

	public static void markBelow(float[] maxY, byte[] out, int n) {
		if (!active() || maxY == null || out == null || n <= 0) {
			if (out != null && n > 0) {
				int len = Math.min(n, out.length);
				for (int i = 0; i < len; i++) {
					out[i] = 0;
				}
			}
			return;
		}
		int len = Math.min(n, Math.min(maxY.length, out.length));
		float floor = (float) minVisibleY;
		if (NativeBridge.horizonY(maxY, floor, out, len)) {
			return;
		}
		if (WasmSimdKernel.horizonY(maxY, floor, out, len)) {
			return;
		}
		markBelowJava(maxY, floor, out, len);
	}

	public static void markAabb(float[] aabb, byte[] out, int n) {
		if (!active() || aabb == null || out == null || n <= 0) {
			return;
		}
		int len = Math.min(n, Math.min(aabb.length / 6, out.length));
		float[] planes = CameraSnapshot.frustumReady() ? CameraSnapshot.planes() : null;
		if (NativeBridge.horizonAabb(planes, aabb, (float) camY, (float) keep, out, len)) {
			return;
		}
		markAabbJava(planes, aabb, (float) camY, (float) keep, out, len);
	}

	static void markBelowJava(float[] maxY, float floor, byte[] out, int n) {
		int i = 0;
		int bound8 = n - 7;
		while (i < bound8) {
			out[i] = (byte) (maxY[i] < floor ? 1 : 0);
			out[i + 1] = (byte) (maxY[i + 1] < floor ? 1 : 0);
			out[i + 2] = (byte) (maxY[i + 2] < floor ? 1 : 0);
			out[i + 3] = (byte) (maxY[i + 3] < floor ? 1 : 0);
			out[i + 4] = (byte) (maxY[i + 4] < floor ? 1 : 0);
			out[i + 5] = (byte) (maxY[i + 5] < floor ? 1 : 0);
			out[i + 6] = (byte) (maxY[i + 6] < floor ? 1 : 0);
			out[i + 7] = (byte) (maxY[i + 7] < floor ? 1 : 0);
			i += 8;
		}
		while (i < n) {
			out[i] = (byte) (maxY[i] < floor ? 1 : 0);
			i++;
		}
	}

	static void markAabbJava(float[] planes, float[] aabb, float cy, float keepBand, byte[] out, int n) {
		float band = cy - keepBand;
		for (int i = 0; i < n; i++) {
			int b = i * 6;
			float maxy = aabb[b + 4];
			if (maxy >= band) {
				out[i] = 0;
				continue;
			}
			if (planes != null && planes.length >= 24 && planeOut(planes, 16,
					aabb[b], aabb[b + 1], aabb[b + 2], aabb[b + 3], aabb[b + 4], aabb[b + 5])) {
				out[i] = 1;
			} else {
				out[i] = (byte) (maxy < band ? 1 : 0);
			}
		}
	}

	private static boolean planeOut(float[] p, int o,
			float minx, float miny, float minz, float maxx, float maxy, float maxz) {
		float px = p[o] >= 0f ? maxx : minx;
		float py = p[o + 1] >= 0f ? maxy : miny;
		float pz = p[o + 2] >= 0f ? maxz : minz;
		return p[o] * px + p[o + 1] * py + p[o + 2] * pz + p[o + 3] < 0f;
	}
}
