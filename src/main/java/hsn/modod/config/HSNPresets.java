package hsn.modod.config;

/**
 * Performance presets. Each preset sets a coherent package of distances,
 * keep-rates and the new rendering helpers so users can switch with one click.
 */
public final class HSNPresets {

	private HSNPresets() {
	}

	public static void apply(HSNConfig c, HSNConfig.Preset preset) {
		switch (preset) {
			case ULTRA_LOW -> {
				// Most aggressive – aimed at very weak integrated GPUs
				c.particleCullingEnabled = true;
				c.maxParticles = 180;
				c.maxParticleDistance = 10.0;
				c.rainKeepChance = 0.06;
				c.smokeKeepChance = 0.10;
				c.explosionKeepChance = 0.35;
				c.fireSmokeKeepChance = 0.40;
				c.bubbleKeepChance = 0.30;

				c.entityCullingEnabled = true;
				c.maxEntityRenderDistance = 20.0;
				c.maxItemEntityRenderDistance = 12.0;
				c.maxXpOrbRenderDistance = 10.0;
				c.maxDecorationEntityDistance = 10.0;

				c.shadowCullingEnabled = true;
				c.maxShadowDistance = 6.0;
				c.nameTagCullEnabled = true;
				c.maxNameTagDistance = 14.0;

				c.blockEntityCullingEnabled = true;
				c.maxBlockEntityRenderDistance = 14.0;

				c.soundDistanceCullingEnabled = true;
				c.maxSoundDistance = 16.0;
				c.weatherSoundReductionEnabled = true;
				c.weatherSoundKeepChance = 0.08;
				c.soundBurstLimitEnabled = true;
				c.maxNewSoundsPerTick = 12;

				c.fogScaleEnabled = true;
				c.fogScaleFactor = 0.65;
				c.toastLimitEnabled = true;

				// Unique helpers – all on and aggressive
				c.beaconBeamCullingEnabled = true;
				c.maxBeaconBeamDistance = 24.0;
				c.glowOutlineCullingEnabled = true;
				c.maxGlowOutlineDistance = 16.0;
				c.itemSpinThrottleEnabled = true;
				c.itemSpinThrottleDistance = 8.0;
				c.textureAnimThrottleEnabled = true;
				c.textureAnimInterval = 2;
				c.textureAnimUseAdaptive = true;
				c.textureAnimMaxInterval = 5;
				c.particlePriorityEnabled = true;
				c.highPriorityKeepChance = 0.75;
				c.lowPriorityKeepChance = 0.12;
				c.weakGpuAutoEnabled = true;
				c.weakGpuFpsThreshold = 40;
				c.blockEntityLodEnabled = true;
				c.blockEntityLodDistance = 10.0;
				c.progressiveLodEnabled = true;
				c.progressiveLodStart = 0.40;
				c.progressiveLodMinQuality = 0.10;
				c.entityLodStagesEnabled = true;
				c.blockTextureLodEnabled = true;
				c.blockTextureLodBias = 2.0;
				c.blockTextureLodAdaptive = true;
				c.particleQualityCurveEnabled = true;
				c.pathfindingThrottleEnabled = true;
				c.pathfindingFullDistance = 24.0;
				c.pathfindingMaxInterval = 10;
				c.lowEndHardwareTuneEnabled = true;
				c.laptopPowerSaveEnabled = true;
				c.adaptiveUploadBudgetEnabled = true;
				c.uploadBudgetFraction = 0.20;
				c.sectionOccupancyCullingEnabled = true;
				c.lightmapCacheEnabled = true;
				c.entityInterpSkipEnabled = true;
				c.entityInterpSkipDistance = 28;
				c.distantClientTickSkipEnabled = true;
				c.distantClientTickDistance = 24;
				c.distantClientTickInterval = 6;
				c.unfocusedFpsCapEnabled = true;
				c.unfocusedFpsCap = 20;
				c.cloudLodEnabled = true;
				c.weatherRendererLodEnabled = true;
				c.livingAnimThrottleEnabled = true;
				c.livingAnimThrottleDistance = 22;
				c.mapRendererThrottleEnabled = true;
				c.mapRendererInterval = 6;
				c.skyExtrasThrottleEnabled = true;
				c.fireworkParticleCapEnabled = true;
				c.maxFireworkParticlesPerTick = 16;
				c.idleAiThrottleEnabled = true;
				c.idleAiFullDistance = 28;
				c.idleAiMaxInterval = 12;
				c.worldBorderLodEnabled = true;
				c.dripParticleThrottleEnabled = true;
				c.skipEmptyBossOverlayEnabled = true;
				c.hardParticleCapEnabled = true;
			}
			case SAFE -> {
				// Reliable low-end defaults (close to previous 3.8.4 SAFE)
				c.particleCullingEnabled = true;
				c.maxParticles = 320;
				c.maxParticleDistance = 14.0;
				c.rainKeepChance = 0.12;
				c.smokeKeepChance = 0.22;
				c.explosionKeepChance = 0.55;
				c.fireSmokeKeepChance = 0.60;
				c.bubbleKeepChance = 0.50;

				c.entityCullingEnabled = true;
				c.maxEntityRenderDistance = 26.0;
				c.maxItemEntityRenderDistance = 16.0;
				c.maxXpOrbRenderDistance = 12.0;
				c.maxDecorationEntityDistance = 14.0;

				c.shadowCullingEnabled = true;
				c.maxShadowDistance = 10.0;
				c.nameTagCullEnabled = true;
				c.maxNameTagDistance = 20.0;

				c.blockEntityCullingEnabled = true;
				c.maxBlockEntityRenderDistance = 20.0;

				c.soundDistanceCullingEnabled = true;
				c.maxSoundDistance = 22.0;
				c.weatherSoundReductionEnabled = true;
				c.weatherSoundKeepChance = 0.18;
				c.soundBurstLimitEnabled = false;

				c.fogScaleEnabled = false;
				c.toastLimitEnabled = true;

				c.beaconBeamCullingEnabled = true;
				c.maxBeaconBeamDistance = 36.0;
				c.glowOutlineCullingEnabled = true;
				c.maxGlowOutlineDistance = 22.0;
				c.itemSpinThrottleEnabled = true;
				c.itemSpinThrottleDistance = 10.0;
				c.textureAnimThrottleEnabled = true;
				c.textureAnimInterval = 1;
				c.textureAnimUseAdaptive = true;
				c.textureAnimMaxInterval = 4;
				c.particlePriorityEnabled = true;
				c.highPriorityKeepChance = 0.85;
				c.lowPriorityKeepChance = 0.22;
				c.weakGpuAutoEnabled = true;
				c.weakGpuFpsThreshold = 35;
				c.blockEntityLodEnabled = true;
				c.blockEntityLodDistance = 12.0;
				c.progressiveLodEnabled = true;
				c.progressiveLodStart = 0.45;
				c.progressiveLodMinQuality = 0.15;
				c.entityLodStagesEnabled = true;
				c.blockTextureLodEnabled = true;
				c.blockTextureLodBias = 1.5;
				c.blockTextureLodAdaptive = true;
				c.particleQualityCurveEnabled = true;
				c.pathfindingThrottleEnabled = true;
				c.pathfindingFullDistance = 28.0;
				c.pathfindingMaxInterval = 8;
				c.lowEndHardwareTuneEnabled = true;
				c.laptopPowerSaveEnabled = true;
				c.adaptiveUploadBudgetEnabled = true;
				c.uploadBudgetFraction = 0.14;
				c.sectionOccupancyCullingEnabled = true;
				c.lightmapCacheEnabled = true;
				c.entityInterpSkipEnabled = true;
				c.entityInterpSkipDistance = 36;
				c.distantClientTickSkipEnabled = true;
				c.distantClientTickDistance = 32;
				c.distantClientTickInterval = 4;
				c.unfocusedFpsCapEnabled = true;
				c.unfocusedFpsCap = 30;
				c.cloudLodEnabled = true;
				c.weatherRendererLodEnabled = true;
				c.livingAnimThrottleEnabled = true;
				c.livingAnimThrottleDistance = 28;
				c.mapRendererThrottleEnabled = true;
				c.mapRendererInterval = 4;
				c.skyExtrasThrottleEnabled = true;
				c.fireworkParticleCapEnabled = true;
				c.maxFireworkParticlesPerTick = 32;
				c.idleAiThrottleEnabled = true;
				c.idleAiFullDistance = 36;
				c.idleAiMaxInterval = 10;
				c.worldBorderLodEnabled = true;
				c.dripParticleThrottleEnabled = true;
				c.skipEmptyBossOverlayEnabled = true;
				c.hardParticleCapEnabled = true;
			}
			case BALANCED -> {
				c.particleCullingEnabled = true;
				c.maxParticles = 480;
				c.maxParticleDistance = 18.0;
				c.rainKeepChance = 0.28;
				c.smokeKeepChance = 0.38;
				c.explosionKeepChance = 0.80;
				c.fireSmokeKeepChance = 0.85;
				c.bubbleKeepChance = 0.80;

				c.entityCullingEnabled = true;
				c.maxEntityRenderDistance = 38.0;
				c.maxItemEntityRenderDistance = 22.0;
				c.maxXpOrbRenderDistance = 16.0;
				c.maxDecorationEntityDistance = 18.0;

				c.shadowCullingEnabled = true;
				c.maxShadowDistance = 14.0;
				c.nameTagCullEnabled = true;
				c.maxNameTagDistance = 28.0;

				c.blockEntityCullingEnabled = true;
				c.maxBlockEntityRenderDistance = 28.0;

				c.soundDistanceCullingEnabled = true;
				c.maxSoundDistance = 28.0;
				c.weatherSoundReductionEnabled = false;
				c.soundBurstLimitEnabled = false;

				c.fogScaleEnabled = false;
				c.toastLimitEnabled = false;

				c.beaconBeamCullingEnabled = true;
				c.maxBeaconBeamDistance = 48.0;
				c.glowOutlineCullingEnabled = true;
				c.maxGlowOutlineDistance = 28.0;
				c.itemSpinThrottleEnabled = true;
				c.itemSpinThrottleDistance = 14.0;
				c.textureAnimThrottleEnabled = true;
				c.textureAnimInterval = 1;
				c.textureAnimUseAdaptive = true;
				c.textureAnimMaxInterval = 3;
				c.particlePriorityEnabled = true;
				c.highPriorityKeepChance = 0.90;
				c.lowPriorityKeepChance = 0.35;
				c.weakGpuAutoEnabled = true;
				c.weakGpuFpsThreshold = 30;
				c.blockEntityLodEnabled = true;
				c.blockEntityLodDistance = 14.0;
				c.progressiveLodEnabled = true;
				c.progressiveLodStart = 0.55;
				c.progressiveLodMinQuality = 0.20;
				c.entityLodStagesEnabled = true;
				c.blockTextureLodEnabled = true;
				c.blockTextureLodBias = 1.25;
				c.blockTextureLodAdaptive = true;
				c.particleQualityCurveEnabled = true;
				c.pathfindingThrottleEnabled = true;
				c.pathfindingFullDistance = 32.0;
				c.pathfindingMaxInterval = 6;
				c.lowEndHardwareTuneEnabled = true;
				c.laptopPowerSaveEnabled = true;
				c.adaptiveUploadBudgetEnabled = true;
				c.uploadBudgetFraction = 0.12;
				c.sectionOccupancyCullingEnabled = true;
				c.lightmapCacheEnabled = true;
				c.entityInterpSkipEnabled = true;
				c.entityInterpSkipDistance = 48;
				c.distantClientTickSkipEnabled = true;
				c.distantClientTickDistance = 40;
				c.distantClientTickInterval = 4;
				c.unfocusedFpsCapEnabled = true;
				c.unfocusedFpsCap = 30;
				c.cloudLodEnabled = true;
				c.weatherRendererLodEnabled = true;
				c.livingAnimThrottleEnabled = true;
				c.livingAnimThrottleDistance = 36;
				c.mapRendererThrottleEnabled = true;
				c.mapRendererInterval = 4;
				c.skyExtrasThrottleEnabled = true;
				c.fireworkParticleCapEnabled = true;
				c.maxFireworkParticlesPerTick = 48;
				c.idleAiThrottleEnabled = true;
				c.idleAiFullDistance = 48;
				c.idleAiMaxInterval = 8;
				c.worldBorderLodEnabled = true;
				c.dripParticleThrottleEnabled = true;
				c.skipEmptyBossOverlayEnabled = true;
				c.hardParticleCapEnabled = true;
			}
			case QUALITY -> {
				// Almost vanilla – only the lightest helpers stay on
				c.particleCullingEnabled = false;
				c.maxParticles = 1200;
				c.maxParticleDistance = 48.0;
				c.rainKeepChance = 1.0;
				c.smokeKeepChance = 1.0;
				c.explosionKeepChance = 1.0;
				c.fireSmokeKeepChance = 1.0;
				c.bubbleKeepChance = 1.0;

				c.entityCullingEnabled = false;
				c.maxEntityRenderDistance = 64.0;
				c.maxItemEntityRenderDistance = 48.0;
				c.maxXpOrbRenderDistance = 32.0;
				c.maxDecorationEntityDistance = 48.0;

				c.shadowCullingEnabled = false;
				c.maxShadowDistance = 32.0;
				c.nameTagCullEnabled = false;
				c.maxNameTagDistance = 64.0;

				c.blockEntityCullingEnabled = false;
				c.maxBlockEntityRenderDistance = 64.0;

				c.soundDistanceCullingEnabled = false;
				c.maxSoundDistance = 64.0;
				c.weatherSoundReductionEnabled = false;
				c.soundBurstLimitEnabled = false;

				c.fogScaleEnabled = false;
				c.toastLimitEnabled = false;

				c.beaconBeamCullingEnabled = false;
				c.maxBeaconBeamDistance = 128.0;
				c.glowOutlineCullingEnabled = false;
				c.maxGlowOutlineDistance = 64.0;
				c.itemSpinThrottleEnabled = false;
				c.itemSpinThrottleDistance = 32.0;
				c.textureAnimThrottleEnabled = false;
				c.textureAnimInterval = 1;
				c.textureAnimUseAdaptive = false;
				c.textureAnimMaxInterval = 2;
				c.particlePriorityEnabled = false;
				c.highPriorityKeepChance = 1.0;
				c.lowPriorityKeepChance = 1.0;
				c.weakGpuAutoEnabled = false;
				c.weakGpuFpsThreshold = 25;
				c.blockEntityLodEnabled = false;
				c.blockEntityLodDistance = 24.0;
				c.progressiveLodEnabled = false;
				c.progressiveLodStart = 0.70;
				c.progressiveLodMinQuality = 0.35;
				c.entityLodStagesEnabled = false;
				c.blockTextureLodEnabled = false;
				c.blockTextureLodBias = 0.35;
				c.blockTextureLodAdaptive = false;
				c.particleQualityCurveEnabled = false;
				c.pathfindingThrottleEnabled = false;
				c.pathfindingFullDistance = 48.0;
				c.pathfindingMaxInterval = 4;
				c.lowEndHardwareTuneEnabled = false;
				c.laptopPowerSaveEnabled = false;
				c.adaptiveUploadBudgetEnabled = false;
				c.uploadBudgetFraction = 0.08;
				c.sectionOccupancyCullingEnabled = false;
				c.lightmapCacheEnabled = true;
				c.entityInterpSkipEnabled = true;
				c.entityInterpSkipDistance = 80;
				c.distantClientTickSkipEnabled = false;
				c.distantClientTickDistance = 64;
				c.distantClientTickInterval = 2;
				c.unfocusedFpsCapEnabled = true;
				c.unfocusedFpsCap = 60;
				c.cloudLodEnabled = false;
				c.weatherRendererLodEnabled = false;
				c.livingAnimThrottleEnabled = false;
				c.livingAnimThrottleDistance = 64;
				c.mapRendererThrottleEnabled = true;
				c.mapRendererInterval = 2;
				c.skyExtrasThrottleEnabled = false;
				c.fireworkParticleCapEnabled = false;
				c.maxFireworkParticlesPerTick = 128;
				c.idleAiThrottleEnabled = false;
				c.idleAiFullDistance = 64;
				c.idleAiMaxInterval = 4;
				c.worldBorderLodEnabled = true;
				c.dripParticleThrottleEnabled = false;
				c.skipEmptyBossOverlayEnabled = true;
				c.hardParticleCapEnabled = false;
			}
			case COMPETITIVE -> {
				c.particleCullingEnabled = true;
				c.maxParticles = 1600;
				c.maxParticleDistance = 48.0;
				c.rainKeepChance = 0.55;
				c.smokeKeepChance = 0.70;
				c.explosionKeepChance = 1.0;
				c.fireSmokeKeepChance = 1.0;
				c.bubbleKeepChance = 0.85;

				c.entityCullingEnabled = true;
				c.maxEntityRenderDistance = 96.0;
				c.maxItemEntityRenderDistance = 48.0;
				c.maxXpOrbRenderDistance = 32.0;
				c.maxDecorationEntityDistance = 48.0;

				c.shadowCullingEnabled = true;
				c.maxShadowDistance = 28.0;
				c.nameTagCullEnabled = true;
				c.maxNameTagDistance = 48.0;

				c.blockEntityCullingEnabled = true;
				c.maxBlockEntityRenderDistance = 64.0;

				c.soundDistanceCullingEnabled = true;
				c.maxSoundDistance = 64.0;
				c.weatherSoundReductionEnabled = false;
				c.soundBurstLimitEnabled = false;

				c.fogScaleEnabled = false;
				c.toastLimitEnabled = true;

				c.beaconBeamCullingEnabled = true;
				c.maxBeaconBeamDistance = 128.0;
				c.glowOutlineCullingEnabled = true;
				c.maxGlowOutlineDistance = 64.0;
				c.itemSpinThrottleEnabled = false;
				c.textureAnimThrottleEnabled = false;
				c.particlePriorityEnabled = true;
				c.highPriorityKeepChance = 1.0;
				c.lowPriorityKeepChance = 0.55;
				c.weakGpuAutoEnabled = false;
				c.blockEntityLodEnabled = false;
				c.progressiveLodEnabled = false;
				c.entityLodStagesEnabled = false;
				c.blockTextureLodEnabled = false;
				c.particleQualityCurveEnabled = true;
				c.pathfindingThrottleEnabled = true;
				c.pathfindingFullDistance = 64.0;
				c.pathfindingMaxInterval = 6;
				c.lowEndHardwareTuneEnabled = false;
				c.laptopPowerSaveEnabled = false;
				c.adaptiveUploadBudgetEnabled = false;
				c.sectionOccupancyCullingEnabled = true;
				c.adaptiveCullingEnabled = false;
				c.targetFps = 360;

				c.lightmapCacheEnabled = true;
				c.entityInterpSkipEnabled = true;
				c.entityInterpSkipDistance = 64;
				c.distantClientTickSkipEnabled = true;
				c.distantClientTickDistance = 56;
				c.distantClientTickInterval = 3;
				c.unfocusedFpsCapEnabled = true;
				c.unfocusedFpsCap = 30;
				c.cloudLodEnabled = true;
				c.weatherRendererLodEnabled = false;
				c.livingAnimThrottleEnabled = true;
				c.livingAnimThrottleDistance = 48;
				c.mapRendererThrottleEnabled = true;
				c.mapRendererInterval = 3;
				c.skyExtrasThrottleEnabled = true;
				c.fireworkParticleCapEnabled = true;
				c.maxFireworkParticlesPerTick = 64;
				c.idleAiThrottleEnabled = true;
				c.idleAiFullDistance = 64;
				c.idleAiMaxInterval = 8;
				c.worldBorderLodEnabled = true;
				c.dripParticleThrottleEnabled = true;
				c.skipEmptyBossOverlayEnabled = true;
				c.hardParticleCapEnabled = true;
			}
		}
	}
}
