package hsn.optimizations.client.config;

import hsn.optimizations.client.compat.HSNModCompat;
import hsn.optimizations.config.HSNConfig;
import hsn.optimizations.config.HSNConfig.Preset;
import hsn.optimizations.config.HSNPresets;
import hsn.optimizations.config.ScaleFilter;
import hsn.optimizations.config.SimdMode;
import hsn.optimizations.config.WorldRenderShape;
import hsn.optimizations.optimize.HotPath;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.Screen;

/**
 * Primary settings UI. Cloth sliders drag and scroll natively on 26.3 SDL.
 */
public final class HSNClothConfigScreen {

	private HSNClothConfigScreen() {
	}

	public static Screen create(Screen parent) {
		HSNConfig cfg = HSNConfig.get();
		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Component.literal("HSN Optimizations"))
				.setSavingRunnable(() -> {
					cfg.sanitize();
					cfg.save();
					HotPath.rebuild(cfg);
				});
		ConfigEntryBuilder e = builder.entryBuilder();

		general(builder.getOrCreateCategory(Component.literal("General")), e, cfg);
		entities(builder.getOrCreateCategory(Component.literal("Entities")), e, cfg);
		particles(builder.getOrCreateCategory(Component.literal("Particles")), e, cfg);
		rendering(builder.getOrCreateCategory(Component.literal("Rendering")), e, cfg);
		audio(builder.getOrCreateCategory(Component.literal("Audio")), e, cfg);
		server(builder.getOrCreateCategory(Component.literal("Server")), e, cfg);
		advanced(builder.getOrCreateCategory(Component.literal("Advanced")), e, cfg);
		return builder.build();
	}

	private static void general(ConfigCategory cat, ConfigEntryBuilder e, HSNConfig cfg) {
		cat.addEntry(e.startTextDescription(Component.literal(
				"HSN Optimizations " + HSNConfig.modVersionLabel + " — Cloth sliders drag. YACL is optional fallback."))
				.build());
		cat.addEntry(e.startEnumSelector(Component.literal("Quality preset"), Preset.class, cfg.lastAppliedPreset)
				.setDefaultValue(Preset.BALANCED)
				.setTooltip(Component.literal("Applies a full distance and quality profile."))
				.setSaveConsumer(v -> {
					HSNPresets.apply(cfg, v);
					HotPath.rebuild(cfg);
				})
				.build());
		cat.addEntry(toggle(e, "Master switch", cfg.modEnabled, true,
				"Off restores vanilla plus your other mods.",
				v -> {
					cfg.modEnabled = v;
					HotPath.rebuild(cfg);
				}));
		cat.addEntry(toggle(e, "Performance mode", cfg.performanceModeEnabled, false,
				"When FPS dips, ease distances toward 70% of the sliders.",
				v -> {
					cfg.performanceModeEnabled = v;
					HotPath.rebuild(cfg);
				}));
		cat.addEntry(toggle(e, "Adaptive culling", cfg.adaptiveCullingEnabled, true,
				"Scale entity and decoration distances toward the FPS target.",
				v -> {
					cfg.adaptiveCullingEnabled = v;
					HotPath.rebuild(cfg);
				}));
		cat.addEntry(slider(e, "Target FPS", cfg.targetFps, 60, 30, 360,
				"Frame rate Adaptive Culling tries to protect.",
				v -> cfg.targetFps = v));
		cat.addEntry(percent(e, "Min adaptive scale", cfg.minAdaptiveScale, 50, 25, 100,
				"Floor for how far adaptive culling may shrink distances.",
				v -> cfg.minAdaptiveScale = v));
		cat.addEntry(toggle(e, "Frame pacing fix", cfg.framePacingFixEnabled, true,
				"Prefer mutable buffers on weak Intel iGPUs.",
				v -> cfg.framePacingFixEnabled = v));
		cat.addEntry(toggle(e, "Smart yield", cfg.smartYieldEnabled, true,
				"Yield only when the last frame already missed ~10 ms.",
				v -> cfg.smartYieldEnabled = v));
		cat.addEntry(toggle(e, "Weak-GPU auto", cfg.weakGpuAutoEnabled, true,
				"Extra-low layer after sustained FPS under the threshold.",
				v -> cfg.weakGpuAutoEnabled = v));
		cat.addEntry(slider(e, "Weak-GPU FPS floor", cfg.weakGpuFpsThreshold, 35, 10, 120,
				"Smoothed FPS that engages the extra-low layer.",
				v -> cfg.weakGpuFpsThreshold = v));
		cat.addEntry(toggle(e, "Unfocused FPS cap", cfg.unfocusedFpsCapEnabled, true,
				"Limit FPS when the window is in the background.",
				v -> cfg.unfocusedFpsCapEnabled = v));
		cat.addEntry(slider(e, "Unfocused FPS", cfg.unfocusedFpsCap, 30, 5, 240,
				"Target FPS while unfocused.",
				v -> cfg.unfocusedFpsCap = v));
		cat.addEntry(toggle(e, "F3 status", cfg.f3ShowStatus, true,
				"HSN summary on the right F3 column.",
				v -> cfg.f3ShowStatus = v));
		cat.addEntry(toggle(e, "FPS overlay", cfg.fpsOverlayEnabled, false,
				"Compact on-screen FPS readout.",
				v -> cfg.fpsOverlayEnabled = v));
	}

	private static void entities(ConfigCategory cat, ConfigEntryBuilder e, HSNConfig cfg) {
		cat.addEntry(toggle(e, "Entity culling", cfg.entityCullingEnabled, true,
				"Skip far entities.",
				v -> {
					cfg.entityCullingEnabled = v;
					HotPath.rebuild(cfg);
				}));
		cat.addEntry(blocks(e, "Entity distance", cfg.maxEntityRenderDistance, 32, 8, 256,
				"Full-rate entity render distance.",
				v -> {
					cfg.maxEntityRenderDistance = v;
					HotPath.rebuild(cfg);
				}));
		cat.addEntry(blocks(e, "Item entity distance", cfg.maxItemEntityRenderDistance, 24, 8, 128,
				"Dropped-item render distance.",
				v -> cfg.maxItemEntityRenderDistance = v));
		cat.addEntry(blocks(e, "XP orb distance", cfg.maxXpOrbRenderDistance, 16, 8, 128,
				"Experience-orb render distance.",
				v -> cfg.maxXpOrbRenderDistance = v));
		cat.addEntry(toggle(e, "Behind-camera cull", cfg.behindCameraCullEnabled, false,
				"Skip entities behind the camera past half the slider.",
				v -> {
					cfg.behindCameraCullEnabled = v;
					HotPath.rebuild(cfg);
				}));
		cat.addEntry(toggle(e, "Name tags", cfg.nameTagCullEnabled, true,
				"Tighten name-tag distance.",
				v -> cfg.nameTagCullEnabled = v));
		cat.addEntry(blocks(e, "Name-tag distance", cfg.maxNameTagDistance, 24, 8, 128,
				"Distance at which name tags still draw.",
				v -> cfg.maxNameTagDistance = v));
		cat.addEntry(toggle(e, "Shadows", cfg.shadowCullingEnabled, true,
				"Distance-cull entity shadows.",
				v -> cfg.shadowCullingEnabled = v));
		cat.addEntry(blocks(e, "Shadow distance", cfg.maxShadowDistance, 24, 8, 128,
				"Shadow draw distance.",
				v -> cfg.maxShadowDistance = v));
		cat.addEntry(toggle(e, "Block-entity culling", cfg.blockEntityCullingEnabled, true,
				"Skip far chests, signs, and other block entities.",
				v -> cfg.blockEntityCullingEnabled = v));
		cat.addEntry(blocks(e, "Block-entity distance", cfg.maxBlockEntityRenderDistance, 32, 8, 128,
				"Block-entity draw distance.",
				v -> cfg.maxBlockEntityRenderDistance = v));
	}

	private static void particles(ConfigCategory cat, ConfigEntryBuilder e, HSNConfig cfg) {
		cat.addEntry(toggle(e, "Particle culling", cfg.particleCullingEnabled, true,
				"Distance and facing filter for particles.",
				v -> {
					cfg.particleCullingEnabled = v;
					HotPath.rebuild(cfg);
				}));
		cat.addEntry(slider(e, "Particle cap", cfg.maxParticles, 400, 50, 4000,
				"Soft live-particle budget.",
				v -> cfg.maxParticles = v));
		cat.addEntry(blocks(e, "Particle distance", cfg.maxParticleDistance, 16, 4, 64,
				"How far particles still spawn.",
				v -> cfg.maxParticleDistance = v));
		cat.addEntry(toggle(e, "Hard particle cap", cfg.hardParticleCapEnabled, true,
				"Refuse new particles when the live count is at the budget.",
				v -> cfg.hardParticleCapEnabled = v));
		cat.addEntry(percent(e, "Rain keep", cfg.rainKeepChance, 15, 0, 100,
				"Fraction of rain splashes kept.",
				v -> cfg.rainKeepChance = v));
		cat.addEntry(percent(e, "Smoke keep", cfg.smokeKeepChance, 25, 0, 100,
				"Fraction of smoke puffs kept.",
				v -> cfg.smokeKeepChance = v));
	}

	private static void rendering(ConfigCategory cat, ConfigEntryBuilder e, HSNConfig cfg) {
		cat.addEntry(toggle(e, "Always-on world scale", cfg.renderScaleEnabled, false,
				"Draw the 3D world at a fixed lower resolution. HUD stays native. Off by default on 26.3.",
				v -> cfg.renderScaleEnabled = v));
		cat.addEntry(percent(e, "World scale amount", cfg.renderScale, 70, 25, 100,
				"Framebuffer size used when scale is active. 70% is the adaptive drop target.",
				v -> cfg.renderScale = v));
		cat.addEntry(e.startEnumSelector(Component.literal("Scale filter"), ScaleFilter.class, cfg.renderScaleFilter)
				.setDefaultValue(ScaleFilter.LINEAR)
				.setTooltip(Component.literal("Linear is softer. Nearest keeps pixels."))
				.setSaveConsumer(v -> cfg.renderScaleFilter = v)
				.build());
		cat.addEntry(toggle(e, "Adaptive world scale", cfg.renderScaleAdaptive, true,
				"Lower world resolution only while smoothed FPS is at or below the threshold.",
				v -> cfg.renderScaleAdaptive = v));
		cat.addEntry(slider(e, "Adaptive scale below FPS", cfg.adaptiveRenderScaleFps, 45, 15, 120,
				"When smoothed FPS is at or below this value, world scale eases toward the scale amount. Default 45.",
				v -> cfg.adaptiveRenderScaleFps = v));
		cat.addEntry(e.startEnumSelector(Component.literal("World shape"), WorldRenderShape.class, cfg.worldRenderShape)
				.setDefaultValue(WorldRenderShape.OFF)
				.setTooltip(Component.literal("Off is vanilla square. Circle / hex trim far corners."))
				.setSaveConsumer(v -> {
					if (!HSNModCompat.shapeModPresent()) {
						cfg.worldRenderShape = v;
						cfg.circularRenderingEnabled = v != WorldRenderShape.OFF;
						HotPath.rebuild(cfg);
					}
				})
				.build());
		cat.addEntry(toggle(e, "Horizon Y cull", cfg.horizonYCullEnabled, true,
				"Skip sections above the visible sky line.",
				v -> cfg.horizonYCullEnabled = v));
		cat.addEntry(toggle(e, "Fog scale", cfg.fogScaleEnabled, false,
				"Optional fog-distance scaling. Off by default.",
				v -> cfg.fogScaleEnabled = v));
		cat.addEntry(toggle(e, "Cloud LOD", cfg.cloudLodEnabled, true,
				"Skip clouds when the camera cannot see the sky.",
				v -> cfg.cloudLodEnabled = v));
		cat.addEntry(toggle(e, "Weather LOD", cfg.weatherRendererLodEnabled, true,
				"Skip weather when the camera cannot see the sky.",
				v -> cfg.weatherRendererLodEnabled = v));
		cat.addEntry(toggle(e, "Lightmap cache", cfg.lightmapCacheEnabled, true,
				"Skip unchanged lightmap rebuilds.",
				v -> cfg.lightmapCacheEnabled = v));
	}

	private static void audio(ConfigCategory cat, ConfigEntryBuilder e, HSNConfig cfg) {
		cat.addEntry(toggle(e, "Sound distance cull", cfg.soundDistanceCullingEnabled, true,
				"Drop far sounds. No frustum test.",
				v -> cfg.soundDistanceCullingEnabled = v));
		cat.addEntry(blocks(e, "Max sound distance", cfg.maxSoundDistance, 32, 8, 128,
				"Sounds farther than this are skipped.",
				v -> cfg.maxSoundDistance = v));
		cat.addEntry(slider(e, "New sounds per tick", cfg.maxNewSoundsPerTick, 8, 1, 32,
				"Burst cap after a sound is allowed.",
				v -> cfg.maxNewSoundsPerTick = v));
	}

	private static void server(ConfigCategory cat, ConfigEntryBuilder e, HSNConfig cfg) {
		cat.addEntry(toggle(e, "Idle AI throttle", cfg.idleAiThrottleEnabled, true,
				"Integrated-server only. Slow far mob AI.",
				v -> cfg.idleAiThrottleEnabled = v));
		cat.addEntry(toggle(e, "Pathfinding throttle", cfg.pathfindingThrottleEnabled, true,
				"Integrated-server only. Rate-limit far path searches.",
				v -> cfg.pathfindingThrottleEnabled = v));
		cat.addEntry(toggle(e, "Item / XP tick skip", cfg.itemThrottleEnabled, true,
				"Skip far item and XP ticks on the integrated server.",
				v -> cfg.itemThrottleEnabled = v));
		cat.addEntry(toggle(e, "Fast world load", cfg.fastWorldLoadEnabled, true,
				"Drain the integrated-server join queue faster.",
				v -> cfg.fastWorldLoadEnabled = v));
		cat.addEntry(toggle(e, "Locate cache", cfg.locateOptimizeEnabled, true,
				"Cache /locate results for a short TTL.",
				v -> cfg.locateOptimizeEnabled = v));
	}

	private static void advanced(ConfigCategory cat, ConfigEntryBuilder e, HSNConfig cfg) {
		cat.addEntry(toggle(e, "Native kernels", cfg.nativeHotpathEnabled, true,
				"Use bundled Linux natives when present. Missing .so files stay on Java.",
				v -> cfg.nativeHotpathEnabled = v));
		cat.addEntry(e.startEnumSelector(Component.literal("SIMD"), SimdMode.class, cfg.simdMode)
				.setDefaultValue(SimdMode.AUTO)
				.setSaveConsumer(v -> cfg.simdMode = v)
				.build());
		cat.addEntry(toggle(e, "Shader safe mode", cfg.shaderSafeMode, true,
				"Skip framebuffer swaps that fight Iris.",
				v -> cfg.shaderSafeMode = v));
		cat.addEntry(toggle(e, "Defer entity cull", cfg.deferToDedicatedEntityCullingMods, false,
				"Skip HSN entity distance cull when Entity Culling is installed.",
				v -> cfg.deferToDedicatedEntityCullingMods = v));
	}

	private static me.shedaniel.clothconfig2.api.AbstractConfigListEntry<?> toggle(
			ConfigEntryBuilder e, String name, boolean value, boolean def, String tip, java.util.function.Consumer<Boolean> set) {
		return e.startBooleanToggle(Component.literal(name), value)
				.setDefaultValue(def)
				.setTooltip(Component.literal(tip))
				.setSaveConsumer(set)
				.build();
	}

	private static me.shedaniel.clothconfig2.api.AbstractConfigListEntry<?> slider(
			ConfigEntryBuilder e, String name, int value, int def, int min, int max, String tip, java.util.function.IntConsumer set) {
		return e.startIntSlider(Component.literal(name), value, min, max)
				.setDefaultValue(def)
				.setTooltip(Component.literal(tip))
				.setSaveConsumer(set::accept)
				.build();
	}

	private static me.shedaniel.clothconfig2.api.AbstractConfigListEntry<?> percent(
			ConfigEntryBuilder e, String name, double value, int def, int min, int max, String tip, java.util.function.DoubleConsumer set) {
		int cur = (int) Math.round(value * 100.0);
		return e.startIntSlider(Component.literal(name), cur, min, max)
				.setDefaultValue(def)
				.setTooltip(Component.literal(tip))
				.setSaveConsumer(v -> set.accept(v / 100.0))
				.build();
	}

	private static me.shedaniel.clothconfig2.api.AbstractConfigListEntry<?> blocks(
			ConfigEntryBuilder e, String name, double value, int def, int min, int max, String tip, java.util.function.DoubleConsumer set) {
		int cur = (int) Math.round(value);
		return e.startIntSlider(Component.literal(name), cur, min, max)
				.setDefaultValue(def)
				.setTooltip(Component.literal(tip))
				.setSaveConsumer(v -> set.accept((double) v))
				.build();
	}
}
