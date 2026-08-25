package hsn.modod.mixin;

import hsn.modod.config.HSNConfig;
import hsn.modod.optimize.ThrottleStats;
import hsn.modod.optimize.ThrottleUtil;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Graduated distance tick-throttle for dropped item entities. Off by default —
 * see {@link HSNConfig#itemThrottleEnabled}. Physics, stack merging, and the
 * despawn timer all keep progressing for distant items, just less often, instead
 * of freezing outright.
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityThrottleMixin {

	private static final double SEARCH_RADIUS = 256.0;
	private static final int RECHECK_INTERVAL_TICKS = 10;

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

		if (self.tickCount >= hsn$nextRecheckTick) {
			double startDistance = cfg.itemThrottleStartDistance;
			double maxDistance = startDistance * 3.0;

			Level level = self.level();
			Player nearest = level.getNearestPlayer(self, SEARCH_RADIUS);
			double distance = nearest == null ? maxDistance : Math.sqrt(nearest.distanceToSqr(self));

			hsn$cachedInterval = ThrottleUtil.intervalForDistance(distance, startDistance, maxDistance,
					cfg.itemThrottleMaxInterval);
			hsn$nextRecheckTick = self.tickCount + RECHECK_INTERVAL_TICKS;
		}

		if (!ThrottleUtil.shouldTick(self.tickCount, self.getId(), hsn$cachedInterval)) {
			ThrottleStats.tickSkipped();
			ci.cancel();
		}
	}
}
