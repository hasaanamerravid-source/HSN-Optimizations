package hsn.modod.client.mixin;

import hsn.modod.client.optimize.AdaptiveCuller;
import hsn.modod.client.optimize.CullStats;
import hsn.modod.client.optimize.EntityPriority;
import hsn.modod.config.HSNConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Distance-based entity render culling.
 * Players are never culled. Item/XP/decoration entities use tighter limits.
 * Limits are further scaled by live FPS (AdaptiveCuller) and entity
 * priority (EntityPriority), so low-value entities drop first under load.
 */
@Mixin(EntityRenderer.class)
public class EntityRendererMixin {

	@Inject(
			method = "shouldRender(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/culling/Frustum;DDD)Z",
			at = @At("HEAD"), cancellable = true, require = 0)
	private void hsnCull(Entity entity, net.minecraft.client.renderer.culling.Frustum frustum,
						 double camX, double camY, double camZ,
						 CallbackInfoReturnable<Boolean> cir) {
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.entityCullingEnabled) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		Player player = client.player;
		if (player == null || entity == player) {
			return;
		}

		double dx = entity.getX() - player.getX();
		double dy = entity.getY() - player.getY();
		double dz = entity.getZ() - player.getZ();
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

		if (distSq > limit * limit) {
			CullStats.entitySkip();
			cir.setReturnValue(false);
		}
	}
}
