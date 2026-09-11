package hsn.optimizations.client.mixin;

import hsn.optimizations.client.compat.IrisCompat;
import hsn.optimizations.client.optimize.CameraSnapshot;
import hsn.optimizations.optimize.HotPath;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = {
		"net.minecraft.client.renderer.WeatherEffectRenderer"
}, priority = 910)
public class WeatherLodMixin {

	@Inject(method = {
			"render",
			"renderSnowAndRain",
			"tickRain"
	}, at = @At("HEAD"), cancellable = true, require = 0)
	private void hsn$skipHiddenWeather(CallbackInfo ci) {
		if (!HotPath.flag(HotPath.WEATHER_LOD)) {
			return;
		}
		if (IrisCompat.shadersOn()) {
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
