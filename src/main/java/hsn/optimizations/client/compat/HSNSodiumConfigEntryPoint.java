package hsn.optimizations.client.compat;

import hsn.optimizations.HSNOptimizations;
import hsn.optimizations.config.HSNConfig;
import hsn.optimizations.optimize.HotPath;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.structure.BooleanOptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Sodium Video Settings pages. Each page is a real tab
 * (General, Entities, Particles, Rendering, Audio, Server).
 */
public class HSNSodiumConfigEntryPoint implements ConfigEntryPoint {

	private static final StorageEventHandler SAVE = () -> {
		HSNConfig cfg = HSNConfig.get();
		cfg.sanitize();
		cfg.save();
		HotPath.rebuild(cfg);
	};

	@Override
	public void registerConfigLate(ConfigBuilder builder) {
		try {
			HSNConfig cfg = HSNConfig.get();
			var mod = builder.registerOwnModOptions()
					.setName("HSN Optimizations")
					.setVersion(HSNConfig.modVersionLabel);
			try {
				mod.setIcon(Identifier.fromNamespaceAndPath("hsn-optimizations", "icon.png"));
			} catch (Throwable ignored) {
			}

			mod.addPage(builder.createOptionPage()
					.setName(Component.literal("General"))
					.addOptionGroup(builder.createOptionGroup()
							.setName(Component.literal("General"))
							.addOption(bool(builder, "mod_enabled", "Enable optimizations",
									"Master switch. Off restores vanilla behavior.",
									() -> cfg.modEnabled, v -> {
										cfg.modEnabled = v;
										HotPath.rebuild(cfg);
									}, true))
							.addOption(bool(builder, "performance_mode", "Performance mode",
									"When frame rate drops, ease distances down to 70% of the sliders.",
									() -> cfg.performanceModeEnabled, v -> {
										cfg.performanceModeEnabled = v;
										HotPath.rebuild(cfg);
									}, true))
							.addOption(bool(builder, "adaptive_culling", "Adaptive culling",
									"Scale distances toward the target frame rate.",
									() -> cfg.adaptiveCullingEnabled, v -> {
										cfg.adaptiveCullingEnabled = v;
										HotPath.rebuild(cfg);
									}, true))
							.addOption(bool(builder, "unfocused_cap", "Unfocused frame cap",
									"Limit frame rate when the window is in the background.",
									() -> cfg.unfocusedFpsCapEnabled, v -> cfg.unfocusedFpsCapEnabled = v, true))
							.addOption(openFull(builder, "full_settings_general"))));

			mod.addPage(builder.createOptionPage()
					.setName(Component.literal("Entities"))
					.addOptionGroup(builder.createOptionGroup()
							.setName(Component.literal("Entities"))
							.addOption(bool(builder, "entity_culling", "Entity culling",
									"Skip entities beyond the configured distance.",
									() -> cfg.entityCullingEnabled, v -> {
										cfg.entityCullingEnabled = v;
										HotPath.rebuild(cfg);
									}, true))
							.addOption(bool(builder, "entity_lod", "Entity level of detail",
									"Use simpler poses on distant mobs.",
									() -> cfg.entityLodStagesEnabled, v -> {
										cfg.entityLodStagesEnabled = v;
										HotPath.rebuild(cfg);
									}, true))
							.addOption(bool(builder, "shadow_culling", "Shadow culling",
									"Hide distant entity shadows.",
									() -> cfg.shadowCullingEnabled, v -> {
										cfg.shadowCullingEnabled = v;
										HotPath.rebuild(cfg);
									}, true))
							.addOption(bool(builder, "name_tags", "Name tag culling",
									"Hide distant name tags.",
									() -> cfg.nameTagCullEnabled, v -> {
										cfg.nameTagCullEnabled = v;
										HotPath.rebuild(cfg);
									}, true))
							.addOption(openFull(builder, "full_settings_entities"))));

			mod.addPage(builder.createOptionPage()
					.setName(Component.literal("Particles"))
					.addOptionGroup(builder.createOptionGroup()
							.setName(Component.literal("Particles"))
							.addOption(bool(builder, "particle_culling", "Particle culling",
									"Limit particle distance and spawn rate.",
									() -> cfg.particleCullingEnabled, v -> {
										cfg.particleCullingEnabled = v;
										HotPath.rebuild(cfg);
									}, true))
							.addOption(bool(builder, "hard_particle_cap", "Hard particle cap",
									"Refuse new particles once the budget is full.",
									() -> cfg.hardParticleCapEnabled, v -> {
										cfg.hardParticleCapEnabled = v;
										HotPath.rebuild(cfg);
									}, true))
							.addOption(bool(builder, "particle_priority", "Particle priority",
									"Keep combat particles ahead of ambient ones.",
									() -> cfg.particlePriorityEnabled, v -> {
										cfg.particlePriorityEnabled = v;
										HotPath.rebuild(cfg);
									}, true))
							.addOption(bool(builder, "drip_throttle", "Drip throttle",
									"Thin dripping water and lava particles.",
									() -> cfg.dripParticleThrottleEnabled, v -> {
										cfg.dripParticleThrottleEnabled = v;
										HotPath.rebuild(cfg);
									}, true))
							.addOption(openFull(builder, "full_settings_particles"))));

			mod.addPage(builder.createOptionPage()
					.setName(Component.literal("Rendering"))
					.addOptionGroup(builder.createOptionGroup()
							.setName(Component.literal("Rendering"))
							.addOption(bool(builder, "render_scale", "World render scale",
									"Draw the world at a lower resolution. The HUD stays native.",
									() -> cfg.renderScaleEnabled, v -> {
										cfg.renderScaleEnabled = v;
										HotPath.rebuild(cfg);
									}, true))
							.addOption(bool(builder, "render_scale_adaptive", "Adaptive render scale",
									"Ease world resolution toward the target frame rate.",
									() -> cfg.renderScaleAdaptive, v -> cfg.renderScaleAdaptive = v, true))
							.addOption(bool(builder, "horizon_y", "Horizon culling",
									"Skip sections below the visible horizon.",
									() -> cfg.horizonYCullEnabled, v -> {
										cfg.horizonYCullEnabled = v;
										HotPath.rebuild(cfg);
									}, true))
							.addOption(bool(builder, "texture_lod", "Block texture LOD",
									"Apply mip bias to distant block textures.",
									() -> cfg.blockTextureLodEnabled, v -> {
										cfg.blockTextureLodEnabled = v;
										HotPath.rebuild(cfg);
									}, true))
							.addOption(bool(builder, "cloud_lod", "Cloud LOD",
									"Simplify distant cloud layers.",
									() -> cfg.cloudLodEnabled, v -> cfg.cloudLodEnabled = v, true))
							.addOption(openFull(builder, "full_settings_rendering"))));

			mod.addPage(builder.createOptionPage()
					.setName(Component.literal("Audio"))
					.addOptionGroup(builder.createOptionGroup()
							.setName(Component.literal("Audio"))
							.addOption(bool(builder, "sound_culling", "Sound distance culling",
									"Skip sounds beyond the configured distance.",
									() -> cfg.soundDistanceCullingEnabled, v -> {
										cfg.soundDistanceCullingEnabled = v;
										HotPath.rebuild(cfg);
									}, true))
							.addOption(bool(builder, "weather_sound", "Weather sound reduction",
									"Thin rain and thunder samples.",
									() -> cfg.weatherSoundReductionEnabled, v -> cfg.weatherSoundReductionEnabled = v, false))
							.addOption(bool(builder, "sound_burst", "Sound burst limit",
									"Cap how many new sounds can start in one tick.",
									() -> cfg.soundBurstLimitEnabled, v -> cfg.soundBurstLimitEnabled = v, false))
							.addOption(openFull(builder, "full_settings_audio"))));

			mod.addPage(builder.createOptionPage()
					.setName(Component.literal("Server"))
					.addOptionGroup(builder.createOptionGroup()
							.setName(Component.literal("Server"))
							.addOption(bool(builder, "pathfinding", "Pathfinding throttle",
									"Reuse paths for distant mobs on the integrated server.",
									() -> cfg.pathfindingThrottleEnabled, v -> cfg.pathfindingThrottleEnabled = v, true))
							.addOption(bool(builder, "idle_ai", "Idle AI throttle",
									"Skip idle goal evaluation for distant mobs.",
									() -> cfg.idleAiThrottleEnabled, v -> cfg.idleAiThrottleEnabled = v, true))
							.addOption(bool(builder, "item_throttle", "Item tick throttle",
									"Slow distant dropped-item simulation.",
									() -> cfg.itemThrottleEnabled, v -> cfg.itemThrottleEnabled = v, false))
							.addOption(bool(builder, "fast_world_load", "Faster world load",
									"Drain extra chunk tasks while a singleplayer world opens.",
									() -> cfg.fastWorldLoadEnabled, v -> cfg.fastWorldLoadEnabled = v, true))
							.addOption(openFull(builder, "full_settings_server"))));

			HSNOptimizations.LOGGER.info("HSN Sodium tabs registered: General, Entities, Particles, Rendering, Audio, Server");
		} catch (Throwable t) {
			HSNOptimizations.LOGGER.warn("Sodium config tabs skipped: {}", t.toString());
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

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static OptionBuilder openFull(ConfigBuilder builder, String path) {
		Object option = builder.createExternalButtonOption(id(path))
				.setName(Component.literal("All settings"))
				.setTooltip(Component.literal("Open the full HSN Optimizations settings screen."))
				.setScreenConsumer(parent -> ClientScreens.open(HSNConfigEntry.createScreen(parent)));
		return (OptionBuilder) option;
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath("hsn-optimizations", path);
	}
}
