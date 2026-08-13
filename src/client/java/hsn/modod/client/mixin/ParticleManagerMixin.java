package hsn.modod.client.mixin;

import hsn.modod.client.optimize.CullStats;
import hsn.modod.config.HSNConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Simple particle distance + density culling for weak GPUs.
 */
@Mixin(ParticleEngine.class)
public class ParticleManagerMixin {

	@Unique
	private static int particleCounter = 0;
	@Unique
	private static long lastResetMs = 0L;

	@Inject(method = "createParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)Lnet/minecraft/client/particle/Particle;",
			at = @At("HEAD"), cancellable = true, require = 0)
	private void hsn$cullParticle(ParticleOptions options, double x, double y, double z,
								  double xSpeed, double ySpeed, double zSpeed,
								  CallbackInfoReturnable<Particle> cir) {
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.particleCullingEnabled) {
			return;
		}

		// Soft reset of the budget every second
		long now = System.currentTimeMillis();
		if (now - lastResetMs > 1000L) {
			particleCounter = 0;
			lastResetMs = now;
		}

		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null) {
			return;
		}

		double dx = x - player.getX();
		double dy = y - player.getY();
		double dz = z - player.getZ();
		double distSq = dx * dx + dy * dy + dz * dz;
		double maxDist = cfg.maxParticleDistance;
		if (distSq > maxDist * maxDist) {
			CullStats.particleSkip();
			cir.setReturnValue(null);
			return;
		}

		if (particleCounter > cfg.maxParticles) {
			CullStats.particleSkip();
			cir.setReturnValue(null);
			return;
		}

		ParticleType<?> type = options.getType();
		if (cfg.rainKeepChance < 1.0 && isRainLike(type) && Math.random() > cfg.rainKeepChance) {
			CullStats.particleSkip();
			cir.setReturnValue(null);
			return;
		}
		if (cfg.smokeKeepChance < 1.0 && isSmokeLike(type) && Math.random() > cfg.smokeKeepChance) {
			CullStats.particleSkip();
			cir.setReturnValue(null);
			return;
		}

		particleCounter++;
	}

	@Unique
	private static boolean isRainLike(ParticleType<?> type) {
		return type == ParticleTypes.RAIN || type == ParticleTypes.DRIPPING_WATER
				|| type == ParticleTypes.FALLING_WATER;
	}

	@Unique
	private static boolean isSmokeLike(ParticleType<?> type) {
		return type == ParticleTypes.SMOKE || type == ParticleTypes.LARGE_SMOKE
				|| type == ParticleTypes.CAMPFIRE_COSY_SMOKE || type == ParticleTypes.CAMPFIRE_SIGNAL_SMOKE;
	}
}
