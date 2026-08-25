package hsn.modod.client.mixin;

import hsn.modod.client.optimize.AdaptiveCuller;
import hsn.modod.client.optimize.CullStats;
import hsn.modod.config.HSNConfig;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Strips the glowing outline from distant entities after vanilla fills render state.
 * Does not hide the entity itself — only clears outlineColor / glowing on the state.
 */
@Mixin(EntityRenderer.class)
public class GlowOutlineMixin {

	@Inject(method = "extractRenderState", at = @At("RETURN"))
	private void hsn$cullDistantGlow(Entity entity, EntityRenderState state, float partialTick, CallbackInfo ci) {
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.glowOutlineCullingEnabled || entity == null || state == null) {
			return;
		}
		if (!state.appearsGlowing()) {
			return;
		}
		double limit = cfg.maxGlowOutlineDistance * AdaptiveCuller.getScale();
		if (state.distanceToCameraSq > limit * limit) {
			state.outlineColor = EntityRenderState.NO_OUTLINE;
			CullStats.entitySkip();
		}
	}
}
