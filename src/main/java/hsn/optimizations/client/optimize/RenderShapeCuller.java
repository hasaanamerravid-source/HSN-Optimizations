package hsn.optimizations.client.optimize;

import hsn.optimizations.client.compat.HSNModCompat;
import hsn.optimizations.config.HSNConfig;
import hsn.optimizations.config.WorldRenderShape;
import hsn.optimizations.optimize.HotPath;
import hsn.optimizations.optimize.NativeBridge;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.world.phys.AABB;

import java.util.Iterator;
import java.util.List;

/**
 * Optional draw-mask for already-built sections. Loading and simulation stay
 * square. Nearby sections around the camera are never removed so the ground
 * under the player cannot disappear.
 */
public final class RenderShapeCuller {

	private static final double SQRT3 = Math.sqrt(3.0);
	private static final double SECTION = 16.0;
	private static final int MIN_KEEP_CHUNKS = 2;

	private static boolean[] dropBuf = new boolean[0];
	private static double[] xsBuf = new double[0];
	private static double[] ysBuf = new double[0];
	private static double[] zsBuf = new double[0];
	private static float[] aabbBuf = new float[0];
	private static byte[] farBuf = new byte[0];
	private static byte[] frustumBuf = new byte[0];
	private static byte[] yDropBuf = new byte[0];
	private static int[] mapBuf = new int[0];

	private static void ensure(int n) {
		if (dropBuf.length >= n) {
			return;
		}
		int cap = Math.max(n, dropBuf.length * 2);
		if (cap < 64) {
			cap = 64;
		}
		dropBuf = new boolean[cap];
		xsBuf = new double[cap];
		ysBuf = new double[cap];
		zsBuf = new double[cap];
		aabbBuf = new float[cap * 6];
		farBuf = new byte[cap];
		frustumBuf = new byte[cap];
		yDropBuf = new byte[cap];
		mapBuf = new int[cap];
	}

	private RenderShapeCuller() {
	}

	public static boolean isActive() {
		if (HSNModCompat.shapeModPresent() || hsn.optimizations.client.compat.VoxelSniperCompat.pauseTerrainMask()) {
			return false;
		}
		HSNConfig cfg = HSNConfig.get();
		return cfg != null && cfg.circularRenderingEnabled && cfg.worldRenderShape != WorldRenderShape.OFF;
	}

	public static void filterSections(List<SectionRenderDispatcher.RenderSection> sections) {
		filterSections(sections, false);
	}

	/**
	 * @param nearby if true, the list is the renderer's near ring and must not
	 *               be thinned (holes under / beside the player).
	 */
	public static void filterSections(List<SectionRenderDispatcher.RenderSection> sections, boolean nearby) {
		if (nearby || sections == null || sections.isEmpty()) {
			return;
		}
		boolean shapeOn = isActive();
		boolean yOn = HorizonYCull.active() || (HSNConfig.get() != null && HSNConfig.get().circularVerticalRangeEnabled);
		if (!shapeOn && !yOn) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		Camera camera = camera(mc);
		if (camera == null) {
			return;
		}

		double camX = camera.position().x;
		double camY = camera.position().y;
		double camZ = camera.position().z;
		double yaw = camera.yRot() * (Math.PI / 180.0);
		double fwdX = -Math.sin(yaw);
		double fwdZ = Math.cos(yaw);
		double radius = radiusBlocks(mc);
		double keep = keepBlocks();

		try {
			int n = sections.size();
			ensure(n);
			boolean[] drop = dropBuf;
			double[] xs = xsBuf;
			double[] ys = ysBuf;
			double[] zs = zsBuf;
			float[] aabb = aabbBuf;
			byte[] yDrop = yDropBuf;
			byte[] far = farBuf;
			byte[] frustum = frustumBuf;
			int[] map = mapBuf;
			java.util.Arrays.fill(drop, 0, n, false);
			int filled = 0;
			for (int i = 0; i < n; i++) {
				SectionRenderDispatcher.RenderSection section = sections.get(i);
				if (section == null) {
					continue;
				}
				AABB box = section.getBoundingBox();
				if (box == null) {
					continue;
				}
				if (containsCamera(box, camX, camY, camZ) || nearCamera(box, camX, camZ, keep)) {
					continue;
				}
				if (HSNConfig.get().circularVerticalRangeEnabled) {
					double range = HSNConfig.get().circularVerticalRange;
					if (box.maxY < camY - range || box.minY > camY + range) {
						drop[i] = true;
						continue;
					}
				}
				map[filled] = i;
				xs[filled] = closest(camX, box.minX, box.maxX);
				ys[filled] = closest(camY, box.minY, box.maxY);
				zs[filled] = closest(camZ, box.minZ, box.maxZ);
				int b = filled * 6;
				aabb[b] = (float) box.minX;
				aabb[b + 1] = (float) box.minY;
				aabb[b + 2] = (float) box.minZ;
				aabb[b + 3] = (float) box.maxX;
				aabb[b + 4] = (float) box.maxY;
				aabb[b + 5] = (float) box.maxZ;
				filled++;
			}
			if (HorizonYCull.active() && filled > 0) {
				HorizonYCull.markAabb(aabb, yDrop, filled);
				for (int i = 0; i < filled; i++) {
					if (yDrop[i] != 0) {
						drop[map[i]] = true;
					}
				}
			}
			double limitSq = radius * radius;
			if (shapeOn && filled > 0) {
				BatchDistance.cullXyz(xs, ys, zs, filled, camX, camY, camZ, limitSq, far);
			}
			boolean frustumOn = shapeOn && HotPath.flag(HotPath.NATIVE_FRUSTUM) && NativeBridge.frustumEnabled();
			if (frustumOn && filled > 0) {
				float[] planes;
				if (CameraSnapshot.frustumReady()) {
					planes = CameraSnapshot.planes();
				} else {
					planes = new float[24];
					double lookX = CameraSnapshot.lookX();
					double lookY = CameraSnapshot.lookY();
					double lookZ = CameraSnapshot.lookZ();
					if (!CameraSnapshot.valid()) {
						lookX = fwdX;
						lookY = 0;
						lookZ = fwdZ;
					}
					CameraFrustum.fillPlanes(planes, camX, camY, camZ, lookX, lookY, lookZ,
							0.05f, (float) Math.max(radius * 1.25, keep + 16.0),
							(float) Math.toRadians(70.0), (float) Math.toRadians(50.0));
				}
				NativeBridge.cullAabb(planes, aabb, frustum, filled);
			}
			boolean any = false;
			for (int i = 0; i < n; i++) {
				if (drop[i]) {
					any = true;
					break;
				}
			}
			for (int i = 0; i < filled; i++) {
				boolean hide = false;
				if (shapeOn) {
					hide = far[i] != 0
							|| !insideShape(xs[i] - camX, zs[i] - camZ, fwdX, fwdZ, radius, keep)
							|| (frustumOn && frustum[i] != 0);
				}
				if (hide) {
					drop[map[i]] = true;
					any = true;
				}
			}
			if (any) {
				Iterator<SectionRenderDispatcher.RenderSection> it = sections.iterator();
				int idx = 0;
				while (it.hasNext()) {
					it.next();
					if (idx < drop.length && drop[idx]) {
						it.remove();
						CullStats.sectionSkip();
					}
					idx++;
				}
			}
		} catch (UnsupportedOperationException ignored) {
			// Some render lists are immutable; skip rather than crash.
		}
	}

	public static boolean shouldDrawWorldPoint(double x, double y, double z) {
		if (HorizonYCull.below(x, y, z)) {
			return false;
		}
		if (!isActive()) {
			return true;
		}
		Minecraft mc = Minecraft.getInstance();
		Camera camera = camera(mc);
		if (camera == null) {
			return true;
		}
		double dx = x - camera.position().x;
		double dz = z - camera.position().z;
		double keep = keepBlocks();
		if (dx * dx + dz * dz <= keep * keep) {
			return true;
		}
		double yaw = camera.yRot() * (Math.PI / 180.0);
		return insideShape(dx, dz, -Math.sin(yaw), Math.cos(yaw), radiusBlocks(mc), keep);
	}

	private static boolean containsCamera(AABB box, double x, double y, double z) {
		return x >= box.minX - 1.0 && x <= box.maxX + 1.0
				&& z >= box.minZ - 1.0 && z <= box.maxZ + 1.0
				&& y >= box.minY - SECTION && y <= box.maxY + SECTION;
	}

	private static boolean nearCamera(AABB box, double camX, double camZ, double keep) {
		double dx = closest(camX, box.minX, box.maxX) - camX;
		double dz = closest(camZ, box.minZ, box.maxZ) - camZ;
		return dx * dx + dz * dz <= keep * keep;
	}

	private static double closest(double value, double min, double max) {
		if (value < min) {
			return min;
		}
		if (value > max) {
			return max;
		}
		return value;
	}

	private static boolean insideShape(double dx, double dz, double fwdX, double fwdZ, double radius, double keep) {
		double distSq = dx * dx + dz * dz;
		if (distSq <= keep * keep) {
			return true;
		}
		if (radius <= keep) {
			return distSq <= keep * keep;
		}
		double along = dx * fwdX + dz * fwdZ;
		double side = dx * (-fwdZ) + dz * fwdX;
		WorldRenderShape shape = HSNConfig.get().worldRenderShape;
		return switch (shape) {
			case CIRCLE -> distSq <= radius * radius;
			case SEMICIRCLE -> along >= -keep && distSq <= radius * radius;
			case HEXAGON -> insideHexagon(side, along, radius);
			default -> true;
		};
	}

	private static boolean insideHexagon(double x, double z, double radius) {
		if (radius <= 1.0) {
			return true;
		}
		double q = (SQRT3 / 3.0 * x - z / 3.0) / radius;
		double r = (2.0 / 3.0 * z) / radius;
		double s = -q - r;
		return Math.max(Math.abs(q), Math.max(Math.abs(r), Math.abs(s))) <= 1.0;
	}

	private static double radiusBlocks(Minecraft mc) {
		int view = 12;
		try {
			if (mc != null && mc.options != null) {
				view = Math.max(2, mc.options.renderDistance().get());
			}
		} catch (Throwable ignored) {
		}
		double scale = HSNConfig.get().circularRadiusScale;
		if (scale < 0.25) {
			scale = 0.25;
		}
		return view * SECTION * scale;
	}

	private static double keepBlocks() {
		int chunks = HSNConfig.get().alwaysKeepChunks;
		if (chunks < MIN_KEEP_CHUNKS) {
			chunks = MIN_KEEP_CHUNKS;
		}
		return chunks * SECTION;
	}

	private static Camera camera(Minecraft mc) {
		if (mc == null || mc.gameRenderer == null) {
			return null;
		}
		Camera camera = mc.gameRenderer.mainCamera();
		if (camera == null || !camera.isInitialized()) {
			return null;
		}
		return camera;
	}
}
