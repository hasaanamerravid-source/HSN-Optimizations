package hsn.optimizations.client.mixin;

import hsn.optimizations.client.optimize.RenderScale;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draw the sky dome onto the native framebuffer so a later world blit
 * cannot replace it with the scaled target's clear color.
 */
@Mixin(targets = "net.minecraft.client.renderer.SkyRenderer", priority = 890)
public class SkyNativeMixin {

	@Inject(method = {
			"renderSky",
			"render",
			"renderSkyDisc",
			"renderDarkDisc",
			"renderSunriseAndSunset",
			"renderStars"
	}, at = @At("HEAD"), require = 0)
	private void hsn$skyNativeBegin(CallbackInfo ci) {
		RenderScale.pauseForSky();
	}

	@Inject(method = {
			"renderSky",
			"render",
			"renderSkyDisc",
			"renderDarkDisc",
			"renderSunriseAndSunset",
			"renderStars"
	}, at = @At("RETURN"), require = 0)
	private void hsn$skyNativeEnd(CallbackInfo ci) {
		RenderScale.resumeAfterSky();
	}
}
