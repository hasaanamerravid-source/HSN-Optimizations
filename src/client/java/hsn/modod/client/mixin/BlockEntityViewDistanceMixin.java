package hsn.modod.client.mixin;

import hsn.modod.config.HSNConfig;
import hsn.modod.optimize.HotPath;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityRenderer.class)
public interface BlockEntityViewDistanceMixin {

	@Inject(method = "getViewDistance", at = @At("HEAD"), cancellable = true)
	private void hsn$scaleViewDistance(CallbackInfoReturnable<Integer> cir) {
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.modEnabled || !cfg.blockEntityCullingEnabled) {
			return;
		}
		int blocks = (int) Math.round(Math.sqrt(HotPath.blockEntityDistSq()));
		cir.setReturnValue(Math.max(4, blocks));
	}
}
