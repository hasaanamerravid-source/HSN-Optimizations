package hsn.modod.client.mixin;

import hsn.modod.client.optimize.HighEndCounters;
import hsn.modod.client.optimize.LightmapGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Same optimization as {@link LightTextureMixin}, targeting the classic
 * Yarn-mappings name/package ({@code net.minecraft.client.render.LightmapTextureManager}).
 * Only registered by {@link HSNMixinPlugin} when that class is actually
 * present.
 */
@Mixin(targets = "net.minecraft.client.render.LightmapTextureManager", priority = 900)
public class LightmapTextureManagerYarnMixin {

	@Inject(method = {
			"updateLightTexture",
			"update",
			"tick"
	}, at = @At("HEAD"), cancellable = true, require = 0)
	private void hsn$skipUnchangedLightmap(CallbackInfo ci) {
		if (LightmapGate.shouldSkip()) {
			HighEndCounters.lightSkip();
			ci.cancel();
		}
	}
}
