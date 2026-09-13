package hsn.optimizations.client.optimize;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
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
	private static volatile boolean canSeeSky = true;
	private static volatile boolean lookingUp;
	private static volatile int dimensionHash;
	private static volatile double gamma;
	private static volatile int effectBits;
	private static final float[] PLANES = new float[24];
	private static volatile boolean frustumReady;
	private static volatile long lastCaptureNs;

	private CameraSnapshot() {
	}

	public static void capture() {
		long now = System.nanoTime();
		if (valid && now - lastCaptureNs < 2_000_000L) {
			return;
		}
		lastCaptureNs = now;
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
		pullTrueCamera(mc);
		lookingUp = lookY > 0.18;
		Level level = cam.level();
		gameTime = level != null ? level.getGameTime() : 0L;
		dimensionHash = 0;
		hasCeiling = false;
		canSeeSky = true;
		if (level != null) {
			try {
				var dim = level.dimension();
				dimensionHash = System.identityHashCode(dim);
			} catch (Throwable ignored) {
			}
			hasCeiling = detectCeiling(level);
			canSeeSky = !hasCeiling && detectSkyVisible(level, x, y, z);
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
		float halfH = (float) halfHorizontalFov();
		float halfV = (float) halfVerticalFov();
		CameraFrustum.fillPlanes(PLANES, x, y, z, lookX, lookY, lookZ,
				0.05f, 384.0f, halfH, halfV);
		hsn.optimizations.client.compat.VoxelSniperCompat.tick();
		HorizonYCull.update(x, y, z, lookY);
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

	public static boolean canSeeSky() {
		return canSeeSky;
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

	private static boolean detectCeiling(Level level) {
		try {
			Object type = level.getClass().getMethod("dimensionType").invoke(level);
			if (type != null) {
				Object flag = type.getClass().getMethod("hasCeiling").invoke(type);
				if (flag instanceof Boolean b) {
					return b;
				}
			}
		} catch (Throwable ignored) {
		}
		try {
			Object effects = level.getClass().getMethod("dimensionTypeRegistration").invoke(level);
			String text = String.valueOf(effects).toLowerCase();
			if (text.contains("nether") || text.contains("the_nether")) {
				return true;
			}
		} catch (Throwable ignored) {
		}
		return false;
	}

	private static boolean detectSkyVisible(Level level, double px, double py, double pz) {
		try {
			BlockPos pos = BlockPos.containing(px, py + 1.0, pz);
			Object seen = level.getClass().getMethod("canSeeSky", BlockPos.class).invoke(level, pos);
			if (seen instanceof Boolean b) {
				return b;
			}
		} catch (Throwable ignored) {
		}
		try {
			BlockPos pos = BlockPos.containing(px, py + 1.0, pz);
			Object light = level.getClass().getMethod("getRawBrightness", BlockPos.class, int.class)
					.invoke(level, pos, 0);
			if (light instanceof Integer n) {
				return n >= 15;
			}
		} catch (Throwable ignored) {
		}
		return true;
	}

	public static double halfHorizontalFov() {
		double fov = 70.0;
		try {
			Minecraft mc = Minecraft.getInstance();
			if (mc != null && mc.options != null) {
				fov = mc.options.fov().get();
			}
		} catch (Throwable ignored) {
		}
		return Math.toRadians(Math.max(30.0, Math.min(110.0, fov)) * 0.5);
	}

	public static double halfVerticalFov() {
		double halfH = halfHorizontalFov();
		double aspect = 16.0 / 9.0;
		try {
			Minecraft mc = Minecraft.getInstance();
			if (mc != null) {
				var window = mc.getWindow();
				if (window != null && window.getHeight() > 0) {
					aspect = (double) window.getWidth() / (double) window.getHeight();
				}
			}
		} catch (Throwable ignored) {
		}
		if (aspect < 0.5) {
			aspect = 0.5;
		}
		return Math.atan(Math.tan(halfH) / aspect) + Math.toRadians(10.0);
	}

	public static boolean facingAway(double px, double py, double pz) {
		double dx = px - x;
		double dy = py - y;
		double dz = pz - z;
		// Slack so mobs beside the camera stay visible.
		return dx * lookX + dy * lookY + dz * lookZ < -0.15;
	}

	private static void pullTrueCamera(Minecraft mc) {
		try {
			Object renderer = mc.gameRenderer;
			if (renderer == null) {
				return;
			}
			Object camera = null;
			for (String name : new String[]{"getMainCamera", "getCamera"}) {
				try {
					camera = renderer.getClass().getMethod(name).invoke(renderer);
					if (camera != null) {
						break;
					}
				} catch (NoSuchMethodException ignored) {
				}
			}
			if (camera == null) {
				return;
			}
			for (String name : new String[]{"getPosition", "position"}) {
				try {
					Object pos = camera.getClass().getMethod(name).invoke(camera);
					if (pos instanceof Vec3 v) {
						x = v.x;
						y = v.y;
						z = v.z;
						break;
					}
				} catch (NoSuchMethodException ignored) {
				}
			}
			for (String name : new String[]{"getLookVector", "getForwardVector", "forward"}) {
				try {
					Object dir = camera.getClass().getMethod(name).invoke(camera);
					if (dir instanceof Vec3 v) {
						lookX = v.x;
						lookY = v.y;
						lookZ = v.z;
						return;
					}
				} catch (NoSuchMethodException ignored) {
				}
			}
		} catch (Throwable ignored) {
		}
	}
}
