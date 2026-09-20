package hsn.optimizations.client.config;

import dev.isxander.yacl3.api.ButtonOption;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.LabelOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import hsn.optimizations.client.compat.HSNModCompat;
import hsn.optimizations.client.optimize.AdaptiveCuller;
import hsn.optimizations.config.HSNConfig;
import hsn.optimizations.config.HSNConfig.Preset;
import hsn.optimizations.config.HSNPresets;
import hsn.optimizations.config.ScaleFilter;
import hsn.optimizations.config.SimdMode;
import hsn.optimizations.config.WorldRenderShape;
import hsn.optimizations.optimize.HotPath;
import hsn.optimizations.optimize.NativeBridge;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Optional YACL UI. Same option set as the built-in seven-tab screen, with
 * the older 3.8 YACL presentation: colored on/off controllers, grouped pages,
 * hover text that matches what the mixins actually do, and real default
 * bindings so Reset works.
 */
public final class HSNYaclConfigScreen {

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

	private HSNYaclConfigScreen() {
	}

	public static Screen create(Screen parent) {
		HSNConfig cfg = HSNConfig.get();
		return YetAnotherConfigLib.createBuilder()
				.title(Component.literal("HSN Optimizations").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
				.category(general(cfg))
				.category(entities(cfg))
				.category(particles(cfg))
				.category(rendering(cfg))
				.category(audio(cfg))
				.category(server(cfg))
				.category(extra(cfg))
				.category(fpsHelper(cfg))
				.save(() -> {
					cfg.sanitize();
					cfg.save();
					HotPath.rebuild(cfg);
				})
				.build()
				.generateScreen(parent);
	}

	private static ConfigCategory general(HSNConfig cfg) {
		return ConfigCategory.createBuilder()
				.name(Component.literal("General").withStyle(ChatFormatting.GOLD))
				.tooltip(Component.literal("Profiles and master switches. Most players only need this page."))
				.option(label("HSN Optimizations " + HSNConfig.modVersionLabel,
						"Complements Sodium. Does not replace it. YACL is optional; the same fields exist on the built-in screen."))
				.option(Option.<Preset>createBuilder()
						.name(Component.literal("Quality preset"))
						.description(desc(
								"Applies a complete, tested set of distances, keep-rates, and feature flags in one step.",
								"Ultra Low — oldest iGPUs. Safe — conservative low-end. Balanced — default. Quality — near-vanilla. Competitive — high refresh, cut CPU waste.",
								"Frame-rate impact: Ultra Low / Safe raise FPS. Quality keeps more of the world and lowers FPS."))
						.binding(Preset.BALANCED, () -> cfg.lastAppliedPreset, v -> {
							cfg.lastAppliedPreset = v;
							HSNPresets.apply(cfg, v);
							HotPath.rebuild(cfg);
						})
						.controller(opt -> EnumControllerBuilder.create(opt).enumClass(Preset.class))
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Master switches").withStyle(ChatFormatting.AQUA))
						.option(toggle("Master switch", () -> cfg.modEnabled, v -> { cfg.modEnabled = v; HotPath.rebuild(cfg); }, true,
								"When off, every HSN mixin becomes a no-op. The world renders like vanilla plus your other mods.",
								"Leave on unless you are isolating a bug. This is the kill switch, not a quality slider."))
						.option(toggle("Performance Mode", () -> cfg.performanceModeEnabled, v -> { cfg.performanceModeEnabled = v; HotPath.rebuild(cfg); }, false,
								"When FPS dips, eases live distances toward 70% of the sliders. Adaptive Culling still owns the full 25-100% scaler.",
								"Also bound to F6. Use it for crowded bases or weak cooling — not as an everyday default."))
						.option(toggle("Adaptive Culling", () -> cfg.adaptiveCullingEnabled, v -> { cfg.adaptiveCullingEnabled = v; HotPath.rebuild(cfg); }, false,
								"When smoothed FPS falls below the target, every distance limit is scaled down toward the minimum scale.",
								"Off by default in 4.0 so a fresh install does not shrink the world."))
						.option(slider("Target Frame Rate", () -> cfg.targetFps, v -> cfg.targetFps = v, 60, 30, 360, "FPS",
								"Frame rate Adaptive Culling and Performance Mode try to protect.",
								"Default 60 FPS.", FPS_LOWER_LOW))
						.option(percent("Minimum Distance Scale", () -> cfg.minAdaptiveScale, v -> cfg.minAdaptiveScale = v, 50, 25, 100,
								"Floor for how far adaptive scaling may shrink distances. 50% means a 32-block slider will not go below 16.",
								"Default 50%."))
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Hardware helpers").withStyle(ChatFormatting.AQUA))
						.option(toggle("Frame-Pacing Workaround", () -> cfg.framePacingFixEnabled, v -> cfg.framePacingFixEnabled = v, true,
								"Skips immutable GL buffer storage. Intended for older Intel HD / Iris Xe drivers that hitch on that path.",
								"On by default in 4.0. Restart after changing. Turn off on modern dedicated GPUs.", FPS_ON_LOW))
						.option(toggle("Smart yield", () -> cfg.smartYieldEnabled, v -> cfg.smartYieldEnabled = v, true,
								"Yields the client thread only when the last frame already missed about 10 ms. Does not yield when the frame is ahead of schedule.",
								"Matches the built-in tooltip and HSNScheduler.shouldYield()."))
						.option(toggle("Thread scheduler", () -> cfg.schedulerEnabled, v -> cfg.schedulerEnabled = v, true,
								"Frame-first thread policy for the render thread. Does not fake Runtime.availableProcessors().",
								"Leave on unless you are isolating a threading issue.", FPS_NONE))
						.option(toggle("Weak-GPU Auto", () -> cfg.weakGpuAutoEnabled, v -> cfg.weakGpuAutoEnabled = v, true,
								"If smoothed FPS stays under the weak-GPU threshold, an extra-low quality layer is applied on top of adaptive scaling.",
								"Recommended: on for integrated GPUs and thin laptops."))
						.option(slider("Weak-GPU Threshold", () -> cfg.weakGpuFpsThreshold, v -> cfg.weakGpuFpsThreshold = v, 35, 10, 120, "FPS",
								"Smoothed FPS at which the extra-low layer engages.",
								"Default 35 FPS.", FPS_LOWER_LOW))
						.option(toggle("Low-End Hardware Tune", () -> cfg.lowEndHardwareTuneEnabled, v -> cfg.lowEndHardwareTuneEnabled = v, true,
								"Extra client-side tightening when the GPU string looks like an integrated or entry-level device. Iris shaders are not treated as an iGPU.",
								"Dedicated GPUs ignore most of this path."))
						.option(toggle("Laptop Power-Save", () -> cfg.laptopPowerSaveEnabled, v -> cfg.laptopPowerSaveEnabled = v, true,
								"Reads /sys/class/power_supply/BAT*/status when present. While the battery is discharging, reserves more of the frame and skips extra texture work.",
								"Recommended on battery or weak cooling."))
						.option(toggle("Adaptive Work Budget", () -> cfg.adaptiveUploadBudgetEnabled, v -> cfg.adaptiveUploadBudgetEnabled = v, true,
								"Shrinks optional client-side work when a frame is already late. Does not replace Sodium chunk uploads.",
								"Recommended: on."))
						.option(percent("Work-Budget Reserve", () -> cfg.uploadBudgetFraction, v -> cfg.uploadBudgetFraction = v, 12, 5, 40,
								"Share of a loaded frame kept free of extra HSN work.",
								"Default 12%.", FPS_LOWER_LOW))
						.option(toggle("Unfocused FPS cap", () -> cfg.unfocusedFpsCapEnabled, v -> cfg.unfocusedFpsCapEnabled = v, true,
								"Limits FPS when the game window is not focused. Applied once per frame.",
								"Recommended: on."))
						.option(slider("Unfocused FPS", () -> cfg.unfocusedFpsCap, v -> cfg.unfocusedFpsCap = v, 30, 5, 240, "FPS",
								"Target frame rate used only while the window is in the background.",
								"Default 30 FPS.", FPS_LOWER_LOW))
						.option(toggle("Shader safe mode", () -> cfg.shaderSafeMode, v -> cfg.shaderSafeMode = v, true,
								"When a shader pack is bound, skip framebuffer swaps and lightmap freezes that fight Iris / Oculus.",
								"Leave on if you use shader packs."))
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Interface").withStyle(ChatFormatting.AQUA))
						.option(toggle("F3 Status", () -> cfg.f3ShowStatus, v -> cfg.f3ShowStatus = v, true,
								"Adds a colored [HSN] summary block to the right column of the debug screen.",
								"Display only.", FPS_NONE))
						.option(toggle("F3 Compact", () -> cfg.f3Compact, v -> cfg.f3Compact = v, true,
								"Two-line F3 block instead of a long dump.",
								"Display only.", FPS_NONE))
						.option(toggle("F3 Details", () -> cfg.f3ShowDetails, v -> cfg.f3ShowDetails = v, true,
								"Includes live distances, feature flags, and cull counters on the F3 summary.",
								"Display only.", FPS_NONE))
						.option(toggle("FPS Overlay", () -> cfg.fpsOverlayEnabled, v -> cfg.fpsOverlayEnabled = v, false,
								"Draws a compact on-screen FPS readout. Also bound to F7.",
								"Default: off.", FPS_ON_LOW))
						.option(slider("Overlay X", () -> cfg.fpsOverlayX, v -> cfg.fpsOverlayX = v, 4, 0, 400, "px",
								"Horizontal offset of the FPS overlay from the left edge.",
								"Position only.", FPS_NONE))
						.option(slider("Overlay Y", () -> cfg.fpsOverlayY, v -> cfg.fpsOverlayY = v, 4, 0, 400, "px",
								"Vertical offset of the FPS overlay from the top edge.",
								"Position only.", FPS_NONE))
						.option(toggle("Toast Limit", () -> cfg.toastLimitEnabled, v -> cfg.toastLimitEnabled = v, true,
								"Restricts recipe and system toasts so advancement spam cannot stall the UI thread.",
								"Recommended: on."))
						.build())
				.build();
	}

	private static ConfigCategory entities(HSNConfig cfg) {
		return ConfigCategory.createBuilder()
				.name(Component.literal("Entities").withStyle(ChatFormatting.GOLD))
				.tooltip(Component.literal("Living mobs, items, XP, decorations, block entities, and overlays."))
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Distance culling").withStyle(ChatFormatting.AQUA))
						.option(toggle("Entity Culling", () -> cfg.entityCullingEnabled, v -> { cfg.entityCullingEnabled = v; HotPath.rebuild(cfg); }, true,
								"Stops submitting living mobs, dropped items, XP orbs, armor stands, and item frames once they pass their configured distance. Dedicated sliders are honored exactly.",
								"Recommended: on."))
						.option(slider("Entity Distance", () -> (int) cfg.maxEntityRenderDistance, v -> cfg.maxEntityRenderDistance = v, 32, 4, 128, "blocks",
								"Maximum draw distance for living entities. Does not control items, XP orbs, or decorations.",
								"Default 32 blocks."))
						.option(slider("Item Distance", () -> (int) cfg.maxItemEntityRenderDistance, v -> cfg.maxItemEntityRenderDistance = v, 20, 4, 96, "blocks",
								"Maximum draw distance for dropped item entities. Used as-is.",
								"Default 20 blocks."))
						.option(slider("XP Orb Distance", () -> (int) cfg.maxXpOrbRenderDistance, v -> cfg.maxXpOrbRenderDistance = v, 16, 4, 64, "blocks",
								"Maximum draw distance for experience orbs. Honored exactly.",
								"Default 16 blocks."))
						.option(slider("Decoration Distance", () -> (int) cfg.maxDecorationEntityDistance, v -> cfg.maxDecorationEntityDistance = v, 16, 4, 96, "blocks",
								"Maximum draw distance for armor stands, item frames, paintings, and display entities.",
								"Default 16 blocks."))
						.option(toggle("Behind-camera cull", () -> cfg.behindCameraCullEnabled, v -> { cfg.behindCameraCullEnabled = v; HotPath.rebuild(cfg); }, false,
								"When on, entities (especially decorations, items, and XP) that are behind the camera and farther than half their slider are skipped. Off by default so a short slider still shows mobs beside you.",
								"Default: off. Honored by EntityRendererMixin and the particle behind-camera path."))
						.option(toggle("Defer to Entity-Culling Mods", () -> cfg.deferToDedicatedEntityCullingMods, v -> { cfg.deferToDedicatedEntityCullingMods = v; HotPath.rebuild(cfg); }, false,
								"When Entity Culling or MoreCulling is loaded, HSN skips its own entity-visibility pass.",
								"Off by default in 4.0.", FPS_COMPAT))
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("LOD and client ticks").withStyle(ChatFormatting.AQUA))
						.option(toggle("Entity LOD Stages", () -> cfg.entityLodStagesEnabled, v -> { cfg.entityLodStagesEnabled = v; HotPath.rebuild(cfg); }, true,
								"Far, low-priority living entities use a slightly shorter draw distance. Players, ridden vehicles, and recently damaged mobs are excluded.",
								"Does not apply to items, XP, armor stands, or frames."))
						.option(toggle("Entity interp skip", () -> cfg.entityInterpSkipEnabled, v -> { cfg.entityInterpSkipEnabled = v; HotPath.rebuild(cfg); }, true,
								"Skip client interpolation for far, non-combat entities.",
								"Recommended: on."))
						.option(slider("Interp skip distance", () -> (int) cfg.entityInterpSkipDistance, v -> cfg.entityInterpSkipDistance = v, 48, 8, 128, "blocks",
								"Distance at which interpolation becomes eligible to skip.",
								"Default 48 blocks."))
						.option(toggle("Distant client tick skip", () -> cfg.distantClientTickSkipEnabled, v -> { cfg.distantClientTickSkipEnabled = v; HotPath.rebuild(cfg); }, true,
								"Skip some client ticks on distant decorations and displays. Does not freeze dropped items or XP orbs.",
								"Recommended: on."))
						.option(slider("Distant tick distance", () -> (int) cfg.distantClientTickDistance, v -> cfg.distantClientTickDistance = v, 40, 8, 128, "blocks",
								"Distance at which client ticks begin skipping.",
								"Default 40 blocks."))
						.option(slider("Distant tick interval", () -> cfg.distantClientTickInterval, v -> cfg.distantClientTickInterval = v, 4, 2, 20, "ticks",
								"How often a distant decoration is allowed to client-tick.",
								"Default 4 ticks.", FPS_LOWER_LOW))
						.option(toggle("Living anim throttle", () -> cfg.livingAnimThrottleEnabled, v -> { cfg.livingAnimThrottleEnabled = v; HotPath.rebuild(cfg); }, true,
								"Skip living-entity pose / limb-swing work past the distance. Does not cancel tickDeath or tickEffects.",
								"Recommended: on."))
						.option(slider("Living anim distance", () -> (int) cfg.livingAnimThrottleDistance, v -> cfg.livingAnimThrottleDistance = v, 36, 8, 128, "blocks",
								"Distance at which pose / limb-swing work is eligible to skip.",
								"Default 36 blocks."))
						.option(toggle("Item Spin Throttle", () -> cfg.itemSpinThrottleEnabled, v -> cfg.itemSpinThrottleEnabled = v, true,
								"Freezes dropped-item rotation bobbing while Performance Mode is active, or for items past the spin distance under load.",
								"Pickup and physics are unchanged."))
						.option(slider("Item Spin Distance", () -> (int) cfg.itemSpinThrottleDistance, v -> cfg.itemSpinThrottleDistance = v, 12, 2, 64, "blocks",
								"Beyond this distance, item spin is eligible to freeze under load.",
								"Default 12 blocks."))
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Block entities").withStyle(ChatFormatting.AQUA))
						.option(toggle("Block-Entity Culling", () -> cfg.blockEntityCullingEnabled, v -> { cfg.blockEntityCullingEnabled = v; HotPath.rebuild(cfg); }, true,
								"Stops rendering chests, signs, furnaces, banners, and skulls past the block-entity distance.",
								"Recommended: on in storage rooms."))
						.option(slider("Block-Entity Distance", () -> (int) cfg.maxBlockEntityRenderDistance, v -> cfg.maxBlockEntityRenderDistance = v, 24, 4, 128, "blocks",
								"Block entities past this distance are not submitted to the renderer.",
								"Default 24 blocks."))
						.option(toggle("Block-Entity LOD", () -> cfg.blockEntityLodEnabled, v -> cfg.blockEntityLodEnabled = v, true,
								"Distant tile entities switch to a cheaper presentation before they are fully culled.",
								"Recommended: on."))
						.option(slider("Block-Entity LOD Distance", () -> (int) cfg.blockEntityLodDistance, v -> cfg.blockEntityLodDistance = v, 14, 4, 128, "blocks",
								"Distance at which the cheaper block-entity pass begins.",
								"Default 14 blocks."))
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Overlays").withStyle(ChatFormatting.AQUA))
						.collapsed(true)
						.option(toggle("Shadow Culling", () -> cfg.shadowCullingEnabled, v -> { cfg.shadowCullingEnabled = v; HotPath.rebuild(cfg); }, true,
								"Skips the ground-blob shadow pass for entities farther than the shadow distance.",
								"Recommended: on."))
						.option(slider("Shadow Distance", () -> (int) cfg.maxShadowDistance, v -> cfg.maxShadowDistance = v, 12, 1, 64, "blocks",
								"Entity shadows beyond this distance are not drawn.",
								"Default 12 blocks."))
						.option(toggle("Name-Tag Culling", () -> cfg.nameTagCullEnabled, v -> { cfg.nameTagCullEnabled = v; HotPath.rebuild(cfg); }, true,
								"Hides floating name tags once the owner is past the name-tag distance.",
								"Recommended: on in multiplayer hubs."))
						.option(slider("Name-Tag Distance", () -> (int) cfg.maxNameTagDistance, v -> cfg.maxNameTagDistance = v, 24, 2, 128, "blocks",
								"Name tags farther than this are not drawn.",
								"Default 24 blocks."))
						.option(toggle("Glow-Outline Culling", () -> cfg.glowOutlineCullingEnabled, v -> { cfg.glowOutlineCullingEnabled = v; HotPath.rebuild(cfg); }, true,
								"Skips the glowing-entity outline pass past the glow distance. The entity mesh can still render.",
								"Recommended: on."))
						.option(slider("Glow-Outline Distance", () -> (int) cfg.maxGlowOutlineDistance, v -> cfg.maxGlowOutlineDistance = v, 28, 4, 128, "blocks",
								"Glow outlines beyond this distance are not submitted.",
								"Default 28 blocks."))
						.build())
				.build();
	}

	private static ConfigCategory particles(HSNConfig cfg) {
		return ConfigCategory.createBuilder()
				.name(Component.literal("Particles").withStyle(ChatFormatting.GOLD))
				.tooltip(Component.literal("Budget, distance, keep-rates, and ambient-tick filters."))
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Budget").withStyle(ChatFormatting.AQUA))
						.option(toggle("Particle Culling", () -> cfg.particleCullingEnabled, v -> { cfg.particleCullingEnabled = v; HotPath.rebuild(cfg); }, true,
								"Enforces the particle budget and maximum particle distance so rain, smoke, and splash effects cannot flood the particle manager.",
								"Recommended: on."))
						.option(slider("Particle Budget", () -> cfg.maxParticles, v -> cfg.maxParticles = v, 400, 10, 2000, "",
								"Hard cap on how many particles may remain alive in one frame after priority and distance filters.",
								"Default 400."))
						.option(slider("Particle Distance", () -> (int) cfg.maxParticleDistance, v -> cfg.maxParticleDistance = v, 16, 2, 64, "blocks",
								"Particles spawned farther than this are discarded before they are ticked or drawn. A zero slider is treated as infinity.",
								"Default 16 blocks."))
						.option(toggle("Hard particle cap", () -> cfg.hardParticleCapEnabled, v -> { cfg.hardParticleCapEnabled = v; HotPath.rebuild(cfg); }, true,
								"Hard-caps the live particle list each tick after spawn filters. The live counter only moves when this is on. Turning does not pop already-spawned particles.",
								"Recommended: on."))
						.option(toggle("Particle Quality Curve", () -> cfg.particleQualityCurveEnabled, v -> { cfg.particleQualityCurveEnabled = v; HotPath.rebuild(cfg); }, true,
								"Retains fewer particles as they approach the maximum particle distance.",
								"Recommended: on."))
						.option(toggle("Particle Priority", () -> cfg.particlePriorityEnabled, v -> { cfg.particlePriorityEnabled = v; HotPath.rebuild(cfg); }, true,
								"When the budget is exceeded, combat and player-owned effects are kept ahead of rain and decoration wisps.",
								"Recommended: on."))
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Keep-rates").withStyle(ChatFormatting.AQUA))
						.collapsed(true)
						.option(percent("Rain Keep Rate", () -> cfg.rainKeepChance, v -> cfg.rainKeepChance = v, 15, 0, 100,
								"Chance that an individual rain-splash particle is kept.",
								"Default 15%."))
						.option(percent("Smoke Keep Rate", () -> cfg.smokeKeepChance, v -> cfg.smokeKeepChance = v, 25, 0, 100,
								"Keep chance for campfire and furnace smoke.",
								"Default 25%."))
						.option(percent("Explosion Keep Rate", () -> cfg.explosionKeepChance, v -> cfg.explosionKeepChance = v, 100, 0, 100,
								"Keep chance for explosion bursts.",
								"Default 100%."))
						.option(percent("Fire Keep Rate", () -> cfg.fireSmokeKeepChance, v -> cfg.fireSmokeKeepChance = v, 100, 0, 100,
								"Keep chance for fire and lava sparks.",
								"Default 100%."))
						.option(percent("Bubble Keep Rate", () -> cfg.bubbleKeepChance, v -> cfg.bubbleKeepChance = v, 100, 0, 100,
								"Keep chance for underwater bubbles.",
								"Default 100%."))
						.option(percent("High-Priority Keep Rate", () -> cfg.highPriorityKeepChance, v -> cfg.highPriorityKeepChance = v, 85, 10, 100,
								"Keep chance for combat and player-owned particles while Particle Priority is enabled.",
								"Default 85%."))
						.option(percent("Low-Priority Keep Rate", () -> cfg.lowPriorityKeepChance, v -> cfg.lowPriorityKeepChance = v, 25, 0, 100,
								"Keep chance for decoration particles when the budget is tight.",
								"Default 25%."))
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Special sources").withStyle(ChatFormatting.AQUA))
						.option(toggle("Firework particle cap", () -> cfg.fireworkParticleCapEnabled, v -> { cfg.fireworkParticleCapEnabled = v; HotPath.rebuild(cfg); }, true,
								"Caps firework burst particles independently of the global budget.",
								"Recommended: on during firework shows."))
						.option(slider("Max firework particles / tick", () -> cfg.maxFireworkParticlesPerTick, v -> cfg.maxFireworkParticlesPerTick = v, 48, 4, 200, "",
								"Independent firework spark budget used only while the firework cap is on.",
								"Default 48."))
						.option(toggle("Drip particle throttle", () -> cfg.dripParticleThrottleEnabled, v -> { cfg.dripParticleThrottleEnabled = v; HotPath.rebuild(cfg); }, true,
								"Skip dripping / falling block particles underground or far away.",
								"Recommended: on."))
						.option(toggle("Ambient tick cull", () -> cfg.ambientTickCullEnabled, v -> { cfg.ambientTickCullEnabled = v; HotPath.rebuild(cfg); }, true,
								"Throttle ClientLevel.animateTick torch / drip / spore / furnace ambience. Does not change block updates or combat.",
								"Recommended: on."))
						.option(slider("Ambient tick interval", () -> cfg.ambientTickInterval, v -> cfg.ambientTickInterval = v, 2, 1, 8, "ticks",
								"How often the ambient block-particle walk runs. 1 is vanilla.",
								"Default 2 ticks.", FPS_LOWER_LOW))
						.option(slider("Ambient tick range", () -> cfg.ambientTickRange, v -> cfg.ambientTickRange = v, 12, 4, 16, "blocks",
								"How far the ambient walk looks. Vanilla is about 16.",
								"Default 12 blocks."))
						.build())
				.build();
	}

	private static ConfigCategory rendering(HSNConfig cfg) {
		return ConfigCategory.createBuilder()
				.name(Component.literal("Rendering").withStyle(ChatFormatting.GOLD))
				.tooltip(Component.literal("World render scale, terrain mask, LOD, textures, fog, and sky."))
				.group(OptionGroup.createBuilder()
						.name(Component.literal("World render scale").withStyle(ChatFormatting.AQUA))
						.option(toggle("World render scale", () -> cfg.renderScaleEnabled, v -> cfg.renderScaleEnabled = v, true,
								"Draw the world at a separate resolution. The HUD stays native. Disabled when Resolution Control or RenderScale is present.",
								"Default: on at 75%."))
						.option(percent("Render scale", () -> cfg.renderScale, v -> cfg.renderScale = v, 75, 25, 200,
								"Size of the 3D framebuffer relative to the window. 100% is native.",
								"Default 100%."))
						.option(Option.<ScaleFilter>createBuilder()
								.name(Component.literal("Scale filter"))
								.description(desc("How the scaled 3D buffer is sampled back onto the native HUD framebuffer.",
										"Linear is softer. Nearest keeps a pixel look below native resolution.", FPS_NONE))
								.binding(ScaleFilter.LINEAR, () -> cfg.renderScaleFilter, v -> cfg.renderScaleFilter = v)
								.controller(opt -> EnumControllerBuilder.create(opt).enumClass(ScaleFilter.class))
								.build())
						.option(toggle("Adaptive scale", () -> cfg.renderScaleAdaptive, v -> cfg.renderScaleAdaptive = v, true,
								"Lower world resolution only while smoothed FPS is at or below the threshold.",
								"Default: on."))
						.option(slider("Adaptive scale below FPS", () -> cfg.adaptiveRenderScaleFps, v -> cfg.adaptiveRenderScaleFps = v, 45, 15, 120, "FPS",
								"When smoothed FPS is at or below this value, world scale eases toward the scale amount.",
								"Default 45 FPS."))
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Terrain mask").withStyle(ChatFormatting.AQUA))
						.option(Option.<WorldRenderShape>createBuilder()
								.name(Component.literal("World shape"))
								.description(desc(
										"One control for the drawn world outline. Off is vanilla square. Circle and hexagon trim far corners. Front half keeps terrain ahead of the camera.",
										"Changing this also sets circularRenderingEnabled, so the mask cannot be left half-on. Ignored while a dedicated world-shape mod is present.",
										"Tighter shapes raise FPS by drawing fewer far sections."))
								.binding(WorldRenderShape.OFF, () -> cfg.worldRenderShape, v -> {
									if (!HSNModCompat.shapeModPresent()) {
										cfg.worldRenderShape = v;
										cfg.circularRenderingEnabled = v != WorldRenderShape.OFF;
										HotPath.rebuild(cfg);
									}
								})
								.controller(opt -> EnumControllerBuilder.create(opt).enumClass(WorldRenderShape.class))
								.build())
						.option(percent("Shape radius", () -> cfg.circularRadiusScale, v -> {
									if (!HSNModCompat.shapeModPresent()) {
										cfg.circularRadiusScale = v;
									}
								}, 100, 25, 100,
								"Portion of the current view distance covered by the mask.",
								"Default 100%."))
						.option(slider("Always-keep chunks", () -> cfg.alwaysKeepChunks, v -> cfg.alwaysKeepChunks = v, 3, 2, 8, "chunks",
								"Chunk radius around the camera that the mask is forbidden to hide.",
								"Default 3 chunks."))
						.option(toggle("Vertical range limit", () -> cfg.circularVerticalRangeEnabled, v -> cfg.circularVerticalRangeEnabled = v, false,
								"Also clips the mask on the Y axis so sections far above or below the camera are not drawn.",
								"Default: off."))
						.option(slider("Vertical range", () -> cfg.circularVerticalRange, v -> cfg.circularVerticalRange = v, 16, 1, 64, "sections",
								"How far above and below the camera the mask may still draw.",
								"Default 16 sections."))
						.option(toggle("Horizon Y cull", () -> cfg.horizonYCullEnabled, v -> { cfg.horizonYCullEnabled = v; HotPath.rebuild(cfg); }, true,
								"Skip sections whose top Y is below the lowest world Y the camera can see this frame.",
								"Ground under the camera is kept by the keep-below slider."))
						.option(slider("Horizon keep-below", () -> (int) cfg.horizonYKeepBelow, v -> cfg.horizonYKeepBelow = v, 24, 8, 96, "blocks",
								"Blocks kept under the camera so the ground at your feet never pops.",
								"Default 24 blocks."))
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Level of detail").withStyle(ChatFormatting.AQUA))
						.option(toggle("Progressive LOD", () -> cfg.progressiveLodEnabled, v -> cfg.progressiveLodEnabled = v, true,
								"Scales presentation quality down as an object approaches its cull distance.",
								"Recommended: on."))
						.option(percent("LOD Start", () -> cfg.progressiveLodStart, v -> cfg.progressiveLodStart = v, 50, 15, 95,
								"Share of the maximum distance that stays at full quality.",
								"Default 50%."))
						.option(percent("Minimum LOD Quality", () -> cfg.progressiveLodMinQuality, v -> cfg.progressiveLodMinQuality = v, 15, 5, 100,
								"Quality reserved for objects sitting just inside the cull distance.",
								"Default 15%."))
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Textures").withStyle(ChatFormatting.AQUA))
						.option(toggle("Block Texture LOD", () -> cfg.blockTextureLodEnabled, v -> { cfg.blockTextureLodEnabled = v; HotPath.rebuild(cfg); }, true,
								"Applies a mip bias to the blocks atlas while the world is drawn. Video Settings mipmaps must be enabled.",
								"Recommended: on."))
						.option(slider("Texture LOD Bias", () -> (int) Math.round(cfg.blockTextureLodBias * 100), v -> cfg.blockTextureLodBias = v / 100.0, 125, 0, 300, "",
								"0 keeps vanilla sharpness. Larger values sample blurrier mips on the horizon.",
								"Default 125.", FPS_LOWER_LOW))
						.option(toggle("Adaptive Texture LOD", () -> cfg.blockTextureLodAdaptive, v -> cfg.blockTextureLodAdaptive = v, true,
								"Raises the mip bias automatically while smoothed frame rate is below the target.",
								"Recommended: on."))
						.option(toggle("Animated Texture Throttle", () -> cfg.textureAnimThrottleEnabled, v -> cfg.textureAnimThrottleEnabled = v, true,
								"Updates water, lava, portal, and fire atlas frames less often when the client is under load.",
								"Animation still plays; it just steps more slowly."))
						.option(toggle("Adaptive Texture Interval", () -> cfg.textureAnimUseAdaptive, v -> cfg.textureAnimUseAdaptive = v, true,
								"Chooses the atlas update interval from live frame rate.",
								"Recommended: on."))
						.option(slider("Texture Interval", () -> cfg.textureAnimInterval, v -> cfg.textureAnimInterval = v, 1, 1, 16, "ticks",
								"Atlas update interval when frame rate is healthy. 1 tick is vanilla.",
								"Default 1 tick.", FPS_LOWER_LOW))
						.option(slider("Texture Interval (Load)", () -> cfg.textureAnimMaxInterval, v -> cfg.textureAnimMaxInterval = v, 4, 1, 16, "ticks",
								"Slowest atlas update interval when frame rate is below target.",
								"Default 4 ticks.", FPS_LOWER_LOW))
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Fog, sky, extras").withStyle(ChatFormatting.AQUA))
						.collapsed(true)
						.option(toggle("Fog Scale", () -> cfg.fogScaleEnabled, v -> cfg.fogScaleEnabled = v, false,
								"Shortens vanilla fog so the far plane fades sooner.",
								"Default: off. Prefer Sodium Extra fog when that mod is loaded."))
						.option(percent("Fog Scale Factor", () -> cfg.fogScaleFactor, v -> cfg.fogScaleFactor = v, 85, 35, 100,
								"Multiplier on vanilla fog distance while Fog Scale is enabled.",
								"100% is unchanged."))
						.option(toggle("Lightmap cache", () -> cfg.lightmapCacheEnabled, v -> { cfg.lightmapCacheEnabled = v; HotPath.rebuild(cfg); }, true,
								"Skip LightTexture rebuilds when gamma, effects, and dimension did not change.",
								"Recommended: on."))
						.option(toggle("Cloud LOD", () -> cfg.cloudLodEnabled, v -> { cfg.cloudLodEnabled = v; HotPath.rebuild(cfg); }, true,
								"Cheapen / skip far cloud layers when the camera is not looking up or the sky is not visible.",
								"Recommended: on."))
						.option(toggle("Weather renderer LOD", () -> cfg.weatherRendererLodEnabled, v -> { cfg.weatherRendererLodEnabled = v; HotPath.rebuild(cfg); }, true,
								"Thin weather overlay work when FPS is healthy, the player is underground, or the sky is not visible.",
								"Recommended: on."))
						.option(toggle("Sky extras throttle", () -> cfg.skyExtrasThrottleEnabled, v -> cfg.skyExtrasThrottleEnabled = v, false,
								"Skip star / sunrise extras when looking down or under a ceiling.",
								"Default: off."))
						.option(toggle("World-border LOD", () -> cfg.worldBorderLodEnabled, v -> cfg.worldBorderLodEnabled = v, true,
								"Skip the world-border mesh when the camera is far inside the border.",
								"Recommended: on."))
						.option(toggle("Beacon-Beam Culling", () -> cfg.beaconBeamCullingEnabled, v -> { cfg.beaconBeamCullingEnabled = v; HotPath.rebuild(cfg); }, true,
								"Hides beacon beams that start farther than the beacon distance. The beacon block itself is unaffected.",
								"Recommended: on."))
						.option(slider("Beacon-Beam Distance", () -> (int) cfg.maxBeaconBeamDistance, v -> cfg.maxBeaconBeamDistance = v, 48, 8, 256, "blocks",
								"Beacon beams originating past this distance are not drawn.",
								"Default 48 blocks."))
						.option(toggle("Map renderer throttle", () -> cfg.mapRendererThrottleEnabled, v -> cfg.mapRendererThrottleEnabled = v, true,
								"Rebuild filled-map textures less often.",
								"Recommended: on."))
						.option(slider("Map rebuild interval", () -> cfg.mapRendererInterval, v -> cfg.mapRendererInterval = v, 4, 1, 20, "ticks",
								"How often a filled map is allowed to rebuild its texture.",
								"Default 4 ticks.", FPS_LOWER_LOW))
						.build())
				.build();
	}

	private static ConfigCategory audio(HSNConfig cfg) {
		return ConfigCategory.createBuilder()
				.name(Component.literal("Audio").withStyle(ChatFormatting.GOLD))
				.tooltip(Component.literal("Sounds are distance-culled only. No frustum test."))
				.option(toggle("Sound Distance Culling", () -> cfg.soundDistanceCullingEnabled, v -> { cfg.soundDistanceCullingEnabled = v; HotPath.rebuild(cfg); }, true,
						"Prevents new sound instances from starting beyond the sound distance. Already-playing clips are not cut. Distance only — no frustum test.",
						"Recommended: on."))
				.option(slider("Sound Distance", () -> (int) cfg.maxSoundDistance, v -> cfg.maxSoundDistance = v, 48, 4, 128, "blocks",
						"New sounds whose source is farther than this are never started.",
						"Default 48 blocks."))
				.option(toggle("Weather Sound Reduction", () -> cfg.weatherSoundReductionEnabled, v -> cfg.weatherSoundReductionEnabled = v, false,
						"Keeps only a configurable fraction of rain and thunder loop attempts.",
						"Default: off."))
				.option(percent("Weather Keep Rate", () -> cfg.weatherSoundKeepChance, v -> cfg.weatherSoundKeepChance = v, 20, 0, 100,
						"Probability that an individual weather sound is allowed to start while reduction is enabled.",
						"Default 20%."))
				.option(toggle("Sound Burst Limit", () -> cfg.soundBurstLimitEnabled, v -> cfg.soundBurstLimitEnabled = v, false,
						"Caps how many brand-new sounds may begin in one tick. The burst counter increments after the sound is allowed.",
						"Default: off."))
				.option(slider("Maximum New Sounds", () -> cfg.maxNewSoundsPerTick, v -> cfg.maxNewSoundsPerTick = v, 24, 1, 64, "",
						"Burst cap used only while Sound Burst Limit is enabled.",
						"Default 24."))
				.build();
	}

	private static ConfigCategory server(HSNConfig cfg) {
		return ConfigCategory.createBuilder()
				.name(Component.literal("Server").withStyle(ChatFormatting.GOLD))
				.tooltip(Component.literal("Integrated-server helpers. Harmless on a dedicated server."))
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Safety").withStyle(ChatFormatting.AQUA))
						.option(toggle("Integrated Server Only", () -> cfg.integratedServerOnly, v -> cfg.integratedServerOnly = v, true,
								"Keeps pathfinding, idle-AI, and item-tick extras off on dedicated / remote sessions. Unknown sessions are treated as remote.",
								"Recommended: on.", FPS_NONE))
						.option(toggle("Defer Pathfinding to Lithium", () -> cfg.deferPathfindingToLithium, v -> cfg.deferPathfindingToLithium = v, true,
								"Skips HSN path throttling when Lithium is loaded.",
								"Recommended: on.", FPS_COMPAT))
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("AI and items").withStyle(ChatFormatting.AQUA))
						.option(toggle("Pathfinding Throttle", () -> cfg.pathfindingThrottleEnabled, v -> cfg.pathfindingThrottleEnabled = v, true,
								"Distant idle mobs reuse their current path instead of rebuilding every tick. Combat and close mobs are left alone.",
								"Recommended: on in singleplayer."))
						.option(slider("Pathfinding Full-Rate Distance", () -> (int) cfg.pathfindingFullDistance, v -> cfg.pathfindingFullDistance = v, 32, 8, 128, "blocks",
								"Inside this radius, pathfinding runs at vanilla rate.",
								"Default 32 blocks."))
						.option(slider("Pathfinding Maximum Interval", () -> cfg.pathfindingMaxInterval, v -> cfg.pathfindingMaxInterval = v, 8, 2, 40, "ticks",
								"Longest delay between path rebuilds for distant idle mobs.",
								"Default 8 ticks.", FPS_LOWER_LOW))
						.option(toggle("Idle AI throttle", () -> cfg.idleAiThrottleEnabled, v -> { cfg.idleAiThrottleEnabled = v; HotPath.rebuild(cfg); }, true,
								"Skip idle GoalSelector evaluation for far, non-combat mobs on the integrated server.",
								"Recommended: on in singleplayer."))
						.option(slider("Idle AI full distance", () -> (int) cfg.idleAiFullDistance, v -> cfg.idleAiFullDistance = v, 48, 8, 128, "blocks",
								"Inside this radius, idle AI runs at vanilla rate.",
								"Default 48 blocks."))
						.option(slider("Idle AI max interval", () -> cfg.idleAiMaxInterval, v -> cfg.idleAiMaxInterval = v, 10, 2, 40, "ticks",
								"Longest delay between idle GoalSelector evaluations.",
								"Default 10 ticks.", FPS_LOWER_LOW))
						.option(toggle("Item / XP Tick Throttle", () -> cfg.itemThrottleEnabled, v -> cfg.itemThrottleEnabled = v, false,
								"Reduces physics ticks on dropped items and XP orbs farther than the throttle distance.",
								"Default: off."))
						.option(slider("Item Throttle Distance", () -> (int) cfg.itemThrottleStartDistance, v -> cfg.itemThrottleStartDistance = v, 24, 8, 128, "blocks",
								"Distance at which item and XP physics begin skipping ticks.",
								"Default 24 blocks."))
						.option(slider("Item Tick Interval", () -> cfg.itemThrottleMaxInterval, v -> cfg.itemThrottleMaxInterval = v, 8, 2, 40, "ticks",
								"Physics interval for distant items while throttling is enabled.",
								"Default 8 ticks.", FPS_LOWER_LOW))
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("World open").withStyle(ChatFormatting.AQUA))
						.option(toggle("World-open helper", () -> cfg.worldOpenEnabled, v -> cfg.worldOpenEnabled = v, true,
								"Master for prefetch + extra chunk drain + admit-when-ready.",
								"Turn off if join hitching feels worse, not better."))
						.option(toggle("Accelerated World Load", () -> cfg.fastWorldLoadEnabled, v -> cfg.fastWorldLoadEnabled = v, true,
								"Drains extra chunk tasks for a short window after the integrated server starts.",
								"Join-time only. Not an FPS booster during play.", FPS_NONE))
						.option(slider("World-Load Window", () -> cfg.fastWorldLoadWindowSeconds, v -> cfg.fastWorldLoadWindowSeconds = v, 8, 1, 30, "s",
								"How many seconds after server start the extra chunk drain remains active.",
								"Default 8 seconds.", FPS_NONE))
						.option(slider("World-Load Chunk Boost", () -> cfg.fastWorldLoadChunkBoost, v -> cfg.fastWorldLoadChunkBoost = v, 12, 1, 32, "",
								"Extra chunk tasks drained per tick during the load window.",
								"Default 12.", FPS_NONE))
						.option(toggle("Prefetch region files", () -> cfg.prefetchRegionFiles, v -> cfg.prefetchRegionFiles = v, true,
								"Prefetch region files when a world opens so the OS cache is warm.",
								"Recommended: on."))
						.option(toggle("Admit play when ready", () -> cfg.admitPlayWhenReady, v -> cfg.admitPlayWhenReady = v, true,
								"Close joining overlays after the local player exists. Integrated / LAN only. Remote multiplayer keeps vanilla ReceivingLevelScreen. Generic dirt screens are not closed as join overlays.",
								"Recommended: on for singleplayer.", FPS_NONE))
						.option(toggle("Locate Cache", () -> cfg.locateOptimizeEnabled, v -> cfg.locateOptimizeEnabled = v, true,
								"Reuses recent /locate results for the configured TTL.",
								"Affects command hitch time more than frame rate."))
						.option(slider("Locate Cache TTL", () -> cfg.locateCacheTtlSeconds, v -> cfg.locateCacheTtlSeconds = v, 30, 5, 300, "s",
								"How long a cached /locate result may be reused.",
								"Default 30 seconds.", FPS_NONE))
						.build())
				.build();
	}

	private static ConfigCategory extra(HSNConfig cfg) {
		return ConfigCategory.createBuilder()
				.name(Component.literal("Advanced").withStyle(ChatFormatting.GOLD))
				.tooltip(Component.literal("Natives, Sodium Extra deferrals, editor compat."))
				.group(OptionGroup.createBuilder()
						.name(Component.literal("SIMD / native").withStyle(ChatFormatting.AQUA))
						.option(label("CPU: " + cpuSummary(),
								"Requested mode is capped to what this machine actually has. Missing .so files fail open to Java."))
						.option(toggle("Native Hotpath", () -> cfg.nativeHotpathEnabled, v -> { cfg.nativeHotpathEnabled = v; HotPath.rebuild(cfg); }, true,
								"Uses the optional native library for batched distance tests. Missing library or unsupported CPU falls back to Java.",
								"Leave on unless you are isolating a native crash."))
						.option(toggle("Native frustum culling", () -> cfg.nativeFrustumCullingEnabled, v -> { cfg.nativeFrustumCullingEnabled = v; HotPath.rebuild(cfg); }, true,
								"Assembly / C / Rust frustum tests on packed AABBs and spheres. Off = Java frustum path.",
								"Leave on. Fail-open if the library is missing."))
						.option(Option.<SimdMode>createBuilder()
								.name(Component.literal("SIMD Mode"))
								.description(desc(
										"Which vector kernel the native library should prefer.",
										"Auto tries AVX-512, then AVX2, then scalar. Unsupported choices fall back automatically.",
										"This setting never forces an instruction the chip lacks."))
								.binding(SimdMode.AUTO, () -> cfg.simdMode, v -> cfg.simdMode = v)
								.controller(opt -> EnumControllerBuilder.create(opt).enumClass(SimdMode.class))
								.build())
						.option(toggle("Sodium Section Occupancy", () -> cfg.sectionOccupancyCullingEnabled, v -> { cfg.sectionOccupancyCullingEnabled = v; HotPath.rebuild(cfg); }, false,
								"Skips entities whose 16^3 section Sodium did not visit this frame. Fails open if Sodium is missing or the set is empty.",
								"Off by default in 4.0."))
						.option(toggle("Skip empty boss overlay", () -> cfg.skipEmptyBossOverlayEnabled, v -> cfg.skipEmptyBossOverlayEnabled = v, true,
								"Skip boss-bar overlay layout when no boss events are active (BossOverlayMixin).",
								"Recommended: on."))
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Sodium Extra").withStyle(ChatFormatting.AQUA))
						.collapsed(true)
						.option(toggle("Defer Fog to Sodium Extra", () -> cfg.deferFogToSodiumExtra, v -> cfg.deferFogToSodiumExtra = v, false,
								"Disables HSN fog scaling when Sodium Extra is loaded.",
								"Off by default in 4.0. Restart after changing.", FPS_COMPAT))
						.option(toggle("Defer Toasts to Sodium Extra", () -> cfg.deferToastsToSodiumExtra, v -> cfg.deferToastsToSodiumExtra = v, false,
								"Disables HSN toast limiting when Sodium Extra is loaded.",
								"Restart after changing.", FPS_COMPAT))
						.option(toggle("Defer Beacons to Sodium Extra", () -> cfg.deferBeaconToSodiumExtra, v -> cfg.deferBeaconToSodiumExtra = v, false,
								"Disables HSN beacon-beam distance when Sodium Extra is loaded.",
								"Restart after changing.", FPS_COMPAT))
						.option(toggle("Defer Texture Anim to Sodium Extra", () -> cfg.deferTextureAnimToSodiumExtra, v -> cfg.deferTextureAnimToSodiumExtra = v, false,
								"Disables HSN atlas throttling when Sodium Extra animation toggles are present.",
								"Restart after changing.", FPS_COMPAT))
						.option(toggle("Defer Particles to Sodium Extra", () -> cfg.deferParticlesToSodiumExtra, v -> cfg.deferParticlesToSodiumExtra = v, false,
								"HSN still applies distance and budget unless this is enabled.",
								"Restart after changing.", FPS_COMPAT))
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("VoxelSniper / WorldEdit").withStyle(ChatFormatting.AQUA))
						.collapsed(true)
						.option(toggle("VoxelSniper compat", () -> cfg.voxelSniperCompatEnabled, v -> cfg.voxelSniperCompatEnabled = v, true,
								"Soft VoxelSniper / WorldEdit integration. Idle when those mods are absent.",
								"Recommended: on if you build with WE / VS."))
						.option(toggle("Detect WorldEdit", () -> cfg.voxelSniperDetectWorldEdit, v -> cfg.voxelSniperDetectWorldEdit = v, true,
								"Also watch WorldEdit / FAWE tools, not just VoxelSniper.",
								"Recommended: on."))
						.option(toggle("Always scan tools", () -> cfg.voxelSniperAlwaysScanTools, v -> cfg.voxelSniperAlwaysScanTools = v, false,
								"Scan arrow / gunpowder / wand even if the editor mod id was not detected.",
								"Default: off."))
						.option(toggle("Pause horizon cull", () -> cfg.voxelSniperPauseHorizon, v -> cfg.voxelSniperPauseHorizon = v, true,
								"Disable horizon Y while an editor tool is held.",
								"Recommended: on."))
						.option(toggle("Pause terrain mask", () -> cfg.voxelSniperPauseTerrainMask, v -> cfg.voxelSniperPauseTerrainMask = v, true,
								"Disable circular / shape masks while editing.",
								"Recommended: on."))
						.option(toggle("Pause entity cull", () -> cfg.voxelSniperPauseEntityCull, v -> cfg.voxelSniperPauseEntityCull = v, false,
								"Disable entity cull while editing.",
								"Default: off."))
						.option(slider("Editor tool range", () -> (int) cfg.voxelSniperToolRange, v -> cfg.voxelSniperToolRange = v, 128, 16, 384, "blocks",
								"How far the editor-tool scan looks when deciding whether to pause culls.",
								"Default 128 blocks."))
						.build())
				.build();
	}

	private static ConfigCategory fpsHelper(HSNConfig cfg) {
		return ConfigCategory.createBuilder()
				.name(Component.literal("FPS Helper").withStyle(ChatFormatting.GOLD))
				.tooltip(Component.literal("Live FPS snapshot and one-click profile suggestions."))
				.option(label("Live snapshot",
						"Numbers update when you re-open this screen. The Apply button re-reads FPS at click time."))
				.option(label("Current FPS: " + currentFps(), "Vanilla client FPS counter."))
				.option(label("Adaptive scale: " + Math.round(AdaptiveCuller.getScale() * 100.0) + "%",
						"Live distance scale published by Adaptive Culler."))
				.option(label("Last preset: " + (cfg.lastAppliedPreset != null ? cfg.lastAppliedPreset.title() : "CUSTOM"),
						"Written whenever a profile is applied from this menu, the built-in screen, or a hotkey."))
				.option(label("Suggested: " + suggestPreset().title() + "  (" + suggestPreset().audience() + ")",
						"Bands: <30 Ultra Low, <45 Safe, <70 Balanced, <120 Quality, else Competitive."))
				.option(ButtonOption.createBuilder()
						.name(Component.literal("Apply best profile for current FPS"))
						.description(OptionDescription.of(Component.literal(
								"Re-reads FPS right now and applies Ultra Low / Safe / Balanced / Quality / Competitive.")))
						.action((screen, opt) -> {
							Preset best = suggestPreset();
							HSNPresets.apply(HSNConfig.get(), best);
							HotPath.rebuild(HSNConfig.get());
							Minecraft mc = Minecraft.getInstance();
							if (mc.player != null) {
								mc.player.sendSystemMessage(Component.literal(
										"HSN: applied " + best.title() + " for ~" + currentFps() + " FPS"));
							}
						})
						.build())
				.group(OptionGroup.createBuilder()
						.name(Component.literal("Manual presets").withStyle(ChatFormatting.AQUA))
						.option(presetButton("Ultra Low", Preset.ULTRA_LOW))
						.option(presetButton("Safe", Preset.SAFE))
						.option(presetButton("Balanced", Preset.BALANCED))
						.option(presetButton("Quality", Preset.QUALITY))
						.option(presetButton("Competitive", Preset.COMPETITIVE))
						.build())
				.build();
	}

	private static ButtonOption presetButton(String label, Preset preset) {
		return ButtonOption.createBuilder()
				.name(Component.literal(label))
				.description(OptionDescription.of(Component.literal(preset.audience())))
				.action((screen, opt) -> {
					HSNPresets.apply(HSNConfig.get(), preset);
					HotPath.rebuild(HSNConfig.get());
					Minecraft mc = Minecraft.getInstance();
					if (mc.player != null) {
						mc.player.sendSystemMessage(Component.literal("HSN: applied " + preset.title()));
					}
				})
				.build();
	}

	private static int currentFps() {
		Minecraft mc = Minecraft.getInstance();
		return mc != null ? mc.getFps() : 0;
	}

	private static Preset suggestPreset() {
		int fps = currentFps();
		if (fps <= 0) return Preset.BALANCED;
		if (fps < 30) return Preset.ULTRA_LOW;
		if (fps < 45) return Preset.SAFE;
		if (fps < 70) return Preset.BALANCED;
		if (fps < 120) return Preset.QUALITY;
		return Preset.COMPETITIVE;
	}

	private static Option<Component> label(String title, String body) {
		return LabelOption.createBuilder()
				.line(Component.literal(title).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
				.line(Component.literal(body).withStyle(ChatFormatting.GRAY))
				.build();
	}

	private static Option<Boolean> toggle(String name, Supplier<Boolean> get, Consumer<Boolean> set,
			boolean def, String what, String advice) {
		return toggle(name, get, set, def, what, advice, FPS_ON_HIGH);
	}

	private static Option<Boolean> toggle(String name, Supplier<Boolean> get, Consumer<Boolean> set,
			boolean def, String what, String advice, String fps) {
		return Option.<Boolean>createBuilder()
				.name(Component.literal(name))
				.description(desc(what, advice, fps))
				.binding(def, get, set)
				.controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
				.build();
	}

	private static Option<Integer> slider(String name, Supplier<Integer> get, Consumer<Integer> set,
			int def, int min, int max, String unit, String what, String advice) {
		return slider(name, get, set, def, min, max, unit, what, advice, FPS_LOWER_HIGH);
	}

	private static Option<Integer> slider(String name, Supplier<Integer> get, Consumer<Integer> set,
			int def, int min, int max, String unit, String what, String advice, String fps) {
		return HSNYaclSlider.create(name, get, set, def, min, max, unit, what, advice, fps);
	}

	private static Option<Integer> percent(String name, Supplier<Double> get, Consumer<Double> set,
			int def, int min, int max, String what, String advice) {
		return percent(name, get, set, def, min, max, what, advice, FPS_LOWER_HIGH);
	}

	private static Option<Integer> percent(String name, Supplier<Double> get, Consumer<Double> set,
			int def, int min, int max, String what, String advice, String fps) {
		return HSNYaclSlider.percent(name, get, set, def, min, max, what, advice, fps);
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
