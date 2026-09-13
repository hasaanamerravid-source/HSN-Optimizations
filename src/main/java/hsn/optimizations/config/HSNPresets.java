package hsn.optimizations.config;

import hsn.optimizations.optimize.HotPath;

/**
 * Real one-click profiles. Every gameplay field is written so Ultra Low
 * is actually "max FPS" and Quality is actually "near vanilla", not a
 * handful of particle sliders with the rest left on whatever the last
 * JSON happened to contain.
 * <p>
 * Compatibility switches (Sodium Extra deferrals, Lithium path deferral,
 * integrated-server-only, overlay position) are left alone.
 */
public final class HSNPresets {

	private HSNPresets() {
	}

	public static void apply(HSNConfig c, HSNConfig.Preset preset) {
		if (c == null || preset == null) {
			return;
		}
		c.modEnabled = true;
		c.nativeHotpathEnabled = true;
		c.nativeFrustumCullingEnabled = true;
		c.behindCameraCullEnabled = false;
		c.simdMode = SimdMode.AUTO;
		c.framePacingFixEnabled = true;
		c.locateOptimizeEnabled = true;
		c.locateCacheTtlSeconds = 30;
		switch (preset) {
			case ULTRA_LOW -> ultraLow(c);
			case SAFE -> safe(c);
			case BALANCED -> balanced(c);
			case QUALITY -> quality(c);
			case COMPETITIVE -> competitive(c);
		}
		c.lastAppliedPreset = preset;
		c.sanitize();
		HotPath.rebuild(c);
	}

	/** Weak iGPU / software renderer. Shortest distances, every saver on. */
	private static void ultraLow(HSNConfig c) {
		c.performanceModeEnabled = true;
		c.adaptiveCullingEnabled = true;
		c.targetFps = 60;
		c.minAdaptiveScale = 0.25;
		c.weakGpuAutoEnabled = true;
		c.weakGpuFpsThreshold = 45;
		c.lowEndHardwareTuneEnabled = true;
		c.laptopPowerSaveEnabled = true;
		c.adaptiveUploadBudgetEnabled = true;
		c.uploadBudgetFraction = 0.28;
		c.sectionOccupancyCullingEnabled = false;

		c.particleCullingEnabled = true;
		c.maxParticles = 80;
		c.maxParticleDistance = 8.0;
		c.rainKeepChance = 0.04;
		c.smokeKeepChance = 0.08;
		c.explosionKeepChance = 0.25;
		c.fireSmokeKeepChance = 0.30;
		c.bubbleKeepChance = 0.15;
		c.particlePriorityEnabled = true;
		c.highPriorityKeepChance = 0.60;
		c.lowPriorityKeepChance = 0.05;
		c.particleQualityCurveEnabled = true;
		c.hardParticleCapEnabled = true;
		c.fireworkParticleCapEnabled = true;
		c.maxFireworkParticlesPerTick = 8;
		c.dripParticleThrottleEnabled = true;

		c.entityCullingEnabled = true;
		c.maxEntityRenderDistance = 16.0;
		c.maxItemEntityRenderDistance = 8.0;
		c.maxXpOrbRenderDistance = 6.0;
		c.maxDecorationEntityDistance = 8.0;
		c.entityLodStagesEnabled = true;
		c.entityInterpSkipEnabled = true;
		c.entityInterpSkipDistance = 16.0;
		c.distantClientTickSkipEnabled = true;
		c.distantClientTickDistance = 12.0;
		c.distantClientTickInterval = 10;
		c.livingAnimThrottleEnabled = true;
		c.livingAnimThrottleDistance = 12.0;
		c.itemSpinThrottleEnabled = true;
		c.itemSpinThrottleDistance = 4.0;
		c.itemThrottleEnabled = true;
		c.itemThrottleStartDistance = 8.0;
		c.itemThrottleMaxInterval = 16;

		c.blockEntityCullingEnabled = true;
		c.maxBlockEntityRenderDistance = 10.0;
		c.blockEntityLodEnabled = true;
		c.blockEntityLodDistance = 6.0;

		c.shadowCullingEnabled = true;
		c.maxShadowDistance = 4.0;
		c.nameTagCullEnabled = true;
		c.maxNameTagDistance = 10.0;
		c.glowOutlineCullingEnabled = true;
		c.maxGlowOutlineDistance = 8.0;
		c.beaconBeamCullingEnabled = true;
		c.maxBeaconBeamDistance = 16.0;

		c.soundDistanceCullingEnabled = true;
		c.maxSoundDistance = 12.0;
		c.weatherSoundReductionEnabled = true;
		c.weatherSoundKeepChance = 0.05;
		c.soundBurstLimitEnabled = true;
		c.maxNewSoundsPerTick = 6;

		c.fogScaleEnabled = true;
		c.fogScaleFactor = 0.40;
		c.toastLimitEnabled = true;

		c.circularRenderingEnabled = true;
		c.worldRenderShape = WorldRenderShape.CIRCLE;
		c.circularRadiusScale = 0.55;
		c.circularVerticalRangeEnabled = true;
		c.circularVerticalRange = 8;
		c.alwaysKeepChunks = 2;

		c.progressiveLodEnabled = true;
		c.progressiveLodStart = 0.20;
		c.progressiveLodMinQuality = 0.05;
		c.blockTextureLodEnabled = true;
		c.blockTextureLodBias = 3.0;
		c.blockTextureLodAdaptive = true;
		c.textureAnimThrottleEnabled = true;
		c.textureAnimInterval = 4;
		c.textureAnimUseAdaptive = true;
		c.textureAnimMaxInterval = 12;

		c.pathfindingThrottleEnabled = true;
		c.pathfindingFullDistance = 12.0;
		c.pathfindingMaxInterval = 16;
		c.idleAiThrottleEnabled = true;
		c.idleAiFullDistance = 16.0;
		c.idleAiMaxInterval = 20;
		c.fastWorldLoadEnabled = true;
		c.fastWorldLoadWindowSeconds = 6;
		c.fastWorldLoadChunkBoost = 4;

		c.lightmapCacheEnabled = true;
		c.unfocusedFpsCapEnabled = true;
		c.unfocusedFpsCap = 15;
		c.cloudLodEnabled = true;
		c.weatherRendererLodEnabled = true;
		c.mapRendererThrottleEnabled = true;
		c.mapRendererInterval = 10;
		c.skyExtrasThrottleEnabled = true;
		c.ambientTickCullEnabled = true;
		c.ambientTickInterval = 4;
		c.ambientTickRange = 8;
		c.worldBorderLodEnabled = true;
		c.skipEmptyBossOverlayEnabled = true;
		c.renderScaleEnabled = true;
		c.renderScale = 0.50;
		c.renderScaleFilter = ScaleFilter.LINEAR;
		c.renderScaleAdaptive = true;
	}

	/** Low-end but still readable nearby. */
	private static void safe(HSNConfig c) {
		c.performanceModeEnabled = false;
		c.adaptiveCullingEnabled = false;
		c.targetFps = 60;
		c.minAdaptiveScale = 0.40;
		c.weakGpuAutoEnabled = true;
		c.weakGpuFpsThreshold = 35;
		c.lowEndHardwareTuneEnabled = true;
		c.laptopPowerSaveEnabled = true;
		c.adaptiveUploadBudgetEnabled = true;
		c.uploadBudgetFraction = 0.18;
		c.sectionOccupancyCullingEnabled = false;

		c.particleCullingEnabled = true;
		c.maxParticles = 220;
		c.maxParticleDistance = 12.0;
		c.rainKeepChance = 0.10;
		c.smokeKeepChance = 0.18;
		c.explosionKeepChance = 0.55;
		c.fireSmokeKeepChance = 0.55;
		c.bubbleKeepChance = 0.40;
		c.particlePriorityEnabled = true;
		c.highPriorityKeepChance = 0.80;
		c.lowPriorityKeepChance = 0.15;
		c.particleQualityCurveEnabled = true;
		c.hardParticleCapEnabled = true;
		c.fireworkParticleCapEnabled = true;
		c.maxFireworkParticlesPerTick = 20;
		c.dripParticleThrottleEnabled = true;

		c.entityCullingEnabled = true;
		c.maxEntityRenderDistance = 24.0;
		c.maxItemEntityRenderDistance = 14.0;
		c.maxXpOrbRenderDistance = 10.0;
		c.maxDecorationEntityDistance = 12.0;
		c.entityLodStagesEnabled = true;
		c.entityInterpSkipEnabled = true;
		c.entityInterpSkipDistance = 28.0;
		c.distantClientTickSkipEnabled = true;
		c.distantClientTickDistance = 20.0;
		c.distantClientTickInterval = 6;
		c.livingAnimThrottleEnabled = true;
		c.livingAnimThrottleDistance = 20.0;
		c.itemSpinThrottleEnabled = true;
		c.itemSpinThrottleDistance = 8.0;
		c.itemThrottleEnabled = false;
		c.itemThrottleStartDistance = 16.0;
		c.itemThrottleMaxInterval = 10;

		c.blockEntityCullingEnabled = true;
		c.maxBlockEntityRenderDistance = 16.0;
		c.blockEntityLodEnabled = true;
		c.blockEntityLodDistance = 10.0;

		c.shadowCullingEnabled = true;
		c.maxShadowDistance = 8.0;
		c.nameTagCullEnabled = true;
		c.maxNameTagDistance = 16.0;
		c.glowOutlineCullingEnabled = true;
		c.maxGlowOutlineDistance = 16.0;
		c.beaconBeamCullingEnabled = true;
		c.maxBeaconBeamDistance = 28.0;

		c.soundDistanceCullingEnabled = true;
		c.maxSoundDistance = 20.0;
		c.weatherSoundReductionEnabled = true;
		c.weatherSoundKeepChance = 0.15;
		c.soundBurstLimitEnabled = true;
		c.maxNewSoundsPerTick = 12;

		c.fogScaleEnabled = false;
		c.fogScaleFactor = 0.70;
		c.toastLimitEnabled = true;

		c.circularRenderingEnabled = false;
		c.worldRenderShape = WorldRenderShape.OFF;
		c.circularRadiusScale = 0.85;
		c.circularVerticalRangeEnabled = false;
		c.circularVerticalRange = 12;
		c.alwaysKeepChunks = 3;

		c.progressiveLodEnabled = true;
		c.progressiveLodStart = 0.40;
		c.progressiveLodMinQuality = 0.12;
		c.blockTextureLodEnabled = true;
		c.blockTextureLodBias = 1.75;
		c.blockTextureLodAdaptive = true;
		c.textureAnimThrottleEnabled = true;
		c.textureAnimInterval = 2;
		c.textureAnimUseAdaptive = true;
		c.textureAnimMaxInterval = 6;

		c.pathfindingThrottleEnabled = true;
		c.pathfindingFullDistance = 24.0;
		c.pathfindingMaxInterval = 10;
		c.idleAiThrottleEnabled = true;
		c.idleAiFullDistance = 28.0;
		c.idleAiMaxInterval = 12;
		c.fastWorldLoadEnabled = false;
		c.fastWorldLoadWindowSeconds = 8;
		c.fastWorldLoadChunkBoost = 4;

		c.lightmapCacheEnabled = true;
		c.unfocusedFpsCapEnabled = true;
		c.unfocusedFpsCap = 20;
		c.cloudLodEnabled = true;
		c.weatherRendererLodEnabled = true;
		c.mapRendererThrottleEnabled = true;
		c.mapRendererInterval = 6;
		c.skyExtrasThrottleEnabled = true;
		c.ambientTickCullEnabled = true;
		c.ambientTickInterval = 3;
		c.ambientTickRange = 10;
		c.worldBorderLodEnabled = true;
		c.skipEmptyBossOverlayEnabled = true;
		c.renderScaleEnabled = true;
		c.renderScale = 0.75;
		c.renderScaleFilter = ScaleFilter.LINEAR;
		c.renderScaleAdaptive = false;
	}

	/** Default mid-range laptop / desktop. */
	private static void balanced(HSNConfig c) {
		c.performanceModeEnabled = false;
		c.adaptiveCullingEnabled = false;
		c.targetFps = 60;
		c.minAdaptiveScale = 0.50;
		c.weakGpuAutoEnabled = true;
		c.weakGpuFpsThreshold = 30;
		c.lowEndHardwareTuneEnabled = true;
		c.laptopPowerSaveEnabled = true;
		c.adaptiveUploadBudgetEnabled = true;
		c.uploadBudgetFraction = 0.12;
		c.sectionOccupancyCullingEnabled = false;

		c.particleCullingEnabled = true;
		c.maxParticles = 400;
		c.maxParticleDistance = 16.0;
		c.rainKeepChance = 0.15;
		c.smokeKeepChance = 0.25;
		c.explosionKeepChance = 1.0;
		c.fireSmokeKeepChance = 1.0;
		c.bubbleKeepChance = 1.0;
		c.particlePriorityEnabled = true;
		c.highPriorityKeepChance = 0.85;
		c.lowPriorityKeepChance = 0.25;
		c.particleQualityCurveEnabled = true;
		c.hardParticleCapEnabled = true;
		c.fireworkParticleCapEnabled = true;
		c.maxFireworkParticlesPerTick = 48;
		c.dripParticleThrottleEnabled = true;

		c.entityCullingEnabled = true;
		c.maxEntityRenderDistance = 32.0;
		c.maxItemEntityRenderDistance = 20.0;
		c.maxXpOrbRenderDistance = 16.0;
		c.maxDecorationEntityDistance = 16.0;
		c.entityLodStagesEnabled = true;
		c.entityInterpSkipEnabled = true;
		c.entityInterpSkipDistance = 48.0;
		c.distantClientTickSkipEnabled = true;
		c.distantClientTickDistance = 40.0;
		c.distantClientTickInterval = 4;
		c.livingAnimThrottleEnabled = true;
		c.livingAnimThrottleDistance = 36.0;
		c.itemSpinThrottleEnabled = true;
		c.itemSpinThrottleDistance = 12.0;
		c.itemThrottleEnabled = false;
		c.itemThrottleStartDistance = 24.0;
		c.itemThrottleMaxInterval = 8;

		c.blockEntityCullingEnabled = true;
		c.maxBlockEntityRenderDistance = 24.0;
		c.blockEntityLodEnabled = true;
		c.blockEntityLodDistance = 14.0;

		c.shadowCullingEnabled = true;
		c.maxShadowDistance = 12.0;
		c.nameTagCullEnabled = true;
		c.maxNameTagDistance = 24.0;
		c.glowOutlineCullingEnabled = true;
		c.maxGlowOutlineDistance = 28.0;
		c.beaconBeamCullingEnabled = true;
		c.maxBeaconBeamDistance = 48.0;

		c.soundDistanceCullingEnabled = true;
		c.maxSoundDistance = 48.0;
		c.weatherSoundReductionEnabled = false;
		c.weatherSoundKeepChance = 0.20;
		c.soundBurstLimitEnabled = false;
		c.maxNewSoundsPerTick = 24;

		c.fogScaleEnabled = false;
		c.fogScaleFactor = 0.85;
		c.toastLimitEnabled = true;

		c.circularRenderingEnabled = false;
		c.worldRenderShape = WorldRenderShape.OFF;
		c.circularRadiusScale = 1.0;
		c.circularVerticalRangeEnabled = false;
		c.circularVerticalRange = 16;
		c.alwaysKeepChunks = 3;

		c.progressiveLodEnabled = true;
		c.progressiveLodStart = 0.50;
		c.progressiveLodMinQuality = 0.15;
		c.blockTextureLodEnabled = true;
		c.blockTextureLodBias = 1.25;
		c.blockTextureLodAdaptive = true;
		c.textureAnimThrottleEnabled = true;
		c.textureAnimInterval = 1;
		c.textureAnimUseAdaptive = true;
		c.textureAnimMaxInterval = 4;

		c.pathfindingThrottleEnabled = true;
		c.pathfindingFullDistance = 32.0;
		c.pathfindingMaxInterval = 8;
		c.idleAiThrottleEnabled = true;
		c.idleAiFullDistance = 48.0;
		c.idleAiMaxInterval = 10;
		c.fastWorldLoadEnabled = false;
		c.fastWorldLoadWindowSeconds = 8;
		c.fastWorldLoadChunkBoost = 6;

		c.lightmapCacheEnabled = true;
		c.unfocusedFpsCapEnabled = true;
		c.unfocusedFpsCap = 30;
		c.cloudLodEnabled = true;
		c.weatherRendererLodEnabled = true;
		c.mapRendererThrottleEnabled = true;
		c.mapRendererInterval = 4;
		c.skyExtrasThrottleEnabled = true;
		c.ambientTickCullEnabled = true;
		c.ambientTickInterval = 2;
		c.ambientTickRange = 12;
		c.worldBorderLodEnabled = true;
		c.skipEmptyBossOverlayEnabled = true;
	}

	/** Near-vanilla look. Distances long, almost every cut off. */
	private static void quality(HSNConfig c) {
		c.framePacingFixEnabled = false;
		c.performanceModeEnabled = false;
		c.adaptiveCullingEnabled = false;
		c.targetFps = 30;
		c.minAdaptiveScale = 1.0;
		c.weakGpuAutoEnabled = false;
		c.weakGpuFpsThreshold = 20;
		c.lowEndHardwareTuneEnabled = false;
		c.laptopPowerSaveEnabled = false;
		c.adaptiveUploadBudgetEnabled = false;
		c.uploadBudgetFraction = 0.05;
		c.sectionOccupancyCullingEnabled = false;

		c.particleCullingEnabled = false;
		c.maxParticles = 2000;
		c.maxParticleDistance = 64.0;
		c.rainKeepChance = 1.0;
		c.smokeKeepChance = 1.0;
		c.explosionKeepChance = 1.0;
		c.fireSmokeKeepChance = 1.0;
		c.bubbleKeepChance = 1.0;
		c.particlePriorityEnabled = false;
		c.highPriorityKeepChance = 1.0;
		c.lowPriorityKeepChance = 1.0;
		c.particleQualityCurveEnabled = false;
		c.hardParticleCapEnabled = false;
		c.fireworkParticleCapEnabled = false;
		c.maxFireworkParticlesPerTick = 200;
		c.dripParticleThrottleEnabled = false;

		c.entityCullingEnabled = false;
		c.maxEntityRenderDistance = 128.0;
		c.maxItemEntityRenderDistance = 96.0;
		c.maxXpOrbRenderDistance = 64.0;
		c.maxDecorationEntityDistance = 96.0;
		c.entityLodStagesEnabled = false;
		c.entityInterpSkipEnabled = false;
		c.entityInterpSkipDistance = 128.0;
		c.distantClientTickSkipEnabled = false;
		c.distantClientTickDistance = 96.0;
		c.distantClientTickInterval = 2;
		c.livingAnimThrottleEnabled = false;
		c.livingAnimThrottleDistance = 128.0;
		c.itemSpinThrottleEnabled = false;
		c.itemSpinThrottleDistance = 48.0;
		c.itemThrottleEnabled = false;
		c.itemThrottleStartDistance = 64.0;
		c.itemThrottleMaxInterval = 2;

		c.blockEntityCullingEnabled = false;
		c.maxBlockEntityRenderDistance = 96.0;
		c.blockEntityLodEnabled = false;
		c.blockEntityLodDistance = 48.0;

		c.shadowCullingEnabled = false;
		c.maxShadowDistance = 64.0;
		c.nameTagCullEnabled = false;
		c.maxNameTagDistance = 128.0;
		c.glowOutlineCullingEnabled = false;
		c.maxGlowOutlineDistance = 64.0;
		c.beaconBeamCullingEnabled = false;
		c.maxBeaconBeamDistance = 256.0;

		c.soundDistanceCullingEnabled = false;
		c.maxSoundDistance = 64.0;
		c.weatherSoundReductionEnabled = false;
		c.weatherSoundKeepChance = 1.0;
		c.soundBurstLimitEnabled = false;
		c.maxNewSoundsPerTick = 64;

		c.fogScaleEnabled = false;
		c.fogScaleFactor = 1.0;
		c.toastLimitEnabled = false;

		c.circularRenderingEnabled = false;
		c.worldRenderShape = WorldRenderShape.OFF;
		c.circularRadiusScale = 1.0;
		c.circularVerticalRangeEnabled = false;
		c.circularVerticalRange = 32;
		c.alwaysKeepChunks = 8;

		c.progressiveLodEnabled = false;
		c.progressiveLodStart = 0.90;
		c.progressiveLodMinQuality = 0.80;
		c.blockTextureLodEnabled = false;
		c.blockTextureLodBias = 0.0;
		c.blockTextureLodAdaptive = false;
		c.textureAnimThrottleEnabled = false;
		c.textureAnimInterval = 1;
		c.textureAnimUseAdaptive = false;
		c.textureAnimMaxInterval = 1;

		c.pathfindingThrottleEnabled = false;
		c.pathfindingFullDistance = 96.0;
		c.pathfindingMaxInterval = 2;
		c.idleAiThrottleEnabled = false;
		c.idleAiFullDistance = 128.0;
		c.idleAiMaxInterval = 2;
		c.fastWorldLoadEnabled = false;
		c.fastWorldLoadWindowSeconds = 8;
		c.fastWorldLoadChunkBoost = 6;

		c.lightmapCacheEnabled = false;
		c.unfocusedFpsCapEnabled = false;
		c.unfocusedFpsCap = 60;
		c.cloudLodEnabled = false;
		c.weatherRendererLodEnabled = false;
		c.mapRendererThrottleEnabled = false;
		c.mapRendererInterval = 1;
		c.skyExtrasThrottleEnabled = false;
		c.ambientTickCullEnabled = false;
		c.ambientTickInterval = 1;
		c.ambientTickRange = 16;
		c.worldBorderLodEnabled = false;
		c.skipEmptyBossOverlayEnabled = false;
	}

	/** High refresh: keep draw distance, cut CPU waste players do not see. */
	private static void competitive(HSNConfig c) {
		c.performanceModeEnabled = false;
		c.adaptiveCullingEnabled = false;
		c.targetFps = 360;
		c.minAdaptiveScale = 0.70;
		c.weakGpuAutoEnabled = false;
		c.weakGpuFpsThreshold = 40;
		c.lowEndHardwareTuneEnabled = false;
		c.laptopPowerSaveEnabled = false;
		c.adaptiveUploadBudgetEnabled = true;
		c.uploadBudgetFraction = 0.08;
		c.sectionOccupancyCullingEnabled = false;

		c.particleCullingEnabled = true;
		c.maxParticles = 600;
		c.maxParticleDistance = 28.0;
		c.rainKeepChance = 0.20;
		c.smokeKeepChance = 0.35;
		c.explosionKeepChance = 1.0;
		c.fireSmokeKeepChance = 1.0;
		c.bubbleKeepChance = 0.70;
		c.particlePriorityEnabled = true;
		c.highPriorityKeepChance = 1.0;
		c.lowPriorityKeepChance = 0.30;
		c.particleQualityCurveEnabled = true;
		c.hardParticleCapEnabled = true;
		c.fireworkParticleCapEnabled = true;
		c.maxFireworkParticlesPerTick = 40;
		c.dripParticleThrottleEnabled = true;

		c.entityCullingEnabled = true;
		c.maxEntityRenderDistance = 80.0;
		c.maxItemEntityRenderDistance = 28.0;
		c.maxXpOrbRenderDistance = 20.0;
		c.maxDecorationEntityDistance = 24.0;
		c.entityLodStagesEnabled = false;
		c.entityInterpSkipEnabled = true;
		c.entityInterpSkipDistance = 56.0;
		c.distantClientTickSkipEnabled = true;
		c.distantClientTickDistance = 48.0;
		c.distantClientTickInterval = 3;
		c.livingAnimThrottleEnabled = true;
		c.livingAnimThrottleDistance = 40.0;
		c.itemSpinThrottleEnabled = true;
		c.itemSpinThrottleDistance = 16.0;
		c.itemThrottleEnabled = false;
		c.itemThrottleStartDistance = 32.0;
		c.itemThrottleMaxInterval = 6;

		c.blockEntityCullingEnabled = true;
		c.maxBlockEntityRenderDistance = 40.0;
		c.blockEntityLodEnabled = true;
		c.blockEntityLodDistance = 24.0;

		c.shadowCullingEnabled = true;
		c.maxShadowDistance = 16.0;
		c.nameTagCullEnabled = true;
		c.maxNameTagDistance = 40.0;
		c.glowOutlineCullingEnabled = true;
		c.maxGlowOutlineDistance = 40.0;
		c.beaconBeamCullingEnabled = true;
		c.maxBeaconBeamDistance = 64.0;

		c.soundDistanceCullingEnabled = true;
		c.maxSoundDistance = 40.0;
		c.weatherSoundReductionEnabled = true;
		c.weatherSoundKeepChance = 0.25;
		c.soundBurstLimitEnabled = true;
		c.maxNewSoundsPerTick = 20;

		c.fogScaleEnabled = false;
		c.fogScaleFactor = 1.0;
		c.toastLimitEnabled = true;

		c.circularRenderingEnabled = false;
		c.worldRenderShape = WorldRenderShape.OFF;
		c.circularRadiusScale = 1.0;
		c.circularVerticalRangeEnabled = false;
		c.circularVerticalRange = 24;
		c.alwaysKeepChunks = 4;

		c.progressiveLodEnabled = false;
		c.progressiveLodStart = 0.70;
		c.progressiveLodMinQuality = 0.35;
		c.blockTextureLodEnabled = false;
		c.blockTextureLodBias = 0.50;
		c.blockTextureLodAdaptive = false;
		c.textureAnimThrottleEnabled = true;
		c.textureAnimInterval = 1;
		c.textureAnimUseAdaptive = true;
		c.textureAnimMaxInterval = 3;

		c.pathfindingThrottleEnabled = true;
		c.pathfindingFullDistance = 48.0;
		c.pathfindingMaxInterval = 6;
		c.idleAiThrottleEnabled = true;
		c.idleAiFullDistance = 48.0;
		c.idleAiMaxInterval = 8;
		c.fastWorldLoadEnabled = false;
		c.fastWorldLoadWindowSeconds = 6;
		c.fastWorldLoadChunkBoost = 8;

		c.lightmapCacheEnabled = true;
		c.unfocusedFpsCapEnabled = true;
		c.unfocusedFpsCap = 30;
		c.cloudLodEnabled = true;
		c.weatherRendererLodEnabled = true;
		c.mapRendererThrottleEnabled = true;
		c.mapRendererInterval = 3;
		c.skyExtrasThrottleEnabled = true;
		c.ambientTickCullEnabled = true;
		c.ambientTickInterval = 2;
		c.ambientTickRange = 10;
		c.worldBorderLodEnabled = true;
		c.skipEmptyBossOverlayEnabled = true;
	}
}
