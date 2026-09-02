package hsn.modod.client.config;

import hsn.modod.client.compat.HSNModCompat;
import hsn.modod.config.HSNConfig;
import hsn.modod.config.HSNConfig.Preset;
import hsn.modod.config.HSNPresets;
import hsn.modod.config.SimdMode;
import hsn.modod.config.WorldRenderShape;
import hsn.modod.optimize.NativeBridge;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.LabelOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public final class HSNConfigScreen {

	private HSNConfigScreen() {
	}

	public static Screen create(Screen parent) {
		HSNConfig cfg = HSNConfig.get();
		return YetAnotherConfigLib.createBuilder()
				.title(Component.literal("HSN Optimizations").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
				.category(general(cfg))
				.category(culling(cfg))
				.category(rendering(cfg))
				.category(performance(cfg))
				.category(advanced(cfg))
				.save(cfg::save)
				.build()
				.generateScreen(parent);
	}

	private static ConfigCategory general(HSNConfig cfg) {
		return ConfigCategory.createBuilder()
				.name(Component.literal("General").withStyle(ChatFormatting.GOLD))
				.tooltip(Component.literal("Profiles and master switches. Most players only need this page."))
				.option(label("HSN-Optimizations " + HSNConfig.modVersionLabel,
						"Complements Sodium. Does not replace it."))
				.option(Option.<Preset>createBuilder()
						.name(Component.literal("Profile"))
						.description(desc(
								"Applies a complete set of distances and feature flags.",
								"Ultra Low — weakest integrated GPUs.",
								"Safe — conservative low-end preset.",
								"Balanced — default if you are unsure.",
								"Quality — near-vanilla rendering."))
						.binding(Preset.BALANCED, () -> cfg.lastAppliedPreset, v -> {
							cfg.lastAppliedPreset = v;
							HSNPresets.apply(cfg, v);
						})
						.controller(opt -> EnumControllerBuilder.create(opt).enumClass(Preset.class))
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Master switches").withStyle(ChatFormatting.AQUA))
						.option(toggle("Performance Mode", () -> cfg.performanceModeEnabled, v -> cfg.performanceModeEnabled = v, false,
								"Enables the extra-low quality layer until disabled.", "Toggle in-game with F6."))
						.option(toggle("Particle Culling", () -> cfg.particleCullingEnabled, v -> cfg.particleCullingEnabled = v, true,
								"Limits particle count and hides far particles.", "Recommended: on."))
						.option(toggle("Entity Culling", () -> cfg.entityCullingEnabled, v -> cfg.entityCullingEnabled = v, true,
								"Stops rendering entities beyond the configured distance.", "Recommended: on."))
						.option(toggle("Block-Entity Culling", () -> cfg.blockEntityCullingEnabled, v -> cfg.blockEntityCullingEnabled = v, true,
								"Stops rendering chests, signs, and banners at range.", "Recommended: on."))
						.option(toggle("Sound Culling", () -> cfg.soundDistanceCullingEnabled, v -> cfg.soundDistanceCullingEnabled = v, true,
								"Prevents new sounds from starting beyond the sound distance.", "Recommended: on."))
						.build())
				.build();
	}

	private static ConfigCategory culling(HSNConfig cfg) {
		return ConfigCategory.createBuilder()
				.name(Component.literal("Culling").withStyle(ChatFormatting.GOLD))
				.tooltip(Component.literal("Distance limits. Lower values improve frame rate."))
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Particles").withStyle(ChatFormatting.AQUA))
						.option(slider("Particle Distance", () -> (int) cfg.maxParticleDistance, v -> cfg.maxParticleDistance = v, 16, 8, 48, "blocks",
								"Particles beyond this distance are discarded.", "Default 16 blocks."))
						.option(slider("Particle Budget", () -> cfg.maxParticles, v -> cfg.maxParticles = v, 400, 80, 1200, "",
								"Maximum particles retained per frame.", "Default 400."))
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Entities").withStyle(ChatFormatting.AQUA))
						.option(slider("Entity Distance", () -> (int) cfg.maxEntityRenderDistance, v -> cfg.maxEntityRenderDistance = v, 32, 12, 96, "blocks",
								"Living entities beyond this distance are not rendered.", "Default 32 blocks."))
						.option(slider("Item Distance", () -> (int) cfg.maxItemEntityRenderDistance, v -> cfg.maxItemEntityRenderDistance = v, 20, 6, 48, "blocks",
								"Dropped items beyond this distance are not rendered.", "Default 20 blocks."))
						.option(slider("XP Orb Distance", () -> (int) cfg.maxXpOrbRenderDistance, v -> cfg.maxXpOrbRenderDistance = v, 16, 6, 48, "blocks",
								"Experience orbs beyond this distance are not rendered.", "Default 16 blocks."))
						.option(slider("Decoration Distance", () -> (int) cfg.maxDecorationEntityDistance, v -> cfg.maxDecorationEntityDistance = v, 16, 4, 64, "blocks",
								"Armor stands and item frames.", "Default 16 blocks."))
						.option(toggle("Defer to Entity-Culling Mods", () -> cfg.deferToDedicatedEntityCullingMods, v -> cfg.deferToDedicatedEntityCullingMods = v, true,
								"Steps aside when Entity Culling or MoreCulling is present.", "Recommended: on."))
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Block entities").withStyle(ChatFormatting.AQUA))
						.option(slider("Block-Entity Distance", () -> (int) cfg.maxBlockEntityRenderDistance, v -> cfg.maxBlockEntityRenderDistance = v, 24, 8, 64, "blocks",
								"Chests, signs, furnaces, and banners beyond this distance are not rendered.", "Default 24 blocks."))
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Overlays").withStyle(ChatFormatting.AQUA))
						.collapsed(true)
						.option(toggle("Shadow Culling", () -> cfg.shadowCullingEnabled, v -> cfg.shadowCullingEnabled = v, true,
								"Skips entity shadows beyond the shadow distance.", "Recommended: on."))
						.option(slider("Shadow Distance", () -> (int) cfg.maxShadowDistance, v -> cfg.maxShadowDistance = v, 12, 2, 32, "blocks",
								"Shadows beyond this distance are not drawn.", "Default 12 blocks."))
						.option(toggle("Name-Tag Culling", () -> cfg.nameTagCullEnabled, v -> cfg.nameTagCullEnabled = v, true,
								"Hides name tags beyond the name-tag distance.", "Recommended: on."))
						.option(slider("Name-Tag Distance", () -> (int) cfg.maxNameTagDistance, v -> cfg.maxNameTagDistance = v, 24, 4, 64, "blocks",
								"Name tags beyond this distance are hidden.", "Default 24 blocks."))
						.option(toggle("Glow-Outline Culling", () -> cfg.glowOutlineCullingEnabled, v -> cfg.glowOutlineCullingEnabled = v, true,
								"Hides glowing outlines beyond the glow distance.", "Recommended: on."))
						.option(slider("Glow-Outline Distance", () -> (int) cfg.maxGlowOutlineDistance, v -> cfg.maxGlowOutlineDistance = v, 28, 4, 64, "blocks",
								"Glow outlines beyond this distance are skipped.", "Default 28 blocks."))
						.option(toggle("Beacon-Beam Culling", () -> cfg.beaconBeamCullingEnabled, v -> cfg.beaconBeamCullingEnabled = v, true,
								"Hides beacon beams beyond the beacon distance.", "Recommended: on."))
						.option(slider("Beacon-Beam Distance", () -> (int) cfg.maxBeaconBeamDistance, v -> cfg.maxBeaconBeamDistance = v, 48, 8, 128, "blocks",
								"Beacon beams beyond this distance are not drawn.", "Default 48 blocks."))
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Audio").withStyle(ChatFormatting.AQUA))
						.option(slider("Sound Distance", () -> (int) cfg.maxSoundDistance, v -> cfg.maxSoundDistance = v, 48, 12, 64, "blocks",
								"New sounds beyond this distance are not started.", "Default 48 blocks."))
						.option(toggle("Weather Sound Reduction", () -> cfg.weatherSoundReductionEnabled, v -> cfg.weatherSoundReductionEnabled = v, false,
								"Keeps only a fraction of rain and thunder loops.", "Default: off."))
						.option(percent("Weather Keep Rate", () -> cfg.weatherSoundKeepChance, v -> cfg.weatherSoundKeepChance = v, 20, 0, 100,
								"Chance to keep each weather sound when reduction is enabled.", "Default 20%."))
						.option(toggle("Sound Burst Limit", () -> cfg.soundBurstLimitEnabled, v -> cfg.soundBurstLimitEnabled = v, false,
								"Caps how many new sounds may start in a short window.", "Default: off."))
						.option(slider("Maximum New Sounds", () -> cfg.maxNewSoundsPerTick, v -> cfg.maxNewSoundsPerTick = v, 24, 1, 64, "",
								"Burst cap when the limiter is enabled.", "Default 24."))
						.build())
				.build();
	}

	private static ConfigCategory rendering(HSNConfig cfg) {
		return ConfigCategory.createBuilder()
				.name(Component.literal("Rendering").withStyle(ChatFormatting.GOLD))
				.tooltip(Component.literal("LOD, textures, terrain mask, and fog."))
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Level of detail").withStyle(ChatFormatting.AQUA))
						.option(toggle("Progressive LOD", () -> cfg.progressiveLodEnabled, v -> cfg.progressiveLodEnabled = v, true,
								"Reduces quality as objects approach their maximum distance.", "Recommended: on."))
						.option(percent("LOD Start", () -> cfg.progressiveLodStart, v -> cfg.progressiveLodStart = v, 50, 20, 90,
								"Full quality until this percentage of max distance.", "Default 50%."))
						.option(percent("Minimum LOD Quality", () -> cfg.progressiveLodMinQuality, v -> cfg.progressiveLodMinQuality = v, 15, 5, 50,
								"Quality at the far edge, immediately before culling.", "Default 15%."))
						.option(toggle("Entity LOD Stages", () -> cfg.entityLodStagesEnabled, v -> cfg.entityLodStagesEnabled = v, true,
								"Far low-priority entities use a slightly shorter draw distance.",
								"Players, vehicles, and damaged mobs are excluded."))
						.option(toggle("Block-Entity LOD", () -> cfg.blockEntityLodEnabled, v -> cfg.blockEntityLodEnabled = v, true,
								"Distant chests and signs use a cheaper render pass.", "Recommended: on."))
						.option(slider("Block-Entity LOD Distance", () -> (int) cfg.blockEntityLodDistance, v -> cfg.blockEntityLodDistance = v, 14, 4, 48, "blocks",
								"Distance at which the cheaper pass begins.", "Default 14 blocks."))
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Textures").withStyle(ChatFormatting.AQUA))
						.option(toggle("Block Texture LOD", () -> cfg.blockTextureLodEnabled, v -> cfg.blockTextureLodEnabled = v, true,
								"Applies a mip bias to far terrain. Requires mipmaps in Video Settings.", "Recommended: on."))
						.option(slider("Texture LOD Bias", () -> (int) Math.round(cfg.blockTextureLodBias * 100), v -> cfg.blockTextureLodBias = v / 100.0, 125, 0, 300, "",
								"0 is vanilla sharpness. Higher values soften the horizon.", "Default 125."))
						.option(toggle("Adaptive Texture LOD", () -> cfg.blockTextureLodAdaptive, v -> cfg.blockTextureLodAdaptive = v, true,
								"Increases blur when frame rate is low.", "Recommended: on."))
						.option(toggle("Animated Texture Throttle", () -> cfg.textureAnimThrottleEnabled, v -> cfg.textureAnimThrottleEnabled = v, true,
								"Updates water, lava, portal, and fire atlases less often under load.", "Recommended: on."))
						.option(toggle("Adaptive Texture Interval", () -> cfg.textureAnimUseAdaptive, v -> cfg.textureAnimUseAdaptive = v, true,
								"Texture update rate follows live frame rate.", "Recommended: on."))
						.option(slider("Texture Interval", () -> cfg.textureAnimInterval, v -> cfg.textureAnimInterval = v, 1, 1, 8, "ticks",
								"Atlas update interval at full frame rate.", "Default 1 tick."))
						.option(slider("Texture Interval (Load)", () -> cfg.textureAnimMaxInterval, v -> cfg.textureAnimMaxInterval = v, 4, 1, 12, "ticks",
								"Slowest atlas update interval when frame rate is low.", "Default 4 ticks."))
						.option(toggle("Item Spin Throttle", () -> cfg.itemSpinThrottleEnabled, v -> cfg.itemSpinThrottleEnabled = v, true,
								"Freezes dropped-item rotation while Performance Mode is active.", "Recommended: on."))
						.option(slider("Item Spin Distance", () -> (int) cfg.itemSpinThrottleDistance, v -> cfg.itemSpinThrottleDistance = v, 12, 2, 48, "blocks",
								"Item spin freeze applies beyond this distance under load.", "Default 12 blocks."))
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Terrain mask").withStyle(ChatFormatting.AQUA))
						.collapsed(true)
						.option(toggle("Circular Terrain Mask",
								() -> cfg.circularRenderingEnabled && !HSNModCompat.shapeModPresent(),
								v -> {
									if (!HSNModCompat.shapeModPresent()) {
										cfg.circularRenderingEnabled = v;
									}
								}, false,
								"Hides far section corners. Ground under you is always drawn.", "Default: off."))
						.option(Option.<WorldRenderShape>createBuilder()
								.name(Component.literal("Mask Shape"))
								.description(desc("Square is vanilla. Circle and hexagon trim corners.",
										"Front half keeps terrain ahead of the camera."))
								.binding(WorldRenderShape.OFF, () -> cfg.worldRenderShape, v -> {
									if (!HSNModCompat.shapeModPresent()) {
										cfg.worldRenderShape = v;
									}
								})
								.controller(opt -> EnumControllerBuilder.create(opt).enumClass(WorldRenderShape.class))
								.build())
						.option(percent("Mask Coverage", () -> cfg.circularRadiusScale, v -> {
									if (!HSNModCompat.shapeModPresent()) {
										cfg.circularRadiusScale = v;
									}
								}, 100, 25, 100,
								"Portion of view distance covered by the mask.", "Default 100%."))
						.option(toggle("Vertical Range Limit", () -> cfg.circularVerticalRangeEnabled, v -> cfg.circularVerticalRangeEnabled = v, false,
								"Also clips the mask on the Y axis.", "Default: off."))
						.option(slider("Vertical Range", () -> cfg.circularVerticalRange, v -> cfg.circularVerticalRange = v, 16, 4, 64, "blocks",
								"How far above and below the camera the mask still draws.", "Default 16 blocks."))
						.option(slider("Always-Keep Chunks", () -> cfg.alwaysKeepChunks, v -> cfg.alwaysKeepChunks = v, 3, 1, 8, "chunks",
								"Chunk radius around the camera that is never masked.", "Default 3 chunks."))
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Fog").withStyle(ChatFormatting.AQUA))
						.collapsed(true)
						.option(toggle("Fog Scale", () -> cfg.fogScaleEnabled, v -> cfg.fogScaleEnabled = v, false,
								"Shortens vanilla fog distance.", "Default: off. May conflict with shader packs."))
						.option(percent("Fog Scale Factor", () -> cfg.fogScaleFactor, v -> cfg.fogScaleFactor = v, 85, 35, 100,
								"Lower values pull fog closer.", "Used only when Fog Scale is enabled."))
						.build())
				.build();
	}

	private static ConfigCategory performance(HSNConfig cfg) {
		return ConfigCategory.createBuilder()
				.name(Component.literal("Performance").withStyle(ChatFormatting.GOLD))
				.tooltip(Component.literal("Adaptive scaling and platform workarounds."))
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Adaptive scaling").withStyle(ChatFormatting.AQUA))
						.option(toggle("Adaptive Culling", () -> cfg.adaptiveCullingEnabled, v -> cfg.adaptiveCullingEnabled = v, true,
								"Shortens distances when frame rate falls below the target.", "Recommended: on."))
						.option(slider("Target Frame Rate", () -> cfg.targetFps, v -> cfg.targetFps = v, 60, 20, 120, "FPS",
								"Adaptive culling aims for this frame rate.", "Default 60 FPS."))
						.option(percent("Minimum Distance Scale", () -> cfg.minAdaptiveScale, v -> cfg.minAdaptiveScale = v, 50, 25, 100,
								"Lower bound for how far distances may shrink under load.", "Default 50%."))
						.option(toggle("Weak-GPU Auto", () -> cfg.weakGpuAutoEnabled, v -> cfg.weakGpuAutoEnabled = v, true,
								"Applies an extra-low layer when smoothed FPS stays under the threshold.",
								"Recommended: on for integrated GPUs."))
						.option(slider("Weak-GPU Threshold", () -> cfg.weakGpuFpsThreshold, v -> cfg.weakGpuFpsThreshold = v, 35, 15, 60, "FPS",
								"Smoothed FPS at which the weak-GPU layer engages.", "Default 35 FPS."))
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("SIMD / native").withStyle(ChatFormatting.AQUA))
						.option(label("CPU: " + cpuSummary(),
								"Requested mode is capped to what this machine actually has."))
						.option(toggle("Native Hotpath", () -> cfg.nativeHotpathEnabled, v -> cfg.nativeHotpathEnabled = v, true,
								"Uses the optional native library for batch distance tests.",
								"Turn off to stay on Java scalar. Safe on every CPU."))
						.option(Option.<SimdMode>createBuilder()
								.name(Component.literal("SIMD Mode"))
								.description(desc(
										"Which vector kernel to prefer.",
										"Auto — AVX-512, then AVX2, then scalar.",
										"AVX-512 / AVX2 — used only if the CPU supports them.",
										"Off / scalar — never uses vector instructions.",
										"Unsupported choices fall back automatically."))
								.binding(SimdMode.AUTO, () -> cfg.simdMode, v -> cfg.simdMode = v)
								.controller(opt -> EnumControllerBuilder.create(opt).enumClass(SimdMode.class))
								.build())
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Platform").withStyle(ChatFormatting.AQUA))
						.option(toggle("Frame-Pacing Workaround", () -> cfg.framePacingFixEnabled, v -> cfg.framePacingFixEnabled = v, false,
								"Skips immutable GL buffer storage. Intended for older Intel HD GPUs.",
								"Default: off. Restart after changing."))
						.build())
				.build();
	}

	private static ConfigCategory advanced(HSNConfig cfg) {
		return ConfigCategory.createBuilder()
				.name(Component.literal("Advanced").withStyle(ChatFormatting.GOLD))
				.tooltip(Component.literal("Keep-rates, interface, and simulation extras."))
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Particle keep-rates").withStyle(ChatFormatting.AQUA))
						.collapsed(true)
						.option(toggle("Particle Quality Curve", () -> cfg.particleQualityCurveEnabled, v -> cfg.particleQualityCurveEnabled = v, true,
								"Retains fewer particles as they approach the maximum distance.", "Recommended: on."))
						.option(toggle("Particle Priority", () -> cfg.particlePriorityEnabled, v -> cfg.particlePriorityEnabled = v, true,
								"Prefers combat and player effects when over budget.", "Recommended: on."))
						.option(percent("Rain Keep Rate", () -> cfg.rainKeepChance, v -> cfg.rainKeepChance = v, 15, 0, 100,
								"Chance to keep each rain splash.", "Default 15%."))
						.option(percent("Smoke Keep Rate", () -> cfg.smokeKeepChance, v -> cfg.smokeKeepChance = v, 25, 0, 100,
								"Campfire and furnace smoke.", "Default 25%."))
						.option(percent("Explosion Keep Rate", () -> cfg.explosionKeepChance, v -> cfg.explosionKeepChance = v, 100, 0, 100,
								"Explosion bursts.", "Default 100%."))
						.option(percent("Fire Keep Rate", () -> cfg.fireSmokeKeepChance, v -> cfg.fireSmokeKeepChance = v, 100, 0, 100,
								"Fire and lava sparks.", "Default 100%."))
						.option(percent("Bubble Keep Rate", () -> cfg.bubbleKeepChance, v -> cfg.bubbleKeepChance = v, 100, 0, 100,
								"Underwater bubbles.", "Default 100%."))
						.option(percent("High-Priority Keep Rate", () -> cfg.highPriorityKeepChance, v -> cfg.highPriorityKeepChance = v, 85, 10, 100,
								"Keep rate for combat particles.", "Default 85%."))
						.option(percent("Low-Priority Keep Rate", () -> cfg.lowPriorityKeepChance, v -> cfg.lowPriorityKeepChance = v, 25, 0, 100,
								"Keep rate for decoration particles.", "Default 25%."))
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Interface").withStyle(ChatFormatting.AQUA))
						.option(toggle("F3 Status", () -> cfg.f3ShowStatus, v -> cfg.f3ShowStatus = v, true,
								"Adds a colored [HSN] block to the F3 right column.", "Display only."))
						.option(toggle("F3 Details", () -> cfg.f3ShowDetails, v -> cfg.f3ShowDetails = v, true,
								"Includes distances and feature flags on F3.", "Display only."))
						.option(toggle("FPS Overlay", () -> cfg.fpsOverlayEnabled, v -> cfg.fpsOverlayEnabled = v, false,
								"Draws a compact FPS overlay. Also bound to F7.", "Default: off."))
						.option(slider("Overlay X", () -> cfg.fpsOverlayX, v -> cfg.fpsOverlayX = v, 4, 0, 400, "px",
								"Horizontal offset from the left edge.", "Default 4."))
						.option(slider("Overlay Y", () -> cfg.fpsOverlayY, v -> cfg.fpsOverlayY = v, 4, 0, 400, "px",
								"Vertical offset from the top edge.", "Default 4."))
						.option(toggle("Toast Limit", () -> cfg.toastLimitEnabled, v -> cfg.toastLimitEnabled = v, true,
								"Restricts toast notifications to a few every two seconds.", "Recommended: on."))
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Simulation").withStyle(ChatFormatting.AQUA))
						.collapsed(true)
						.option(toggle("Integrated Server Only", () -> cfg.integratedServerOnly, v -> cfg.integratedServerOnly = v, true,
								"Pathfinding and item extras stay disabled on dedicated servers.", "Recommended: on."))
						.option(toggle("Defer Pathfinding to Lithium", () -> cfg.deferPathfindingToLithium, v -> cfg.deferPathfindingToLithium = v, true,
								"Skips HSN path throttling when Lithium is loaded.", "Recommended: on."))
						.option(toggle("Pathfinding Throttle", () -> cfg.pathfindingThrottleEnabled, v -> cfg.pathfindingThrottleEnabled = v, true,
								"Distant idle mobs reuse the current path. Combat range is unchanged.",
								"Recommended: on in singleplayer."))
						.option(slider("Pathfinding Full-Rate Distance", () -> (int) cfg.pathfindingFullDistance, v -> cfg.pathfindingFullDistance = v, 32, 8, 96, "blocks",
								"Inside this range pathfinding is unmodified.", "Default 32 blocks."))
						.option(slider("Pathfinding Maximum Interval", () -> cfg.pathfindingMaxInterval, v -> cfg.pathfindingMaxInterval = v, 8, 2, 20, "ticks",
								"Maximum delay between path rebuilds for distant mobs.", "Default 8 ticks."))
						.option(toggle("Locate Cache", () -> cfg.locateOptimizeEnabled, v -> cfg.locateOptimizeEnabled = v, true,
								"Reuses /locate results for a short period.", "Recommended: on."))
						.option(slider("Locate Cache TTL", () -> cfg.locateCacheTtlSeconds, v -> cfg.locateCacheTtlSeconds = v, 30, 5, 120, "s",
								"How long a /locate result is reused.", "Default 30 seconds."))
						.option(toggle("Accelerated World Load", () -> cfg.fastWorldLoadEnabled, v -> cfg.fastWorldLoadEnabled = v, false,
								"Drains extra chunk tasks for a short window after the world opens.", "Default: off."))
						.option(slider("World-Load Window", () -> cfg.fastWorldLoadWindowSeconds, v -> cfg.fastWorldLoadWindowSeconds = v, 8, 1, 20, "s",
								"Duration of the extra drain after server start.", "Default 8 seconds."))
						.option(slider("World-Load Chunk Boost", () -> cfg.fastWorldLoadChunkBoost, v -> cfg.fastWorldLoadChunkBoost = v, 6, 1, 16, "",
								"Extra chunk tasks drained per tick during the window.", "Default 6."))
						.option(toggle("Item / XP Tick Throttle", () -> cfg.itemThrottleEnabled, v -> cfg.itemThrottleEnabled = v, false,
								"Reduces physics ticks on distant dropped items.", "Default: off."))
						.option(slider("Item Throttle Distance", () -> (int) cfg.itemThrottleStartDistance, v -> cfg.itemThrottleStartDistance = v, 24, 8, 96, "blocks",
								"Distance at which item tick throttling begins.", "Default 24 blocks."))
						.option(slider("Item Tick Interval", () -> cfg.itemThrottleMaxInterval, v -> cfg.itemThrottleMaxInterval = v, 8, 2, 20, "ticks",
								"Physics interval for distant items when throttling is enabled.", "Default 8 ticks."))
						.build())
				.build();
	}

	private static Option<Component> label(String title, String body) {
		return LabelOption.createBuilder()
				.line(Component.literal(title).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
				.line(Component.literal(body).withStyle(ChatFormatting.GRAY))
				.build();
	}

	private static Option<Boolean> toggle(String name, java.util.function.Supplier<Boolean> get, Consumer<Boolean> set,
			boolean def, String what, String advice) {
		return Option.<Boolean>createBuilder()
				.name(Component.literal(name))
				.description(desc(what, advice))
				.binding(def, get, set)
				.controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
				.build();
	}

	private static Option<Integer> slider(String name, java.util.function.Supplier<Integer> get, Consumer<Integer> set,
			int def, int min, int max, String unit, String what, String advice) {
		return Option.<Integer>createBuilder()
				.name(Component.literal(name))
				.description(desc(what, advice))
				.binding(def, get, set)
				.controller(opt -> IntegerSliderControllerBuilder.create(opt)
						.range(min, max)
						.step(1)
						.formatValue(v -> {
							if (unit == null || unit.isEmpty()) {
								return Component.literal(Integer.toString(v)).withStyle(ChatFormatting.AQUA);
							}
							return Component.literal(v + " " + unit).withStyle(ChatFormatting.AQUA);
						}))
				.build();
	}

	private static Option<Integer> percent(String name, java.util.function.Supplier<Double> get, Consumer<Double> set,
			int def, int min, int max, String what, String advice) {
		return slider(name, () -> (int) Math.round(get.get() * 100), v -> set.accept(v / 100.0),
				def, min, max, "%", what, advice);
	}

	private static String cpuSummary() {
		if (!NativeBridge.available()) {
			return "native library not loaded — Java scalar only";
		}
		StringBuilder sb = new StringBuilder();
		sb.append(NativeBridge.avx512() ? "AVX-512 " : "");
		sb.append(NativeBridge.avx2() ? "AVX2 " : "");
		sb.append(NativeBridge.avx() ? "AVX " : "");
		if (sb.length() == 0) {
			sb.append("no AVX ");
		}
		sb.append("| active ").append(NativeBridge.activeLabel());
		return sb.toString().trim();
	}

	private static OptionDescription desc(String... lines) {
		var text = Component.empty();
		for (int i = 0; i < lines.length; i++) {
			if (i > 0) {
				text = text.copy().append(Component.literal("\n"));
			}
			text = text.copy().append(Component.literal(lines[i])
					.withStyle(i == 0 ? ChatFormatting.GRAY : ChatFormatting.DARK_GRAY));
		}
		return OptionDescription.of(text);
	}
}
