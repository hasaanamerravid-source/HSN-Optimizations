package hsn.optimizations.client.mixin;

import hsn.optimizations.client.compat.IrisCompat;
import hsn.optimizations.client.optimize.CameraSnapshot;
import hsn.optimizations.config.HSNConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skip stars / sunrise only under a real dimension ceiling (Nether).
 * Never cancel {@code renderSkyDisc} / {@code renderSky} — that is the
 * blue dome and looks like the sky option was turned off.
 */
@Mixin(targets = {
		"net.minecraft.client.renderer.SkyRenderer"
}, priority = 905)
public class SkyExtrasMixin {

	@Inject(method = {
			"renderStars",
			"renderSunriseAndSunset",
			"renderSunMoonAndStars"
	}, at = @At("HEAD"), cancellable = true, require = 0)
	private void hsn$skipHiddenSkyExtras(CallbackInfo ci) {
		if (!hsn.optimizations.optimize.HotPath.masterOn()) {
			return;
		}
		if (IrisCompat.shadersOn()) {
			return;
		}
		if (!HSNConfig.get().skyExtrasThrottleEnabled) {
			return;
		}
		if (!CameraSnapshot.valid()) {
			return;
		}
		if (CameraSnapshot.hasCeiling()) {
			ci.cancel();
		}
	}
}
