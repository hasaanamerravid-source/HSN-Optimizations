package hsn.modod.client.mixin;

import hsn.modod.client.optimize.AdaptiveCuller;
import hsn.modod.config.HSNConfig;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Reduces the visual spin of item entities under load.
 *
 * In Minecraft 26.2 {@code ItemEntity.getSpin(FF)F} is static, so the inject
 * callback must also be static. Without an instance we cannot do per-entity
 * distance checks here; instead we freeze/slow spin when Performance Mode is
 * on or the adaptive scale is low (weak-GPU path).
 */
@Mixin(ItemEntity.class)
public class ItemSpinMixin {

	@Inject(method = "getSpin", at = @At("HEAD"), cancellable = true)
	private static void hsn$throttleSpin(float age, float partialTick, CallbackInfoReturnable<Float> cir) {
		HSNConfig cfg = HSNConfig.get();
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
		float frozen = (age * 0.173f) % 6.2832f;
		cir.setReturnValue(frozen);
	}
}
