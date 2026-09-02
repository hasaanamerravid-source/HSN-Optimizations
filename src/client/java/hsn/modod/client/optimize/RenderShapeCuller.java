package hsn.modod.client.optimize;

import hsn.modod.client.compat.HSNModCompat;
import hsn.modod.config.HSNConfig;
import hsn.modod.config.WorldRenderShape;
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

	private RenderShapeCuller() {
	}

	public static boolean isActive() {
		if (HSNModCompat.shapeModPresent()) {
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
		if (nearby || sections == null || sections.isEmpty() || !isActive()) {
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
			Iterator<SectionRenderDispatcher.RenderSection> it = sections.iterator();
			while (it.hasNext()) {
				SectionRenderDispatcher.RenderSection section = it.next();
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
				double x = closest(camX, box.minX, box.maxX);
				double z = closest(camZ, box.minZ, box.maxZ);
				if (!insideShape(x - camX, z - camZ, fwdX, fwdZ, radius, keep)) {
					it.remove();
					CullStats.sectionSkip();
				}
			}
		} catch (UnsupportedOperationException ignored) {
			// Some render lists are immutable; skip rather than crash.
		}
	}

	public static boolean shouldDrawWorldPoint(double x, double y, double z) {
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
