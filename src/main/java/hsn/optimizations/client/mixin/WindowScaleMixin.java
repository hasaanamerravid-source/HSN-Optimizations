package hsn.optimizations.client.mixin;

import hsn.optimizations.client.optimize.RenderScale;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Only rewrite framebuffer pixel queries during the world pass.
 * {@code getWidth}/{@code getHeight} stay native so the HUD, mouse, and
 * 26.3 windowing backend keep a stable GUI size. Scaling those used to
 * collapse the window or stretch the overlay.
 */
@Mixin(targets = "com.mojang.blaze3d.platform.Window", priority = 900)
public class WindowScaleMixin {

	@Inject(method = {"getFramebufferWidth"},
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

	@Inject(method = {"getFramebufferHeight"},
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
