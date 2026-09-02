package hsn.modod.client.mixin;

import hsn.modod.client.optimize.AdaptiveCuller;
import hsn.modod.config.HSNConfig;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Block-entity view distance + soft LOD.
 *
 * 1. Caps the renderer view distance (chests, signs, banners, skulls, …).
 * 2. When LOD is enabled, further reduces the distance under adaptive load
 *    so expensive BE renderers drop out earlier on weak GPUs.
 *
 * Terrain / block models stay with Sodium — this only affects the extra
 * per-frame draw calls from block-entity renderers.
 */
@Mixin(BlockEntityRenderer.class)
public interface BlockEntityViewDistanceMixin {

	@Inject(method = "getViewDistance", at = @At("HEAD"), cancellable = true)
	private void hsn$scaleViewDistance(CallbackInfoReturnable<Integer> cir) {
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.blockEntityCullingEnabled) {
			return;
		}

		double dist = cfg.maxBlockEntityRenderDistance;

		// Soft LOD: under load, start culling earlier
		if (cfg.blockEntityLodEnabled) {
			double scale = AdaptiveCuller.getScale();
			double lodStart = cfg.blockEntityLodDistance;
			// Blend between full distance and LOD start based on adaptive scale
			dist = lodStart + (dist - lodStart) * scale;
			if (cfg.performanceModeEnabled) {
				dist = Math.min(dist, lodStart);
			}
		}

		int scaled = (int) Math.max(8, Math.round(dist));
		cir.setReturnValue(scaled);
	}
}
