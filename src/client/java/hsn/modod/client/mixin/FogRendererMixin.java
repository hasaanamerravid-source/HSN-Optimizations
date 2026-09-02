package hsn.modod.client.mixin;

import hsn.modod.config.HSNConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Optional light fog distance scaling. Disabled by default in config.
 * Targets the 26.2 FogRenderer (package net.minecraft.client.renderer.fog).
 */
@Mixin(targets = {
		"net.minecraft.client.renderer.fog.FogRenderer"
}, priority = 900)
public class FogRendererMixin {

	@ModifyVariable(
			method = {"setupFog"},
			at = @At("HEAD"),
			argsOnly = true,
			ordinal = 1, // renderDistanceInChunks is typically the int after Camera
			require = 0
	)
	private static int hsn$scaleFogChunks(int value) {
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.fogScaleEnabled) {
			return value;
		}
		if (value > 2 && value < 64) {
			return Math.max(2, (int) (value * cfg.fogScaleFactor));
		}
		return value;
	}

	@ModifyVariable(
			method = {"setupFog"},
			at = @At("HEAD"),
			argsOnly = true,
			ordinal = 0,
			require = 0
	)
	private static float hsn$scaleFogFloat(float value) {
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.fogScaleEnabled) {
			return value;
		}
		if (value > 8.0f && value < 512.0f) {
			return value * (float) cfg.fogScaleFactor;
		}
		return value;
	}
}
