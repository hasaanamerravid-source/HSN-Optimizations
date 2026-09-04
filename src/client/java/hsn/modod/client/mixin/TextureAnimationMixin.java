package hsn.modod.client.mixin;

import hsn.modod.client.optimize.AdaptiveCuller;
import hsn.modod.client.optimize.LowEndTuner;
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
public abstract class TextureAnimationMixin {

    @Unique
    private int hsn$tickCounter = 0;

    @Inject(
        method = {"cycleAnimation", "tickAnimatedSprites"},
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void hsn$throttleAnimatedTextures(CallbackInfo ci) {
        HSNConfig cfg = HSNConfig.get();
        if (cfg == null || !cfg.textureAnimThrottleEnabled) {
            return;
        }

        int baseInterval = Math.max(1, cfg.textureAnimInterval);
        int maxInterval = Math.max(baseInterval, cfg.textureAnimMaxInterval);
        int interval = baseInterval;

        if (cfg.textureAnimUseAdaptive) {
            double scale = AdaptiveCuller.getScale();
            if (scale < 0.85) {
                int adaptive = scale < 0.55 ? maxInterval
                        : (scale < 0.7 ? Math.max(baseInterval, 3) : Math.max(baseInterval, 2));
                interval = Math.min(maxInterval, adaptive);
            }
            if (cfg.performanceModeEnabled) {
                interval = Math.max(interval, maxInterval);
            }
            interval = Math.min(maxInterval, interval + LowEndTuner.extraTextureSkip());
        }

        // Interval <= 1 means full speed animation (never cancel)
        if (interval <= 1) {
            this.hsn$tickCounter = 0;
            return;
        }

        this.hsn$tickCounter++;
        
        // Modulo check ensures clean frame skips even when 'interval' changes dynamically
        if (this.hsn$tickCounter % interval != 0) {
            ci.cancel();
        } else {
            this.hsn$tickCounter = 0;
        }
    }
}