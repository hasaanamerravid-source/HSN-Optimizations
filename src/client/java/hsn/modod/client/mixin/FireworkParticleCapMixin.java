package hsn.modod.client.mixin;

import hsn.modod.client.optimize.CullStats;
import hsn.modod.optimize.HotPath;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.client.particle.ParticleEngine.class)
public class FireworkParticleCapMixin {

	@Unique
	private static int hsn$fw;
	@Unique
	private static long hsn$fwTick = Long.MIN_VALUE;

	@Inject(
			method = "createParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)Lnet/minecraft/client/particle/Particle;",
			at = @At("HEAD"),
			cancellable = true,
			require = 0
	)
	private void hsn$capFireworks(ParticleOptions options, double x, double y, double z,
			double xSpeed, double ySpeed, double zSpeed,
			CallbackInfoReturnable<?> cir) {
		if (!HotPath.flag(HotPath.FIREWORK_CAP) && !HotPath.flag(HotPath.DRIP_THROTTLE)) {
			return;
		}
		var type = options.getType();
		long t = hsn.modod.client.optimize.CameraSnapshot.gameTime();
		if (t != hsn$fwTick) {
			hsn$fw = 0;
			hsn$fwTick = t;
		}
		boolean firework = type == ParticleTypes.FIREWORK
				|| type == ParticleTypes.FLASH;
		if (firework && HotPath.flag(HotPath.FIREWORK_CAP)) {
			if (hsn$fw++ >= HotPath.fireworkBudget()) {
				CullStats.particleSkip();
				cir.setReturnValue(null);
				return;
			}
		}
		if (HotPath.flag(HotPath.DRIP_THROTTLE)
				&& (type == ParticleTypes.DRIPPING_WATER
				|| type == ParticleTypes.DRIPPING_LAVA
				|| type == ParticleTypes.FALLING_WATER
				|| type == ParticleTypes.FALLING_LAVA
				|| type == ParticleTypes.LANDING_LAVA)) {
			if (hsn.modod.client.optimize.CameraSnapshot.hasCeiling()
					|| (t & 1L) == 0L) {
				CullStats.particleSkip();
				cir.setReturnValue(null);
			}
		}
	}
}
