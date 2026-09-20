package hsn.optimizations.client.mixin;

import hsn.optimizations.client.optimize.CameraSnapshot;
import hsn.optimizations.client.optimize.CullStats;
import hsn.optimizations.client.optimize.ParticleLive;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drops already-spawned particles that have moved out of the cull distance.
 * Spawn culling alone cannot shrink a rain/campfire cloud that is already alive.
 */
@Mixin(Particle.class)
public abstract class ParticleTickMixin {

	@Shadow
	protected double x;
	@Shadow
	protected double y;
	@Shadow
	protected double z;

	@Shadow
	public abstract void remove();

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 0)
	private void hsn$cullDistantParticleTick(CallbackInfo ci) {
		if (!hsn.optimizations.optimize.HotPath.flag(hsn.optimizations.optimize.HotPath.PARTICLE_CULL)) {
			return;
		}
		double cx;
		double cy;
		double cz;
		if (CameraSnapshot.valid()) {
			cx = CameraSnapshot.x();
			cy = CameraSnapshot.y();
			cz = CameraSnapshot.z();
		} else {
			return;
		}
		double dx = this.x - cx;
		double dy = this.y - cy;
		double dz = this.z - cz;
		double distSq = dx * dx + dy * dy + dz * dz;
		double limitSq = hsn.optimizations.optimize.HotPath.particleDistSq() * 1.3225;
		// Distance only. Frustum / facing-away would pop weather that already
		// spawned the moment the camera turns.
		boolean drop = distSq > limitSq;
		if (drop) {
			this.remove();
			CullStats.particleSkip();
			ci.cancel();
		}
	}

	@Inject(method = "remove", at = @At("HEAD"), require = 0)
	private void hsn$releaseLive(CallbackInfo ci) {
		if (hsn.optimizations.optimize.HotPath.flag(hsn.optimizations.optimize.HotPath.HARD_PARTICLE_CAP)) {
			ParticleLive.released();
		}
	}
}
