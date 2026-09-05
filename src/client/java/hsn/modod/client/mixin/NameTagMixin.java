package hsn.modod.client.mixin;

import hsn.modod.config.HSNConfig;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tightens the distance at which name tags are shown, independent of the
 * entity's own render-distance cutoff (EntityRendererMixin).
 */
@Mixin(EntityRenderer.class)
public class NameTagMixin {

	@Inject(method = "shouldShowName", at = @At("HEAD"), cancellable = true, require = 0)
	private void hsn$nameTagCull(Entity entity, double distanceSq, CallbackInfoReturnable<Boolean> cir) {
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.modEnabled) {
			return;
		}
		if (!cfg.nameTagCullEnabled || entity == null) {
			return;
		}
		double limit = cfg.maxNameTagDistance;
		if (distanceSq > limit * limit) {
			cir.setReturnValue(false);
		}
	}
}
