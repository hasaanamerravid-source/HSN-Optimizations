package hsn.modod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import hsn.modod.HSNOptimizations;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * All tunables live here. YACL + Sodium Video Settings + hotkeys all read/write
 * this instance. {@link #sanitize()} runs after load and before save so a
 * bad JSON value cannot leave distances at 0 or NaN.
 */
public class HSNConfig {

	public static final String modVersionLabel = "3.8.7 R";

	/** Master kill switch. False = every HSN mixin becomes a no-op. */
	public boolean modEnabled = true;
	/** Bumped when slider math changes so old JSON cannot keep broken flags. */
	public int configRevision = 0;

	// Particles
	public boolean particleCullingEnabled = true;
	public int maxParticles = 400;
	public double maxParticleDistance = 16.0;
	public double rainKeepChance = 0.15;
	public double smokeKeepChance = 0.25;
	public double explosionKeepChance = 1.0;
	public double fireSmokeKeepChance = 1.0;
	public double bubbleKeepChance = 1.0;
	public boolean particlePriorityEnabled = true;
	public double highPriorityKeepChance = 0.85;
	public double lowPriorityKeepChance = 0.25;
	public boolean particleQualityCurveEnabled = true;

	// Entities
	public boolean entityCullingEnabled = true;
	public double maxEntityRenderDistance = 32.0;
	public double maxItemEntityRenderDistance = 20.0;
	public double maxXpOrbRenderDistance = 16.0;
	public double maxDecorationEntityDistance = 16.0;
	public boolean deferToDedicatedEntityCullingMods = true;

	// Shadows & name tags
	public boolean shadowCullingEnabled = true;
	public double maxShadowDistance = 12.0;
	public boolean nameTagCullEnabled = true;
	public double maxNameTagDistance = 24.0;

	// Block entities
	public boolean blockEntityCullingEnabled = true;
	public double maxBlockEntityRenderDistance = 24.0;
	public boolean blockEntityLodEnabled = true;
	public double blockEntityLodDistance = 14.0;

	// Sound
	public boolean soundDistanceCullingEnabled = true;
	public double maxSoundDistance = 48.0;
	public boolean weatherSoundReductionEnabled = false;
	public double weatherSoundKeepChance = 0.2;
	public boolean soundBurstLimitEnabled = false;
	public int maxNewSoundsPerTick = 24;

	// Fog
	public boolean fogScaleEnabled = false;
	public double fogScaleFactor = 0.85;

	// UI
	public boolean toastLimitEnabled = true;
	public boolean fpsOverlayEnabled = false;
	public int fpsOverlayX = 4;
	public int fpsOverlayY = 4;
	public boolean f3ShowStatus = true;
	public boolean f3ShowDetails = true;

	// Adaptive
	public boolean adaptiveCullingEnabled = false;
	public int targetFps = 60;
	public double minAdaptiveScale = 0.5;
	public boolean performanceModeEnabled = false;
	public boolean framePacingFixEnabled = false;
	public boolean weakGpuAutoEnabled = true;
	public int weakGpuFpsThreshold = 35;

	/** Extra low-end / integrated-GPU tuning (original HSN helpers). */
	public boolean lowEndHardwareTuneEnabled = true;
	public boolean laptopPowerSaveEnabled = true;
	public boolean adaptiveUploadBudgetEnabled = true;
	/** 0.05–0.40 of a frame reserved for chunk-like client work under load. */
	public double uploadBudgetFraction = 0.12;

	/** Hide entities whose section was not visited by Sodium this frame. Fail-open. */
	public boolean sectionOccupancyCullingEnabled = false;

	public boolean deferFogToSodiumExtra = true;
	public boolean deferToastsToSodiumExtra = true;
	public boolean deferBeaconToSodiumExtra = true;
	public boolean deferTextureAnimToSodiumExtra = true;
	public boolean deferParticlesToSodiumExtra = false;

	/**
	 * Optional native batch path. Off = Java scalar only, works on every CPU.
	 * SIMD mode never forces an instruction the CPU lacks.
	 */
	public boolean nativeHotpathEnabled = true;
	/** Assembly / C / Rust frustum tests on packed AABBs and spheres. Off = skip. */
	public boolean nativeFrustumCullingEnabled = true;
	public SimdMode simdMode = SimdMode.AUTO;

	/**
	 * Simulation extras (path throttle, item/XP tick skip, extra chunk drain)
	 * run only on the integrated server unless this is false.
	 */
	public boolean integratedServerOnly = true;
	/** When Lithium is loaded, skip HSN pathfinding throttle. */
	public boolean deferPathfindingToLithium = true;

	// Distant item / XP tick throttle (off by default)
	public boolean itemThrottleEnabled = false;
	public double itemThrottleStartDistance = 24.0;
	public int itemThrottleMaxInterval = 8;

	// AI pathfinding (reuse current path only when the goal is unchanged)
	public boolean pathfindingThrottleEnabled = true;
	public double pathfindingFullDistance = 32.0;
	public int pathfindingMaxInterval = 8;

	public boolean locateOptimizeEnabled = true;
	public int locateCacheTtlSeconds = 30;
	public boolean fastWorldLoadEnabled = false;
	public int fastWorldLoadChunkBoost = 6;
	/** Extra chunk-task drain only during this many seconds after server start. */
	public int fastWorldLoadWindowSeconds = 8;

	// Sky / extras
	public boolean beaconBeamCullingEnabled = true;
	public double maxBeaconBeamDistance = 48.0;
	public boolean glowOutlineCullingEnabled = true;
	public double maxGlowOutlineDistance = 28.0;
	public boolean itemSpinThrottleEnabled = true;
	public double itemSpinThrottleDistance = 12.0;

	// Animated textures
	public boolean textureAnimThrottleEnabled = true;
	public int textureAnimInterval = 1;
	public boolean textureAnimUseAdaptive = true;
	public int textureAnimMaxInterval = 4;

	// LOD
	public boolean progressiveLodEnabled = true;
	public double progressiveLodStart = 0.50;
	public double progressiveLodMinQuality = 0.15;
	public boolean entityLodStagesEnabled = true;
	/** Mip bias on the blocks atlas while the world is drawn. */
	public boolean blockTextureLodEnabled = true;
	public double blockTextureLodBias = 1.25;
	public boolean blockTextureLodAdaptive = true;

	// Circular chunk drawing (same idea as Uniaball circular-rendering):
	// square load, circular/elliptical draw. Radius = view distance.
	public boolean circularRenderingEnabled = false;
	/** 1.0 = view-distance radius. Lower values shrink the mask only. */
	public double circularRadiusScale = 1.0;
	public boolean circularVerticalRangeEnabled = false;
	public int circularVerticalRange = 16;

	public WorldRenderShape worldRenderShape = WorldRenderShape.OFF;
	public int alwaysKeepChunks = 3;

	// --- 3.8.7 R high-end CPU pass (helps even when the GPU is idle) ---
	/** Skip LightTexture rebuilds when gamma/effects/dimension did not change. */
	public boolean lightmapCacheEnabled = true;
	/** Skip client interpolation for far, non-combat entities. */
	public boolean entityInterpSkipEnabled = true;
	public double entityInterpSkipDistance = 48.0;
	/** Skip some client ticks on distant decorations / items / displays. */
	public boolean distantClientTickSkipEnabled = true;
	public double distantClientTickDistance = 40.0;
	public int distantClientTickInterval = 4;
	/** Cap FPS when the game window is not focused. */
	public boolean unfocusedFpsCapEnabled = true;
	public int unfocusedFpsCap = 30;
	/** Cheapen / skip far cloud layers when the camera is not looking up. */
	public boolean cloudLodEnabled = true;
	/** Thin weather overlay work when FPS is healthy or the player is underground. */
	public boolean weatherRendererLodEnabled = true;
	/** Skip living-entity pose / limb-swing work past a distance. */
	public boolean livingAnimThrottleEnabled = true;
	public double livingAnimThrottleDistance = 36.0;
	/** Rebuild map textures less often. */
	public boolean mapRendererThrottleEnabled = true;
	public int mapRendererInterval = 4;
	/** Skip star / sunrise extras when looking down or under a ceiling. */
	public boolean skyExtrasThrottleEnabled = true;
	/** Cap firework burst particles independently of the global budget. */
	public boolean fireworkParticleCapEnabled = true;
	public int maxFireworkParticlesPerTick = 48;
	/** Skip idle GoalSelector evaluation for far, non-combat mobs (integrated server). */
	public boolean idleAiThrottleEnabled = true;
	public double idleAiFullDistance = 48.0;
	public int idleAiMaxInterval = 10;
	/** Skip world-border mesh when the camera is far inside the border. */
	public boolean worldBorderLodEnabled = true;
	/** Skip dripping / falling block particles underground or far away. */
	public boolean dripParticleThrottleEnabled = true;
	/** Skip boss-bar overlay layout when no boss events are active (cheap no-op guard). */
	public boolean skipEmptyBossOverlayEnabled = true;
	/** Hard-cap live particle list each tick after spawn filters. */
	public boolean hardParticleCapEnabled = true;

	public Preset lastAppliedPreset = Preset.BALANCED;

	/**
	 * Set the first time HSN sees a real GPU string and picks a starting
	 * preset for it (see {@code GpuAutoTune}). Existing configs are marked
	 * as already-applied on load so upgrading never overwrites a preset the
	 * player picked themselves.
	 */
	public boolean autoHardwareTierApplied = false;

	public enum Preset {
		ULTRA_LOW("Ultra Low", "Minimum quality, maximum frame rate"),
		SAFE("Safe", "Conservative distances for low-end hardware"),
		BALANCED("Balanced", "Default profile for most systems"),
		QUALITY("Quality", "Near-vanilla rendering"),
		COMPETITIVE("Competitive", "High refresh: keep visuals, cut CPU waste");

		private final String title;
		private final String audience;

		Preset(String title, String audience) {
			this.title = title;
			this.audience = audience;
		}

		public String title() {
			return title;
		}

		public String audience() {
			return audience;
		}

		@Override
		public String toString() {
			return title;
		}
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
	private static volatile HSNConfig INSTANCE;
	private static Path configPath;

	public static HSNConfig get() {
		HSNConfig current = INSTANCE;
		if (current == null) {
			load();
			current = INSTANCE;
		}
		return current;
	}

	public static synchronized void load() {
		configPath = FabricLoader.getInstance().getConfigDir().resolve("hsn-optimizations.json");
		if (Files.exists(configPath)) {
			try {
				String json = Files.readString(configPath);
				INSTANCE = GSON.fromJson(json, HSNConfig.class);
				if (INSTANCE == null) {
					INSTANCE = new HSNConfig();
				} else {
					// A config file already existed before this player ever saw the
					// GPU auto-tier feature (or they've already been tiered once).
					// Never let it silently re-pick a preset out from under them.
					INSTANCE.autoHardwareTierApplied = true;
					if (INSTANCE.configRevision < 3872) {
						// Pre-fix JSON stacked adaptive scale + occupancy on the
						// distance sliders. Turn those off once so 32 means 32.
						INSTANCE.adaptiveCullingEnabled = false;
						INSTANCE.sectionOccupancyCullingEnabled = false;
						INSTANCE.performanceModeEnabled = false;
						INSTANCE.configRevision = 3872;
					}
				}
			} catch (Exception e) {
				HSNOptimizations.LOGGER.warn("Failed to load config, using defaults: {}", e.toString());
				INSTANCE = new HSNConfig();
			}
		} else {
			INSTANCE = new HSNConfig();
			INSTANCE.configRevision = 3872;
		}
		INSTANCE.sanitize();
		hsn.modod.optimize.HotPath.rebuild(INSTANCE);
		INSTANCE.save();
	}

	public synchronized void save() {
		sanitize();
		if (configPath == null) {
			configPath = FabricLoader.getInstance().getConfigDir().resolve("hsn-optimizations.json");
		}
		try {
			Files.createDirectories(configPath.getParent());
			Path tmp = configPath.resolveSibling(configPath.getFileName().toString() + ".tmp");
			Files.writeString(tmp, GSON.toJson(this));
			try {
				Files.move(tmp, configPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (IOException atomicUnsupported) {
				Files.move(tmp, configPath, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			HSNOptimizations.LOGGER.warn("Failed to save config: {}", e.toString());
		}
		hsn.modod.optimize.HotPath.rebuild(this);
	}

	/** Clamp every numeric field so mixins never see 0 / NaN / inverted ranges. */
	public void sanitize() {
		maxParticles = clampInt(maxParticles, 10, 20000);
		maxParticleDistance = clamp(maxParticleDistance, 2.0, 256.0);
		rainKeepChance = clamp01(rainKeepChance);
		smokeKeepChance = clamp01(smokeKeepChance);
		explosionKeepChance = clamp01(explosionKeepChance);
		fireSmokeKeepChance = clamp01(fireSmokeKeepChance);
		bubbleKeepChance = clamp01(bubbleKeepChance);
		highPriorityKeepChance = clamp01(highPriorityKeepChance);
		lowPriorityKeepChance = clamp01(lowPriorityKeepChance);

		maxEntityRenderDistance = clamp(maxEntityRenderDistance, 4.0, 512.0);
		maxItemEntityRenderDistance = clamp(maxItemEntityRenderDistance, 4.0, 256.0);
		maxXpOrbRenderDistance = clamp(maxXpOrbRenderDistance, 4.0, 256.0);
		maxDecorationEntityDistance = clamp(maxDecorationEntityDistance, 4.0, 256.0);
		maxShadowDistance = clamp(maxShadowDistance, 1.0, 64.0);
		maxNameTagDistance = clamp(maxNameTagDistance, 2.0, 128.0);
		maxBlockEntityRenderDistance = clamp(maxBlockEntityRenderDistance, 4.0, 128.0);
		blockEntityLodDistance = clamp(blockEntityLodDistance, 4.0, 128.0);

		maxSoundDistance = clamp(maxSoundDistance, 4.0, 128.0);
		weatherSoundKeepChance = clamp01(weatherSoundKeepChance);
		maxNewSoundsPerTick = clampInt(maxNewSoundsPerTick, 1, 128);
		fogScaleFactor = clamp(fogScaleFactor, 0.35, 1.0);

		fpsOverlayX = clampInt(fpsOverlayX, 0, 400);
		fpsOverlayY = clampInt(fpsOverlayY, 0, 400);
		targetFps = clampInt(targetFps, 10, 1000);
		minAdaptiveScale = clamp(minAdaptiveScale, 0.25, 1.0);
		weakGpuFpsThreshold = clampInt(weakGpuFpsThreshold, 10, 120);
		uploadBudgetFraction = clamp(uploadBudgetFraction, 0.05, 0.40);

		itemThrottleStartDistance = clamp(itemThrottleStartDistance, 8.0, 128.0);
		itemThrottleMaxInterval = clampInt(itemThrottleMaxInterval, 2, 40);
		pathfindingFullDistance = clamp(pathfindingFullDistance, 8.0, 128.0);
		pathfindingMaxInterval = clampInt(pathfindingMaxInterval, 2, 40);
		locateCacheTtlSeconds = clampInt(locateCacheTtlSeconds, 5, 300);
		fastWorldLoadChunkBoost = clampInt(fastWorldLoadChunkBoost, 1, 32);
		fastWorldLoadWindowSeconds = clampInt(fastWorldLoadWindowSeconds, 1, 30);

		maxBeaconBeamDistance = clamp(maxBeaconBeamDistance, 8.0, 256.0);
		maxGlowOutlineDistance = clamp(maxGlowOutlineDistance, 4.0, 128.0);
		itemSpinThrottleDistance = clamp(itemSpinThrottleDistance, 2.0, 64.0);

		textureAnimInterval = clampInt(textureAnimInterval, 1, 16);
		textureAnimMaxInterval = clampInt(textureAnimMaxInterval, 1, 16);
		if (textureAnimMaxInterval < textureAnimInterval) {
			textureAnimMaxInterval = textureAnimInterval;
		}

		progressiveLodStart = clamp(progressiveLodStart, 0.15, 0.95);
		progressiveLodMinQuality = clamp(progressiveLodMinQuality, 0.05, 1.0);
		blockTextureLodBias = clamp(blockTextureLodBias, 0.0, 3.0);
		circularRadiusScale = clamp(circularRadiusScale, 0.25, 1.0);
		circularVerticalRange = clampInt(circularVerticalRange, 1, 32);
		alwaysKeepChunks = clampInt(alwaysKeepChunks, 2, 8);
		entityInterpSkipDistance = clamp(entityInterpSkipDistance, 8.0, 256.0);
		distantClientTickDistance = clamp(distantClientTickDistance, 8.0, 256.0);
		distantClientTickInterval = clampInt(distantClientTickInterval, 2, 20);
		unfocusedFpsCap = clampInt(unfocusedFpsCap, 5, 240);
		livingAnimThrottleDistance = clamp(livingAnimThrottleDistance, 8.0, 256.0);
		mapRendererInterval = clampInt(mapRendererInterval, 1, 20);
		maxFireworkParticlesPerTick = clampInt(maxFireworkParticlesPerTick, 4, 256);
		idleAiFullDistance = clamp(idleAiFullDistance, 8.0, 256.0);
		idleAiMaxInterval = clampInt(idleAiMaxInterval, 2, 40);

		if (worldRenderShape == null) {
			worldRenderShape = WorldRenderShape.OFF;
		}
		if (simdMode == null) {
			simdMode = SimdMode.AUTO;
		}
		if (lastAppliedPreset == null) {
			lastAppliedPreset = Preset.BALANCED;
		}
	}

	public double maxParticleDistanceSq() {
		return maxParticleDistance * maxParticleDistance;
	}

	public double maxSoundDistanceSq() {
		return maxSoundDistance * maxSoundDistance;
	}

	private static double clamp(double v, double min, double max) {
		if (Double.isNaN(v) || Double.isInfinite(v)) {
			return min;
		}
		return Math.max(min, Math.min(max, v));
	}

	private static double clamp01(double v) {
		return clamp(v, 0.0, 1.0);
	}

	private static int clampInt(int v, int min, int max) {
		return Math.max(min, Math.min(max, v));
	}
}
