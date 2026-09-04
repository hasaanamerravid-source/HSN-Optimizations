package hsn.modod.client.mixin;

import hsn.modod.client.optimize.CullStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.player.LocalPlayer;
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
		if (!hsn.modod.optimize.HotPath.flag(hsn.modod.optimize.HotPath.PARTICLE_CULL)) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null) {
			return;
		}
		double dx = this.x - player.getX();
		double dy = this.y - player.getY();
		double dz = this.z - player.getZ();
		// Slightly looser than spawn cull so live particles do not pop at the cap.
		double limitSq = hsn.modod.optimize.HotPath.particleDistSq() * 1.3225;
		if (dx * dx + dy * dy + dz * dz > limitSq) {
			this.remove();
			CullStats.particleSkip();
			ci.cancel();
		}
	}
}
