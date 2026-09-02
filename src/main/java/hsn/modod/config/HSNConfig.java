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
 * All tunables live here. Cloth Config + Sodium + hotkeys all read/write
 * this instance. {@link #sanitize()} runs after load and before save so a
 * bad JSON value cannot leave distances at 0 or NaN.
 */
public class HSNConfig {

	public static final String modVersionLabel = "3.8.5";

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
	public boolean adaptiveCullingEnabled = true;
	public int targetFps = 60;
	public double minAdaptiveScale = 0.5;
	public boolean performanceModeEnabled = false;
	public boolean framePacingFixEnabled = false;
	public boolean weakGpuAutoEnabled = true;
	public int weakGpuFpsThreshold = 35;

	/**
	 * Optional native batch path. Off = Java scalar only, works on every CPU.
	 * SIMD mode never forces an instruction the CPU lacks.
	 */
	public boolean nativeHotpathEnabled = true;
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

	public Preset lastAppliedPreset = Preset.BALANCED;

	public enum Preset {
		ULTRA_LOW("Ultra Low", "Minimum quality, maximum frame rate"),
		SAFE("Safe", "Conservative distances for low-end hardware"),
		BALANCED("Balanced", "Default profile for most systems"),
		QUALITY("Quality", "Near-vanilla rendering");

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
				}
			} catch (Exception e) {
				HSNOptimizations.LOGGER.warn("Failed to load config, using defaults: {}", e.toString());
				INSTANCE = new HSNConfig();
			}
		} else {
			INSTANCE = new HSNConfig();
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
		maxParticles = clampInt(maxParticles, 10, 8000);
		maxParticleDistance = clamp(maxParticleDistance, 2.0, 128.0);
		rainKeepChance = clamp01(rainKeepChance);
		smokeKeepChance = clamp01(smokeKeepChance);
		explosionKeepChance = clamp01(explosionKeepChance);
		fireSmokeKeepChance = clamp01(fireSmokeKeepChance);
		bubbleKeepChance = clamp01(bubbleKeepChance);
		highPriorityKeepChance = clamp01(highPriorityKeepChance);
		lowPriorityKeepChance = clamp01(lowPriorityKeepChance);

		maxEntityRenderDistance = clamp(maxEntityRenderDistance, 8.0, 256.0);
		maxItemEntityRenderDistance = clamp(maxItemEntityRenderDistance, 2.0, 128.0);
		maxXpOrbRenderDistance = clamp(maxXpOrbRenderDistance, 2.0, 128.0);
		maxDecorationEntityDistance = clamp(maxDecorationEntityDistance, 2.0, 128.0);
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
		targetFps = clampInt(targetFps, 10, 240);
		minAdaptiveScale = clamp(minAdaptiveScale, 0.25, 1.0);
		weakGpuFpsThreshold = clampInt(weakGpuFpsThreshold, 10, 120);

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
