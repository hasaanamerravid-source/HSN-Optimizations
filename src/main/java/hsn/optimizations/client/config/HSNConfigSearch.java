package hsn.optimizations.client.config;

import hsn.optimizations.client.compat.ClientScreens;
import hsn.optimizations.config.HSNConfig;
import hsn.optimizations.optimize.HotPath;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.LabelOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * First-tab search bar for the YACL config screen.
 * Matches against option name, category and group, then builds the same
 * kind of live slider / toggle that the other tabs use so results are editable.
 */
public final class HSNConfigSearch {

	private static volatile String query = "";

	private HSNConfigSearch() {
	}

	/**
	 * YACL 3.9 exposes {@code screenInit}. Older jars skip the hook.
	 * Reflection keeps this file compiling against either API.
	 */
	static void attachScreenInit(Object builder) {
		if (builder == null) {
			return;
		}
		try {
			for (var method : builder.getClass().getMethods()) {
				if (!"screenInit".equals(method.getName()) || method.getParameterCount() != 1) {
					continue;
				}
				java.util.function.Consumer<Object> hook = HSNConfigSearch::focusSearchBar;
				method.invoke(builder, hook);
				return;
			}
		} catch (Throwable ignored) {
		}
	}

	private static void focusSearchBar(Object screen) {
		if (screen == null) {
			return;
		}
		try {
			var setFocus = screen.getClass().getMethod("setInitialFocus", Object.class);
			Object box = findEditBox(screen, 0);
			if (box != null) {
				setFocus.invoke(screen, box);
			}
		} catch (Throwable ignored) {
		}
	}

	private static Object findEditBox(Object node, int depth) {
		if (node == null || depth > 8) {
			return null;
		}
		String name = node.getClass().getName();
		if (name.endsWith("EditBox") || name.endsWith("TextFieldWidget")
				|| name.contains("StringController") || name.contains("searchField")) {
			return node;
		}
		try {
			var children = node.getClass().getMethod("children");
			Object list = children.invoke(node);
			if (list instanceof Iterable<?> it) {
				for (Object child : it) {
					Object hit = findEditBox(child, depth + 1);
					if (hit != null) {
						return hit;
					}
				}
			}
		} catch (Throwable ignored) {
		}
		return null;
	}

	public static String query() {
		String q = query;
		return q == null ? "" : q;
	}

	static ConfigCategory category(HSNConfig cfg, Screen parent) {
		String current = query();
		List<String[]> hits = catalogHits(current);
		var group = OptionGroup.createBuilder()
				.name(Component.literal("Find a setting").withStyle(ChatFormatting.AQUA))
				.option(Option.<String>createBuilder()
						.name(Component.literal("Search"))
						.description(OptionDescription.of(Component.literal(
								"Type part of a setting name, category or group (for example \"particle\", \"beacon\", \"F3\"). "
										+ "Click Apply search to rebuild this tab with live sliders for every match. "
										+ "Sliders apply immediately — you do not have to press Done first.")))
						.binding(query(), HSNConfigSearch::query, v -> query = v == null ? "" : v)
						.controller(StringControllerBuilder::create)
						.instant(true)
						.build())
				.option(Option.<Boolean>createBuilder()
						.name(Component.literal("Apply search"))
						.description(OptionDescription.of(Component.literal(
								"Rebuild this screen so the match list below updates with working sliders.")))
						.binding(false, () -> false, v -> {
							if (v) {
								ClientScreens.open(HSNConfigScreen.create(parent));
							}
						})
						.instant(true)
						.controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
						.build())
				.option(Option.<Boolean>createBuilder()
						.name(Component.literal("Clear search"))
						.description(OptionDescription.of(Component.literal(
								"Empty the search box and reload every category.")))
						.binding(false, () -> false, v -> {
							if (v) {
								query = "";
								ClientScreens.open(HSNConfigScreen.create(parent));
							}
						})
						.instant(true)
						.controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
						.build())
				.option(label(current.isBlank()
						? "Showing every setting. Type above, then Apply search."
						: hits.isEmpty()
						? "No settings match \"" + current + "\"."
						: hits.size() + " setting(s) match \"" + current + "\". Drag the sliders below; they apply instantly."));

		int shown = 0;
		for (String[] hit : hits) {
			if (shown >= 36) {
				group.option(label("… " + (hits.size() - shown) + " more. Narrow the search."));
				break;
			}
			Option<?> live = liveOption(cfg, hit);
			if (live != null) {
				group.option(live);
			} else {
				group.option(label(hit[1] + " → " + hit[2] + " → " + hit[0]));
			}
			shown++;
		}

		return ConfigCategory.createBuilder()
				.name(Component.literal("Search").withStyle(ChatFormatting.GOLD))
				.tooltip(Component.literal("Search bar. Filter the long settings list by name."))
				.option(LabelOption.createBuilder()
						.line(Component.literal("HSN-Optimizations " + HSNConfig.modVersionLabel)
								.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
						.line(Component.literal("Type in the search bar, then Apply search. Sliders apply while you drag. F7 opens this screen, F6 is the kill switch.")
								.withStyle(ChatFormatting.GRAY))
						.build())
				.group(group.build())
				.build();
	}

	private static Option<Component> label(String line) {
		return LabelOption.createBuilder()
				.line(Component.literal(line).withStyle(ChatFormatting.GRAY))
				.build();
	}

	private static List<String[]> catalogHits(String raw) {
		String q = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
		List<String[]> hits = new ArrayList<>();
		for (String[] row : CATALOG) {
			if (q.isEmpty()
					|| row[0].toLowerCase(Locale.ROOT).contains(q)
					|| row[1].toLowerCase(Locale.ROOT).contains(q)
					|| row[2].toLowerCase(Locale.ROOT).contains(q)
					|| row[4].toLowerCase(Locale.ROOT).contains(q)) {
				hits.add(row);
			}
		}
		return hits;
	}

	private static Option<?> liveOption(HSNConfig cfg, String[] row) {
		if (cfg == null || row.length < 5) {
			return null;
		}
		String name = row[0];
		String kind = row[3];
		String field = row[4];
		try {
			Field f = HSNConfig.class.getField(field);
			f.setAccessible(true);
			return switch (kind) {
				case "toggle" -> toggle(name, cfg, f);
				case "slider_i" -> sliderInt(name, cfg, f, parse(row, 5, 0), parse(row, 6, 0), parse(row, 7, 1), unit(row));
				case "slider_d" -> sliderDouble(name, cfg, f, parse(row, 5, 0), parse(row, 6, 0), parse(row, 7, 1), unit(row));
				case "percent" -> percent(name, cfg, f, parse(row, 5, 0), parse(row, 6, 0), parse(row, 7, 100));
				default -> null;
			};
		} catch (Throwable ignored) {
			return null;
		}
	}

	private static int parse(String[] row, int idx, int fallback) {
		if (idx >= row.length) {
			return fallback;
		}
		try {
			return Integer.parseInt(row[idx]);
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	private static String unit(String[] row) {
		return row.length > 8 ? row[8] : "";
	}

	private static Option<Boolean> toggle(String name, HSNConfig cfg, Field f) {
		boolean def = readBool(cfg, f);
		return Option.<Boolean>createBuilder()
				.name(Component.literal(name))
				.description(OptionDescription.of(Component.literal("Live search result. Applies immediately.")))
				.instant(true)
				.binding(def, () -> readBool(cfg, f), v -> {
					writeBool(cfg, f, v);
					HotPath.rebuild();
				})
				.controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
				.build();
	}

	private static Option<Integer> sliderInt(String name, HSNConfig cfg, Field f, int def, int min, int max, String unit) {
		int lo = Math.min(min, max);
		int hi = Math.max(min, max);
		int safeDef = clampInt(def, lo, hi);
		return Option.<Integer>createBuilder()
				.name(Component.literal(name))
				.description(OptionDescription.of(Component.literal("Live search result. Applies immediately while you drag.")))
				.instant(true)
				.binding(safeDef, () -> clampInt(readInt(cfg, f), lo, hi), v -> {
					writeInt(cfg, f, clampInt(v, lo, hi));
					HotPath.rebuild();
				})
				.controller(opt -> IntegerSliderControllerBuilder.create(opt)
						.range(lo, hi)
						.step(1)
						.formatValue(v -> Component.literal(unit == null || unit.isEmpty() ? Integer.toString(v) : v + " " + unit)
								.withStyle(ChatFormatting.AQUA)))
				.build();
	}

	private static Option<Integer> sliderDouble(String name, HSNConfig cfg, Field f, int def, int min, int max, String unit) {
		int lo = Math.min(min, max);
		int hi = Math.max(min, max);
		int safeDef = clampInt(def, lo, hi);
		return Option.<Integer>createBuilder()
				.name(Component.literal(name))
				.description(OptionDescription.of(Component.literal("Live search result. Applies immediately while you drag.")))
				.instant(true)
				.binding(safeDef, () -> clampInt((int) Math.round(readDouble(cfg, f)), lo, hi), v -> {
					writeDouble(cfg, f, clampInt(v, lo, hi));
					HotPath.rebuild();
				})
				.controller(opt -> IntegerSliderControllerBuilder.create(opt)
						.range(lo, hi)
						.step(1)
						.formatValue(v -> Component.literal(unit == null || unit.isEmpty() ? Integer.toString(v) : v + " " + unit)
								.withStyle(ChatFormatting.AQUA)))
				.build();
	}

	private static Option<Integer> percent(String name, HSNConfig cfg, Field f, int def, int min, int max) {
		int lo = Math.min(min, max);
		int hi = Math.max(min, max);
		int safeDef = clampInt(def, lo, hi);
		return Option.<Integer>createBuilder()
				.name(Component.literal(name))
				.description(OptionDescription.of(Component.literal("Live search result. Applies immediately while you drag.")))
				.instant(true)
				.binding(safeDef, () -> clampInt((int) Math.round(readDouble(cfg, f) * 100.0), lo, hi), v -> {
					writeDouble(cfg, f, clampInt(v, lo, hi) / 100.0);
					HotPath.rebuild();
				})
				.controller(opt -> IntegerSliderControllerBuilder.create(opt)
						.range(lo, hi)
						.step(1)
						.formatValue(v -> Component.literal(v + " %").withStyle(ChatFormatting.AQUA)))
				.build();
	}

	private static boolean readBool(HSNConfig cfg, Field f) {
		try {
			return f.getBoolean(cfg);
		} catch (Throwable t) {
			return false;
		}
	}

	private static void writeBool(HSNConfig cfg, Field f, boolean v) {
		try {
			f.setBoolean(cfg, v);
		} catch (Throwable ignored) {
		}
	}

	private static int readInt(HSNConfig cfg, Field f) {
		try {
			return f.getInt(cfg);
		} catch (Throwable t) {
			return 0;
		}
	}

	private static void writeInt(HSNConfig cfg, Field f, int v) {
		try {
			f.setInt(cfg, v);
		} catch (Throwable ignored) {
		}
	}

	private static double readDouble(HSNConfig cfg, Field f) {
		try {
			return f.getDouble(cfg);
		} catch (Throwable t) {
			return 0.0;
		}
	}

	private static void writeDouble(HSNConfig cfg, Field f, double v) {
		try {
			f.setDouble(cfg, v);
		} catch (Throwable ignored) {
		}
	}

	private static int clampInt(int v, int lo, int hi) {
		if (v < lo) {
			return lo;
		}
		if (v > hi) {
			return hi;
		}
		return v;
	}

	// name, category, group, kind, field, def, min, max, unit
	private static final String[][] CATALOG = {
			{"HSN Enabled (kill switch)", "Start here", "Master switches", "toggle", "modEnabled"},
			{"Shader Safe Mode", "Simple", "Master switches", "toggle", "shaderSafeMode"},
			{"Performance Mode", "Start here", "Master switches", "toggle", "performanceModeEnabled"},
			{"Particle Culling", "Start here", "Master switches", "toggle", "particleCullingEnabled"},
			{"Entity Culling", "Start here", "Master switches", "toggle", "entityCullingEnabled"},
			{"Block-Entity Culling", "Start here", "Master switches", "toggle", "blockEntityCullingEnabled"},
			{"Sound Culling", "Start here", "Master switches", "toggle", "soundDistanceCullingEnabled"},
			{"Particle Distance", "Distance", "Particles", "slider_d", "maxParticleDistance", "16", "4", "128", "blocks"},
			{"Particle Budget", "Distance", "Particles", "slider_i", "maxParticles", "400", "80", "1200", ""},
			{"Entity Distance", "Distance", "Entities", "slider_d", "maxEntityRenderDistance", "32", "4", "256", "blocks"},
			{"Item Distance", "Distance", "Entities", "slider_d", "maxItemEntityRenderDistance", "20", "4", "128", "blocks"},
			{"XP Orb Distance", "Distance", "Entities", "slider_d", "maxXpOrbRenderDistance", "16", "4", "128", "blocks"},
			{"Decoration Distance", "Distance", "Entities", "slider_d", "maxDecorationEntityDistance", "16", "4", "128", "blocks"},
			{"Defer to Entity-Culling Mods", "Distance", "Entities", "toggle", "deferToDedicatedEntityCullingMods"},
			{"Block-Entity Distance", "Distance", "Block entities", "slider_d", "maxBlockEntityRenderDistance", "24", "4", "128", "blocks"},
			{"Shadow Culling", "Distance", "Overlays", "toggle", "shadowCullingEnabled"},
			{"Shadow Distance", "Distance", "Overlays", "slider_d", "maxShadowDistance", "12", "2", "64", "blocks"},
			{"Name-Tag Culling", "Distance", "Overlays", "toggle", "nameTagCullEnabled"},
			{"Name-Tag Distance", "Distance", "Overlays", "slider_d", "maxNameTagDistance", "24", "4", "128", "blocks"},
			{"Glow-Outline Culling", "Distance", "Overlays", "toggle", "glowOutlineCullingEnabled"},
			{"Glow-Outline Distance", "Distance", "Overlays", "slider_d", "maxGlowOutlineDistance", "28", "4", "64", "blocks"},
			{"Beacon-Beam Culling", "Distance", "Overlays", "toggle", "beaconBeamCullingEnabled"},
			{"Beacon-Beam Distance", "Distance", "Overlays", "slider_d", "maxBeaconBeamDistance", "48", "8", "128", "blocks"},
			{"Sound Distance", "Distance", "Audio", "slider_d", "maxSoundDistance", "48", "12", "64", "blocks"},
			{"Weather Sound Reduction", "Distance", "Audio", "toggle", "weatherSoundReductionEnabled"},
			{"Weather Keep Rate", "Distance", "Audio", "percent", "weatherSoundKeepChance", "20", "0", "100", "%"},
			{"Sound Burst Limit", "Distance", "Audio", "toggle", "soundBurstLimitEnabled"},
			{"Maximum New Sounds", "Distance", "Audio", "slider_i", "maxNewSoundsPerTick", "24", "1", "64", ""},
			{"Progressive LOD", "Picture", "Level of detail", "toggle", "progressiveLodEnabled"},
			{"LOD Start", "Picture", "Level of detail", "percent", "progressiveLodStart", "50", "20", "90", "%"},
			{"Minimum LOD Quality", "Picture", "Level of detail", "percent", "progressiveLodMinQuality", "15", "5", "50", "%"},
			{"Entity LOD Stages", "Picture", "Level of detail", "toggle", "entityLodStagesEnabled"},
			{"Block-Entity LOD", "Picture", "Level of detail", "toggle", "blockEntityLodEnabled"},
			{"Block-Entity LOD Distance", "Picture", "Level of detail", "slider_d", "blockEntityLodDistance", "14", "4", "48", "blocks"},
			{"Block Texture LOD", "Picture", "Textures", "toggle", "blockTextureLodEnabled"},
			{"Animated Texture Throttle", "Picture", "Textures", "toggle", "textureAnimThrottleEnabled"},
			{"Adaptive Texture LOD", "Picture", "Textures", "toggle", "blockTextureLodAdaptive"},
			{"Item Spin Throttle", "Picture", "Textures", "toggle", "itemSpinThrottleEnabled"},
			{"Item Spin Distance", "Picture", "Textures", "slider_d", "itemSpinThrottleDistance", "12", "2", "48", "blocks"},
			{"Texture Interval", "Picture", "Textures", "slider_i", "textureAnimInterval", "1", "1", "8", "ticks"},
			{"Texture Interval (Load)", "Picture", "Textures", "slider_i", "textureAnimMaxInterval", "4", "1", "12", "ticks"},
			{"Circular Terrain Mask", "Picture", "Terrain", "toggle", "circularRenderingEnabled"},
			{"Mask Coverage", "Picture", "Terrain", "percent", "circularRadiusScale", "100", "25", "100", "%"},
			{"Vertical Range Limit", "Picture", "Terrain", "toggle", "circularVerticalRangeEnabled"},
			{"Vertical Range", "Picture", "Terrain", "slider_i", "circularVerticalRange", "16", "4", "64", "blocks"},
			{"Horizon Y Cull", "Picture", "Terrain", "toggle", "horizonYCullEnabled"},
			{"Horizon Keep-Below", "Picture", "Terrain", "slider_d", "horizonYKeepBelow", "24", "8", "96", "blocks"},
			{"Always-Keep Chunks", "Picture", "Terrain", "slider_i", "alwaysKeepChunks", "3", "1", "8", "chunks"},
			{"VoxelSniper Integration", "Other mods", "VoxelSniper", "toggle", "voxelSniperCompatEnabled"},
			{"Detect WorldEdit / FAWE", "Other mods", "VoxelSniper", "toggle", "voxelSniperDetectWorldEdit"},
			{"Always Scan Tools", "Other mods", "VoxelSniper", "toggle", "voxelSniperAlwaysScanTools"},
			{"Pause Horizon Y While Aiming", "Other mods", "VoxelSniper", "toggle", "voxelSniperPauseHorizon"},
			{"Pause Terrain Mask While Aiming", "Other mods", "VoxelSniper", "toggle", "voxelSniperPauseTerrainMask"},
			{"Pause Entity Cull While Aiming", "Other mods", "VoxelSniper", "toggle", "voxelSniperPauseEntityCull"},
			{"Sniper Keep Range", "Other mods", "VoxelSniper", "slider_d", "voxelSniperToolRange", "128", "16", "384", "blocks"},
			{"F3 Compact", "Tweaks", "Interface", "toggle", "f3Compact"},
			{"Skip Empty Boss Overlay", "Tweaks", "Interface", "toggle", "skipEmptyBossOverlayEnabled"},
			{"Adaptive World Scale", "Picture", "Scale", "toggle", "renderScaleAdaptive"},
			{"Fog Scale", "Picture", "Fog", "toggle", "fogScaleEnabled"},
			{"Fog Scale Factor", "Picture", "Fog", "percent", "fogScaleFactor", "85", "35", "100", "%"},
			{"Adaptive Culling", "Speed", "Adaptive", "toggle", "adaptiveCullingEnabled"},
			{"Target Frame Rate", "Speed", "Adaptive", "slider_i", "targetFps", "60", "20", "1000", "FPS"},
			{"Minimum Distance Scale", "Speed", "Adaptive", "percent", "minAdaptiveScale", "50", "25", "100", "%"},
			{"Weak-GPU Auto", "Speed", "Adaptive", "toggle", "weakGpuAutoEnabled"},
			{"Weak-GPU Threshold", "Speed", "Adaptive", "slider_i", "weakGpuFpsThreshold", "35", "15", "60", "FPS"},
			{"Low-End Hardware Tune", "Speed", "Adaptive", "toggle", "lowEndHardwareTuneEnabled"},
			{"Laptop Power-Save", "Speed", "Adaptive", "toggle", "laptopPowerSaveEnabled"},
			{"Adaptive Work Budget", "Speed", "Adaptive", "toggle", "adaptiveUploadBudgetEnabled"},
			{"Work-Budget Reserve", "Speed", "Adaptive", "percent", "uploadBudgetFraction", "12", "5", "40", "%"},
			{"Sodium Section Occupancy", "Speed", "Sodium", "toggle", "sectionOccupancyCullingEnabled"},
			{"Native Hotpath", "Speed", "Native", "toggle", "nativeHotpathEnabled"},
			{"Native Frustum Culling", "Speed", "Native", "toggle", "nativeFrustumCullingEnabled"},
			{"Frame-Pacing Workaround", "Speed", "Platform", "toggle", "framePacingFixEnabled"},
			{"Lightmap Cache", "Extras", "CPU", "toggle", "lightmapCacheEnabled"},
			{"Unfocused FPS Cap", "Extras", "CPU", "toggle", "unfocusedFpsCapEnabled"},
			{"Unfocused Cap", "Extras", "CPU", "slider_i", "unfocusedFpsCap", "30", "5", "240", "FPS"},
			{"Skip Far Interpolation", "Extras", "Entity CPU", "toggle", "entityInterpSkipEnabled"},
			{"Interp Skip Distance", "Extras", "Entity CPU", "slider_d", "entityInterpSkipDistance", "48", "16", "160", "blocks"},
			{"Distant Client Ticks", "Extras", "Entity CPU", "toggle", "distantClientTickSkipEnabled"},
			{"Client-Tick Distance", "Extras", "Entity CPU", "slider_d", "distantClientTickDistance", "40", "12", "128", "blocks"},
			{"Client-Tick Interval", "Extras", "Entity CPU", "slider_i", "distantClientTickInterval", "4", "2", "12", "ticks"},
			{"Living Anim Throttle", "Extras", "Entity CPU", "toggle", "livingAnimThrottleEnabled"},
			{"Anim Throttle Distance", "Extras", "Entity CPU", "slider_d", "livingAnimThrottleDistance", "36", "12", "128", "blocks"},
			{"Idle AI Throttle", "Extras", "Entity CPU", "toggle", "idleAiThrottleEnabled"},
			{"Idle AI Full-Rate Distance", "Extras", "Entity CPU", "slider_d", "idleAiFullDistance", "48", "16", "128", "blocks"},
			{"Idle AI Interval", "Extras", "Entity CPU", "slider_i", "idleAiMaxInterval", "10", "2", "20", "ticks"},
			{"Cloud LOD", "Extras", "World extras", "toggle", "cloudLodEnabled"},
			{"Weather Renderer LOD", "Extras", "World extras", "toggle", "weatherRendererLodEnabled"},
			{"Sky Extras Throttle", "Extras", "World extras", "toggle", "skyExtrasThrottleEnabled"},
			{"Ambient Block Ticks", "Extras", "World extras", "toggle", "ambientTickCullEnabled"},
			{"Ambient Tick Interval", "Extras", "World extras", "slider_i", "ambientTickInterval", "2", "1", "8", "ticks"},
			{"Ambient Tick Range", "Extras", "World extras", "slider_i", "ambientTickRange", "12", "4", "16", "blocks"},
			{"World-Border LOD", "Extras", "World extras", "toggle", "worldBorderLodEnabled"},
			{"Map Renderer Throttle", "Extras", "World extras", "toggle", "mapRendererThrottleEnabled"},
			{"Map Interval", "Extras", "World extras", "slider_i", "mapRendererInterval", "4", "1", "12", "ticks"},
			{"Firework Particle Cap", "Extras", "World extras", "toggle", "fireworkParticleCapEnabled"},
			{"Firework Budget", "Extras", "World extras", "slider_i", "maxFireworkParticlesPerTick", "48", "8", "200", ""},
			{"Drip Particle Throttle", "Extras", "World extras", "toggle", "dripParticleThrottleEnabled"},
			{"Hard Particle Cap", "Extras", "World extras", "toggle", "hardParticleCapEnabled"},
			{"Particle Quality Curve", "Tweaks", "Particle keep-rates", "toggle", "particleQualityCurveEnabled"},
			{"Particle Priority", "Tweaks", "Particle keep-rates", "toggle", "particlePriorityEnabled"},
			{"Rain Keep Rate", "Tweaks", "Particle keep-rates", "percent", "rainKeepChance", "15", "0", "100", "%"},
			{"Smoke Keep Rate", "Tweaks", "Particle keep-rates", "percent", "smokeKeepChance", "25", "0", "100", "%"},
			{"Explosion Keep Rate", "Tweaks", "Particle keep-rates", "percent", "explosionKeepChance", "100", "0", "100", "%"},
			{"Fire Keep Rate", "Tweaks", "Particle keep-rates", "percent", "fireSmokeKeepChance", "100", "0", "100", "%"},
			{"Bubble Keep Rate", "Tweaks", "Particle keep-rates", "percent", "bubbleKeepChance", "100", "0", "100", "%"},
			{"High-Priority Keep Rate", "Tweaks", "Particle keep-rates", "percent", "highPriorityKeepChance", "85", "10", "100", "%"},
			{"Low-Priority Keep Rate", "Tweaks", "Particle keep-rates", "percent", "lowPriorityKeepChance", "25", "0", "100", "%"},
			{"F3 Status", "Tweaks", "Interface", "toggle", "f3ShowStatus"},
			{"F3 Details", "Tweaks", "Interface", "toggle", "f3ShowDetails"},
			{"FPS Overlay", "Tweaks", "Interface", "toggle", "fpsOverlayEnabled"},
			{"Overlay X", "Tweaks", "Interface", "slider_i", "fpsOverlayX", "4", "0", "400", "px"},
			{"Overlay Y", "Tweaks", "Interface", "slider_i", "fpsOverlayY", "4", "0", "400", "px"},
			{"Toast Limit", "Tweaks", "Interface", "toggle", "toastLimitEnabled"},
			{"Integrated Server Only", "Tweaks", "Simulation", "toggle", "integratedServerOnly"},
			{"Defer Pathfinding to Lithium", "Tweaks", "Simulation", "toggle", "deferPathfindingToLithium"},
			{"Pathfinding Throttle", "Tweaks", "Simulation", "toggle", "pathfindingThrottleEnabled"},
			{"Pathfinding Full-Rate Distance", "Tweaks", "Simulation", "slider_d", "pathfindingFullDistance", "32", "8", "96", "blocks"},
			{"Pathfinding Maximum Interval", "Tweaks", "Simulation", "slider_i", "pathfindingMaxInterval", "8", "2", "20", "ticks"},
			{"Locate Cache", "Tweaks", "Simulation", "toggle", "locateOptimizeEnabled"},
			{"Locate Cache TTL", "Tweaks", "Simulation", "slider_i", "locateCacheTtlSeconds", "30", "5", "120", "s"},
			{"Accelerated World Load", "Tweaks", "Simulation", "toggle", "fastWorldLoadEnabled"},
			{"World-Load Window", "Tweaks", "Simulation", "slider_i", "fastWorldLoadWindowSeconds", "8", "1", "20", "s"},
			{"World-Load Chunk Boost", "Tweaks", "Simulation", "slider_i", "fastWorldLoadChunkBoost", "6", "1", "16", ""},
			{"Item / XP Tick Throttle", "Tweaks", "Simulation", "toggle", "itemThrottleEnabled"},
			{"Item Throttle Distance", "Tweaks", "Simulation", "slider_d", "itemThrottleStartDistance", "24", "8", "96", "blocks"},
			{"Item Tick Interval", "Tweaks", "Simulation", "slider_i", "itemThrottleMaxInterval", "8", "2", "20", "ticks"},
			{"Defer Fog to Sodium Extra", "Tweaks", "Sodium Extra", "toggle", "deferFogToSodiumExtra"},
			{"Defer Toasts to Sodium Extra", "Tweaks", "Sodium Extra", "toggle", "deferToastsToSodiumExtra"},
			{"Defer Beacons to Sodium Extra", "Tweaks", "Sodium Extra", "toggle", "deferBeaconToSodiumExtra"},
			{"Defer Texture Anim to Sodium Extra", "Tweaks", "Sodium Extra", "toggle", "deferTextureAnimToSodiumExtra"},
			{"Defer Particles to Sodium Extra", "Tweaks", "Sodium Extra", "toggle", "deferParticlesToSodiumExtra"},
	};
}
