package hsn.modod.mixin;

import hsn.modod.config.HSNConfig;
import hsn.modod.optimize.ServerFeatures;
import hsn.modod.optimize.ThrottleStats;
import hsn.modod.optimize.ThrottleUtil;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityThrottleMixin {

	@Shadow
	private int age;

	private static final double SEARCH_RADIUS = 96.0;
	private static final int RECHECK_INTERVAL_TICKS = 10;
	private static final double PICKUP_RANGE_SQ = 4.0;

	@Unique
	private int hsn$cachedInterval = 1;
	@Unique
	private long hsn$nextRecheckTick = Long.MIN_VALUE;

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void hsn$throttleTick(CallbackInfo ci) {
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.itemThrottleEnabled) {
			return;
		}

		ItemEntity self = (ItemEntity) (Object) this;
		Level level = self.level();
		if (level == null || level.isClientSide() || !self.isAlive()) {
			return;
		}
		if (!ServerFeatures.allowSimulationExtras(self)) {
			return;
		}
		if (self.isInWater() || self.isInLava() || self.isOnFire()) {
			return;
		}

		if (self.tickCount >= hsn$nextRecheckTick) {
			double startDistance = cfg.itemThrottleStartDistance;
			double maxDistance = startDistance * 3.0;

			Player nearest = level.getNearestPlayer(self, SEARCH_RADIUS);
			double distSq = nearest == null ? maxDistance * maxDistance : nearest.distanceToSqr(self);
			if (nearest != null && distSq <= PICKUP_RANGE_SQ) {
				hsn$cachedInterval = 1;
			} else {
				hsn$cachedInterval = ThrottleUtil.intervalForDistanceSq(distSq, startDistance, maxDistance,
						cfg.itemThrottleMaxInterval);
			}
			hsn$nextRecheckTick = self.tickCount + RECHECK_INTERVAL_TICKS;
		}

		if (!ThrottleUtil.shouldTick(self.tickCount, self.getId(), hsn$cachedInterval)) {
			this.age++;
			self.tickCount++;
			ThrottleStats.tickSkipped();
			ci.cancel();
		}
	}
}
