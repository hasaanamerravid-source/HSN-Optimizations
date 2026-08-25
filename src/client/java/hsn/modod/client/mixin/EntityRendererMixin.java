package hsn.modod.client.mixin;

import hsn.modod.client.compat.HSNModCompat;
import hsn.modod.client.optimize.AdaptiveCuller;
import hsn.modod.client.optimize.CullStats;
import hsn.modod.client.optimize.EntityPriority;
import hsn.modod.client.optimize.DistanceLod;
import hsn.modod.config.HSNConfig;
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

/**
 * Distance-based entity render culling, applied on top of vanilla's frustum-only
 * check in {@link EntityRenderDispatcher#shouldRender}. Item/XP/decoration entities
 * use tighter limits than regular mobs; limits are further scaled by live FPS
 * (AdaptiveCuller) and entity priority (EntityPriority), so low-value entities drop
 * first under load. No-ops when a dedicated entity-culling mod is installed and
 * "Defer to entity-culling mods" is enabled, so the two don't fight over the same
 * decision.
 */
@Mixin(EntityRenderDispatcher.class)
public class EntityRendererMixin {

	@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
	private void hsnCull(Entity entity, Frustum frustum, double camX, double camY, double camZ,
						  CallbackInfoReturnable<Boolean> cir) {
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.entityCullingEnabled) {
			return;
		}
		if (cfg.deferToDedicatedEntityCullingMods && HSNModCompat.entityCullingModPresent()) {
			return;
		}

		double dx = entity.getX() - camX;
		double dy = entity.getY() - camY;
		double dz = entity.getZ() - camZ;
		double distSq = dx * dx + dy * dy + dz * dz;

		double limit;
		if (entity instanceof ItemEntity) {
			limit = cfg.maxItemEntityRenderDistance;
		} else if (entity instanceof ExperienceOrb) {
			limit = cfg.maxXpOrbRenderDistance;
		} else if (entity instanceof ArmorStand || entity instanceof ItemFrame) {
			limit = cfg.maxDecorationEntityDistance;
		} else {
			limit = cfg.maxEntityRenderDistance;
		}

		limit *= AdaptiveCuller.getScale() * EntityPriority.weight(entity);

		// Progressive LOD: near the edge of the limit, quality drops.
		// Stage 3 entities are culled slightly earlier for a smoother falloff.
		if (cfg.progressiveLodEnabled && cfg.entityLodStagesEnabled) {
			int stage = DistanceLod.entityStage(distSq, limit);
			if (stage >= 3) {
				// Treat near-edge stage as a soft early cull (extra ~8% tighter)
				limit *= 0.92;
			}
		}

		if (distSq > limit * limit) {
			CullStats.entitySkip();
			cir.setReturnValue(false);
		}
	}
}
