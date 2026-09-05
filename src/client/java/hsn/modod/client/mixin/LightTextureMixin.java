package hsn.modod.client.mixin;

import hsn.modod.client.optimize.HighEndCounters;
import hsn.modod.client.optimize.LightmapGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skips redundant LightTexture LUT rebuilds when gamma/effects/dimension
 * did not change (see {@link LightmapGate}).
 * <p>
 * This targets the official-mappings name for the light texture manager.
 * Minecraft has used a couple of different names/packages for this class
 * across versions and mapping sets, so {@link HSNMixinPlugin} only adds
 * this mixin to the active set once it has confirmed the target class is
 * actually present on the classpath (see the sibling
 * {@code LightmapTextureManagerMixin} / {@code LightmapTextureManagerYarnMixin}
 * for the other known names). That keeps exactly one of them live and
 * avoids Sponge Mixin warning about targets that don't exist in this
 * version.
 */
@Mixin(targets = "net.minecraft.client.renderer.LightTexture", priority = 900)
public class LightTextureMixin {

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
