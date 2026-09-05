package hsn.modod.client.mixin;

import hsn.modod.client.optimize.AdaptiveCuller;
import hsn.modod.config.HSNConfig;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Freezes item spin when performance mode or weak-GPU scaling is active. */
@Mixin(ItemEntity.class)
public class ItemSpinMixin {

	@Inject(method = "getSpin", at = @At("HEAD"), cancellable = true)
	private static void hsn$throttleSpin(float age, float partialTick, CallbackInfoReturnable<Float> cir) {
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.modEnabled) {
			return;
		}
		if (!cfg.itemSpinThrottleEnabled) {
			return;
		}

		// Only intervene under load / Performance Mode (no entity instance available)
		boolean underLoad = cfg.performanceModeEnabled
				|| AdaptiveCuller.getScale() < 0.75
				|| AdaptiveCuller.isWeakGpuActive();
		if (!underLoad) {
			return;
		}

		// Freeze spin to a stable value derived from age so items still differ
		// visually but stop animating every frame.
		float frozen = (age * 0.173f) % 6.2831855f;
		cir.setReturnValue(frozen);
	}
}
