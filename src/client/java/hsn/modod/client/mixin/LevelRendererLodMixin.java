package hsn.modod.client.mixin;

import hsn.modod.client.optimize.BlockTextureLod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Apply block-atlas mip bias only while the world is drawn, then restore 0
 * so inventory / hotbar items stay sharp.
 */
@Mixin(targets = {
		"net.minecraft.client.renderer.LevelRenderer"
}, priority = 900)
public class LevelRendererLodMixin {

	@Inject(method = {"renderLevel"}, at = @At("HEAD"), require = 0)
	private void hsn$blockLodBegin(CallbackInfo ci) {
		BlockTextureLod.beginWorldPass();
	}

	@Inject(method = {"renderLevel"}, at = @At("RETURN"), require = 0)
	private void hsn$blockLodEnd(CallbackInfo ci) {
		BlockTextureLod.endWorldPass();
	}
}
