package hsn.modod.mixin;

import hsn.modod.optimize.HotPath;
import hsn.modod.optimize.PathfindingStats;
import hsn.modod.optimize.ServerFeatures;
import hsn.modod.config.HSNConfig;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Distant idle mobs do not need a full GoalSelector pass every tick on the
 * integrated server. Combat, players, and nearby mobs are untouched.
 */
@Mixin(Mob.class)
public class IdleAiThrottleMixin {

	@Inject(method = {"serverAiStep", "aiStep"}, at = @At("HEAD"), cancellable = true, require = 0)
	private void hsn$skipIdleAi(CallbackInfo ci) {
		if (!HotPath.flag(HotPath.IDLE_AI)) {
			return;
		}
		Mob self = (Mob) (Object) this;
		if (!ServerFeatures.allowSimulationExtras(self)) {
			return;
		}
		if (!self.isAlive() || self.isNoAi()) {
			return;
		}
		if (self.getTarget() != null || self.hurtTime > 0) {
			return;
		}
		Player nearest = self.level().getNearestPlayer(self, HSNConfig.get().idleAiFullDistance * 3.0);
		if (nearest == null) {
			int interval = Math.max(2, HSNConfig.get().idleAiMaxInterval);
			if ((self.tickCount + self.getId()) % interval != 0) {
				PathfindingStats.tickSkipped();
				ci.cancel();
			}
			return;
		}
		double distSq = nearest.distanceToSqr(self);
		if (distSq <= HotPath.idleAiDistSq()) {
			return;
		}
		int interval = Math.max(2, HSNConfig.get().idleAiMaxInterval);
		if ((self.tickCount + self.getId()) % interval != 0) {
			PathfindingStats.tickSkipped();
			ci.cancel();
		}
	}
}
