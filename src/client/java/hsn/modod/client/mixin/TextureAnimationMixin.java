package hsn.modod.client.mixin;

import hsn.modod.client.optimize.AdaptiveCuller;
import hsn.modod.config.HSNConfig;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Throttle animated block/item textures under load only.
 * Does NOT cancel the whole atlas when interval is 1 (normal path).
 */
@Mixin(TextureAtlas.class)
public class TextureAnimationMixin {

	@Unique
	private static int hsn$tickCounter = 0;

	@Inject(method = {"tick", "cycleAnimation", "tickAnimatedSprites"},
			at = @At("HEAD"), cancellable = true, require = 0)
	private void hsn$throttleAnimatedTextures(CallbackInfo ci) {
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.textureAnimThrottleEnabled) {
			return;
		}

		int interval = Math.max(1, cfg.textureAnimInterval);

		if (cfg.textureAnimUseAdaptive) {
			double scale = AdaptiveCuller.getScale();
			if (scale < 0.85) {
				int adaptive = scale < 0.55 ? cfg.textureAnimMaxInterval
						: (scale < 0.7 ? Math.max(interval, 3) : Math.max(interval, 2));
				interval = Math.min(cfg.textureAnimMaxInterval, adaptive);
			}
			if (cfg.performanceModeEnabled) {
				interval = Math.max(interval, cfg.textureAnimMaxInterval);
			}
		}

		// interval 1 = never cancel (full speed)
		if (interval <= 1) {
			return;
		}

		hsn$tickCounter++;
		if (hsn$tickCounter < interval) {
			ci.cancel();
		} else {
			hsn$tickCounter = 0;
		}
	}
}
