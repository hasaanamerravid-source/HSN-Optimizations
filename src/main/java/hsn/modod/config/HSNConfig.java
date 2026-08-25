package hsn.modod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import hsn.modod.HSNOptimizations;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Focused config for client-side culling, unique rendering helpers and presets.
 * Designed for weak / older GPUs. Complements Sodium rather than fighting it.
 */
public class HSNConfig {

	public static final String modVersionLabel = "3.8.4";

	// Particles
	public boolean particleCullingEnabled = true;
	public int maxParticles = 400;
	public double maxParticleDistance = 16.0;
	public double rainKeepChance = 0.15;
	public double smokeKeepChance = 0.25;

	// Entities
	public boolean entityCullingEnabled = true;
	public double maxEntityRenderDistance = 32.0;
	public double maxItemEntityRenderDistance = 20.0;
	public double maxXpOrbRenderDistance = 16.0;
	public double maxDecorationEntityDistance = 16.0;

	// Shadows & name tags
	public boolean shadowCullingEnabled = true;
	public double maxShadowDistance = 12.0;
	public boolean nameTagCullEnabled = true;
	public double maxNameTagDistance = 24.0;

	// Block entities
	public boolean blockEntityCullingEnabled = true;
	public double maxBlockEntityRenderDistance = 24.0;
	/** Soft LOD: at medium distance skip expensive BE details before full cull. */
	public boolean blockEntityLodEnabled = true;
	public double blockEntityLodDistance = 14.0;

	// Sound
	public boolean soundDistanceCullingEnabled = true;
	public double maxSoundDistance = 24.0;
	public boolean weatherSoundReductionEnabled = true;
	public double weatherSoundKeepChance = 0.2;

	// Fog (optional, mild)
	public boolean fogScaleEnabled = false;
	public double fogScaleFactor = 0.85;

	// UI
	public boolean toastLimitEnabled = true;
	public boolean fpsOverlayEnabled = false;
	public int fpsOverlayX = 4;
	public int fpsOverlayY = 4;
	public boolean f3ShowStatus = true;

	// Adaptive FPS-based culling
	public boolean adaptiveCullingEnabled = true;
	public int targetFps = 60;
	public double minAdaptiveScale = 0.5;

	// Performance Mode (F6): forces the most aggressive adaptive scale on demand
	public boolean performanceModeEnabled = false;

	// Item merge (common side)
	public boolean itemMergeEnabled = true;
	public int itemMergeIntervalTicks = 40;
	public double itemMergeRadius = 2.0;

	// Extended particle categories
	public double explosionKeepChance = 1.0;
	public double fireSmokeKeepChance = 1.0;
	public double bubbleKeepChance = 1.0;

	// Sound burst limiter
	public boolean soundBurstLimitEnabled = false;
	public int maxNewSoundsPerTick = 24;

	// Graduated distance tick-throttling for item drops & XP orbs (off by default)
	public boolean itemThrottleEnabled = false;
	public double itemThrottleStartDistance = 24.0;
	public int itemThrottleMaxInterval = 8;

	// Defer to a dedicated entity-culling mod instead of stacking with it
	public boolean deferToDedicatedEntityCullingMods = true;

	// ── Unique rendering helpers (things Sodium / general cullers usually leave alone) ──

	/** Soft cloud distance + density reduction. Clouds are rarely touched by other optimizers. */
	public boolean cloudCullingEnabled = false;
	public double maxCloudDistance = 96.0;
	public double cloudDensityKeepChance = 0.55;

	/** Beacon beams are surprisingly expensive and often left rendering from very far away. */
	public boolean beaconBeamCullingEnabled = true;
	public double maxBeaconBeamDistance = 48.0;

	/** Glowing outline / team-color edge distance. Outlines add extra geometry that most cullers ignore. */
	public boolean glowOutlineCullingEnabled = true;
	public double maxGlowOutlineDistance = 28.0;

	/** Slow down the visual spin/bob of distant item entities (render only). */
	public boolean itemSpinThrottleEnabled = true;
	public double itemSpinThrottleDistance = 12.0;

	// ── Unique: Animated texture throttling (Sodium leaves this almost alone) ──
	/** Master switch for slowing / freezing animated block textures under load. */
	public boolean textureAnimThrottleEnabled = true;
	/** How often animations are allowed to advance. 1 = every tick, 2 = every 2nd tick, etc. Higher = slower. */
	public int textureAnimInterval = 1;
	/** When FPS is low, multiply the effective interval by this (adaptive). */
	public boolean textureAnimUseAdaptive = true;
	/** Max interval the adaptive system is allowed to push to under heavy load. */
	public int textureAnimMaxInterval = 4;

	// ── Unique: Priority particle system ──
	/** When particle budget is tight, keep high-priority particles first. */
	public boolean particlePriorityEnabled = true;
	/** Keep chance for high-priority particles (combat, player effects, etc.) when over budget. */
	public double highPriorityKeepChance = 0.85;
	/** Keep chance for low-priority / decorative particles when over budget. */
	public double lowPriorityKeepChance = 0.25;

	// ── Unique: Weak-GPU auto layer ──
	/** Automatically enable extra-aggressive rules when smoothed FPS stays low for a while. */
	public boolean weakGpuAutoEnabled = true;
	/** FPS threshold under which the weak-GPU layer activates. */
	public int weakGpuFpsThreshold = 35;

	// ── Progressive LOD (quality falloff near edge of render distance) ──
	/** Master switch: degrade quality of objects near the edge of their max distance. */
	public boolean progressiveLodEnabled = true;
	/** Start degrading quality at this fraction of max distance (0.5 = halfway). */
	public double progressiveLodStart = 0.50;
	/** Floor quality at the outer edge (0.15 = very low detail). */
	public double progressiveLodMinQuality = 0.15;
	/** Entity LOD stages: reduce animation / detail in steps as quality falls. */
	public boolean entityLodStagesEnabled = true;
	/** Particle quality curve: keep-chance falls off smoothly with distance. */
	public boolean particleQualityCurveEnabled = true;

	// Preset tracking
	public Preset lastAppliedPreset = Preset.BALANCED;

	public enum Preset {
		ULTRA_LOW,
		SAFE,
		BALANCED,
		QUALITY
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static HSNConfig INSTANCE;
	private static Path configPath;

	public static HSNConfig get() {
		if (INSTANCE == null) {
			load();
		}
		return INSTANCE;
	}

	public static void load() {
		configPath = FabricLoader.getInstance().getConfigDir().resolve("hsn-optimizations.json");
		if (Files.exists(configPath)) {
			try {
				String json = Files.readString(configPath);
				INSTANCE = GSON.fromJson(json, HSNConfig.class);
				if (INSTANCE == null) {
					INSTANCE = new HSNConfig();
				}
			} catch (Exception e) {
				HSNOptimizations.LOGGER.warn("Failed to load config, using defaults: {}", e.toString());
				INSTANCE = new HSNConfig();
			}
		} else {
			INSTANCE = new HSNConfig();
			INSTANCE.save();
		}
	}

	public void save() {
		if (configPath == null) {
			configPath = FabricLoader.getInstance().getConfigDir().resolve("hsn-optimizations.json");
		}
		try {
			Files.createDirectories(configPath.getParent());
			Files.writeString(configPath, GSON.toJson(this));
		} catch (IOException e) {
			HSNOptimizations.LOGGER.warn("Failed to save config: {}", e.toString());
		}
	}

	public double maxParticleDistanceSq() {
		return maxParticleDistance * maxParticleDistance;
	}

	public double maxSoundDistanceSq() {
		return maxSoundDistance * maxSoundDistance;
	}
}
