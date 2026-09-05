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

@Mixin(ParticleEngine.class)
public class ParticleManagerMixin {

	@Unique
	private static int particleCounter = 0;
	@Unique
	private static long lastResetTick = Long.MIN_VALUE;

	@Inject(
			method = "createParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)Lnet/minecraft/client/particle/Particle;",
			at = @At("HEAD"),
			cancellable = true,
			require = 0
	)
	private void hsn$cullParticle(ParticleOptions options, double x, double y, double z,
								  double xSpeed, double ySpeed, double zSpeed,
								  CallbackInfoReturnable<Particle> cir) {
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.modEnabled) {
			return;
		}
		if (!hsn.modod.optimize.HotPath.flag(hsn.modod.optimize.HotPath.PARTICLE_CULL)) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null) {
			return;
		}

		long gameTime = mc.level != null ? mc.level.getGameTime() : 0L;
		if (gameTime != lastResetTick) {
			particleCounter = 0;
			lastResetTick = gameTime;
		}

		double dx = x - player.getX();
		double dy = y - player.getY();
		double dz = z - player.getZ();
		double distSq = dx * dx + dy * dy + dz * dz;
		double maxDistSq = hsn.modod.optimize.HotPath.particleDistSq();
		if (distSq > maxDistSq) {
			CullStats.particleSkip();
			cir.setReturnValue(null);
			return;
		}

		ParticleType<?> type = options.getType();
		boolean highPriority = isHighPriority(type);
		boolean lowPriority = isLowPriority(type);

		double keepMul = DistanceLod.particleKeepMultiplier(distSq, hsn.modod.optimize.HotPath.particleDist(), highPriority);
		if (keepMul < 0.999 && hash01(x, y, z, gameTime) > keepMul) {
			CullStats.particleSkip();
			cir.setReturnValue(null);
			return;
		}

		if (hsn.modod.optimize.HotPath.rainKeep() < 1.0 && isRainLike(type)
				&& hash01(x + 1, y, z, gameTime) > hsn.modod.optimize.HotPath.rainKeep()) {
			CullStats.particleSkip();
			cir.setReturnValue(null);
			return;
		}
		if (hsn.modod.optimize.HotPath.smokeKeep() < 1.0 && isSmokeLike(type)
				&& hash01(x, y + 1, z, gameTime) > hsn.modod.optimize.HotPath.smokeKeep()) {
			CullStats.particleSkip();
			cir.setReturnValue(null);
			return;
		}
		if (hsn.modod.optimize.HotPath.explosionKeep() < 1.0 && isExplosionLike(type)
				&& hash01(x, y, z + 1, gameTime) > hsn.modod.optimize.HotPath.explosionKeep()) {
			CullStats.particleSkip();
			cir.setReturnValue(null);
			return;
		}
		if (hsn.modod.optimize.HotPath.fireKeep() < 1.0 && isFireLike(type)
				&& hash01(x - 1, y, z, gameTime) > hsn.modod.optimize.HotPath.fireKeep()) {
			CullStats.particleSkip();
			cir.setReturnValue(null);
			return;
		}
		if (hsn.modod.optimize.HotPath.bubbleKeep() < 1.0 && isBubbleLike(type)
				&& hash01(x, y - 1, z, gameTime) > hsn.modod.optimize.HotPath.bubbleKeep()) {
			CullStats.particleSkip();
			cir.setReturnValue(null);
			return;
		}

		// Soft per-tick spawn budget. Live particles are trimmed by ParticleTickMixin.
		int budget = Math.max(8, hsn.modod.optimize.HotPath.particleBudget() / 20);
		if (particleCounter > budget) {
			if (cfg.particlePriorityEnabled) {
				double keep = highPriority ? cfg.highPriorityKeepChance
						: (lowPriority ? cfg.lowPriorityKeepChance : 0.45);
				if (hash01(x + z, y, 0.0, gameTime) > keep) {
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
	private static double hash01(double a, double b, double c, long t) {
		long h = Double.doubleToRawLongBits(a) * 0x9E3779B97F4A7C15L
				^ Double.doubleToRawLongBits(b) * 0xBF58476D1CE4E5B9L
				^ Double.doubleToRawLongBits(c) * 0x94D049BB133111EBL
				^ t * 0x2545F4914F6CDD1DL;
		h ^= (h >>> 33);
		h *= 0xff51afd7ed558ccdL;
		h ^= (h >>> 33);
		return (h >>> 11) * 0x1.0p-53;
	}

	@Unique
	private static boolean isHighPriority(ParticleType<?> type) {
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
