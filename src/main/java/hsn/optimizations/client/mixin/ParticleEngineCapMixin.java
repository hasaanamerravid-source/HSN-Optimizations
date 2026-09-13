package hsn.optimizations.client.mixin;

import hsn.optimizations.client.optimize.ParticleLive;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineCapMixin {

	@Inject(method = {"add", "addParticle"}, at = @At("HEAD"), cancellable = true, require = 0)
	private void hsn$hardCap(Particle particle, CallbackInfo ci) {
		if (particle == null) {
			return;
		}
		if (!ParticleLive.acceptSpawn()) {
			ci.cancel();
		}
	}
}
