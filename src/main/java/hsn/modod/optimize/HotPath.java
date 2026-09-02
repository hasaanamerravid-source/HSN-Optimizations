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

	private static volatile int flags = PARTICLE_CULL | ENTITY_CULL;
	private static volatile double scale = 1.0;
	private static volatile double scaleSq = 1.0;

	private static volatile double particleDistSq = 256.0;
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

		flags = bits;
		particleDistSq = sq(cfg.maxParticleDistance);
		entityDistSq = sq(cfg.maxEntityRenderDistance);
		itemDistSq = sq(cfg.maxItemEntityRenderDistance);
		xpDistSq = sq(cfg.maxXpOrbRenderDistance);
		decoDistSq = sq(cfg.maxDecorationEntityDistance);
		blockEntityDistSq = sq(cfg.maxBlockEntityRenderDistance);
		shadowDistSq = sq(cfg.maxShadowDistance);
		nameTagDistSq = sq(cfg.maxNameTagDistance);
		glowDistSq = sq(cfg.maxGlowOutlineDistance);
		beaconDistSq = sq(cfg.maxBeaconBeamDistance);
		soundDistSq = sq(cfg.maxSoundDistance);
		rainKeep = cfg.rainKeepChance;
		smokeKeep = cfg.smokeKeepChance;
		explosionKeep = cfg.explosionKeepChance;
		fireKeep = cfg.fireSmokeKeepChance;
		bubbleKeep = cfg.bubbleKeepChance;
		particleBudget = cfg.maxParticles;
		NativeBridge.applyConfig(cfg);
	}

	public static void publishScale(double next) {
		if (next < 0.05) next = 0.05;
		else if (next > 1.0) next = 1.0;
		scale = next;
		scaleSq = next * next;
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
		return particleDistSq * scaleSq;
	}

	public static double entityDistSq() {
		return entityDistSq * scaleSq;
	}

	public static double itemDistSq() {
		return itemDistSq * scaleSq;
	}

	public static double xpDistSq() {
		return xpDistSq * scaleSq;
	}

	public static double decoDistSq() {
		return decoDistSq * scaleSq;
	}

	public static double blockEntityDistSq() {
		return blockEntityDistSq * scaleSq;
	}

	public static double shadowDistSq() {
		return shadowDistSq * scaleSq;
	}

	public static double nameTagDistSq() {
		return nameTagDistSq * scaleSq;
	}

	public static double glowDistSq() {
		return glowDistSq * scaleSq;
	}

	public static double beaconDistSq() {
		return beaconDistSq * scaleSq;
	}

	public static double soundDistSq() {
		return soundDistSq * scaleSq;
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

	private static double sq(double v) {
		if (v <= 0.0 || Double.isNaN(v)) {
			return 0.0;
		}
		return v * v;
	}
}
