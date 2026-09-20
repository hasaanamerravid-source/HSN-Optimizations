package hsn.optimizations.client.mixin;

import hsn.optimizations.config.HSNConfig;
import hsn.optimizations.optimize.HotPath;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityRenderer.class)
public interface BlockEntityViewDistanceMixin {

	@Inject(method = "getViewDistance", at = @At("HEAD"), cancellable = true, require = 0)
	private void hsn$scaleViewDistance(CallbackInfoReturnable<Integer> cir) {
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.modEnabled || !cfg.blockEntityCullingEnabled) {
			return;
		}
		int blocks = (int) Math.round(Math.sqrt(HotPath.blockEntityDistSq()));
		cir.setReturnValue(Math.max(4, blocks));
	}
}
