package hsn.optimizations.client.mixin;

import hsn.optimizations.optimize.HotPath;
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
		if (entity == null || !HotPath.flag(HotPath.NAME_TAG_CULL)) {
			return;
		}
		if (distanceSq > HotPath.nameTagDistSq()) {
			cir.setReturnValue(false);
		}
	}
}
