package hsn.optimizations.client.mixin;

import hsn.optimizations.client.optimize.RenderScale;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Official 26.2 mappings only. Yarn {@code client.util.Window} is not on this game.
 */
@Mixin(targets = "com.mojang.blaze3d.platform.Window", priority = 900)
public class WindowScaleMixin {

	@Inject(method = {"getWidth", "getFramebufferWidth"},
			at = @At("RETURN"), cancellable = true, require = 0)
	private void hsn$scaleWidth(CallbackInfoReturnable<Integer> cir) {
		if (!RenderScale.worldPassActive()) {
			return;
		}
		int scaled = RenderScale.scaled(cir.getReturnValueI());
		if (scaled != cir.getReturnValueI()) {
			cir.setReturnValue(scaled);
		}
	}

	@Inject(method = {"getHeight", "getFramebufferHeight"},
			at = @At("RETURN"), cancellable = true, require = 0)
	private void hsn$scaleHeight(CallbackInfoReturnable<Integer> cir) {
		if (!RenderScale.worldPassActive()) {
			return;
		}
		int scaled = RenderScale.scaled(cir.getReturnValueI());
		if (scaled != cir.getReturnValueI()) {
			cir.setReturnValue(scaled);
		}
	}

	@Inject(method = {"onFramebufferResize", "refreshFramebufferSize", "updateFramebufferSize"},
			at = @At("RETURN"), require = 0)
	private void hsn$resized(CallbackInfo ci) {
		RenderScale.onFramebufferResized();
	}
}
