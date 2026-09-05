package hsn.modod.client.mixin;

import hsn.modod.client.compat.HSNModCompat;
import hsn.modod.client.optimize.CullStats;
import hsn.modod.client.optimize.RenderShapeCuller;
import hsn.modod.client.optimize.SodiumVisibleSections;
import hsn.modod.optimize.HotPath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
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
		if (!HotPath.masterOn() || !HotPath.flag(HotPath.ENTITY_CULL)) {
			return;
		}
		if (HotPath.flag(HotPath.DEFER_ENTITY_MODS) && HSNModCompat.entityCullingModPresent()) {
			return;
		}
		if (entity == null) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		if (mc != null && (entity == mc.getCameraEntity() || entity == mc.player)) {
			return;
		}

		// Distance is camera-to-entity in blocks. The slider value is that
		// number. No priority weight, no LOD shrink, no second square.
		double dx = entity.getX() - camX;
		double dy = entity.getY() - camY;
		double dz = entity.getZ() - camZ;
		double distSq = dx * dx + dy * dy + dz * dz;

		double limitSq;
		if (entity instanceof ItemEntity) {
			limitSq = HotPath.itemDistSq();
		} else if (entity instanceof ExperienceOrb) {
			limitSq = HotPath.xpDistSq();
		} else if (entity instanceof ArmorStand || entity instanceof ItemFrame) {
			limitSq = HotPath.decoDistSq();
		} else {
			limitSq = HotPath.entityDistSq();
		}

		if (distSq > limitSq) {
			CullStats.entitySkip();
			cir.setReturnValue(false);
			return;
		}

		// Occupancy is a far-only hint. Applying it inside ~24 blocks hid
		// nearby mobs whenever Sodium's visited-section set was incomplete
		// (looked like "32-block slider culls at 3 blocks").
		if (HotPath.flag(HotPath.SECTION_OCCUPANCY) && distSq > 576.0
				&& !SodiumVisibleSections.containsBlock(entity.getX(), entity.getY(), entity.getZ())) {
			CullStats.entitySkip();
			cir.setReturnValue(false);
			return;
		}

		if (!RenderShapeCuller.shouldDrawWorldPoint(entity.getX(), entity.getY(), entity.getZ())) {
			CullStats.entitySkip();
			cir.setReturnValue(false);
		}
	}
}
