package hsn.modod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import hsn.modod.HSNOptimizations;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Simple, focused config for client-side culling and presets.
 * All experimental / adaptive / hardcore flags have been removed.
 */
public class HSNConfig {

	public static final String modVersionLabel = "3.8.0-clean";

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
