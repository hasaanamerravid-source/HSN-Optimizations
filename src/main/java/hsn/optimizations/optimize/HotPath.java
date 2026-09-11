package hsn.optimizations.optimize;

import hsn.optimizations.config.HSNConfig;
import hsn.optimizations.config.WorldRenderShape;

/**
 * Tick-stable snapshot of the values mixins read thousands of times per frame.
 * Distance sliders are baked in blocks: 8 on the slider is 8 blocks to the
 * entity AABB, plus a half-block pad so nothing pops at 7.99.
 */
public final class HotPath {

	public static final int PARTICLE_CULL = 1;
	public static final int ENTITY_CULL = 1 << 1;
	public static final int DEFER_ENTITY_MODS = 1 << 2;
	public static final int ENTITY_LOD = 1 << 3;
	public static final int BLOCK_ENTITY_CULL = 1 << 4;
	public static final int SHADOW_CULL = 1 << 5;
	public static final int NAME_TAG_CULL = 1 << 6;
	public static final int GLOW_CULL = 1 << 7;
	public static final int BEACON_CULL = 1 << 8;
	public static final int SOUND_CULL = 1 << 9;
	public static final int PARTICLE_PRIORITY = 1 << 10;
	public static final int PARTICLE_CURVE = 1 << 11;
	public static final int TEXTURE_LOD = 1 << 12;
	public static final int PERF_MODE = 1 << 13;
	public static final int SECTION_OCCUPANCY = 1 << 14;
	public static final int LIGHTMAP_CACHE = 1 << 15;
	public static final int INTERP_SKIP = 1 << 16;
	public static final int CLIENT_TICK_SKIP = 1 << 17;
	public static final int LIVING_ANIM = 1 << 18;
	public static final int WEATHER_LOD = 1 << 19;
	public static final int CLOUD_LOD = 1 << 20;
	public static final int HARD_PARTICLE_CAP = 1 << 21;
	public static final int FIREWORK_CAP = 1 << 22;
	public static final int DRIP_THROTTLE = 1 << 23;
	public static final int IDLE_AI = 1 << 24;
	public static final int NATIVE_FRUSTUM = 1 << 25;
	public static final int SHAPE_MASK = 1 << 26;
	public static final int BEHIND_CAMERA = 1 << 27;
	public static final int HORIZON_Y = 1 << 28;
	public static final int AMBIENT_TICK = 1 << 29;

	private static volatile Snapshot SNAP = Snapshot.DISABLED;

	private HotPath() {
	}

	public static final class Snapshot {
		static final Snapshot DISABLED = new Snapshot();

		final boolean master;
		final int flags;
		final double scale;
		final double particleDist;
		final double particleDistSq;
		final double entityDist;
		final double entityDistSq;
		final double itemDist;
		final double itemDistSq;
		final double xpDist;
		final double xpDistSq;
		final double decoDist;
		final double decoDistSq;
		final double blockEntityDist;
		final double blockEntityDistSq;
		final double shadowDistSq;
		final double nameTagDistSq;
		final double glowDistSq;
		final double beaconDistSq;
		final double soundDistSq;
		final double rainKeep;
		final double smokeKeep;
		final double explosionKeep;
		final double fireKeep;
		final double bubbleKeep;
		final double highKeep;
		final double lowKeep;
		final int particleBudget;
		final double interpDistSq;
		final double clientTickDistSq;
		final double livingAnimDistSq;
		final int clientTickInterval;
		final int fireworkBudget;
		final double idleAiDistSq;
		final int ambientInterval;
		final int ambientRange;

		private Snapshot() {
			this.master = false;
			this.flags = 0;
			this.scale = 1.0;
			this.particleDist = 16;
			this.particleDistSq = ExactDistance.limitSq(16);
			this.entityDist = 32;
			this.entityDistSq = ExactDistance.limitSq(32);
			this.itemDist = 20;
			this.itemDistSq = ExactDistance.limitSq(20);
			this.xpDist = 16;
			this.xpDistSq = ExactDistance.limitSq(16);
			this.decoDist = 16;
			this.decoDistSq = ExactDistance.limitSq(16);
			this.blockEntityDist = 24;
			this.blockEntityDistSq = ExactDistance.limitSq(24);
			this.shadowDistSq = ExactDistance.limitSq(12);
			this.nameTagDistSq = ExactDistance.limitSq(24);
			this.glowDistSq = ExactDistance.limitSq(28);
			this.beaconDistSq = ExactDistance.limitSq(48);
			this.soundDistSq = ExactDistance.limitSq(48);
			this.rainKeep = 0.15;
			this.smokeKeep = 0.25;
			this.explosionKeep = 1.0;
			this.fireKeep = 1.0;
			this.bubbleKeep = 1.0;
			this.highKeep = 0.85;
			this.lowKeep = 0.25;
			this.particleBudget = 400;
			this.interpDistSq = ExactDistance.limitSq(48);
			this.clientTickDistSq = ExactDistance.limitSq(40);
			this.livingAnimDistSq = ExactDistance.limitSq(36);
			this.clientTickInterval = 4;
			this.fireworkBudget = 48;
			this.idleAiDistSq = ExactDistance.limitSq(48);
			this.ambientInterval = 2;
			this.ambientRange = 12;
		}

		Snapshot(HSNConfig cfg, double adaptiveScale) {
			this.master = cfg.modEnabled;
			int bits = 0;
			if (cfg.particleCullingEnabled) bits |= PARTICLE_CULL;
			if (cfg.entityCullingEnabled) bits |= ENTITY_CULL;
			if (cfg.deferToDedicatedEntityCullingMods) bits |= DEFER_ENTITY_MODS;
			if (cfg.entityLodStagesEnabled) bits |= ENTITY_LOD;
			if (cfg.blockEntityCullingEnabled) bits |= BLOCK_ENTITY_CULL;
			if (cfg.shadowCullingEnabled) bits |= SHADOW_CULL;
			if (cfg.nameTagCullEnabled) bits |= NAME_TAG_CULL;
			if (cfg.glowOutlineCullingEnabled) bits |= GLOW_CULL;
			if (cfg.beaconBeamCullingEnabled) bits |= BEACON_CULL;
			if (cfg.soundDistanceCullingEnabled) bits |= SOUND_CULL;
			if (cfg.particlePriorityEnabled) bits |= PARTICLE_PRIORITY;
			if (cfg.particleQualityCurveEnabled) bits |= PARTICLE_CURVE;
			if (cfg.blockTextureLodEnabled) bits |= TEXTURE_LOD;
			if (cfg.performanceModeEnabled) bits |= PERF_MODE;
			if (cfg.sectionOccupancyCullingEnabled) bits |= SECTION_OCCUPANCY;
			if (cfg.lightmapCacheEnabled) bits |= LIGHTMAP_CACHE;
			if (cfg.entityInterpSkipEnabled) bits |= INTERP_SKIP;
			if (cfg.distantClientTickSkipEnabled) bits |= CLIENT_TICK_SKIP;
			if (cfg.livingAnimThrottleEnabled) bits |= LIVING_ANIM;
			if (cfg.weatherRendererLodEnabled) bits |= WEATHER_LOD;
			if (cfg.cloudLodEnabled) bits |= CLOUD_LOD;
			if (cfg.hardParticleCapEnabled) bits |= HARD_PARTICLE_CAP;
			if (cfg.fireworkParticleCapEnabled) bits |= FIREWORK_CAP;
			if (cfg.dripParticleThrottleEnabled) bits |= DRIP_THROTTLE;
			if (cfg.idleAiThrottleEnabled) bits |= IDLE_AI;
			if (cfg.nativeHotpathEnabled && cfg.nativeFrustumCullingEnabled) bits |= NATIVE_FRUSTUM;
			if (cfg.circularRenderingEnabled && cfg.worldRenderShape != null
					&& cfg.worldRenderShape != WorldRenderShape.OFF) {
				bits |= SHAPE_MASK;
			}
			if (cfg.behindCameraCullEnabled) bits |= BEHIND_CAMERA;
			if (cfg.horizonYCullEnabled) bits |= HORIZON_Y;
			if (cfg.ambientTickCullEnabled) bits |= AMBIENT_TICK;
			if (!cfg.modEnabled) {
				bits = 0;
			}
			this.flags = bits;

			// Adaptive scale is the ONLY hidden multiplier, and only when that
			// toggle is on. Performance Mode uses the preset's own slider
			// values — it must not shrink an 8-block slider down to 2.
			double s = 1.0;
			if (cfg.modEnabled && cfg.adaptiveCullingEnabled) {
				s = clamp(adaptiveScale, 0.25, 1.0);
			}
			this.scale = s;
			this.particleDist = cfg.maxParticleDistance * s;
			this.particleDistSq = ExactDistance.limitSq(this.particleDist);
			this.entityDist = cfg.maxEntityRenderDistance * s;
			this.entityDistSq = ExactDistance.limitSq(this.entityDist);
			this.itemDist = cfg.maxItemEntityRenderDistance * s;
			this.itemDistSq = ExactDistance.limitSq(this.itemDist);
			this.xpDist = cfg.maxXpOrbRenderDistance * s;
			this.xpDistSq = ExactDistance.limitSq(this.xpDist);
			this.decoDist = cfg.maxDecorationEntityDistance * s;
			this.decoDistSq = ExactDistance.limitSq(this.decoDist);
			this.blockEntityDist = cfg.maxBlockEntityRenderDistance * s;
			this.blockEntityDistSq = ExactDistance.limitSq(this.blockEntityDist);
			this.shadowDistSq = ExactDistance.limitSq(cfg.maxShadowDistance * s);
			this.nameTagDistSq = ExactDistance.limitSq(cfg.maxNameTagDistance * s);
			this.glowDistSq = ExactDistance.limitSq(cfg.maxGlowOutlineDistance * s);
			this.beaconDistSq = ExactDistance.limitSq(cfg.maxBeaconBeamDistance * s);
			this.soundDistSq = ExactDistance.limitSq(cfg.maxSoundDistance * s);
			this.rainKeep = cfg.rainKeepChance;
			this.smokeKeep = cfg.smokeKeepChance;
			this.explosionKeep = cfg.explosionKeepChance;
			this.fireKeep = cfg.fireSmokeKeepChance;
			this.bubbleKeep = cfg.bubbleKeepChance;
			this.highKeep = cfg.highPriorityKeepChance;
			this.lowKeep = cfg.lowPriorityKeepChance;
			this.particleBudget = cfg.maxParticles;
			this.interpDistSq = ExactDistance.limitSq(cfg.entityInterpSkipDistance);
			this.clientTickDistSq = ExactDistance.limitSq(cfg.distantClientTickDistance);
			this.livingAnimDistSq = ExactDistance.limitSq(cfg.livingAnimThrottleDistance);
			this.clientTickInterval = Math.max(2, cfg.distantClientTickInterval);
			this.fireworkBudget = cfg.maxFireworkParticlesPerTick;
			this.idleAiDistSq = ExactDistance.limitSq(cfg.idleAiFullDistance);
			this.ambientInterval = Math.max(1, cfg.ambientTickInterval);
			this.ambientRange = Math.max(4, cfg.ambientTickRange);
		}
	}

	public static void rebuild() {
		rebuild(HSNConfig.get());
	}

	public static void rebuild(HSNConfig cfg) {
		if (cfg == null) {
			cfg = new HSNConfig();
		}
		SNAP = new Snapshot(cfg, SNAP.scale);
		NativeBridge.applyConfig(cfg);
	}

	public static void publishScale(double next) {
		next = clamp(next, 0.25, 1.0);
		HSNConfig cfg = HSNConfig.get();
		if (cfg == null) {
			return;
		}
		if (Math.abs(next - SNAP.scale) < 0.005 && cfg.adaptiveCullingEnabled) {
			return;
		}
		SNAP = new Snapshot(cfg, next);
	}

	public static Snapshot snap() {
		return SNAP;
	}

	public static boolean masterOn() {
		return SNAP.master;
	}

	public static boolean flag(int bit) {
		return (SNAP.flags & bit) != 0;
	}

	public static int bits() {
		return SNAP.flags;
	}

	public static double scale() {
		return SNAP.scale;
	}

	public static double scaleSq() {
		return SNAP.scale * SNAP.scale;
	}

	public static double particleDistSq() {
		return SNAP.particleDistSq;
	}

	public static double particleDist() {
		return SNAP.particleDist;
	}

	public static double entityDist() {
		return SNAP.entityDist;
	}

	public static double entityDistSq() {
		return SNAP.entityDistSq;
	}

	public static double itemDistSq() {
		return SNAP.itemDistSq;
	}

	public static double xpDistSq() {
		return SNAP.xpDistSq;
	}

	public static double decoDistSq() {
		return SNAP.decoDistSq;
	}

	public static double blockEntityDistSq() {
		return SNAP.blockEntityDistSq;
	}

	public static double shadowDistSq() {
		return SNAP.shadowDistSq;
	}

	public static double nameTagDistSq() {
		return SNAP.nameTagDistSq;
	}

	public static double glowDistSq() {
		return SNAP.glowDistSq;
	}

	public static double beaconDistSq() {
		return SNAP.beaconDistSq;
	}

	public static double soundDistSq() {
		return SNAP.soundDistSq;
	}

	public static double rainKeep() {
		return SNAP.rainKeep;
	}

	public static double smokeKeep() {
		return SNAP.smokeKeep;
	}

	public static double explosionKeep() {
		return SNAP.explosionKeep;
	}

	public static double fireKeep() {
		return SNAP.fireKeep;
	}

	public static double bubbleKeep() {
		return SNAP.bubbleKeep;
	}

	public static double highKeep() {
		return SNAP.highKeep;
	}

	public static double lowKeep() {
		return SNAP.lowKeep;
	}

	public static int particleBudget() {
		return SNAP.particleBudget;
	}

	public static double interpDistSq() {
		return SNAP.interpDistSq;
	}

	public static double clientTickDistSq() {
		return SNAP.clientTickDistSq;
	}

	public static double livingAnimDistSq() {
		return SNAP.livingAnimDistSq;
	}

	public static int clientTickInterval() {
		return SNAP.clientTickInterval;
	}

	public static int fireworkBudget() {
		return SNAP.fireworkBudget;
	}

	public static double idleAiDistSq() {
		return SNAP.idleAiDistSq;
	}

	public static int ambientInterval() {
		return SNAP.ambientInterval;
	}

	public static int ambientRange() {
		return SNAP.ambientRange;
	}


	private static double clamp(double v, double lo, double hi) {
		return v < lo ? lo : (v > hi ? hi : v);
	}
}
