package hsn.modod.client.mixin;

import hsn.modod.client.optimize.AdaptiveCuller;
import hsn.modod.client.optimize.CameraSnapshot;
import hsn.modod.optimize.HotPath;
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
		if (!CameraSnapshot.valid()) {
			return;
		}
		if (CameraSnapshot.hasCeiling()) {
			ci.cancel();
			return;
		}
		if (AdaptiveCuller.getScale() < 0.55 && !CameraSnapshot.lookingUp()) {
			ci.cancel();
		}
	}
}
