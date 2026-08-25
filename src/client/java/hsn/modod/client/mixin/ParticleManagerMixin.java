package hsn.modod.client.mixin;

import hsn.modod.client.optimize.CullStats;
import hsn.modod.client.optimize.DistanceLod;
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
 * Particle distance + soft budget + priority system.
 *
 * Priority (unique):
 * - High priority: combat / explosion / player-relevant effects → kept preferentially
 * - Low priority: rain, ambient smoke, bubbles, decorative → culled first when over budget
 *
 * This is far smarter than pure random/distance culling and is rare among optimizers.
 */
@Mixin(ParticleEngine.class)
public class ParticleManagerMixin {

	@Unique
	private static int particleCounter = 0;
	@Unique
	private static long lastResetMs = 0L;

	@Inject(method = {
			"createParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)Lnet/minecraft/client/particle/Particle;",
			"createParticle"
	}, at = @At("HEAD"), cancellable = true, require = 1)
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

		ParticleType<?> type = options.getType();
		boolean highPriority = isHighPriority(type);
		boolean lowPriority = isLowPriority(type);

		// Progressive quality curve: farther particles keep less often
		double keepMul = DistanceLod.particleKeepMultiplier(distSq, maxDist, highPriority);
		if (keepMul < 0.999 && Math.random() > keepMul) {
			CullStats.particleSkip();
			cir.setReturnValue(null);
			return;
		}

		// Category keep-chances (always applied)
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
		if (cfg.explosionKeepChance < 1.0 && isExplosionLike(type) && Math.random() > cfg.explosionKeepChance) {
			CullStats.particleSkip();
			cir.setReturnValue(null);
			return;
		}
		if (cfg.fireSmokeKeepChance < 1.0 && isFireLike(type) && Math.random() > cfg.fireSmokeKeepChance) {
			CullStats.particleSkip();
			cir.setReturnValue(null);
			return;
		}
		if (cfg.bubbleKeepChance < 1.0 && isBubbleLike(type) && Math.random() > cfg.bubbleKeepChance) {
			CullStats.particleSkip();
			cir.setReturnValue(null);
			return;
		}

		// Soft budget with priority
		if (particleCounter > cfg.maxParticles) {
			if (cfg.particlePriorityEnabled) {
				double keep = highPriority ? cfg.highPriorityKeepChance
						: (lowPriority ? cfg.lowPriorityKeepChance : 0.45);
				if (Math.random() > keep) {
					CullStats.particleSkip();
					cir.setReturnValue(null);
					return;
				}
			} else {
				CullStats.particleSkip();
				cir.setReturnValue(null);
				return;
			}
		}

		particleCounter++;
	}

	@Unique
	private static boolean isHighPriority(ParticleType<?> type) {
		// Combat, explosions, damage, important status
		return type == ParticleTypes.EXPLOSION
				|| type == ParticleTypes.EXPLOSION_EMITTER
				|| type == ParticleTypes.CRIT
				|| type == ParticleTypes.ENCHANTED_HIT
				|| type == ParticleTypes.DAMAGE_INDICATOR
				|| type == ParticleTypes.SWEEP_ATTACK
				|| type == ParticleTypes.FLASH
				|| type == ParticleTypes.ANGRY_VILLAGER
				|| type == ParticleTypes.HEART
				|| type == ParticleTypes.TOTEM_OF_UNDYING;
	}

	@Unique
	private static boolean isLowPriority(ParticleType<?> type) {
		return isRainLike(type) || isSmokeLike(type) || isBubbleLike(type)
				|| type == ParticleTypes.ASH
				|| type == ParticleTypes.WHITE_ASH
				|| type == ParticleTypes.SPORE_BLOSSOM_AIR
				|| type == ParticleTypes.FALLING_SPORE_BLOSSOM
				|| type == ParticleTypes.CHERRY_LEAVES;
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

	@Unique
	private static boolean isExplosionLike(ParticleType<?> type) {
		return type == ParticleTypes.EXPLOSION || type == ParticleTypes.EXPLOSION_EMITTER
				|| type == ParticleTypes.POOF;
	}

	@Unique
	private static boolean isFireLike(ParticleType<?> type) {
		return type == ParticleTypes.FLAME || type == ParticleTypes.LAVA
				|| type == ParticleTypes.SMALL_FLAME;
	}

	@Unique
	private static boolean isBubbleLike(ParticleType<?> type) {
		return type == ParticleTypes.BUBBLE || type == ParticleTypes.BUBBLE_POP
				|| type == ParticleTypes.BUBBLE_COLUMN_UP;
	}
}
