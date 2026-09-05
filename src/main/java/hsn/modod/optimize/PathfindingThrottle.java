package hsn.modod.optimize;

import hsn.modod.config.HSNConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;

public final class PathfindingThrottle {

	public static final int FULL_RATE = 1;
	public static final int RECHECK_TICKS = 10;
	public static final double MIN_FULL_RATE_DISTANCE = 8.0;
	public static final double FALL_OFF_MULTIPLIER = 3.0;
	public static final double GOAL_MATCH_DIST_SQ = 2.25;

	private PathfindingThrottle() {
	}

	public static boolean enabled() {
		HSNConfig cfg = HSNConfig.get();
		return cfg.modEnabled && cfg.pathfindingThrottleEnabled;
	}

	public static boolean enabledFor(Mob mob) {
		return enabled() && ServerFeatures.allowPathfindingThrottle(mob != null ? mob.level() : null);
	}

	public static boolean shouldSkipRebuild(Mob mob, int interval) {
		return interval > FULL_RATE && !ThrottleUtil.shouldTick(mob.tickCount, mob.getId(), interval);
	}

	public static boolean shouldReusePath(Mob mob, Path current, BlockPos newGoal, int interval) {
		if (mob == null || current == null || newGoal == null) {
			return false;
		}
		if (!enabledFor(mob) || isPlayerCombatRelevant(mob)) {
			return false;
		}
		if (current.isDone()) {
			return false;
		}
		BlockPos oldGoal = current.getTarget();
		if (oldGoal == null) {
			return false;
		}
		if (oldGoal.distSqr(newGoal) > GOAL_MATCH_DIST_SQ) {
			return false;
		}
		return shouldSkipRebuild(mob, interval);
	}

	public static float nodeBudget(int interval) {
		if (interval <= FULL_RATE) return 1.0f;
		if (interval <= 2) return 0.85f;
		if (interval <= 4) return 0.60f;
		if (interval <= 6) return 0.45f;
		return 0.35f;
	}

	public static int computeInterval(Mob mob) {
		if (mob == null || !mob.isAlive()) {
			return FULL_RATE;
		}
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.pathfindingThrottleEnabled || isPlayerCombatRelevant(mob)) {
			return FULL_RATE;
		}
		Level level = mob.level();
		if (level == null || !ServerFeatures.allowPathfindingThrottle(level)) {
			return FULL_RATE;
		}

		double nearestSq = nearestPlayerDistanceSq(mob, level);
		int maxInterval = Math.max(2, cfg.pathfindingMaxInterval);
		if (nearestSq == Double.POSITIVE_INFINITY) {
			return maxInterval;
		}

		double fullRateDistance = Math.max(MIN_FULL_RATE_DISTANCE, cfg.pathfindingFullDistance);
		return ThrottleUtil.intervalForDistanceSq(
				nearestSq,
				fullRateDistance,
				fullRateDistance * FALL_OFF_MULTIPLIER,
				maxInterval);
	}

	static boolean isPlayerCombatRelevant(Mob mob) {
		LivingEntity target = mob.getTarget();
		return target instanceof Player || mob.getLastHurtByMob() instanceof Player;
	}

	private static double nearestPlayerDistanceSq(Mob mob, Level level) {
		double nearestSq = Double.POSITIVE_INFINITY;
		for (Player player : level.players()) {
			if (player == null || !player.isAlive()) {
				continue;
			}
			double distSq = player.distanceToSqr(mob);
			if (distSq < nearestSq) {
				nearestSq = distSq;
			}
		}
		return nearestSq;
	}

	public static final class Schedule {
		private int interval = FULL_RATE;
		private int recheckAtTick = Integer.MIN_VALUE;
		private float lastBudget = 1.0f;

		public int interval(Mob mob) {
			if (mob.tickCount < recheckAtTick) {
				return interval;
			}
			interval = computeInterval(mob);
			lastBudget = PathfindingThrottle.nodeBudget(interval);
			recheckAtTick = mob.tickCount + RECHECK_TICKS;
			return interval;
		}

		public float nodeBudget() {
			return lastBudget;
		}
	}
}
