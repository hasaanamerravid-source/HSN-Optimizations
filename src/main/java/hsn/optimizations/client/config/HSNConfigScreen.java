package hsn.optimizations.client.config;

import hsn.optimizations.client.compat.ClientScreens;
import hsn.optimizations.config.HSNConfig;
import hsn.optimizations.config.HSNConfig.Preset;
import hsn.optimizations.config.HSNPresets;
import hsn.optimizations.config.ScaleFilter;
import hsn.optimizations.config.SimdMode;
import hsn.optimizations.config.WorldRenderShape;
import hsn.optimizations.optimize.HotPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Built-in editor. No YACL, no Cloth Config.
 * Tabs match the 4.0 settings map: every HSNConfig field is reachable here.
 */
public final class HSNConfigScreen extends Screen {

	private static final int SIDE = 118;
	private static final int HEAD = 34;
	private static final int FOOT = 28;
	private static final int ROW = 22;
	private static final int CTRL = 108;

	public enum Tab {
		GENERAL("General"),
		ENTITIES("Entities"),
		PARTICLES("Particles"),
		RENDERING("Rendering"),
		AUDIO("Audio"),
		SERVER("Server"),
		EXTRA("Advanced");

		final String label;

		Tab(String label) {
			this.label = label;
		}
	}

	private final Screen parent;
	private Tab tab = Tab.GENERAL;
	private int scroll;

	private HSNConfigScreen(Screen parent) {
		super(Component.literal("HSN Optimizations"));
		this.parent = parent;
	}

	public static Screen create(Screen parent) {
		return new HSNConfigScreen(parent);
	}

	@Override
	protected void init() {
		clearWidgets();

		int y = HEAD + 8;
		for (Tab t : Tab.values()) {
			boolean on = t == tab;
			Button b = Button.builder(Component.literal(on ? "> " + t.label : "  " + t.label), btn -> {
				tab = t;
				scroll = 0;
				rebuild();
			}).bounds(8, y, SIDE - 16, 20).build();
			addRenderableWidget(b);
			y += 22;
		}

		int bottom = this.height - FOOT + 4;
		addRenderableWidget(Button.builder(Component.literal("Up"), btn -> {
			scroll = Math.max(0, scroll - ROW * 4);
			rebuild();
		}).bounds(SIDE + 8, bottom, 40, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Down"), btn -> {
			scroll = Math.min(maxScroll(), scroll + ROW * 4);
			rebuild();
		}).bounds(SIDE + 52, bottom, 48, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Balanced"), btn -> {
			resetTab();
			rebuild();
		}).bounds(this.width - 200, bottom, 88, 20)
				.tooltip(Tooltip.create(Component.literal("Write the Balanced profile to every field."))).build());
		addRenderableWidget(Button.builder(Component.literal("Done"), btn -> closeAndSave())
				.bounds(this.width - 104, bottom, 88, 20).build());

		layoutRows();
	}

	private void rebuild() {
		this.rebuildWidgets();
	}

	private int maxScroll() {
		int rows = labelsForTab().length;
		int vis = Math.max(1, (this.height - HEAD - FOOT - 16) / ROW);
		return Math.max(0, (rows - vis) * ROW);
	}

	private int contentLeft() {
		return SIDE + 16;
	}

	private int contentWidth() {
		return Math.min(460, this.width - SIDE - 32);
	}

	private void layoutRows() {
		HSNConfig c = HSNConfig.get();
		int y = HEAD + 12 - scroll;
		int x = contentLeft();
		int w = contentWidth();
		switch (tab) {
			case GENERAL -> {
				y = addToggle(x, y, w, "Enable optimizations", "Master switch. Off restores vanilla behavior.",
						c.modEnabled, v -> {
							c.modEnabled = v;
							HotPath.rebuild(c);
						});
				y = addCycle(x, y, w, "Quality preset", "Applies a complete distance and quality profile.",
						c.lastAppliedPreset, Preset.values(), v -> {
							HSNPresets.apply(c, v);
							c.save();
						});
				y = addToggle(x, y, w, "Performance mode", "When FPS dips, ease distances down to 70% of the sliders.",
						c.performanceModeEnabled, v -> {
							c.performanceModeEnabled = v;
							HotPath.rebuild(c);
						});
				y = addToggle(x, y, w, "Adaptive culling", "Scale distances toward the FPS target.",
						c.adaptiveCullingEnabled, v -> {
							c.adaptiveCullingEnabled = v;
							HotPath.rebuild(c);
						});
				y = addSteps(x, y, w, "Target FPS", c.targetFps, 30, 360, 10,
						v -> c.targetFps = v, v -> v + " fps");
				y = addSteps(x, y, w, "Min adaptive scale", (int) Math.round(c.minAdaptiveScale * 100.0), 25, 100, 5,
						v -> c.minAdaptiveScale = v / 100.0, v -> v + "%");
				y = addToggle(x, y, w, "Frame pacing fix", "Prefer mutable buffers on weak Intel iGPUs.",
						c.framePacingFixEnabled, v -> c.framePacingFixEnabled = v);
				y = addToggle(x, y, w, "Smart yield", "Yield only when the last frame already missed 10 ms.",
						c.smartYieldEnabled, v -> c.smartYieldEnabled = v);
				y = addToggle(x, y, w, "Thread scheduler", "Frame-first thread policy for the render thread.",
						c.schedulerEnabled, v -> c.schedulerEnabled = v);
				y = addToggle(x, y, w, "Low-end hardware tune", "Extra tweaks for integrated GPUs and slow CPUs.",
						c.lowEndHardwareTuneEnabled, v -> c.lowEndHardwareTuneEnabled = v);
				y = addToggle(x, y, w, "Laptop power save", "Cut the work budget when the battery is discharging.",
						c.laptopPowerSaveEnabled, v -> c.laptopPowerSaveEnabled = v);
				y = addToggle(x, y, w, "Weak-GPU auto", "Engage extra cuts after sustained low FPS.",
						c.weakGpuAutoEnabled, v -> c.weakGpuAutoEnabled = v);
				y = addSteps(x, y, w, "Weak-GPU FPS floor", c.weakGpuFpsThreshold, 10, 120, 5,
						v -> c.weakGpuFpsThreshold = v, v -> v + " fps");
				y = addToggle(x, y, w, "Adaptive upload budget", "Reserve a slice of the frame for client work.",
						c.adaptiveUploadBudgetEnabled, v -> c.adaptiveUploadBudgetEnabled = v);
				y = addSteps(x, y, w, "Upload budget", (int) Math.round(c.uploadBudgetFraction * 100.0), 5, 40, 1,
						v -> c.uploadBudgetFraction = v / 100.0, v -> v + "%");
				y = addToggle(x, y, w, "Unfocused cap", "Limit FPS when the window is in the background.",
						c.unfocusedFpsCapEnabled, v -> c.unfocusedFpsCapEnabled = v);
				y = addSteps(x, y, w, "Unfocused FPS", c.unfocusedFpsCap, 5, 240, 5,
						v -> c.unfocusedFpsCap = v, String::valueOf);
				y = addToggle(x, y, w, "Shader safe mode", "Do not fight Iris / Oculus framebuffers.",
						c.shaderSafeMode, v -> c.shaderSafeMode = v);
				y = addToggle(x, y, w, "F3 status line", "Show HSN on the debug overlay.",
						c.f3ShowStatus, v -> c.f3ShowStatus = v);
				y = addToggle(x, y, w, "F3 compact", "Two-line F3 block instead of a long dump.",
						c.f3Compact, v -> c.f3Compact = v);
				y = addToggle(x, y, w, "F3 details", "Show extra counters on the debug overlay.",
						c.f3ShowDetails, v -> c.f3ShowDetails = v);
				y = addToggle(x, y, w, "FPS overlay", "Tiny HUD counter.",
						c.fpsOverlayEnabled, v -> c.fpsOverlayEnabled = v);
				y = addSteps(x, y, w, "FPS overlay X", c.fpsOverlayX, 0, 400, 4,
						v -> c.fpsOverlayX = v, String::valueOf);
				y = addSteps(x, y, w, "FPS overlay Y", c.fpsOverlayY, 0, 400, 4,
						v -> c.fpsOverlayY = v, String::valueOf);
				y = addToggle(x, y, w, "Toast limit", "Cap stacked advancement / recipe toasts.",
						c.toastLimitEnabled, v -> c.toastLimitEnabled = v);
			}
			case ENTITIES -> {
				y = addToggle(x, y, w, "Entity culling", "Skip far entities.",
						c.entityCullingEnabled, v -> {
							c.entityCullingEnabled = v;
							HotPath.rebuild(c);
						});
				y = addSteps(x, y, w, "Entity distance", (int) c.maxEntityRenderDistance, 4, 128, 4,
						v -> c.maxEntityRenderDistance = v, v -> v + " blk");
				y = addSteps(x, y, w, "Item entity", (int) c.maxItemEntityRenderDistance, 4, 96, 2,
						v -> c.maxItemEntityRenderDistance = v, v -> v + " blk");
				y = addSteps(x, y, w, "XP orbs", (int) c.maxXpOrbRenderDistance, 4, 64, 2,
						v -> c.maxXpOrbRenderDistance = v, v -> v + " blk");
				y = addSteps(x, y, w, "Decorations", (int) c.maxDecorationEntityDistance, 4, 96, 2,
						v -> c.maxDecorationEntityDistance = v, v -> v + " blk");
				y = addToggle(x, y, w, "Entity LOD", "Simpler poses on far mobs.",
						c.entityLodStagesEnabled, v -> {
							c.entityLodStagesEnabled = v;
							HotPath.rebuild(c);
						});
				y = addToggle(x, y, w, "Behind-camera cull", "Skip entities behind the camera past half their slider (decorations first).",
						c.behindCameraCullEnabled, v -> {
							c.behindCameraCullEnabled = v;
							HotPath.rebuild(c);
						});
				y = addToggle(x, y, w, "Interp skip", "Stop far-entity interpolation.",
						c.entityInterpSkipEnabled, v -> {
							c.entityInterpSkipEnabled = v;
							HotPath.rebuild(c);
						});
				y = addSteps(x, y, w, "Interp skip distance", (int) c.entityInterpSkipDistance, 8, 128, 4,
						v -> c.entityInterpSkipDistance = v, v -> v + " blk");
				y = addToggle(x, y, w, "Distant client ticks", "Tick far decorations less often.",
						c.distantClientTickSkipEnabled, v -> {
							c.distantClientTickSkipEnabled = v;
							HotPath.rebuild(c);
						});
				y = addSteps(x, y, w, "Distant tick distance", (int) c.distantClientTickDistance, 8, 128, 4,
						v -> c.distantClientTickDistance = v, v -> v + " blk");
				y = addSteps(x, y, w, "Distant tick interval", c.distantClientTickInterval, 2, 20, 1,
						v -> c.distantClientTickInterval = v, v -> "1/" + v);
				y = addToggle(x, y, w, "Living anim throttle", "Skip far limb-swing work.",
						c.livingAnimThrottleEnabled, v -> {
							c.livingAnimThrottleEnabled = v;
							HotPath.rebuild(c);
						});
				y = addSteps(x, y, w, "Living anim distance", (int) c.livingAnimThrottleDistance, 8, 128, 4,
						v -> c.livingAnimThrottleDistance = v, v -> v + " blk");
				y = addToggle(x, y, w, "Item spin", "Stop spinning dropped items in the distance.",
						c.itemSpinThrottleEnabled, v -> c.itemSpinThrottleEnabled = v);
				y = addSteps(x, y, w, "Item spin distance", (int) c.itemSpinThrottleDistance, 2, 64, 2,
						v -> c.itemSpinThrottleDistance = v, v -> v + " blk");
				y = addToggle(x, y, w, "Block-entity culling", "Chests, signs, skulls past the slider.",
						c.blockEntityCullingEnabled, v -> {
							c.blockEntityCullingEnabled = v;
							HotPath.rebuild(c);
						});
				y = addSteps(x, y, w, "Block-entity distance", (int) c.maxBlockEntityRenderDistance, 4, 128, 2,
						v -> c.maxBlockEntityRenderDistance = v, v -> v + " blk");
				y = addToggle(x, y, w, "Block-entity LOD", "Cheaper far block-entity models.",
						c.blockEntityLodEnabled, v -> c.blockEntityLodEnabled = v);
				y = addSteps(x, y, w, "Block-entity LOD start", (int) c.blockEntityLodDistance, 4, 128, 2,
						v -> c.blockEntityLodDistance = v, v -> v + " blk");
				y = addToggle(x, y, w, "Shadows", "Distance-cull entity shadows.",
						c.shadowCullingEnabled, v -> {
							c.shadowCullingEnabled = v;
							HotPath.rebuild(c);
						});
				y = addSteps(x, y, w, "Shadow distance", (int) c.maxShadowDistance, 1, 64, 1,
						v -> c.maxShadowDistance = v, v -> v + " blk");
				y = addToggle(x, y, w, "Name tags", "Hide far name tags.",
						c.nameTagCullEnabled, v -> {
							c.nameTagCullEnabled = v;
							HotPath.rebuild(c);
						});
				y = addSteps(x, y, w, "Name-tag distance", (int) c.maxNameTagDistance, 2, 128, 2,
						v -> c.maxNameTagDistance = v, v -> v + " blk");
				y = addToggle(x, y, w, "Glow outlines", "Distance-cull glowing entities.",
						c.glowOutlineCullingEnabled, v -> {
							c.glowOutlineCullingEnabled = v;
							HotPath.rebuild(c);
						});
				y = addSteps(x, y, w, "Glow distance", (int) c.maxGlowOutlineDistance, 4, 128, 4,
						v -> c.maxGlowOutlineDistance = v, v -> v + " blk");
			}
			case PARTICLES -> {
				y = addToggle(x, y, w, "Particle culling", "Cap and distance-limit particles.",
						c.particleCullingEnabled, v -> {
							c.particleCullingEnabled = v;
							HotPath.rebuild(c);
						});
				y = addSteps(x, y, w, "Particle cap", c.maxParticles, 10, 2000, 10,
						v -> c.maxParticles = v, String::valueOf);
				y = addSteps(x, y, w, "Particle distance", (int) c.maxParticleDistance, 2, 64, 2,
						v -> c.maxParticleDistance = v, v -> v + " blk");
				y = addSteps(x, y, w, "Rain keep", (int) Math.round(c.rainKeepChance * 100.0), 0, 100, 5,
						v -> c.rainKeepChance = v / 100.0, v -> v + "%");
				y = addSteps(x, y, w, "Smoke keep", (int) Math.round(c.smokeKeepChance * 100.0), 0, 100, 5,
						v -> c.smokeKeepChance = v / 100.0, v -> v + "%");
				y = addSteps(x, y, w, "Explosion keep", (int) Math.round(c.explosionKeepChance * 100.0), 0, 100, 5,
						v -> c.explosionKeepChance = v / 100.0, v -> v + "%");
				y = addSteps(x, y, w, "Fire smoke keep", (int) Math.round(c.fireSmokeKeepChance * 100.0), 0, 100, 5,
						v -> c.fireSmokeKeepChance = v / 100.0, v -> v + "%");
				y = addSteps(x, y, w, "Bubble keep", (int) Math.round(c.bubbleKeepChance * 100.0), 0, 100, 5,
						v -> c.bubbleKeepChance = v / 100.0, v -> v + "%");
				y = addToggle(x, y, w, "Particle priority", "Keep combat particles over ambience.",
						c.particlePriorityEnabled, v -> {
							c.particlePriorityEnabled = v;
							HotPath.rebuild(c);
						});
				y = addSteps(x, y, w, "High-priority keep", (int) Math.round(c.highPriorityKeepChance * 100.0), 0, 100, 5,
						v -> c.highPriorityKeepChance = v / 100.0, v -> v + "%");
				y = addSteps(x, y, w, "Low-priority keep", (int) Math.round(c.lowPriorityKeepChance * 100.0), 0, 100, 5,
						v -> c.lowPriorityKeepChance = v / 100.0, v -> v + "%");
				y = addToggle(x, y, w, "Quality curve", "Fade keep-chance with distance.",
						c.particleQualityCurveEnabled, v -> {
							c.particleQualityCurveEnabled = v;
							HotPath.rebuild(c);
						});
				y = addToggle(x, y, w, "Hard particle cap", "Refuse new particles over the budget.",
						c.hardParticleCapEnabled, v -> {
							c.hardParticleCapEnabled = v;
							HotPath.rebuild(c);
						});
				y = addToggle(x, y, w, "Firework cap", "Limit firework spark spawn per tick.",
						c.fireworkParticleCapEnabled, v -> {
							c.fireworkParticleCapEnabled = v;
							HotPath.rebuild(c);
						});
				y = addSteps(x, y, w, "Firework sparks / tick", c.maxFireworkParticlesPerTick, 4, 200, 4,
						v -> c.maxFireworkParticlesPerTick = v, String::valueOf);
				y = addToggle(x, y, w, "Drip throttle", "Thin dripping water / lava particles.",
						c.dripParticleThrottleEnabled, v -> {
							c.dripParticleThrottleEnabled = v;
							HotPath.rebuild(c);
						});
				y = addToggle(x, y, w, "Ambient block ticks", "Space out torch flame / drip scans.",
						c.ambientTickCullEnabled, v -> {
							c.ambientTickCullEnabled = v;
							HotPath.rebuild(c);
						});
				y = addSteps(x, y, w, "Ambient tick interval", c.ambientTickInterval, 1, 8, 1,
						v -> c.ambientTickInterval = v, v -> "1/" + v);
				y = addSteps(x, y, w, "Ambient tick range", c.ambientTickRange, 4, 16, 1,
						v -> c.ambientTickRange = v, v -> v + " blk");
			}
			case RENDERING -> {
				y = addToggle(x, y, w, "World render scale", "Draw the world at a lower resolution. The HUD stays native.",
						c.renderScaleEnabled, v -> c.renderScaleEnabled = v);
				y = addSteps(x, y, w, "Render scale", (int) Math.round(c.renderScale * 100.0), 25, 200, 5,
						v -> c.renderScale = v / 100.0, v -> v + "%");
				y = addCycle(x, y, w, "Scale filter", "Linear is softer. Nearest is sharp.",
						c.renderScaleFilter, ScaleFilter.values(), v -> c.renderScaleFilter = v);
				y = addToggle(x, y, w, "Adaptive scale", "Lower world resolution when FPS is at or below the threshold.",
						c.renderScaleAdaptive, v -> c.renderScaleAdaptive = v);
				y = addSteps(x, y, w, "Adaptive scale below FPS", c.adaptiveRenderScaleFps, 15, 120, 5,
						v -> c.adaptiveRenderScaleFps = v, v -> v + " fps");
				y = addToggle(x, y, w, "Fog scale", "Shorten fog when HSN owns it.",
						c.fogScaleEnabled, v -> c.fogScaleEnabled = v);
				y = addSteps(x, y, w, "Fog scale factor", (int) Math.round(c.fogScaleFactor * 100.0), 35, 100, 5,
						v -> c.fogScaleFactor = v / 100.0, v -> v + "%");
				y = addCycle(x, y, w, "World shape", "Off / Circle / Hexagon / Front half. Off = square vanilla.",
						c.worldRenderShape, WorldRenderShape.values(), v -> {
							c.worldRenderShape = v;
							c.circularRenderingEnabled = v != WorldRenderShape.OFF;
							HotPath.rebuild(c);
						});
				y = addSteps(x, y, w, "Shape radius", (int) Math.round(c.circularRadiusScale * 100.0), 25, 100, 5,
						v -> c.circularRadiusScale = v / 100.0, v -> v + "%");
				y = addSteps(x, y, w, "Always-keep chunks", c.alwaysKeepChunks, 2, 8, 1,
						v -> c.alwaysKeepChunks = v, v -> v + " ch");
				y = addToggle(x, y, w, "Vertical range", "Drop sections outside a Y window around the camera.",
						c.circularVerticalRangeEnabled, v -> c.circularVerticalRangeEnabled = v);
				y = addSteps(x, y, w, "Vertical range size", c.circularVerticalRange, 1, 64, 1,
						v -> c.circularVerticalRange = v, v -> v + " sec");
				y = addToggle(x, y, w, "Horizon Y", "Drop sections below the visible floor.",
						c.horizonYCullEnabled, v -> {
							c.horizonYCullEnabled = v;
							HotPath.rebuild(c);
						});
				y = addSteps(x, y, w, "Horizon keep-below", (int) c.horizonYKeepBelow, 8, 96, 4,
						v -> c.horizonYKeepBelow = v, v -> v + " blk");
				y = addToggle(x, y, w, "Progressive LOD", "Fade quality with distance.",
						c.progressiveLodEnabled, v -> c.progressiveLodEnabled = v);
				y = addSteps(x, y, w, "LOD start", (int) Math.round(c.progressiveLodStart * 100.0), 15, 95, 5,
						v -> c.progressiveLodStart = v / 100.0, v -> v + "%");
				y = addSteps(x, y, w, "LOD min quality", (int) Math.round(c.progressiveLodMinQuality * 100.0), 5, 100, 5,
						v -> c.progressiveLodMinQuality = v / 100.0, v -> v + "%");
				y = addToggle(x, y, w, "Block texture LOD", "Bias terrain mips while the world draws.",
						c.blockTextureLodEnabled, v -> {
							c.blockTextureLodEnabled = v;
							HotPath.rebuild(c);
						});
				y = addSteps(x, y, w, "Texture mip bias", (int) Math.round(c.blockTextureLodBias * 10.0), 0, 30, 1,
						v -> c.blockTextureLodBias = v / 10.0, v -> String.format("%.1f", v / 10.0));
				y = addToggle(x, y, w, "Adaptive texture LOD", "Raise mip bias when FPS is low.",
						c.blockTextureLodAdaptive, v -> c.blockTextureLodAdaptive = v);
				y = addToggle(x, y, w, "Texture anim", "Slow atlas animation under load.",
						c.textureAnimThrottleEnabled, v -> c.textureAnimThrottleEnabled = v);
				y = addToggle(x, y, w, "Adaptive texture anim", "Stretch the anim interval when FPS dips.",
						c.textureAnimUseAdaptive, v -> c.textureAnimUseAdaptive = v);
				y = addSteps(x, y, w, "Anim interval", c.textureAnimInterval, 1, 16, 1,
						v -> c.textureAnimInterval = v, v -> "1/" + v + "f");
				y = addSteps(x, y, w, "Anim max interval", c.textureAnimMaxInterval, 1, 16, 1,
						v -> c.textureAnimMaxInterval = v, v -> "1/" + v + "f");
				y = addToggle(x, y, w, "Lightmap cache", "Skip light-texture rebuilds when nothing changed.",
						c.lightmapCacheEnabled, v -> {
							c.lightmapCacheEnabled = v;
							HotPath.rebuild(c);
						});
				y = addToggle(x, y, w, "Cloud LOD", "Skip clouds when looking down or underground.",
						c.cloudLodEnabled, v -> {
							c.cloudLodEnabled = v;
							HotPath.rebuild(c);
						});
				y = addToggle(x, y, w, "Weather LOD", "Skip rain under a ceiling or underground.",
						c.weatherRendererLodEnabled, v -> {
							c.weatherRendererLodEnabled = v;
							HotPath.rebuild(c);
						});
				y = addToggle(x, y, w, "Sky extras", "Skip stars / sunrise when looking down.",
						c.skyExtrasThrottleEnabled, v -> c.skyExtrasThrottleEnabled = v);
				y = addToggle(x, y, w, "World border", "Skip the border mesh when you are far inside.",
						c.worldBorderLodEnabled, v -> c.worldBorderLodEnabled = v);
				y = addToggle(x, y, w, "Beacon beams", "Distance-cull beams.",
						c.beaconBeamCullingEnabled, v -> {
							c.beaconBeamCullingEnabled = v;
							HotPath.rebuild(c);
						});
				y = addSteps(x, y, w, "Beacon distance", (int) c.maxBeaconBeamDistance, 8, 256, 8,
						v -> c.maxBeaconBeamDistance = v, v -> v + " blk");
				y = addToggle(x, y, w, "Map renderer", "Throttle filled-map redraws.",
						c.mapRendererThrottleEnabled, v -> c.mapRendererThrottleEnabled = v);
				y = addSteps(x, y, w, "Map rebuild interval", c.mapRendererInterval, 1, 20, 1,
						v -> c.mapRendererInterval = v, v -> "1/" + v + "f");
			}
			case AUDIO -> {
				y = addToggle(x, y, w, "Sound distance", "Skip far one-shot sounds.",
						c.soundDistanceCullingEnabled, v -> {
							c.soundDistanceCullingEnabled = v;
							HotPath.rebuild(c);
						});
				y = addSteps(x, y, w, "Max sound distance", (int) c.maxSoundDistance, 4, 128, 4,
						v -> c.maxSoundDistance = v, v -> v + " blk");
				y = addToggle(x, y, w, "Sound burst limit", "Cap new sounds started in one tick.",
						c.soundBurstLimitEnabled, v -> c.soundBurstLimitEnabled = v);
				y = addSteps(x, y, w, "Max new sounds / tick", c.maxNewSoundsPerTick, 1, 64, 1,
						v -> c.maxNewSoundsPerTick = v, String::valueOf);
				y = addToggle(x, y, w, "Weather sound cut", "Thin repetitive rain / thunder ambience.",
						c.weatherSoundReductionEnabled, v -> c.weatherSoundReductionEnabled = v);
				y = addSteps(x, y, w, "Weather sound keep", (int) Math.round(c.weatherSoundKeepChance * 100.0), 0, 100, 5,
						v -> c.weatherSoundKeepChance = v / 100.0, v -> v + "%");
			}
			case SERVER -> {
				y = addToggle(x, y, w, "Idle AI throttle", "Slow far mob AI on the integrated server.",
						c.idleAiThrottleEnabled, v -> {
							c.idleAiThrottleEnabled = v;
							HotPath.rebuild(c);
						});
				y = addSteps(x, y, w, "Idle AI distance", (int) c.idleAiFullDistance, 8, 128, 4,
						v -> c.idleAiFullDistance = v, v -> v + " blk");
				y = addSteps(x, y, w, "Idle AI interval", c.idleAiMaxInterval, 2, 40, 1,
						v -> c.idleAiMaxInterval = v, v -> "1/" + v);
				y = addToggle(x, y, w, "Pathfinding throttle", "Reuse paths when the goal did not move.",
						c.pathfindingThrottleEnabled, v -> c.pathfindingThrottleEnabled = v);
				y = addSteps(x, y, w, "Pathfinding distance", (int) c.pathfindingFullDistance, 8, 128, 4,
						v -> c.pathfindingFullDistance = v, v -> v + " blk");
				y = addSteps(x, y, w, "Pathfinding interval", c.pathfindingMaxInterval, 2, 40, 1,
						v -> c.pathfindingMaxInterval = v, v -> "1/" + v);
				y = addToggle(x, y, w, "Defer pathfinding", "Stand down if Lithium is loaded.",
						c.deferPathfindingToLithium, v -> c.deferPathfindingToLithium = v);
				y = addToggle(x, y, w, "Item / XP tick skip", "Slow far item and XP orb simulation.",
						c.itemThrottleEnabled, v -> c.itemThrottleEnabled = v);
				y = addSteps(x, y, w, "Item tick distance", (int) c.itemThrottleStartDistance, 8, 128, 4,
						v -> c.itemThrottleStartDistance = v, v -> v + " blk");
				y = addSteps(x, y, w, "Item tick interval", c.itemThrottleMaxInterval, 2, 40, 1,
						v -> c.itemThrottleMaxInterval = v, v -> "1/" + v);
				y = addToggle(x, y, w, "Fast world load", "Drain extra chunk tasks when a world opens.",
						c.fastWorldLoadEnabled, v -> c.fastWorldLoadEnabled = v);
				y = addSteps(x, y, w, "Chunk boost", c.fastWorldLoadChunkBoost, 1, 32, 1,
						v -> c.fastWorldLoadChunkBoost = v, String::valueOf);
				y = addSteps(x, y, w, "Open window", c.fastWorldLoadWindowSeconds, 1, 30, 1,
						v -> c.fastWorldLoadWindowSeconds = v, v -> v + "s");
				y = addToggle(x, y, w, "Prefetch regions", "Pre-read region files into the OS cache.",
						c.prefetchRegionFiles, v -> c.prefetchRegionFiles = v);
				y = addToggle(x, y, w, "World-open helper", "Prefetch + drain + admit-when-ready.",
						c.worldOpenEnabled, v -> c.worldOpenEnabled = v);
				y = addToggle(x, y, w, "Admit when ready", "Enter play as soon as the local player exists (SP / LAN).",
						c.admitPlayWhenReady, v -> c.admitPlayWhenReady = v);
				y = addToggle(x, y, w, "Locate cache", "Remember /locate results for a short TTL.",
						c.locateOptimizeEnabled, v -> c.locateOptimizeEnabled = v);
				y = addSteps(x, y, w, "Locate cache TTL", c.locateCacheTtlSeconds, 5, 300, 5,
						v -> c.locateCacheTtlSeconds = v, v -> v + "s");
				y = addToggle(x, y, w, "Integrated server only", "Server extras stay off on remote multiplayer.",
						c.integratedServerOnly, v -> c.integratedServerOnly = v);
			}
			case EXTRA -> {
				y = addToggle(x, y, w, "Native kernels", "Linux .so batch path. Off = Java fallback.",
						c.nativeHotpathEnabled, v -> {
							c.nativeHotpathEnabled = v;
							HotPath.rebuild(c);
						});
				y = addToggle(x, y, w, "Native frustum", "C / ASM / Rust AABB tests. Off = Java.",
						c.nativeFrustumCullingEnabled, v -> {
							c.nativeFrustumCullingEnabled = v;
							HotPath.rebuild(c);
						});
				y = addCycle(x, y, w, "SIMD", "Never forces an instruction the CPU lacks.",
						c.simdMode, SimdMode.values(), v -> c.simdMode = v);
				y = addToggle(x, y, w, "Defer entity cull", "Stand down if EntityCulling / MoreCulling is loaded.",
						c.deferToDedicatedEntityCullingMods, v -> {
							c.deferToDedicatedEntityCullingMods = v;
							HotPath.rebuild(c);
						});
				y = addToggle(x, y, w, "Defer fog", "Hand fog to Sodium Extra when present.",
						c.deferFogToSodiumExtra, v -> c.deferFogToSodiumExtra = v);
				y = addToggle(x, y, w, "Defer toasts", "Hand toast limiting to Sodium Extra.",
						c.deferToastsToSodiumExtra, v -> c.deferToastsToSodiumExtra = v);
				y = addToggle(x, y, w, "Defer beacon", "Stand down beacon cull if Sodium Extra is present.",
						c.deferBeaconToSodiumExtra, v -> c.deferBeaconToSodiumExtra = v);
				y = addToggle(x, y, w, "Defer texture anim", "Stand down if Sodium Extra covers atlas animation.",
						c.deferTextureAnimToSodiumExtra, v -> c.deferTextureAnimToSodiumExtra = v);
				y = addToggle(x, y, w, "Defer particles", "Stand down if Sodium Extra handles particles.",
						c.deferParticlesToSodiumExtra, v -> c.deferParticlesToSodiumExtra = v);
				y = addToggle(x, y, w, "VoxelSniper compat", "Pause terrain masks when WE / VS tools are held.",
						c.voxelSniperCompatEnabled, v -> c.voxelSniperCompatEnabled = v);
				y = addToggle(x, y, w, "Detect WorldEdit", "Treat WorldEdit wands as editor tools.",
						c.voxelSniperDetectWorldEdit, v -> c.voxelSniperDetectWorldEdit = v);
				y = addToggle(x, y, w, "Always scan tools", "Scan arrow / gunpowder / wand even without the mod id.",
						c.voxelSniperAlwaysScanTools, v -> c.voxelSniperAlwaysScanTools = v);
				y = addToggle(x, y, w, "Pause horizon", "Disable horizon Y while an editor tool is held.",
						c.voxelSniperPauseHorizon, v -> c.voxelSniperPauseHorizon = v);
				y = addToggle(x, y, w, "Pause terrain mask", "Disable circular / shape masks while editing.",
						c.voxelSniperPauseTerrainMask, v -> c.voxelSniperPauseTerrainMask = v);
				y = addToggle(x, y, w, "Pause entity cull", "Disable entity cull while editing.",
						c.voxelSniperPauseEntityCull, v -> c.voxelSniperPauseEntityCull = v);
				y = addSteps(x, y, w, "Editor tool range", (int) c.voxelSniperToolRange, 16, 384, 16,
						v -> c.voxelSniperToolRange = v, v -> v + " blk");
				y = addToggle(x, y, w, "Section occupancy", "Hide entities in sections Sodium did not visit.",
						c.sectionOccupancyCullingEnabled, v -> {
							c.sectionOccupancyCullingEnabled = v;
							HotPath.rebuild(c);
						});
				y = addToggle(x, y, w, "Skip empty boss overlay", "Skip boss-bar layout when no events are active.",
						c.skipEmptyBossOverlayEnabled, v -> c.skipEmptyBossOverlayEnabled = v);
			}
		}
	}

	private boolean rowVisible(int y) {
		return y >= HEAD + 4 && y <= this.height - FOOT - ROW;
	}

	private int addToggle(int x, int y, int w, String name, String tip, boolean value, BoolSet set) {
		if (rowVisible(y)) {
			Button b = Button.builder(Component.literal(value ? "On" : "Off"), btn -> {
				set.accept(!value);
				HSNConfig.get().save();
				rebuild();
			}).bounds(x + w - CTRL, y, CTRL, 20).tooltip(Tooltip.create(Component.literal(name + " — " + tip))).build();
			addRenderableWidget(b);
		}
		return y + ROW;
	}

	private <E extends Enum<E>> int addCycle(int x, int y, int w, String name, String tip, E value, E[] all, EnumSet<E> set) {
		if (rowVisible(y)) {
			Button b = Button.builder(Component.literal(labelOf(value)), btn -> {
				int i = 0;
				for (int n = 0; n < all.length; n++) {
					if (all[n] == value) {
						i = n;
						break;
					}
				}
				set.accept(all[(i + 1) % all.length]);
				HSNConfig.get().save();
				rebuild();
			}).bounds(x + w - CTRL, y, CTRL, 20).tooltip(Tooltip.create(Component.literal(name + " — " + tip))).build();
			addRenderableWidget(b);
		}
		return y + ROW;
	}

	private int addSteps(int x, int y, int w, String name, int value, int min, int max, int step,
			IntSet set, java.util.function.IntFunction<String> fmt) {
		if (rowVisible(y)) {
			int sliderX = x + w - CTRL;
			addRenderableWidget(new HSNIntSlider(sliderX, y, CTRL, 20, name, value, min, max, step, fmt, set::accept));
		}
		return y + ROW;
	}

	private static String labelOf(Enum<?> value) {
		if (value instanceof Preset p) {
			return switch (p) {
				case ULTRA_LOW -> "Ultra Low";
				case SAFE -> "Safe";
				case BALANCED -> "Balanced";
				case QUALITY -> "Quality";
				case COMPETITIVE -> "Competitive";
			};
		}
		if (value instanceof ScaleFilter f) {
			return f.title();
		}
		return value.toString();
	}

	private void resetTab() {
		HSNConfig c = HSNConfig.get();
		HSNPresets.apply(c, Preset.BALANCED);
		c.save();
	}

	private void closeAndSave() {
		HSNConfig cfg = HSNConfig.get();
		cfg.save();
		HotPath.rebuild(cfg);
		if (parent != null) {
			ClientScreens.open(parent);
		} else {
			this.onClose();
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		graphics.fill(0, 0, this.width, HEAD, 0xC0101010);
		graphics.fill(0, this.height - FOOT, this.width, this.height, 0xC0101010);
		graphics.text(this.font, "HSN", 8, 10, 0xFFFFFF, false);
		graphics.text(this.font, "HSN Optimizations " + HSNConfig.modVersionLabel, 40, 10, 0xFFE0E0E0, false);
		HSNConfig cfg = HSNConfig.get();
		String status = (cfg.modEnabled ? "On" : "Off") + "  " + labelOf(cfg.lastAppliedPreset);
		graphics.text(this.font, status, 40, 20, 0xFFAAAAAA, false);

		int y = HEAD + 12 - scroll;
		int x = contentLeft();
		String[] labels = labelsForTab();
		for (String label : labels) {
			if (y >= HEAD + 4 && y <= this.height - FOOT - ROW) {
				graphics.text(this.font, label, x, y + 6, 0xFFD4E8D4, false);
			}
			y += ROW;
		}
	}

	private String[] labelsForTab() {
		return switch (tab) {
			case GENERAL -> new String[] {
					"Enable HSN", "Profile", "Performance mode", "Adaptive culling",
					"Target FPS", "Min adaptive scale", "Frame pacing fix", "Smart yield",
					"Thread scheduler", "Low-end hardware tune", "Laptop power save",
					"Weak-GPU auto", "Weak-GPU FPS floor", "Adaptive upload budget",
					"Upload budget", "Unfocused cap", "Unfocused FPS", "Shader safe mode",
					"F3 status line", "F3 compact", "F3 details", "FPS overlay",
					"FPS overlay X", "FPS overlay Y", "Toast limit"
			};
			case ENTITIES -> new String[] {
					"Entity culling", "Entity distance", "Item entity", "XP orbs", "Decorations",
					"Entity LOD", "Behind-camera cull", "Interp skip", "Interp skip distance",
					"Distant client ticks", "Distant tick distance", "Distant tick interval",
					"Living anim throttle", "Living anim distance", "Item spin", "Item spin distance",
					"Block-entity culling", "Block-entity distance", "Block-entity LOD",
					"Block-entity LOD start", "Shadows", "Shadow distance", "Name tags",
					"Name-tag distance", "Glow outlines", "Glow distance"
			};
			case PARTICLES -> new String[] {
					"Particle culling", "Particle cap", "Particle distance", "Rain keep",
					"Smoke keep", "Explosion keep", "Fire smoke keep", "Bubble keep",
					"Particle priority", "High-priority keep", "Low-priority keep",
					"Quality curve", "Hard particle cap", "Firework cap",
					"Firework sparks / tick", "Drip throttle", "Ambient block ticks",
					"Ambient tick interval", "Ambient tick range"
			};
			case RENDERING -> new String[] {
					"World scale", "Scale percent", "Scale filter", "Adaptive scale",
					"Adaptive scale below FPS",
					"Fog scale", "Fog scale factor", "World shape", "Shape radius",
					"Always-keep chunks", "Vertical range", "Vertical range size",
					"Horizon Y", "Horizon keep-below", "Progressive LOD", "LOD start",
					"LOD min quality", "Block texture LOD", "Texture mip bias",
					"Adaptive texture LOD", "Texture anim", "Adaptive texture anim",
					"Anim interval", "Anim max interval", "Lightmap cache",
					"Cloud LOD", "Weather LOD", "Sky extras", "World border",
					"Beacon beams", "Beacon distance", "Map renderer", "Map rebuild interval"
			};
			case AUDIO -> new String[] {
					"Sound distance", "Max sound distance", "Sound burst limit",
					"Max new sounds / tick", "Weather sound cut", "Weather sound keep"
			};
			case SERVER -> new String[] {
					"Idle AI throttle", "Idle AI distance", "Idle AI interval",
					"Pathfinding throttle", "Pathfinding distance", "Pathfinding interval",
					"Defer pathfinding", "Item / XP tick skip", "Item tick distance",
					"Item tick interval", "Fast world load", "Chunk boost", "Open window",
					"Prefetch regions", "World-open helper", "Admit when ready",
					"Locate cache", "Locate cache TTL", "Integrated server only"
			};
			case EXTRA -> new String[] {
					"Native kernels", "Native frustum", "SIMD", "Defer entity cull",
					"Defer fog", "Defer toasts", "Defer beacon", "Defer texture anim",
					"Defer particles", "VoxelSniper compat", "Detect WorldEdit",
					"Always scan tools", "Pause horizon", "Pause terrain mask",
					"Pause entity cull", "Editor tool range", "Section occupancy",
					"Skip empty boss overlay"
			};
		};
	}

	private static void drawMark(GuiGraphicsExtractor g, int x, int y) {
		g.fill(x, y, x + 20, y + 20, 0xFF1A2A1A);
		g.fill(x, y, x + 20, y + 1, 0xFF4A7C59);
		g.fill(x, y + 19, x + 20, y + 20, 0xFF4A7C59);
		g.fill(x, y, x + 1, y + 20, 0xFF4A7C59);
		g.fill(x + 19, y, x + 20, y + 20, 0xFF4A7C59);
		g.fill(x + 5, y + 4, x + 8, y + 16, 0xFF8ACC8A);
		g.fill(x + 12, y + 4, x + 15, y + 16, 0xFF8ACC8A);
		g.fill(x + 8, y + 9, x + 12, y + 11, 0xFF8ACC8A);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
			return true;
		}
		if (mouseX >= SIDE && scrollY != 0.0) {
			scroll = (int) Math.max(0, Math.min(maxScroll(), scroll - (int) Math.round(scrollY * ROW * 2)));
			rebuild();
			return true;
		}
		return false;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@FunctionalInterface
	private interface BoolSet {
		void accept(boolean v);
	}

	@FunctionalInterface
	private interface EnumSet<E> {
		void accept(E v);
	}

	@FunctionalInterface
	private interface IntSet {
		void accept(int v);
	}
}
