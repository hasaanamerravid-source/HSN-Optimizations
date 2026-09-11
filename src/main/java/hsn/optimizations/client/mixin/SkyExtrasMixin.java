package hsn.optimizations.client.mixin;

import hsn.optimizations.client.compat.IrisCompat;
import hsn.optimizations.client.optimize.CameraSnapshot;
import hsn.optimizations.config.HSNConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = {
		"net.minecraft.client.renderer.SkyRenderer"
}, priority = 905)
public class SkyExtrasMixin {

	@Inject(method = {
			"renderStars",
			"renderSunriseAndSunset",
			"renderDarkDisc",
			"renderSky"
	}, at = @At("HEAD"), cancellable = true, require = 0)
	private void hsn$skipHiddenSky(CallbackInfo ci) {
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
