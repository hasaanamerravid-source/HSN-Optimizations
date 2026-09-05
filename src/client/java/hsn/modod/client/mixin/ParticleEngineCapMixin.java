package hsn.modod.client.mixin;

import hsn.modod.optimize.HotPath;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineCapMixin {

	@Inject(method = "countParticles", at = @At("RETURN"), cancellable = true, require = 0)
	private void hsn$reportCappedCount(CallbackInfoReturnable<Integer> cir) {
		if (!HotPath.flag(HotPath.HARD_PARTICLE_CAP)) {
			return;
		}
		int budget = HotPath.particleBudget();
		Integer value = cir.getReturnValue();
		if (value != null && value > budget) {
			cir.setReturnValue(budget);
		}
	}
}
