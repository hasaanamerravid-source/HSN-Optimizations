package hsn.modod.client.compat;

import hsn.modod.HSNOptimizations;
import hsn.modod.client.config.HSNConfigScreen;
import hsn.modod.config.HSNConfig;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.ControlValueFormatter;
import net.caffeinemc.mods.sodium.api.config.structure.BooleanOptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.IntegerOptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionGroupBuilder;
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
					.setName(Component.literal("Performance Tweaks"))
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
					.addOption(bool(builder, "fps_overlay",
							"FPS Overlay (F7)",
							"Show live FPS + cull scale overlay.",
							() -> cfg.fpsOverlayEnabled,
							v -> cfg.fpsOverlayEnabled = v,
							false));

			OptionGroupBuilder entities = builder.createOptionGroup()
					.setName(Component.literal("Entity & Graphics"))
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

			OptionGroupBuilder unique = builder.createOptionGroup()
					.setName(Component.literal("Unique Helpers"))
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
					.addOptionGroup(entities)
					.addOptionGroup(particles)
					.addOptionGroup(unique));

			HSNOptimizations.LOGGER.info("HSN Optimizations tab registered with Sodium Config API");
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

	private static ControlValueFormatter blocks() {
		return v -> Component.literal(v + " blocks");
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath("hsn-optimizations", path);
	}
}
