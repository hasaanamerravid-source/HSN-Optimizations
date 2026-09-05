package hsn.modod.client.config;

import hsn.modod.client.compat.HSNModCompat;
import hsn.modod.config.HSNConfig;
import hsn.modod.config.HSNConfig.Preset;
import hsn.modod.config.HSNPresets;
import hsn.modod.config.SimdMode;
import hsn.modod.config.WorldRenderShape;
import hsn.modod.optimize.HotPath;
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

	private static final String FPS_LOWER_HIGH =
			"Frame-rate impact: lower values draw or simulate less, which raises FPS. Higher values keep more visible and will lower FPS.";
	private static final String FPS_LOWER_LOW =
			"Frame-rate impact: higher values apply the optimization more strongly and raise FPS. Lower values keep more work running and will lower FPS.";
	private static final String FPS_ON_HIGH =
			"Frame-rate impact: enabled skips extra draw or simulation work and raises FPS. Disabled restores full quality and will lower FPS.";
	private static final String FPS_ON_LOW =
			"Frame-rate impact: enabled adds a small overlay cost and can slightly lower FPS. Disabled removes that cost.";
	private static final String FPS_NONE =
			"Frame-rate impact: this control does not change how much of the world is drawn or simulated. FPS is unaffected.";
	private static final String FPS_COMPAT =
			"Frame-rate impact: this only chooses which mod owns the feature. It does not raise or lower FPS by itself.";

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
				.category(highEnd(cfg))
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
								"Applies a complete, tested set of distances, keep-rates, and feature flags in one step. Use this when you do not want to tune every slider by hand.",
								"Ultra Low — oldest integrated GPUs and software renderers. Shortest distances, most features tightened. Highest FPS, lowest visual fidelity.",
								"Safe — conservative low-end preset. Still aggressive, but less likely to hide nearby props.",
								"Balanced — default if you are unsure. Intended for typical laptops and mid-range desktops.",
								"Quality — near-vanilla rendering. Longest distances and fewest cuts. Lowest FPS, highest fidelity.",
								"Competitive — high-refresh profile. Long draw distances, lightmap / interpolation / idle-AI CPU cuts. Aimed at 240–500+ FPS clients.",
								"Frame-rate impact: moving toward Ultra Low / Safe raises FPS. Moving toward Quality lowers FPS. Competitive keeps visuals and cuts CPU waste."))
						.binding(Preset.BALANCED, () -> cfg.lastAppliedPreset, v -> {
							cfg.lastAppliedPreset = v;
							HSNPresets.apply(cfg, v);
						})
						.controller(opt -> EnumControllerBuilder.create(opt).enumClass(Preset.class))
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Master switches").withStyle(ChatFormatting.AQUA))
						.option(toggle("HSN Enabled (kill switch)", () -> cfg.modEnabled, v -> cfg.modEnabled = v, true,
								"Master switch for the whole mod. Off = every HSN cull, throttle and high-end pass is a no-op and vanilla / Sodium render as if HSN was not installed.",
								"Bound to F6. This is the only keybind that is meant to change gameplay. F7 opens this screen."))
						.option(toggle("Performance Mode", () -> cfg.performanceModeEnabled, v -> cfg.performanceModeEnabled = v, false,
								"Turns on the extra-low quality layer immediately: shorter cull distances, cheaper textures, and tighter particle budgets until you turn it off.",
								"Use it for crowded bases or raids. It does not replace the master kill switch."))
						.option(toggle("Particle Culling", () -> cfg.particleCullingEnabled, v -> cfg.particleCullingEnabled = v, true,
								"Enforces the particle budget and maximum particle distance so rain, smoke, and splash effects cannot flood the particle manager.",
								"Recommended: on. Disable only if a pack or screenshot needs every particle."))
						.option(toggle("Entity Culling", () -> cfg.entityCullingEnabled, v -> cfg.entityCullingEnabled = v, true,
								"Stops submitting living mobs, dropped items, XP orbs, armor stands, and item frames once they pass their configured distance. Dedicated sliders for items, orbs, and decorations are honored exactly and are not reduced again by mob priority weights.",
								"Recommended: on. This is the main entity-render saving."))
						.option(toggle("Block-Entity Culling", () -> cfg.blockEntityCullingEnabled, v -> cfg.blockEntityCullingEnabled = v, true,
								"Stops rendering chests, signs, furnaces, banners, and similar tile entities once they are past the block-entity distance.",
								"Recommended: on in storage rooms and item-sorter halls."))
						.option(toggle("Sound Culling", () -> cfg.soundDistanceCullingEnabled, v -> cfg.soundDistanceCullingEnabled = v, true,
								"Prevents new sound instances from starting beyond the sound distance. Sounds already playing are not cut mid-clip.",
								"Recommended: on. Helps when many hoppers, farms, or weather loops compete for the sound engine."))
						.build())
				.build();
	}

	private static ConfigCategory culling(HSNConfig cfg) {
		return ConfigCategory.createBuilder()
				.name(Component.literal("Culling").withStyle(ChatFormatting.GOLD))
				.tooltip(Component.literal("Distance limits. Lower values improve frame rate."))
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Particles").withStyle(ChatFormatting.AQUA))
						.option(slider("Particle Distance", () -> (int) cfg.maxParticleDistance, v -> cfg.maxParticleDistance = v, 16, 4, 128, "blocks",
								"Particles spawned farther than this from the camera are discarded before they are ticked or drawn.",
								"Default 16 blocks. Keep this modest; particle fill-rate is expensive on integrated GPUs."))
						.option(slider("Particle Budget", () -> cfg.maxParticles, v -> cfg.maxParticles = v, 400, 80, 1200, "",
								"Hard cap on how many particles may remain alive in one frame after priority and distance filters.",
								"Default 400. Lower this first if explosions or rain stall the frame."))
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Entities").withStyle(ChatFormatting.AQUA))
						.option(slider("Entity Distance", () -> (int) cfg.maxEntityRenderDistance, v -> cfg.maxEntityRenderDistance = v, 32, 4, 256, "blocks",
								"Maximum draw distance for living entities (mobs and players other than you). Hostile mobs keep the full value; passive mobs may be shortened slightly by priority weighting.",
								"Default 32 blocks. This slider does not control dropped items, XP orbs, or decorations."))
						.option(slider("Item Distance", () -> (int) cfg.maxItemEntityRenderDistance, v -> cfg.maxItemEntityRenderDistance = v, 20, 4, 128, "blocks",
								"Maximum draw distance for dropped item entities. This value is used as-is: it is no longer multiplied by the 0.55 mob-priority weight that previously cut a 20-block slider down to about 9–11 blocks.",
								"Default 20 blocks. Adaptive scale can still shrink it when frame rate is below target."))
						.option(slider("XP Orb Distance", () -> (int) cfg.maxXpOrbRenderDistance, v -> cfg.maxXpOrbRenderDistance = v, 16, 4, 128, "blocks",
								"Maximum draw distance for experience orbs. Honored exactly; the old generic priority weight is not applied on top.",
								"Default 16 blocks. Large orb piles after grinders are cheap to hide past this range."))
						.option(slider("Decoration Distance", () -> (int) cfg.maxDecorationEntityDistance, v -> cfg.maxDecorationEntityDistance = v, 16, 4, 128, "blocks",
								"Maximum draw distance for armor stands and item frames. Honored exactly; the old 0.60 priority weight is not applied on top.",
								"Default 16 blocks. Raise this in museums or shops if frames vanish too early."))
						.option(toggle("Defer to Entity-Culling Mods", () -> cfg.deferToDedicatedEntityCullingMods, v -> cfg.deferToDedicatedEntityCullingMods = v, true,
								"When Entity Culling or MoreCulling is loaded, HSN skips its own entity-visibility pass so the dedicated mod can own occlusion tests.",
								"Recommended: on, to avoid two mods fighting over the same entities.", FPS_COMPAT))
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Block entities").withStyle(ChatFormatting.AQUA))
						.option(slider("Block-Entity Distance", () -> (int) cfg.maxBlockEntityRenderDistance, v -> cfg.maxBlockEntityRenderDistance = v, 24, 4, 128, "blocks",
								"Chests, signs, furnaces, banners, and other block entities past this distance are not submitted to the renderer.",
								"Default 24 blocks. Storage halls benefit the most from a lower value."))
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Overlays").withStyle(ChatFormatting.AQUA))
						.collapsed(true)
						.option(toggle("Shadow Culling", () -> cfg.shadowCullingEnabled, v -> cfg.shadowCullingEnabled = v, true,
								"Skips the ground-blob shadow pass for entities that are farther than the shadow distance.",
								"Recommended: on. Shadows are cheap individually but add up in mob farms."))
						.option(slider("Shadow Distance", () -> (int) cfg.maxShadowDistance, v -> cfg.maxShadowDistance = v, 12, 2, 64, "blocks",
								"Entity shadows beyond this distance are not drawn. Nearby players and mobs keep their shadows.",
								"Default 12 blocks."))
						.option(toggle("Name-Tag Culling", () -> cfg.nameTagCullEnabled, v -> cfg.nameTagCullEnabled = v, true,
								"Hides floating name tags once the owner is past the name-tag distance. Does not change the entity itself.",
								"Recommended: on in multiplayer hubs."))
						.option(slider("Name-Tag Distance", () -> (int) cfg.maxNameTagDistance, v -> cfg.maxNameTagDistance = v, 24, 4, 128, "blocks",
								"Name tags farther than this are not drawn. Useful when many named mobs share the screen.",
								"Default 24 blocks."))
						.option(toggle("Glow-Outline Culling", () -> cfg.glowOutlineCullingEnabled, v -> cfg.glowOutlineCullingEnabled = v, true,
								"Skips the glowing-entity outline pass past the glow distance. Spectral arrows and glowing mobs still exist; only the outline is dropped.",
								"Recommended: on."))
						.option(slider("Glow-Outline Distance", () -> (int) cfg.maxGlowOutlineDistance, v -> cfg.maxGlowOutlineDistance = v, 28, 4, 64, "blocks",
								"Glow outlines beyond this distance are not submitted. The entity mesh can still render if it is inside entity distance.",
								"Default 28 blocks."))
						.option(toggle("Beacon-Beam Culling", () -> cfg.beaconBeamCullingEnabled, v -> cfg.beaconBeamCullingEnabled = v, true,
								"Hides beacon beams that start farther than the beacon distance. The beacon block itself is unaffected.",
								"Recommended: on when several beacons share a spawn chunk."))
						.option(slider("Beacon-Beam Distance", () -> (int) cfg.maxBeaconBeamDistance, v -> cfg.maxBeaconBeamDistance = v, 48, 8, 128, "blocks",
								"Beacon beams originating past this distance are not drawn. Beams are tall and expensive in fill-rate.",
								"Default 48 blocks."))
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Audio").withStyle(ChatFormatting.AQUA))
						.option(slider("Sound Distance", () -> (int) cfg.maxSoundDistance, v -> cfg.maxSoundDistance = v, 48, 12, 64, "blocks",
								"New sounds whose source is farther than this are never started. Already-playing clips are left alone.",
								"Default 48 blocks. Lower this if farms or weather saturate the sound pool."))
						.option(toggle("Weather Sound Reduction", () -> cfg.weatherSoundReductionEnabled, v -> cfg.weatherSoundReductionEnabled = v, false,
								"Keeps only a configurable fraction of rain and thunder loop attempts instead of starting every one.",
								"Default: off. Enable on weak audio mixes or during long storms."))
						.option(percent("Weather Keep Rate", () -> cfg.weatherSoundKeepChance, v -> cfg.weatherSoundKeepChance = v, 20, 0, 100,
								"Probability that an individual weather sound is allowed to start while reduction is enabled. 100% is vanilla density.",
								"Default 20%."))
						.option(toggle("Sound Burst Limit", () -> cfg.soundBurstLimitEnabled, v -> cfg.soundBurstLimitEnabled = v, false,
								"Caps how many brand-new sounds may begin in a short window so piston doors and item showers cannot stall the mixer.",
								"Default: off."))
						.option(slider("Maximum New Sounds", () -> cfg.maxNewSoundsPerTick, v -> cfg.maxNewSoundsPerTick = v, 24, 1, 64, "",
								"Burst cap used only while Sound Burst Limit is enabled. Extra start requests in that window are dropped.",
								"Default 24."))
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
								"Scales presentation quality down as an object approaches its cull distance, instead of staying at full detail until it pops out.",
								"Recommended: on. Makes distance cuts less abrupt."))
						.option(percent("LOD Start", () -> cfg.progressiveLodStart, v -> cfg.progressiveLodStart = v, 50, 20, 90,
								"Share of the maximum distance that stays at full quality. Past this point, quality ramps toward the minimum LOD value.",
								"Default 50%. Lower values begin cheapening objects sooner and raise FPS."))
						.option(percent("Minimum LOD Quality", () -> cfg.progressiveLodMinQuality, v -> cfg.progressiveLodMinQuality = v, 15, 5, 50,
								"Quality reserved for objects sitting just inside the cull distance. Higher values keep far objects sharper.",
								"Default 15%."))
						.option(toggle("Entity LOD Stages", () -> cfg.entityLodStagesEnabled, v -> cfg.entityLodStagesEnabled = v, true,
								"Far, low-priority living entities use a slightly shorter draw distance. Players, ridden vehicles, and recently damaged mobs are excluded so combat stays readable.",
								"Does not apply to items, XP orbs, armor stands, or item frames — those use their dedicated sliders."))
						.option(toggle("Block-Entity LOD", () -> cfg.blockEntityLodEnabled, v -> cfg.blockEntityLodEnabled = v, true,
								"Distant chests, signs, and similar tile entities switch to a cheaper presentation before they are fully culled.",
								"Recommended: on."))
						.option(slider("Block-Entity LOD Distance", () -> (int) cfg.blockEntityLodDistance, v -> cfg.blockEntityLodDistance = v, 14, 4, 48, "blocks",
								"Distance at which the cheaper block-entity pass begins. Objects closer than this stay at full detail.",
								"Default 14 blocks. Lower values start the cheap pass sooner and raise FPS."))
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Textures").withStyle(ChatFormatting.AQUA))
						.option(toggle("Block Texture LOD", () -> cfg.blockTextureLodEnabled, v -> cfg.blockTextureLodEnabled = v, true,
								"Applies a mip bias to the blocks atlas while the world is drawn, so far terrain samples a cheaper mip level. Video Settings mipmaps must be enabled.",
								"Recommended: on."))
						.option(slider("Texture LOD Bias", () -> (int) Math.round(cfg.blockTextureLodBias * 100), v -> cfg.blockTextureLodBias = v / 100.0, 125, 0, 300, "",
								"0 keeps vanilla sharpness. Larger values sample blurrier mips on the horizon and reduce texture bandwidth.",
								"Default 125. Frame-rate impact: higher values raise FPS; lower values keep sharper terrain and lower FPS.", FPS_LOWER_LOW))
						.option(toggle("Adaptive Texture LOD", () -> cfg.blockTextureLodAdaptive, v -> cfg.blockTextureLodAdaptive = v, true,
								"Raises the mip bias automatically while smoothed frame rate is below the target, then relaxes it when FPS recovers.",
								"Recommended: on."))
						.option(toggle("Animated Texture Throttle", () -> cfg.textureAnimThrottleEnabled, v -> cfg.textureAnimThrottleEnabled = v, true,
								"Updates water, lava, portal, and fire atlas frames less often when the client is under load.",
								"Recommended: on. Animation still plays; it just steps more slowly."))
						.option(toggle("Adaptive Texture Interval", () -> cfg.textureAnimUseAdaptive, v -> cfg.textureAnimUseAdaptive = v, true,
								"Chooses the atlas update interval from live frame rate, moving between the two interval sliders below.",
								"Recommended: on."))
						.option(slider("Texture Interval", () -> cfg.textureAnimInterval, v -> cfg.textureAnimInterval = v, 1, 1, 8, "ticks",
								"Atlas update interval used when frame rate is healthy. 1 tick is vanilla (every client tick).",
								"Default 1 tick. Frame-rate impact: higher intervals raise FPS; 1 tick is the most expensive.", FPS_LOWER_LOW))
						.option(slider("Texture Interval (Load)", () -> cfg.textureAnimMaxInterval, v -> cfg.textureAnimMaxInterval = v, 4, 1, 12, "ticks",
								"Slowest atlas update interval used when frame rate is below target. Water and lava appear to animate in larger steps.",
								"Default 4 ticks. Frame-rate impact: higher intervals raise FPS under load.", FPS_LOWER_LOW))
						.option(toggle("Item Spin Throttle", () -> cfg.itemSpinThrottleEnabled, v -> cfg.itemSpinThrottleEnabled = v, true,
								"Freezes dropped-item rotation bobbing while Performance Mode is active, or for items past the spin distance under load.",
								"Recommended: on. Pickup and physics are unchanged."))
						.option(slider("Item Spin Distance", () -> (int) cfg.itemSpinThrottleDistance, v -> cfg.itemSpinThrottleDistance = v, 12, 2, 48, "blocks",
								"Beyond this distance, item spin is eligible to freeze when the client is under load. Closer items keep the vanilla bob.",
								"Default 12 blocks. Lower values freeze spin sooner and raise FPS."))
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
								"Hides far section corners so the drawn world follows a circle (or the selected shape) instead of a square. Ground under the camera is always kept.",
								"Default: off. Enable only if you want the corner-cut look and the extra fill-rate saving."))
						.option(Option.<WorldRenderShape>createBuilder()
								.name(Component.literal("Mask Shape"))
								.description(desc(
										"Geometry used when the terrain mask is active. Square is vanilla coverage. Circle and hexagon trim the far corners. Front half keeps terrain ahead of the camera and drops what is behind.",
										"Ignored while a dedicated world-shape mod is present.",
										"Frame-rate impact: tighter shapes (circle, hexagon, front half) raise FPS by drawing fewer far sections. Square / full coverage lowers FPS."))
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
								"Portion of the current view distance covered by the mask. 100% reaches the view-distance radius; lower percentages shrink the drawn disc.",
								"Default 100%."))
						.option(toggle("Vertical Range Limit", () -> cfg.circularVerticalRangeEnabled, v -> cfg.circularVerticalRangeEnabled = v, false,
								"Also clips the mask on the Y axis so sections far above or below the camera are not drawn.",
								"Default: off. Useful in tall megabuilds."))
						.option(slider("Vertical Range", () -> cfg.circularVerticalRange, v -> cfg.circularVerticalRange = v, 16, 4, 64, "blocks",
								"How far above and below the camera the mask is still allowed to draw when the vertical limit is on.",
								"Default 16 blocks."))
						.option(slider("Always-Keep Chunks", () -> cfg.alwaysKeepChunks, v -> cfg.alwaysKeepChunks = v, 3, 1, 8, "chunks",
								"Chunk radius around the camera that the mask is forbidden to hide. Prevents holes under your feet.",
								"Default 3 chunks. Higher values draw more nearby terrain and lower FPS."))
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Fog").withStyle(ChatFormatting.AQUA))
						.collapsed(true)
						.option(toggle("Fog Scale", () -> cfg.fogScaleEnabled, v -> cfg.fogScaleEnabled = v, false,
								"Shortens vanilla fog so the far plane fades sooner. Can hide pop-in after aggressive culling, but may fight shader packs.",
								"Default: off. Prefer Sodium Extra fog controls when that mod is loaded."))
						.option(percent("Fog Scale Factor", () -> cfg.fogScaleFactor, v -> cfg.fogScaleFactor = v, 85, 35, 100,
								"Multiplier on vanilla fog distance while Fog Scale is enabled. 100% is unchanged; 35% pulls fog very close.",
								"Lower values hide more of the horizon and can raise FPS slightly by covering distant fill."))
						.build())
				.build();
	}

	private static ConfigCategory performance(HSNConfig cfg) {
		return ConfigCategory.createBuilder()
				.name(Component.literal("Performance").withStyle(ChatFormatting.GOLD))
				.tooltip(Component.literal("Adaptive scaling and platform workarounds."))
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Adaptive scaling").withStyle(ChatFormatting.AQUA))
						.option(toggle("Adaptive Culling", () -> cfg.adaptiveCullingEnabled, v -> cfg.adaptiveCullingEnabled = v, false,
								"When smoothed FPS falls below the target, every distance slider is scaled down. Leave this OFF if you want 32 on the slider to mean 32 blocks.",
								"Default: off. Turn on only if you want distances to shrink automatically under load."))
						.option(slider("Target Frame Rate", () -> cfg.targetFps, v -> cfg.targetFps = v, 60, 20, 1000, "FPS",
								"Frame rate Adaptive Culling tries to protect. Raising it makes the scaler engage earlier and cut distances sooner.",
								"Default 60 FPS. Frame-rate impact: higher targets raise delivered FPS by culling more; lower targets keep quality longer.", FPS_LOWER_LOW))
						.option(percent("Minimum Distance Scale", () -> cfg.minAdaptiveScale, v -> cfg.minAdaptiveScale = v, 50, 25, 100,
								"Floor for how far adaptive scaling may shrink distances. 50% means a 32-block entity distance will not go below 16 under load.",
								"Default 50%. Lower floors allow more aggressive savings and higher FPS under load."))
						.option(toggle("Weak-GPU Auto", () -> cfg.weakGpuAutoEnabled, v -> cfg.weakGpuAutoEnabled = v, true,
								"If smoothed FPS stays under the weak-GPU threshold, an extra-low quality layer is applied on top of adaptive scaling.",
								"Recommended: on for integrated GPUs and thin laptops."))
						.option(slider("Weak-GPU Threshold", () -> cfg.weakGpuFpsThreshold, v -> cfg.weakGpuFpsThreshold = v, 35, 15, 60, "FPS",
								"Smoothed FPS at which the extra-low layer engages. A higher threshold means the layer turns on more often.",
								"Default 35 FPS. Frame-rate impact: higher thresholds raise FPS more often; lower thresholds keep quality unless the machine is struggling.", FPS_LOWER_LOW))
						.option(toggle("Low-End Hardware Tune", () -> cfg.lowEndHardwareTuneEnabled, v -> cfg.lowEndHardwareTuneEnabled = v, true,
								"Applies extra client-side tightening when the GPU string looks like an integrated or entry-level device (Intel HD / UHD, similar iGPUs).",
								"Smoother frame times on those chips. Dedicated GPUs ignore most of this path."))
						.option(toggle("Laptop Power-Save", () -> cfg.laptopPowerSaveEnabled, v -> cfg.laptopPowerSaveEnabled = v, true,
								"While FPS is below target, reserves more of the frame and skips extra texture work so the machine runs cooler and more evenly.",
								"Recommended on battery or weak cooling."))
						.option(toggle("Adaptive Work Budget", () -> cfg.adaptiveUploadBudgetEnabled, v -> cfg.adaptiveUploadBudgetEnabled = v, true,
								"Shrinks optional client-side work when a frame is already late. Does not replace Sodium chunk uploads.",
								"Recommended: on."))
						.option(percent("Work-Budget Reserve", () -> cfg.uploadBudgetFraction, v -> cfg.uploadBudgetFraction = v, 12, 5, 40,
								"Share of a loaded frame kept free of extra HSN work. Larger reserves stop optional tasks earlier.",
								"Default 12%. Frame-rate impact: higher reserves raise FPS under load; lower reserves let more extra work run.", FPS_LOWER_LOW))
						.option(toggle("Sodium Section Occupancy", () -> cfg.sectionOccupancyCullingEnabled, v -> cfg.sectionOccupancyCullingEnabled = v, false,
								"Skips entities in 16³ sections Sodium did not visit this frame, and only beyond 24 blocks. Incomplete occupancy used to hide mobs three blocks away.",
								"Default: off. Enable only with Sodium if you want extra occlusion."))
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("SIMD / native").withStyle(ChatFormatting.AQUA))
						.option(label("CPU: " + cpuSummary(),
								"Requested mode is capped to what this machine actually has."))
						.option(toggle("Native Hotpath", () -> cfg.nativeHotpathEnabled, v -> cfg.nativeHotpathEnabled = v, true,
								"Uses the optional native library for batched distance tests when many points are culled in one pass. Missing library or unsupported CPU falls back to Java automatically.",
								"Leave on unless you are isolating a native crash. Safe on every CPU."))
						.option(toggle("Native Frustum Culling", () -> cfg.nativeFrustumCullingEnabled, v -> cfg.nativeFrustumCullingEnabled = v, true,
								"Runs assembly AABB / sphere frustum tests on packed section batches. Off keeps vanilla + distance culling only. Falls back to Java if the kernel is missing.",
								"Recommended: on. Turn off to isolate a native frustum issue."))
						.option(Option.<SimdMode>createBuilder()
								.name(Component.literal("SIMD Mode"))
								.description(desc(
										"Which vector kernel the native library should prefer for batch distance tests.",
										"Auto — try AVX-512, then AVX2, then scalar. Recommended.",
										"AVX-512 / AVX2 — used only if the CPU reports the matching feature flags.",
										"Off / scalar — never issues vector instructions.",
										"Unsupported choices fall back automatically. This setting never forces an instruction the chip lacks.",
										"Frame-rate impact: Auto / AVX-512 / AVX2 raise FPS on large batch tests. Off / scalar is the slowest CPU path."))
								.binding(SimdMode.AUTO, () -> cfg.simdMode, v -> cfg.simdMode = v)
								.controller(opt -> EnumControllerBuilder.create(opt).enumClass(SimdMode.class))
								.build())
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Platform").withStyle(ChatFormatting.AQUA))
						.option(toggle("Frame-Pacing Workaround", () -> cfg.framePacingFixEnabled, v -> cfg.framePacingFixEnabled = v, false,
								"Skips immutable GL buffer storage. Intended only for older Intel HD drivers that hitch on that path.",
								"Default: off. Restart the game after changing. On modern GPUs this can slightly lower FPS; on those old Intel chips it can stabilize frame times.", FPS_ON_LOW))
						.build())
				.build();
	}

	private static ConfigCategory highEnd(HSNConfig cfg) {
		return ConfigCategory.createBuilder()
				.name(Component.literal("High-end").withStyle(ChatFormatting.GOLD))
				.tooltip(Component.literal("CPU-side cuts that still help when the GPU is a 5090-class card."))
				.option(label("3.8.7 R high-end pass",
						"Minecraft is CPU-bound past a point. These skip work the GPU never sees."))
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Frame and light").withStyle(ChatFormatting.AQUA))
						.option(toggle("Lightmap Cache", () -> cfg.lightmapCacheEnabled, v -> cfg.lightmapCacheEnabled = v, true,
								"Skips LightTexture rebuilds while gamma, dimension and player state are unchanged. Rebuilds at least every 20 frames so flashes cannot stick.",
								"Recommended: on. This is free FPS on high-refresh displays."))
						.option(toggle("Unfocused FPS Cap", () -> cfg.unfocusedFpsCapEnabled, v -> cfg.unfocusedFpsCapEnabled = v, true,
								"Sleeps leftover frame time when the game window is in the background.",
								"Recommended: on. Saves heat and lets a second monitor stay smooth."))
						.option(slider("Unfocused Cap", () -> cfg.unfocusedFpsCap, v -> cfg.unfocusedFpsCap = v, 30, 5, 240, "FPS",
								"Frame-rate ceiling used only while the window is unfocused.",
								"Default 30 FPS."))
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Entity CPU").withStyle(ChatFormatting.AQUA))
						.option(toggle("Skip Far Interpolation", () -> cfg.entityInterpSkipEnabled, v -> cfg.entityInterpSkipEnabled = v, true,
								"Stops lerpTo / old-position copies on far non-combat entities. The camera entity and hurt mobs stay vanilla.",
								"Recommended: on. Cuts entity CPU in villages and farms."))
						.option(slider("Interp Skip Distance", () -> (int) cfg.entityInterpSkipDistance, v -> cfg.entityInterpSkipDistance = v, 48, 16, 160, "blocks",
								"Entities farther than this skip interpolation.",
								"Default 48 blocks."))
						.option(toggle("Distant Client Ticks", () -> cfg.distantClientTickSkipEnabled, v -> cfg.distantClientTickSkipEnabled = v, true,
								"Item entities, XP orbs, armor stands, frames, paintings and displays tick less often past a distance on the client.",
								"Recommended: on. Pickup range and nearby loot are unchanged."))
						.option(slider("Client-Tick Distance", () -> (int) cfg.distantClientTickDistance, v -> cfg.distantClientTickDistance = v, 40, 12, 128, "blocks",
								"Decorations closer than this keep a full client tick.",
								"Default 40 blocks."))
						.option(slider("Client-Tick Interval", () -> cfg.distantClientTickInterval, v -> cfg.distantClientTickInterval = v, 4, 2, 12, "ticks",
								"How often a distant decoration is allowed to tick.",
								"Default 4 ticks.", FPS_LOWER_LOW))
						.option(toggle("Living Anim Throttle", () -> cfg.livingAnimThrottleEnabled, v -> cfg.livingAnimThrottleEnabled = v, true,
								"Skips limb-swing and effect-particle updates on far living entities. Does not cancel LivingEntity.tick.",
								"Recommended: on."))
						.option(slider("Anim Throttle Distance", () -> (int) cfg.livingAnimThrottleDistance, v -> cfg.livingAnimThrottleDistance = v, 36, 12, 128, "blocks",
								"Living entities farther than this drop walk-animation work.",
								"Default 36 blocks."))
						.option(toggle("Idle AI Throttle", () -> cfg.idleAiThrottleEnabled, v -> cfg.idleAiThrottleEnabled = v, true,
								"Distant idle mobs skip some GoalSelector passes on the integrated server. Combat and nearby mobs are left alone.",
								"Recommended: on in singleplayer. Deferred on dedicated servers by Integrated Server Only."))
						.option(slider("Idle AI Full-Rate Distance", () -> (int) cfg.idleAiFullDistance, v -> cfg.idleAiFullDistance = v, 48, 16, 128, "blocks",
								"Inside this radius idle AI runs every tick.",
								"Default 48 blocks."))
						.option(slider("Idle AI Interval", () -> cfg.idleAiMaxInterval, v -> cfg.idleAiMaxInterval = v, 10, 2, 20, "ticks",
								"Tick interval for far idle mobs.",
								"Default 10 ticks.", FPS_LOWER_LOW))
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("World extras").withStyle(ChatFormatting.AQUA))
						.option(toggle("Cloud LOD", () -> cfg.cloudLodEnabled, v -> cfg.cloudLodEnabled = v, true,
								"Skips the cloud pass in ceiling dimensions and when the camera is looking down below Y=80.",
								"Recommended: on. Fancy clouds are fill-rate even on flagship GPUs."))
						.option(toggle("Weather Renderer LOD", () -> cfg.weatherRendererLodEnabled, v -> cfg.weatherRendererLodEnabled = v, true,
								"Skips snow/rain overlay in ceiling dimensions and while adaptive scale is already low.",
								"Recommended: on. Particle culling still owns rain splashes."))
						.option(toggle("Sky Extras Throttle", () -> cfg.skyExtrasThrottleEnabled, v -> cfg.skyExtrasThrottleEnabled = v, true,
								"Skips star / sunrise extras in ceiling dimensions (Nether).",
								"Recommended: on."))
						.option(toggle("World-Border LOD", () -> cfg.worldBorderLodEnabled, v -> cfg.worldBorderLodEnabled = v, true,
								"Skips the world-border mesh while the camera is more than 96 blocks inside it.",
								"Recommended: on."))
						.option(toggle("Map Renderer Throttle", () -> cfg.mapRendererThrottleEnabled, v -> cfg.mapRendererThrottleEnabled = v, true,
								"Rebuilds held/item-frame map textures less often.",
								"Recommended: on in map walls."))
						.option(slider("Map Interval", () -> cfg.mapRendererInterval, v -> cfg.mapRendererInterval = v, 4, 1, 12, "ticks",
								"How often map textures may rebuild.",
								"Default 4 ticks.", FPS_LOWER_LOW))
						.option(toggle("Firework Particle Cap", () -> cfg.fireworkParticleCapEnabled, v -> cfg.fireworkParticleCapEnabled = v, true,
								"Separate per-tick ceiling for firework flash particles so shows cannot drown the particle manager.",
								"Recommended: on."))
						.option(slider("Firework Budget", () -> cfg.maxFireworkParticlesPerTick, v -> cfg.maxFireworkParticlesPerTick = v, 48, 8, 200, "",
								"Maximum firework particles accepted in one tick.",
								"Default 48."))
						.option(toggle("Drip Particle Throttle", () -> cfg.dripParticleThrottleEnabled, v -> cfg.dripParticleThrottleEnabled = v, true,
								"Thins dripping/falling water and lava particles, and drops them entirely in the Nether.",
								"Recommended: on."))
						.option(toggle("Hard Particle Cap", () -> cfg.hardParticleCapEnabled, v -> cfg.hardParticleCapEnabled = v, true,
								"Reports the configured particle budget back to ParticleEngine.countParticles so vanilla and Sodium share the same ceiling.",
								"Recommended: on."))
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
								"Retains fewer particles as they approach the maximum particle distance, so density falls off instead of ending in a hard wall.",
								"Recommended: on."))
						.option(toggle("Particle Priority", () -> cfg.particlePriorityEnabled, v -> cfg.particlePriorityEnabled = v, true,
								"When the budget is exceeded, combat and player-owned effects are kept ahead of rain, ash, and decoration wisps.",
								"Recommended: on."))
						.option(percent("Rain Keep Rate", () -> cfg.rainKeepChance, v -> cfg.rainKeepChance = v, 15, 0, 100,
								"Chance that an individual rain-splash particle is kept. 0% removes rain splashes; 100% keeps every attempt that passed distance checks.",
								"Default 15%. Rain is usually the largest particle source."))
						.option(percent("Smoke Keep Rate", () -> cfg.smokeKeepChance, v -> cfg.smokeKeepChance = v, 25, 0, 100,
								"Keep chance for campfire and furnace smoke. Lower values thin kitchen and farm smoke columns.",
								"Default 25%."))
						.option(percent("Explosion Keep Rate", () -> cfg.explosionKeepChance, v -> cfg.explosionKeepChance = v, 100, 0, 100,
								"Keep chance for explosion bursts. Leave high unless a TNT cannon is saturating the budget.",
								"Default 100%."))
						.option(percent("Fire Keep Rate", () -> cfg.fireSmokeKeepChance, v -> cfg.fireSmokeKeepChance = v, 100, 0, 100,
								"Keep chance for fire and lava sparks. Lower only if nether hubs stall the particle manager.",
								"Default 100%."))
						.option(percent("Bubble Keep Rate", () -> cfg.bubbleKeepChance, v -> cfg.bubbleKeepChance = v, 100, 0, 100,
								"Keep chance for underwater bubble columns and breath bubbles.",
								"Default 100%."))
						.option(percent("High-Priority Keep Rate", () -> cfg.highPriorityKeepChance, v -> cfg.highPriorityKeepChance = v, 85, 10, 100,
								"Keep chance applied to combat and player-owned particles while Particle Priority is enabled.",
								"Default 85%. Keep this higher than the low-priority rate."))
						.option(percent("Low-Priority Keep Rate", () -> cfg.lowPriorityKeepChance, v -> cfg.lowPriorityKeepChance = v, 25, 0, 100,
								"Keep chance applied to decoration particles (ash, spores, idle drips) when the budget is tight.",
								"Default 25%."))
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Interface").withStyle(ChatFormatting.AQUA))
						.option(toggle("F3 Status", () -> cfg.f3ShowStatus, v -> cfg.f3ShowStatus = v, true,
								"Adds a colored [HSN] summary block to the right column of the debug screen.",
								"Display only. Does not change world rendering.", FPS_NONE))
						.option(toggle("F3 Details", () -> cfg.f3ShowDetails, v -> cfg.f3ShowDetails = v, true,
								"Includes live distances, feature flags, and cull counters on the F3 summary.",
								"Display only. Does not change world rendering.", FPS_NONE))
						.option(toggle("FPS Overlay", () -> cfg.fpsOverlayEnabled, v -> cfg.fpsOverlayEnabled = v, false,
								"Draws a compact on-screen FPS readout. Also bound to F7. Independent of F3.",
								"Default: off. Cost is a few text draws per frame.", FPS_ON_LOW))
						.option(slider("Overlay X", () -> cfg.fpsOverlayX, v -> cfg.fpsOverlayX = v, 4, 0, 400, "px",
								"Horizontal offset of the FPS overlay from the left edge of the window.",
								"Default 4 pixels. Position only.", FPS_NONE))
						.option(slider("Overlay Y", () -> cfg.fpsOverlayY, v -> cfg.fpsOverlayY = v, 4, 0, 400, "px",
								"Vertical offset of the FPS overlay from the top edge of the window.",
								"Default 4 pixels. Position only.", FPS_NONE))
						.option(toggle("Toast Limit", () -> cfg.toastLimitEnabled, v -> cfg.toastLimitEnabled = v, true,
								"Restricts recipe and system toasts to a few every two seconds so advancement spam cannot stall the UI thread.",
								"Recommended: on."))
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Simulation").withStyle(ChatFormatting.AQUA))
						.collapsed(true)
						.option(toggle("Integrated Server Only", () -> cfg.integratedServerOnly, v -> cfg.integratedServerOnly = v, true,
								"Keeps pathfinding and item-tick extras off on dedicated servers so HSN never changes multiplayer simulation authority.",
								"Recommended: on. This is a safety switch, not a quality slider.", FPS_NONE))
						.option(toggle("Defer Pathfinding to Lithium", () -> cfg.deferPathfindingToLithium, v -> cfg.deferPathfindingToLithium = v, true,
								"Skips HSN path throttling when Lithium is loaded, because Lithium already owns AI pathing.",
								"Recommended: on. Prevents two throttles stacking.", FPS_COMPAT))
						.option(toggle("Pathfinding Throttle", () -> cfg.pathfindingThrottleEnabled, v -> cfg.pathfindingThrottleEnabled = v, true,
								"Distant idle mobs reuse their current path instead of rebuilding every tick. Mobs inside the full-rate distance, and mobs in combat, are left alone.",
								"Recommended: on in singleplayer. Helps villages and animal pens more than FPS overlays do."))
						.option(slider("Pathfinding Full-Rate Distance", () -> (int) cfg.pathfindingFullDistance, v -> cfg.pathfindingFullDistance = v, 32, 8, 96, "blocks",
								"Inside this radius, pathfinding runs at vanilla rate. Outside it, rebuilds are spaced up to the maximum interval.",
								"Default 32 blocks. Lower values throttle more mobs and raise tick rate / FPS in singleplayer."))
						.option(slider("Pathfinding Maximum Interval", () -> cfg.pathfindingMaxInterval, v -> cfg.pathfindingMaxInterval = v, 8, 2, 20, "ticks",
								"Longest delay between path rebuilds for distant idle mobs. Combat and close mobs ignore this.",
								"Default 8 ticks. Frame-rate impact: higher intervals raise FPS / MSPT; lower intervals keep AI snappier and cost more.", FPS_LOWER_LOW))
						.option(toggle("Locate Cache", () -> cfg.locateOptimizeEnabled, v -> cfg.locateOptimizeEnabled = v, true,
								"Reuses recent /locate results for the configured TTL so repeated commands do not scan the world again.",
								"Recommended: on. Affects command hitch time more than frame rate."))
						.option(slider("Locate Cache TTL", () -> cfg.locateCacheTtlSeconds, v -> cfg.locateCacheTtlSeconds = v, 30, 5, 120, "s",
								"How long a cached /locate result may be reused before a fresh search runs.",
								"Default 30 seconds. Does not change world rendering.", FPS_NONE))
						.option(toggle("Accelerated World Load", () -> cfg.fastWorldLoadEnabled, v -> cfg.fastWorldLoadEnabled = v, false,
								"Drains extra chunk tasks for a short window after the integrated server starts so the world becomes playable sooner.",
								"Default: off. Can hitch the first few seconds of a world join; it is not an FPS booster during play.", FPS_NONE))
						.option(slider("World-Load Window", () -> cfg.fastWorldLoadWindowSeconds, v -> cfg.fastWorldLoadWindowSeconds = v, 8, 1, 20, "s",
								"How many seconds after server start the extra chunk drain remains active.",
								"Default 8 seconds. Join-time only.", FPS_NONE))
						.option(slider("World-Load Chunk Boost", () -> cfg.fastWorldLoadChunkBoost, v -> cfg.fastWorldLoadChunkBoost = v, 6, 1, 16, "",
								"Extra chunk tasks drained per tick during the load window. Higher values finish terrain faster but can hitch the join harder.",
								"Default 6. Join-time only.", FPS_NONE))
						.option(toggle("Item / XP Tick Throttle", () -> cfg.itemThrottleEnabled, v -> cfg.itemThrottleEnabled = v, false,
								"Reduces physics ticks on dropped items and XP orbs that are farther than the throttle distance. Nearby loot still ticks every tick.",
								"Default: off. Enable in grinders with thousands of entities on the ground."))
						.option(slider("Item Throttle Distance", () -> (int) cfg.itemThrottleStartDistance, v -> cfg.itemThrottleStartDistance = v, 24, 8, 96, "blocks",
								"Distance at which item and XP physics begin skipping ticks. Closer drops keep full physics.",
								"Default 24 blocks. Lower values throttle sooner and raise tick rate / FPS."))
						.option(slider("Item Tick Interval", () -> cfg.itemThrottleMaxInterval, v -> cfg.itemThrottleMaxInterval = v, 8, 2, 20, "ticks",
								"Physics interval for distant items while throttling is enabled. 8 means those items tick once every eight ticks.",
								"Default 8 ticks. Frame-rate impact: higher intervals raise FPS / MSPT; lower intervals keep physics closer to vanilla.", FPS_LOWER_LOW))
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Sodium Extra").withStyle(ChatFormatting.AQUA))
						.collapsed(true)
						.option(toggle("Defer Fog to Sodium Extra", () -> cfg.deferFogToSodiumExtra, v -> cfg.deferFogToSodiumExtra = v, true,
								"Disables HSN fog scaling when Sodium Extra is loaded so Extra’s fog page is the only owner.",
								"Restart after changing. Compatibility only.", FPS_COMPAT))
						.option(toggle("Defer Toasts to Sodium Extra", () -> cfg.deferToastsToSodiumExtra, v -> cfg.deferToastsToSodiumExtra = v, true,
								"Disables HSN toast limiting when Sodium Extra is loaded.",
								"Restart after changing. Compatibility only.", FPS_COMPAT))
						.option(toggle("Defer Beacons to Sodium Extra", () -> cfg.deferBeaconToSodiumExtra, v -> cfg.deferBeaconToSodiumExtra = v, true,
								"Disables HSN beacon-beam distance when Sodium Extra is loaded.",
								"Restart after changing. Compatibility only.", FPS_COMPAT))
						.option(toggle("Defer Texture Anim to Sodium Extra", () -> cfg.deferTextureAnimToSodiumExtra, v -> cfg.deferTextureAnimToSodiumExtra = v, true,
								"Disables HSN atlas throttling when Sodium Extra animation toggles are present.",
								"Restart after changing. Compatibility only.", FPS_COMPAT))
						.option(toggle("Defer Particles to Sodium Extra", () -> cfg.deferParticlesToSodiumExtra, v -> cfg.deferParticlesToSodiumExtra = v, false,
								"Off by default. Extra usually toggles particle types; HSN still applies distance and budget unless this is enabled.",
								"Restart after changing. Compatibility only.", FPS_COMPAT))
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
		return toggle(name, get, set, def, what, advice, FPS_ON_HIGH);
	}

	private static Option<Boolean> toggle(String name, java.util.function.Supplier<Boolean> get, Consumer<Boolean> set,
			boolean def, String what, String advice, String fps) {
		return Option.<Boolean>createBuilder()
				.name(Component.literal(name))
				.description(desc(what, advice, fps))
				.binding(def, get, v -> {
					set.accept(v);
					HotPath.rebuild();
				})
				.controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
				.build();
	}

	private static Option<Integer> slider(String name, java.util.function.Supplier<Integer> get, Consumer<Integer> set,
			int def, int min, int max, String unit, String what, String advice) {
		return slider(name, get, set, def, min, max, unit, what, advice, FPS_LOWER_HIGH);
	}

	private static Option<Integer> slider(String name, java.util.function.Supplier<Integer> get, Consumer<Integer> set,
			int def, int min, int max, String unit, String what, String advice, String fps) {
		return Option.<Integer>createBuilder()
				.name(Component.literal(name))
				.description(desc(what, advice, fps))
				.binding(def, get, v -> {
					set.accept(v);
					HotPath.rebuild();
				})
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
		return percent(name, get, set, def, min, max, what, advice, FPS_LOWER_HIGH);
	}

	private static Option<Integer> percent(String name, java.util.function.Supplier<Double> get, Consumer<Double> set,
			int def, int min, int max, String what, String advice, String fps) {
		return slider(name, () -> (int) Math.round(get.get() * 100), v -> set.accept(v / 100.0),
				def, min, max, "%", what, advice, fps);
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
