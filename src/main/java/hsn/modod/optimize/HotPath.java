package hsn.modod.optimize;

import hsn.modod.config.HSNConfig;

/**
 * Tick-stable snapshot of the values mixins read thousands of times per frame.
 * <p>
 * Modelled after FerriteCore's approach: do not chase object graphs on the hot
 * path. Vanilla config is a fat heap object with dozens of boxed-looking fields.
 * Mixins used to call {@code HSNConfig.get()} per particle / entity, then read
 * several fields and square a distance. That is pointer-chasing plus repeated
 * work. This class stores the already-squared limits and a packed flag word in
 * static volatiles so a cull check is a handful of primitive reads.
 * <p>
 * Rebuilt on config save and when adaptive scale changes. One rebuild per tick
 * is fine; one rebuild per entity is not.
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


	private static volatile boolean master = true;
	private static volatile int flags = PARTICLE_CULL | ENTITY_CULL;
	private static volatile double scale = 1.0;
	private static volatile double scaleSq = 1.0;

	private static volatile double particleDistSq = 256.0;
	private static volatile double particleDistBase = 16.0;
	private static volatile double entityDistSq = 1024.0;
	private static volatile double itemDistSq = 400.0;
	private static volatile double xpDistSq = 256.0;
	private static volatile double decoDistSq = 256.0;
	private static volatile double blockEntityDistSq = 576.0;
	private static volatile double shadowDistSq = 144.0;
	private static volatile double nameTagDistSq = 576.0;
	private static volatile double glowDistSq = 784.0;
	private static volatile double beaconDistSq = 2304.0;
	private static volatile double soundDistSq = 2304.0;

	private static volatile double rainKeep = 0.15;
	private static volatile double smokeKeep = 0.25;
	private static volatile double explosionKeep = 1.0;
	private static volatile double fireKeep = 1.0;
	private static volatile double bubbleKeep = 1.0;
	private static volatile int particleBudget = 400;
	private static volatile double interpDistSq = 2304.0;
	private static volatile double clientTickDistSq = 1600.0;
	private static volatile double livingAnimDistSq = 1296.0;
	private static volatile int clientTickInterval = 4;
	private static volatile int fireworkBudget = 48;
	private static volatile double idleAiDistSq = 2304.0;


	private HotPath() {
	}

	public static void rebuild() {
		rebuild(HSNConfig.get());
	}

	public static void rebuild(HSNConfig cfg) {
		if (cfg == null) {
			cfg = new HSNConfig();
		}
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


		master = cfg.modEnabled;
		if (!master) {
			bits = 0;
		}
		flags = bits;
		bakeDistances(cfg);
		rainKeep = cfg.rainKeepChance;
		smokeKeep = cfg.smokeKeepChance;
		explosionKeep = cfg.explosionKeepChance;
		fireKeep = cfg.fireSmokeKeepChance;
		bubbleKeep = cfg.bubbleKeepChance;
		particleBudget = cfg.maxParticles;
		interpDistSq = sq(cfg.entityInterpSkipDistance);
		clientTickDistSq = sq(cfg.distantClientTickDistance);
		livingAnimDistSq = sq(cfg.livingAnimThrottleDistance);
		clientTickInterval = Math.max(2, cfg.distantClientTickInterval);
		fireworkBudget = cfg.maxFireworkParticlesPerTick;
		idleAiDistSq = sq(cfg.idleAiFullDistance);

		NativeBridge.applyConfig(cfg);
	}

	public static void publishScale(double next) {
		if (next < 0.05) next = 0.05;
		else if (next > 1.0) next = 1.0;
		scale = next;
		scaleSq = next * next;
		bakeDistances(HSNConfig.get());
	}

	public static boolean masterOn() {
		return master;
	}

	/**
	 * Bake slider blocks into squared limits. Adaptive / performance scale is
	 * applied only when those features are on. Otherwise 32 on the slider is
	 * exactly 32 blocks — no hidden 0.55 weights, no second square.
	 */
	private static void bakeDistances(HSNConfig cfg) {
		if (cfg == null) {
			return;
		}
		double s = 1.0;
		if (cfg.modEnabled && (cfg.adaptiveCullingEnabled || cfg.performanceModeEnabled)) {
			s = scale;
			if (s < 0.05) s = 0.05;
			if (s > 1.0) s = 1.0;
		}
		particleDistBase = cfg.maxParticleDistance * s;
		particleDistSq = sq(particleDistBase);
		entityDistSq = sq(cfg.maxEntityRenderDistance * s);
		itemDistSq = sq(cfg.maxItemEntityRenderDistance * s);
		xpDistSq = sq(cfg.maxXpOrbRenderDistance * s);
		decoDistSq = sq(cfg.maxDecorationEntityDistance * s);
		blockEntityDistSq = sq(cfg.maxBlockEntityRenderDistance * s);
		shadowDistSq = sq(cfg.maxShadowDistance * s);
		nameTagDistSq = sq(cfg.maxNameTagDistance * s);
		glowDistSq = sq(cfg.maxGlowOutlineDistance * s);
		beaconDistSq = sq(cfg.maxBeaconBeamDistance * s);
		soundDistSq = sq(cfg.maxSoundDistance * s);
		interpDistSq = sq(cfg.entityInterpSkipDistance);
		clientTickDistSq = sq(cfg.distantClientTickDistance);
		livingAnimDistSq = sq(cfg.livingAnimThrottleDistance);
		idleAiDistSq = sq(cfg.idleAiFullDistance);
	}

	public static boolean flag(int bit) {
		return (flags & bit) != 0;
	}

	public static double scale() {
		return scale;
	}

	public static double scaleSq() {
		return scaleSq;
	}

	public static double particleDistSq() {
		return particleDistSq;
	}

	/** Same limit as {@link #particleDistSq()} but not squared — precomputed, no sqrt on the hot path. */
	public static double particleDist() {
		return particleDistBase;
	}

	public static double entityDistSq() {
		return entityDistSq;
	}

	public static double itemDistSq() {
		return itemDistSq;
	}

	public static double xpDistSq() {
		return xpDistSq;
	}

	public static double decoDistSq() {
		return decoDistSq;
	}

	public static double blockEntityDistSq() {
		return blockEntityDistSq;
	}

	public static double shadowDistSq() {
		return shadowDistSq;
	}

	public static double nameTagDistSq() {
		return nameTagDistSq;
	}

	public static double glowDistSq() {
		return glowDistSq;
	}

	public static double beaconDistSq() {
		return beaconDistSq;
	}

	public static double soundDistSq() {
		return soundDistSq;
	}

	public static double rainKeep() {
		return rainKeep;
	}

	public static double smokeKeep() {
		return smokeKeep;
	}

	public static double explosionKeep() {
		return explosionKeep;
	}

	public static double fireKeep() {
		return fireKeep;
	}

	public static double bubbleKeep() {
		return bubbleKeep;
	}

	public static int particleBudget() {
		return particleBudget;
	}

	public static double interpDistSq() {
		return interpDistSq;
	}

	public static double clientTickDistSq() {
		return clientTickDistSq;
	}

	public static double livingAnimDistSq() {
		return livingAnimDistSq;
	}

	public static int clientTickInterval() {
		return clientTickInterval;
	}

	public static int fireworkBudget() {
		return fireworkBudget;
	}

	public static double idleAiDistSq() {
		return idleAiDistSq;
	}


	private static double sq(double v) {
		if (v <= 0.0 || Double.isNaN(v)) {
			return 0.0;
		}
		return v * v;
	}
}
