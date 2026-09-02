package hsn.modod.client.compat;

import hsn.modod.HSNOptimizations;
import hsn.modod.client.config.HSNConfigScreen;
import hsn.modod.config.HSNConfig;
import hsn.modod.config.WorldRenderShape;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.ControlValueFormatter;
import net.caffeinemc.mods.sodium.api.config.structure.BooleanOptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.IntegerOptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionGroupBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Official Sodium Config API entrypoint ({@code sodium:config_api_user}).
 * Registers a dedicated "HSN Optimizations" tab in Sodium Video Settings.
 * Reese's Sodium Options picks the same page up automatically.
 */
public class HSNSodiumConfigEntryPoint implements ConfigEntryPoint {

	private static final StorageEventHandler SAVE = () -> HSNConfig.get().save();

	@Override
	public void registerConfigLate(ConfigBuilder builder) {
		try {
			HSNConfig cfg = HSNConfig.get();

			OptionGroupBuilder performance = builder.createOptionGroup()
					.setName(Component.literal("Performance"))
					.addOption(bool(builder, "performance_mode",
							"Performance Mode (F6)",
							"Aggressive adaptive culling for low FPS / weak GPUs.",
							() -> cfg.performanceModeEnabled,
							v -> cfg.performanceModeEnabled = v,
							false))
					.addOption(bool(builder, "adaptive_culling",
							"Adaptive Culling",
							"Scale cull distances based on live FPS.",
							() -> cfg.adaptiveCullingEnabled,
							v -> cfg.adaptiveCullingEnabled = v,
							true))
					.addOption(slider(builder, "target_fps",
							"Target FPS",
							"Adaptive culling aims for this FPS.",
							() -> cfg.targetFps,
							v -> cfg.targetFps = v,
							60, 20, 120, 1,
							v -> Component.literal(v + " FPS")))
					.addOption(bool(builder, "weak_gpu_auto",
							"Weak-GPU Auto Layer",
							"Tighten settings automatically when smoothed FPS stays low.",
							() -> cfg.weakGpuAutoEnabled,
							v -> cfg.weakGpuAutoEnabled = v,
							true))
					.addOption(slider(builder, "weak_gpu_fps",
							"Weak-GPU FPS Threshold",
							"Extra aggression engages under this smoothed FPS.",
							() -> cfg.weakGpuFpsThreshold,
							v -> cfg.weakGpuFpsThreshold = v,
							35, 15, 60, 1,
							v -> Component.literal(v + " FPS")))
					.addOption(bool(builder, "frame_pacing",
							"GPU Frame-Pacing Fix",
							"Old Intel HD only: skip immutable GL buffer storage. Off by default. Restart after change.",
							() -> cfg.framePacingFixEnabled,
							v -> cfg.framePacingFixEnabled = v,
							false))
					.addOption(bool(builder, "fps_overlay",
							"FPS Overlay (F7)",
							"Show live FPS + cull scale overlay.",
							() -> cfg.fpsOverlayEnabled,
							v -> cfg.fpsOverlayEnabled = v,
							false))
					.addOption(bool(builder, "f3_details",
							"Extra F3 Details",
							"Distances and feature flags on the F3 right column.",
							() -> cfg.f3ShowDetails,
							v -> cfg.f3ShowDetails = v,
							true));

			OptionGroupBuilder entities = builder.createOptionGroup()
					.setName(Component.literal("Culling"))
					.addOption(bool(builder, "entity_culling",
							"Entity Culling",
							"Distance-based entity / item / XP / decoration cull.",
							() -> cfg.entityCullingEnabled,
							v -> cfg.entityCullingEnabled = v,
							true))
					.addOption(slider(builder, "entity_distance",
							"Max Entity Distance",
							"Hide entities beyond this distance.",
							() -> (int) cfg.maxEntityRenderDistance,
							v -> cfg.maxEntityRenderDistance = v,
							32, 8, 128, 1,
							blocks()))
					.addOption(bool(builder, "shadow_culling",
							"Shadow Culling",
							"Hide entity shadows beyond a distance.",
							() -> cfg.shadowCullingEnabled,
							v -> cfg.shadowCullingEnabled = v,
							true))
					.addOption(slider(builder, "shadow_distance",
							"Max Shadow Distance",
							"Hide shadows beyond this distance.",
							() -> (int) cfg.maxShadowDistance,
							v -> cfg.maxShadowDistance = v,
							12, 2, 32, 1,
							blocks()))
					.addOption(bool(builder, "block_entity_culling",
							"Block Entity Culling",
							"Distance-cull chests, signs, banners and similar.",
							() -> cfg.blockEntityCullingEnabled,
							v -> cfg.blockEntityCullingEnabled = v,
							true))
					.addOption(bool(builder, "glow_outline",
							"Glow Outline Culling",
							"Hide glowing outlines beyond a distance.",
							() -> cfg.glowOutlineCullingEnabled,
							v -> cfg.glowOutlineCullingEnabled = v,
							true))
					.addOption(bool(builder, "beacon_beams",
							"Beacon Beam Culling",
							"Hide distant beacon beams.",
							() -> cfg.beaconBeamCullingEnabled,
							v -> cfg.beaconBeamCullingEnabled = v,
							true));

			OptionGroupBuilder particles = builder.createOptionGroup()
					.setName(Component.literal("Particles"))
					.addOption(bool(builder, "particle_culling",
							"Particle Culling",
							"Distance + budget + priority particle limits.",
							() -> cfg.particleCullingEnabled,
							v -> cfg.particleCullingEnabled = v,
							true))
					.addOption(slider(builder, "max_particles",
							"Max Particles",
							"Soft particle cap. Lower = more FPS.",
							() -> cfg.maxParticles,
							v -> cfg.maxParticles = v,
							400, 50, 2000, 10,
							v -> Component.literal(v + " particles")))
					.addOption(slider(builder, "particle_distance",
							"Max Particle Distance",
							"Spawn particles only within this distance.",
							() -> (int) cfg.maxParticleDistance,
							v -> cfg.maxParticleDistance = v,
							16, 4, 64, 1,
							blocks()))
					.addOption(bool(builder, "particle_priority",
							"Priority Particles",
							"Keep combat/player effects first when over budget.",
							() -> cfg.particlePriorityEnabled,
							v -> cfg.particlePriorityEnabled = v,
							true));

			OptionGroupBuilder server = builder.createOptionGroup()
					.setName(Component.literal("Simulation"))
					.addOption(bool(builder, "pathfinding",
							"Pathfinding Throttle",
							"Reuse current path and shrink A* node budget for distant idle mobs.",
							() -> cfg.pathfindingThrottleEnabled,
							v -> cfg.pathfindingThrottleEnabled = v,
							true))
					.addOption(slider(builder, "path_full_dist",
							"Pathfinding Full-Rate Distance",
							"Inside this range pathfinding stays vanilla.",
							() -> (int) cfg.pathfindingFullDistance,
							v -> cfg.pathfindingFullDistance = v,
							32, 8, 96, 1,
							blocks()))
					.addOption(slider(builder, "path_interval",
							"Path Rebuild Max Interval",
							"How many ticks between A* rebuilds when far from players.",
							() -> cfg.pathfindingMaxInterval,
							v -> cfg.pathfindingMaxInterval = v,
							8, 2, 20, 1,
							v -> Component.literal("every " + v + " ticks")))
					.addOption(bool(builder, "item_throttle",
							"Item / XP Tick Throttle",
							"Off by default. Slows distant drop physics; despawn age still advances.",
							() -> cfg.itemThrottleEnabled,
							v -> cfg.itemThrottleEnabled = v,
							false))
					.addOption(bool(builder, "locate_opt",
							"Faster /locate",
							"Cache locate results and skip search when structures are disabled.",
							() -> cfg.locateOptimizeEnabled,
							v -> cfg.locateOptimizeEnabled = v,
							true))
					.addOption(bool(builder, "fast_load",
							"Faster World Load",
							"Drain extra chunk tasks each server tick so terrain opens faster.",
							() -> cfg.fastWorldLoadEnabled,
							v -> cfg.fastWorldLoadEnabled = v,
							false));

			OptionGroupBuilder unique = builder.createOptionGroup()
					.setName(Component.literal("Rendering"))
					.addOption(bool(builder, "texture_anim",
							"Texture Animation Throttle",
							"Slow water/lava/fire atlas updates under load.",
							() -> cfg.textureAnimThrottleEnabled,
							v -> cfg.textureAnimThrottleEnabled = v,
							true))
					.addOption(bool(builder, "progressive_lod",
							"Progressive LOD",
							"Quality falls off near the edge of max distance.",
							() -> cfg.progressiveLodEnabled,
							v -> cfg.progressiveLodEnabled = v,
							true))
					.addOption(bool(builder, "item_spin",
							"Item Spin Throttle",
							"Slow or freeze distant item rotation under load.",
							() -> cfg.itemSpinThrottleEnabled,
							v -> cfg.itemSpinThrottleEnabled = v,
							true))
					.addOption(bool(builder, "sound_culling",
							"Sound Distance Culling",
							"Skip distant sound events.",
							() -> cfg.soundDistanceCullingEnabled,
							v -> cfg.soundDistanceCullingEnabled = v,
							true))
					.addOption(builder.createExternalButtonOption(id("full_settings"))
							.setName(Component.literal("Open Full HSN Settings…"))
							.setTooltip(Component.literal("Opens the complete Cloth Config screen (all options and presets)."))
							.setScreenConsumer(parent -> ClientScreens.open(HSNConfigScreen.create(parent))));

			boolean shapeTaken = HSNModCompat.shapeModPresent();
			String takenTip = "Another terrain-mask mod is installed. HSN leaves this disabled.";
			OptionGroupBuilder terrain = builder.createOptionGroup()
					.setName(Component.literal("Terrain"))
					.addOption(bool(builder, "shaped_rendering",
							"Mask distant terrain",
							shapeTaken ? takenTip : "Hide far chunk corners outside the selected mask. Off keeps vanilla square drawing.",
							() -> cfg.circularRenderingEnabled && !shapeTaken,
							v -> {
								if (!HSNModCompat.shapeModPresent()) {
									cfg.circularRenderingEnabled = v;
								}
							},
							false).setEnabled(!shapeTaken).setName(shapeName("Mask distant terrain", shapeTaken)))
					.addOption(enumOpt(builder, "render_shape", WorldRenderShape.class,
							"Mask",
							shapeTaken ? takenTip : "Square, Circle, Hexagon, or Front half. Nearby terrain is always drawn.",
							() -> cfg.worldRenderShape,
							v -> {
								if (!HSNModCompat.shapeModPresent()) {
									cfg.worldRenderShape = v;
								}
							},
							WorldRenderShape.OFF).setEnabled(!shapeTaken).setName(shapeName("Mask", shapeTaken)))
					.addOption(slider(builder, "shape_size",
							"Mask coverage",
							shapeTaken ? takenTip : "Percent of view distance covered by the mask.",
							() -> (int) Math.round(cfg.circularRadiusScale * 100),
							v -> {
								if (!HSNModCompat.shapeModPresent()) {
									cfg.circularRadiusScale = v / 100.0;
								}
							},
							100, 25, 100, 1,
							v -> Component.literal(v + "%").withStyle(shapeTaken ? ChatFormatting.STRIKETHROUGH : ChatFormatting.WHITE))
							.setEnabled(!shapeTaken).setName(shapeName("Mask coverage", shapeTaken)));

			var mod = builder.registerOwnModOptions()
					.setName("HSN Optimizations")
					.setVersion(HSNConfig.modVersionLabel);
			try {
				mod.setIcon(Identifier.fromNamespaceAndPath("hsn-optimizations", "icon.png"));
			} catch (Throwable ignored) {
			}
			mod.addPage(builder.createOptionPage()
					.setName(Component.literal("HSN Optimizations"))
					.addOptionGroup(performance)
					.addOptionGroup(particles)
					.addOptionGroup(entities));
			mod.addPage(builder.createOptionPage()
					.setName(Component.literal("HSN Extra"))
					.addOptionGroup(terrain)
					.addOptionGroup(server)
					.addOptionGroup(unique));

			HSNOptimizations.LOGGER.info("HSN tab registered with Sodium Config API");
		} catch (Throwable t) {
			HSNOptimizations.LOGGER.warn("Failed to register HSN page with Sodium Config API: {}", t.toString());
		}
	}

	private static BooleanOptionBuilder bool(ConfigBuilder builder, String path,
											 String name, String tooltip,
											 Supplier<Boolean> getter, Consumer<Boolean> setter,
											 boolean defaultValue) {
		return builder.createBooleanOption(id(path))
				.setName(Component.literal(name))
				.setTooltip(Component.literal(tooltip))
				.setStorageHandler(SAVE)
				.setBinding(setter, getter)
				.setDefaultValue(defaultValue);
	}

	private static IntegerOptionBuilder slider(ConfigBuilder builder, String path,
											   String name, String tooltip,
											   Supplier<Integer> getter, Consumer<Integer> setter,
											   int defaultValue, int min, int max, int step,
											   ControlValueFormatter formatter) {
		return builder.createIntegerOption(id(path))
				.setName(Component.literal(name))
				.setTooltip(Component.literal(tooltip))
				.setStorageHandler(SAVE)
				.setBinding(setter, getter)
				.setDefaultValue(defaultValue)
				.setRange(min, max, step)
				.setValueFormatter(formatter);
	}

	private static <E extends Enum<E>> net.caffeinemc.mods.sodium.api.config.structure.EnumOptionBuilder<E> enumOpt(
			ConfigBuilder builder, String path, Class<E> type,
			String name, String tooltip,
			Supplier<E> getter, Consumer<E> setter, E defaultValue) {
		return builder.createEnumOption(id(path), type)
				.setName(Component.literal(name))
				.setTooltip(Component.literal(tooltip))
				.setStorageHandler(SAVE)
				.setBinding(setter, getter)
				.setDefaultValue(defaultValue);
	}

	private static Component shapeName(String name, boolean shapeTaken) {
		if (shapeTaken) {
			return Component.literal(name).withStyle(ChatFormatting.STRIKETHROUGH, ChatFormatting.DARK_GRAY);
		}
		return Component.literal(name);
	}

	private static ControlValueFormatter blocks() {
		return v -> Component.literal(v + " blocks");
	}

	private static ControlValueFormatter chunks() {
		return v -> Component.literal(v + " chunks");
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath("hsn-optimizations", path);
	}
}
