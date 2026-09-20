package hsn.optimizations.client.mixin;

import hsn.optimizations.client.optimize.RenderScale;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Official 26.2 target only. Yarn Framebuffer is not on this game.
 * Inject-only so MixinExtras does not wrap a ModifyVariable.
 */
@Mixin(targets = "com.mojang.blaze3d.pipeline.RenderTarget", priority = 999)
public class RenderTargetFilterMixin {

	@Inject(method = {"setFilterMode", "setTexFilter"}, at = @At("HEAD"), require = 0)
	private void hsn$noteFilter(CallbackInfo ci) {
		RenderScale.glFilter();
	}
}
