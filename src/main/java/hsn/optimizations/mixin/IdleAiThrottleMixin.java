package hsn.optimizations.mixin;

import hsn.optimizations.optimize.HotPath;
import hsn.optimizations.optimize.PathfindingStats;
import hsn.optimizations.optimize.ServerFeatures;
import hsn.optimizations.config.HSNConfig;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Distant idle mobs skip GoalSelector work on the integrated server.
 * A player inside the slider radius keeps vanilla AI every tick.
 */
@Mixin(Mob.class)
public class IdleAiThrottleMixin {

	@Unique
	private long hsn$nextScanTick;
	@Unique
	private boolean hsn$playerNearby = true;

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
			hsn$playerNearby = true;
			return;
		}
		int interval = Math.max(2, HSNConfig.get().idleAiMaxInterval);
		if (self.tickCount >= hsn$nextScanTick) {
			hsn$nextScanTick = self.tickCount + interval;
			double range = Math.sqrt(HotPath.idleAiDistSq());
			Player nearest = self.level().getNearestPlayer(self, range);
			hsn$playerNearby = nearest != null;
		}
		if (hsn$playerNearby) {
			return;
		}
		if ((self.tickCount + self.getId()) % interval == 0) {
			return;
		}
		PathfindingStats.tickSkipped();
		ci.cancel();
	}
}
