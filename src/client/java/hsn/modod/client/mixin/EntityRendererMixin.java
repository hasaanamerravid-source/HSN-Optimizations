package hsn.modod.client.mixin;

import hsn.modod.client.compat.HSNModCompat;
import hsn.modod.client.optimize.CullStats;
import hsn.modod.client.optimize.EntityLod;
import hsn.modod.client.optimize.EntityPriority;
import hsn.modod.client.optimize.RenderShapeCuller;
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
		if (!HotPath.flag(HotPath.ENTITY_CULL)) {
			return;
		}
		if (HotPath.flag(HotPath.DEFER_ENTITY_MODS) && HSNModCompat.entityCullingModPresent()) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		if (mc != null && (entity == mc.getCameraEntity() || entity == mc.player)) {
			return;
		}

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

		double weight = EntityPriority.weight(entity);
		limitSq *= weight * weight;
		if (HotPath.flag(HotPath.ENTITY_LOD) && weight < 0.8 && EntityLod.shouldSimplify(entity, distSq)) {
			limitSq *= 0.7225;
		}

		if (distSq > limitSq) {
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
