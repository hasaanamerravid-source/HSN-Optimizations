package hsn.modod.client.optimize;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Camera / player pose captured once per frame. Mixins that run thousands of
 * times per tick read these primitives instead of walking Minecraft.getInstance()
 * + camera entity + level every call.
 */
public final class CameraSnapshot {

	private static volatile double x;
	private static volatile double y;
	private static volatile double z;
	private static volatile double lookX;
	private static volatile double lookY;
	private static volatile double lookZ;
	private static volatile long gameTime;
	private static volatile boolean valid;
	private static volatile boolean windowActive = true;
	private static volatile boolean hasCeiling;
	private static volatile boolean lookingUp;
	private static volatile int dimensionHash;
	private static volatile double gamma;
	private static volatile int effectBits;
	private static final float[] PLANES = new float[24];
	private static volatile boolean frustumReady;

	private CameraSnapshot() {
	}

	public static void capture() {
		Minecraft mc = Minecraft.getInstance();
		if (mc == null) {
			valid = false;
			frustumReady = false;
			return;
		}
		windowActive = mc.isWindowActive();
		Entity cam = mc.getCameraEntity();
		if (cam == null) {
			cam = mc.player;
		}
		if (cam == null) {
			valid = false;
			frustumReady = false;
			return;
		}
		x = cam.getX();
		y = cam.getY();
		z = cam.getZ();
		Vec3 look = cam.getViewVector(1.0f);
		lookX = look.x;
		lookY = look.y;
		lookZ = look.z;
		lookingUp = lookY > 0.18;
		Level level = cam.level();
		gameTime = level != null ? level.getGameTime() : 0L;
		dimensionHash = 0;
		hasCeiling = false;
		if (level != null) {
			try {
				var dim = level.dimension();
				dimensionHash = System.identityHashCode(dim);
				String path = dim.identifier().getPath();
				hasCeiling = "the_nether".equals(path) || "nether".equals(path);
			} catch (Throwable ignored) {
			}
		}
		try {
			gamma = mc.options.gamma().get();
		} catch (Throwable ignored) {
			gamma = 0.0;
		}
		effectBits = 0;
		LocalPlayer player = mc.player;
		if (player != null) {
			if (player.isOnFire()) {
				effectBits |= 4;
			}
			if (player.isUnderWater()) {
				effectBits |= 8;
			}
		}
		CameraFrustum.fillPlanes(PLANES, x, y, z, lookX, lookY, lookZ,
				0.05f, 384.0f,
				(float) Math.toRadians(70.0),
				(float) Math.toRadians(50.0));
		frustumReady = true;
		valid = true;
	}

	public static boolean frustumReady() {
		return frustumReady && valid;
	}

	/** Live 24-float plane pack. Callers must not write it. */
	public static float[] planes() {
		return PLANES;
	}

	public static boolean valid() {
		return valid;
	}

	public static double x() {
		return x;
	}

	public static double y() {
		return y;
	}

	public static double z() {
		return z;
	}

	public static double lookX() {
		return lookX;
	}

	public static double lookY() {
		return lookY;
	}

	public static double lookZ() {
		return lookZ;
	}

	public static long gameTime() {
		return gameTime;
	}

	public static boolean windowActive() {
		return windowActive;
	}

	public static boolean hasCeiling() {
		return hasCeiling;
	}

	public static boolean lookingUp() {
		return lookingUp;
	}

	public static int lightSignature() {
		int g = (int) Math.round(gamma * 100.0);
		return (dimensionHash * 31) ^ (g << 8) ^ effectBits ^ (hasCeiling ? 0x10000 : 0);
	}

	public static double distSq(double px, double py, double pz) {
		double dx = px - x;
		double dy = py - y;
		double dz = pz - z;
		return dx * dx + dy * dy + dz * dz;
	}

	public static boolean facingAway(double px, double py, double pz) {
		double dx = px - x;
		double dy = py - y;
		double dz = pz - z;
		return dx * lookX + dy * lookY + dz * lookZ < 0.0;
	}
}
