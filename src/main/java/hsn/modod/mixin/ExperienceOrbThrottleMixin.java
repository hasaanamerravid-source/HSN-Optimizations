package hsn.modod.mixin;

import hsn.modod.config.HSNConfig;
import hsn.modod.optimize.ServerFeatures;
import hsn.modod.optimize.ThrottleStats;
import hsn.modod.optimize.ThrottleUtil;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbThrottleMixin {

    @Shadow
    private int age;

    @Shadow
    private Player followingPlayer;

    private static final double SEARCH_RADIUS = 64.0;
    private static final int RECHECK_INTERVAL_TICKS = 10;
    private static final double PICKUP_RANGE_SQ = 9.0; // 3 blocks pickup threshold

    @Unique
    private int hsn$cachedInterval = 1;
    @Unique
    private long hsn$nextRecheckTick = Long.MIN_VALUE;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void hsn$throttleTick(CallbackInfo ci) {
        HSNConfig cfg = HSNConfig.get();
        if (cfg == null || !cfg.itemThrottleEnabled) {
            return;
        }

        ExperienceOrb self = (ExperienceOrb) (Object) this;
        Level level = self.level();

        // 1. Never throttle on the client side or when the entity is dead/removed
        if (level == null || level.isClientSide() || !self.isAlive()) {
            return;
        }

        // 2. Bypass throttling if actively tracking a player, in liquid, or on fire
        if (this.followingPlayer != null || self.isInWater() || self.isInLava() || self.isOnFire()) {
            return;
        }

        if (!ServerFeatures.allowSimulationExtras(self)) {
            return;
        }

        // 3. Recheck distance to players at fixed intervals using coordinate lookups
        if (self.tickCount >= this.hsn$nextRecheckTick) {
            double startDistance = cfg.itemThrottleStartDistance;
            double maxDistance = startDistance * 3.0;

            Player nearest = level.getNearestPlayer(self.getX(), self.getY(), self.getZ(), SEARCH_RADIUS, false);
            double distSq = nearest == null ? maxDistance * maxDistance : nearest.distanceToSqr(self);

            if (nearest != null && distSq <= PICKUP_RANGE_SQ) {
                this.hsn$cachedInterval = 1;
            } else {
                this.hsn$cachedInterval = ThrottleUtil.intervalForDistanceSq(
                        distSq, startDistance, maxDistance, cfg.itemThrottleMaxInterval);
            }
            this.hsn$nextRecheckTick = (long) self.tickCount + RECHECK_INTERVAL_TICKS;
        }

        // 4. Cancel tick and advance despawn age when throttled
        if (!ThrottleUtil.shouldTick(self.tickCount, self.getId(), this.hsn$cachedInterval)) {
            this.age++;
            self.tickCount++;
            ThrottleStats.tickSkipped();
            ci.cancel();
        }
    }
}