package hsn.optimizations.optimize;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

/**
 * Slider contract: N on the slider is N blocks from the camera to the
 * entity's bounding box (closest point). No second square, no hidden
 * 0.55 priority weight, no adaptive multiply unless Adaptive Culling is on.
 *
 * All methods are static and final, which lets the JIT inline them
 * at every call-site without a virtual dispatch.
 */
public final class ExactDistance {

    private ExactDistance() {
    }

    /**
     * Returns the squared distance from the camera to the nearest point on the
     * entity's AABB. Falls back to centre-point distance if the entity has no box.
     * Returns {@code Double.POSITIVE_INFINITY} for null entities so callers
     * can safely compare without a null-check.
     */
    public static double closestDistSq(Entity entity, double camX, double camY, double camZ) {
        if (entity == null) {
            return Double.POSITIVE_INFINITY;
        }
        AABB box = entity.getBoundingBox();
        if (box == null) {
            double dx = entity.getX() - camX;
            double dy = entity.getY() - camY;
            double dz = entity.getZ() - camZ;
            return dx * dx + dy * dy + dz * dz;
        }
        return closestDistSq(box, camX, camY, camZ);
    }

    /**
     * Closest point on axis-aligned box to the camera, squared.
     * Uses branchless ternary clamps so the JIT emits CMOV / vblendvpd.
     */
    public static double closestDistSq(AABB box, double camX, double camY, double camZ) {
        // clamp each axis to [min, max] then accumulate squared distance
        double cx = camX < box.minX ? box.minX : (camX > box.maxX ? box.maxX : camX);
        double cy = camY < box.minY ? box.minY : (camY > box.maxY ? box.maxY : camY);
        double cz = camZ < box.minZ ? box.minZ : (camZ > box.maxZ ? box.maxZ : camZ);
        double dx = cx - camX;
        double dy = cy - camY;
        double dz = cz - camZ;
        return Math.fma(dz, dz, Math.fma(dy, dy, dx * dx));
    }

    /** Squared distance from a point to the camera. */
    public static double pointDistSq(double x, double y, double z,
                                     double camX, double camY, double camZ) {
        double dx = x - camX;
        double dy = y - camY;
        double dz = z - camZ;
        return Math.fma(dz, dz, Math.fma(dy, dy, dx * dx));
    }

    /**
     * Convert a slider value in blocks to a squared limit with a half-block pad
     * so a slider value of 8 does not pop at 7.99.
     * Returns 0.0 for zero/negative/NaN inputs (never cull at zero distance).
     */
    public static double limitSq(double blocks) {
        if (!(blocks > 0.0)) {
            return 0.0;
        }
        double padded = blocks + 0.5;
        return padded * padded;
    }
}
