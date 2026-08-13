package hsn.modod.client.mixin;

import hsn.modod.config.HSNConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Optional light fog distance scaling. Disabled by default.
 */
@Mixin(targets = {
		"net.minecraft.client.renderer.FogRenderer",
		"net.minecraft.client.render.BackgroundRenderer",
		"net.minecraft.client.renderer.FogRenderer$FogData"
}, priority = 900)
public class FogRendererMixin {

	@ModifyVariable(method = {"setupFog", "applyFog", "getFogColor"},
			at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
	private static float hsn$scaleFog(float value) {
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.fogScaleEnabled) {
			return value;
		}
		// Mild reduction only
		if (value > 8.0f && value < 512.0f) {
			return value * (float) cfg.fogScaleFactor;
		}
		return value;
	}
}
