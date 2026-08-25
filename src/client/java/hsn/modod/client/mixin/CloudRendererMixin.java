package hsn.modod.client.mixin;

import hsn.modod.client.optimize.AdaptiveCuller;
import hsn.modod.client.optimize.CullStats;
import hsn.modod.config.HSNConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Soft cloud density reduction for Minecraft 26.2.
 * Target: net.minecraft.client.renderer.CloudRenderer
 * Never cancel LevelRenderer — only the dedicated cloud renderer.
 */
@Mixin(targets = {
		"net.minecraft.client.renderer.CloudRenderer"
}, priority = 900)
public class CloudRendererMixin {

	@Inject(method = {
			"render"
	}, at = @At("HEAD"), cancellable = true, require = 0)
	private void hsn$cullClouds(CallbackInfo ci) {
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.cloudCullingEnabled) {
			return;
		}

		double keep = cfg.cloudDensityKeepChance * AdaptiveCuller.getScale();
		if (cfg.performanceModeEnabled) {
			keep *= 0.5;
		}
		if (keep < 0.35) {
			CullStats.particleSkip();
			ci.cancel();
		}
	}
}
