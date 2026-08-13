package hsn.modod.config;

/**
 * Simple performance presets.
 */
public final class HSNPresets {

	private HSNPresets() {
	}

	public static void apply(HSNConfig c, HSNConfig.Preset preset) {
		switch (preset) {
			case ULTRA_LOW -> {
				c.particleCullingEnabled = true;
				c.maxParticles = 200;
				c.maxParticleDistance = 12.0;
				c.rainKeepChance = 0.08;
				c.smokeKeepChance = 0.12;
				c.entityCullingEnabled = true;
				c.maxEntityRenderDistance = 22.0;
				c.maxItemEntityRenderDistance = 14.0;
				c.maxXpOrbRenderDistance = 12.0;
				c.maxDecorationEntityDistance = 12.0;
				c.shadowCullingEnabled = true;
				c.maxShadowDistance = 8.0;
				c.nameTagCullEnabled = true;
				c.maxNameTagDistance = 16.0;
				c.blockEntityCullingEnabled = true;
				c.maxBlockEntityRenderDistance = 16.0;
				c.soundDistanceCullingEnabled = true;
				c.maxSoundDistance = 18.0;
				c.weatherSoundReductionEnabled = true;
				c.weatherSoundKeepChance = 0.1;
				c.fogScaleEnabled = true;
				c.fogScaleFactor = 0.7;
				c.toastLimitEnabled = true;
				c.itemMergeEnabled = true;
			}
			case SAFE -> {
				c.particleCullingEnabled = true;
				c.maxParticles = 350;
				c.maxParticleDistance = 16.0;
				c.rainKeepChance = 0.15;
				c.smokeKeepChance = 0.25;
				c.entityCullingEnabled = true;
				c.maxEntityRenderDistance = 28.0;
				c.maxItemEntityRenderDistance = 18.0;
				c.maxXpOrbRenderDistance = 14.0;
				c.maxDecorationEntityDistance = 16.0;
				c.shadowCullingEnabled = true;
				c.maxShadowDistance = 12.0;
				c.nameTagCullEnabled = true;
				c.maxNameTagDistance = 22.0;
				c.blockEntityCullingEnabled = true;
				c.maxBlockEntityRenderDistance = 22.0;
				c.soundDistanceCullingEnabled = true;
				c.maxSoundDistance = 24.0;
				c.weatherSoundReductionEnabled = true;
				c.weatherSoundKeepChance = 0.2;
				c.fogScaleEnabled = false;
				c.toastLimitEnabled = true;
				c.itemMergeEnabled = true;
			}
			case BALANCED -> {
				c.particleCullingEnabled = true;
				c.maxParticles = 500;
				c.maxParticleDistance = 20.0;
				c.rainKeepChance = 0.3;
				c.smokeKeepChance = 0.4;
				c.entityCullingEnabled = true;
				c.maxEntityRenderDistance = 40.0;
				c.maxItemEntityRenderDistance = 24.0;
				c.maxXpOrbRenderDistance = 18.0;
				c.maxDecorationEntityDistance = 20.0;
				c.shadowCullingEnabled = true;
				c.maxShadowDistance = 16.0;
				c.nameTagCullEnabled = true;
				c.maxNameTagDistance = 32.0;
				c.blockEntityCullingEnabled = true;
				c.maxBlockEntityRenderDistance = 32.0;
				c.soundDistanceCullingEnabled = true;
				c.maxSoundDistance = 32.0;
				c.weatherSoundReductionEnabled = false;
				c.fogScaleEnabled = false;
				c.toastLimitEnabled = false;
				c.itemMergeEnabled = true;
			}
			case QUALITY -> {
				c.particleCullingEnabled = false;
				c.maxParticles = 1000;
				c.maxParticleDistance = 48.0;
				c.rainKeepChance = 1.0;
				c.smokeKeepChance = 1.0;
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
				c.fogScaleEnabled = false;
				c.toastLimitEnabled = false;
				c.itemMergeEnabled = true;
			}
		}
	}
}
