package hsn.modod.client.mixin;

import hsn.modod.config.HSNConfig;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Skips ground-shadow rendering for entities beyond a configurable distance.
 * Returning a shadow radius of 0 makes vanilla's own shadow code short-circuit
 * before any block-state lookups for the shadow shape — this only removes the
 * decorative blob under distant entities, never their model or animation.
 */
@Mixin(EntityRenderer.class)
public class EntityShadowMixin {

	@Inject(method = "getShadowRadius", at = @At("HEAD"), cancellable = true)
	private void hsnCullShadow(EntityRenderState state, CallbackInfoReturnable<Float> cir) {
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.shadowCullingEnabled || state == null) {
			return;
		}
		double max = cfg.maxShadowDistance;
		if (state.distanceToCameraSq > max * max) {
			cir.setReturnValue(0.0f);
		}
	}
}
