package hsn.optimizations.client.mixin;

import hsn.optimizations.client.compat.HSNModCompat;
import hsn.optimizations.client.optimize.CameraSnapshot;
import hsn.optimizations.client.optimize.CullStats;
import hsn.optimizations.client.optimize.HorizonYCull;
import hsn.optimizations.client.optimize.RenderShapeCuller;
import hsn.optimizations.client.optimize.SodiumVisibleSections;
import hsn.optimizations.optimize.ExactDistance;
import hsn.optimizations.optimize.HotPath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public class EntityRendererMixin {

	@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
	private void hsnCull(Entity entity, Frustum frustum, double camX, double camY, double camZ,
						  CallbackInfoReturnable<Boolean> cir) {
		final int bits = HotPath.bits();
		if ((bits & HotPath.ENTITY_CULL) == 0) {
			return;
		}
		if (hsn.optimizations.client.compat.VoxelSniperCompat.pauseEntityCull()) {
			return;
		}
		if ((bits & HotPath.DEFER_ENTITY_MODS) != 0 && HSNModCompat.entityCullingModPresent()) {
			return;
		}
		if (entity == null) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		if (mc != null && (entity == mc.getCameraEntity() || entity == mc.player)) {
			return;
		}

		// Closest point on the AABB to the camera, in blocks.
		// Slider 8 => visible through 8.5 blocks. No second square.
		double distSq = ExactDistance.closestDistSq(entity, camX, camY, camZ);

		double limitSq;
		if (entity instanceof ItemEntity) {
			limitSq = HotPath.itemDistSq();
		} else if (entity instanceof ExperienceOrb) {
			limitSq = HotPath.xpDistSq();
		} else if (entity instanceof ArmorStand || entity instanceof ItemFrame
				|| entity instanceof Painting || entity instanceof Display) {
			limitSq = HotPath.decoDistSq();
		} else {
			limitSq = HotPath.entityDistSq();
		}

		if (distSq > limitSq) {
			CullStats.entitySkip();
			cir.setReturnValue(false);
			return;
		}

		if ((bits & HotPath.BEHIND_CAMERA) != 0 && distSq > limitSq * 0.25) {
			double dx;
			double dy;
			double dz;
			var box = entity.getBoundingBox();
			if (box != null) {
				double cx = camX < box.minX ? box.minX : (camX > box.maxX ? box.maxX : camX);
				double cy = camY < box.minY ? box.minY : (camY > box.maxY ? box.maxY : camY);
				double cz = camZ < box.minZ ? box.minZ : (camZ > box.maxZ ? box.maxZ : camZ);
				dx = cx - camX;
				dy = cy - camY;
				dz = cz - camZ;
			} else {
				dx = entity.getX() - camX;
				dy = entity.getY() - camY;
				dz = entity.getZ() - camZ;
			}
			double lx, ly, lz;
			if (CameraSnapshot.valid()) {
				lx = CameraSnapshot.lookX();
				ly = CameraSnapshot.lookY();
				lz = CameraSnapshot.lookZ();
			} else {
				lx = ly = lz = 0.0;
			}
			if (lx * lx + ly * ly + lz * lz > 1.0e-6 && dx * lx + dy * ly + dz * lz < 0.0) {
				CullStats.entitySkip();
				cir.setReturnValue(false);
				return;
			}
		}

		// Occupancy is a far-only hint. Never apply inside 24 blocks, and
		// fail-open when Sodium has not published a usable set this frame.
		if ((bits & HotPath.SECTION_OCCUPANCY) != 0 && distSq > 576.0
				&& SodiumVisibleSections.isUsable()
				&& !SodiumVisibleSections.containsBlock(entity.getX(), entity.getY(), entity.getZ())) {
			CullStats.entitySkip();
			cir.setReturnValue(false);
			return;
		}

		if ((bits & HotPath.HORIZON_Y) != 0) {
			var box = entity.getBoundingBox();
			double top = box != null ? box.maxY : entity.getY() + 1.0;
			if (HorizonYCull.boxBelow(entity.getX(), entity.getZ(), top)) {
				CullStats.entitySkip();
				cir.setReturnValue(false);
				return;
			}
		}

		if ((bits & HotPath.SHAPE_MASK) != 0
				&& !RenderShapeCuller.shouldDrawWorldPoint(entity.getX(), entity.getY(), entity.getZ())) {
			CullStats.entitySkip();
			cir.setReturnValue(false);
		}
	}
}
