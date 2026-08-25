package hsn.modod.client.config;

import hsn.modod.config.HSNConfig;
import hsn.modod.config.HSNConfig.Preset;
import hsn.modod.config.HSNPresets;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Cloth Config — left category sidebar (globalized), each category its own page.
 * Sub-categories group related options. Transparent = default MC panorama.
 */
public final class HSNConfigScreen {

	private HSNConfigScreen() {
	}

	private static MutableComponent cat(String text, ChatFormatting color) {
		return Component.literal(text).withStyle(color, ChatFormatting.BOLD);
	}

	private static MutableComponent label(String text) {
		return Component.literal(text).withStyle(ChatFormatting.WHITE);
	}

	private static MutableComponent hint(String text) {
		return Component.literal(text).withStyle(ChatFormatting.GRAY);
	}

	private static MutableComponent unit(int value, String unit) {
		return Component.literal(value + " " + unit).withStyle(ChatFormatting.AQUA);
	}

	private static MutableComponent percent(int value) {
		return Component.literal(value + "%").withStyle(ChatFormatting.GREEN);
	}

	public static Screen create(Screen parent) {
		HSNConfig cfg = HSNConfig.get();

		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Component.literal("HSN Optimizations")
						.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
				.setTransparentBackground(true)
				.setSavingRunnable(cfg::save);

		// Left sidebar categories, each is its own page
		builder.setGlobalized(true);
		builder.setGlobalizedExpanded(true);
		builder.setAlwaysShowTabs(false);

		ConfigEntryBuilder e = builder.entryBuilder();

		// ── GENERAL ──────────────────────────────────────────────
		ConfigCategory gen = builder.getOrCreateCategory(cat(" General", ChatFormatting.YELLOW));

		gen.addEntry(e.startTextDescription(
				Component.literal("Lightweight client culling for weak hardware.")
						.withStyle(ChatFormatting.GRAY)).build());
		gen.addEntry(e.startTextDescription(
				Component.literal("Hotkeys: ")
						.append(Component.literal("F6").withStyle(ChatFormatting.AQUA))
						.append(Component.literal(" Perf  ").withStyle(ChatFormatting.DARK_GRAY))
						.append(Component.literal("F7").withStyle(ChatFormatting.AQUA))
						.append(Component.literal(" FPS  ").withStyle(ChatFormatting.DARK_GRAY))
						.append(Component.literal("F8").withStyle(ChatFormatting.AQUA))
						.append(Component.literal(" ULTRA  ").withStyle(ChatFormatting.DARK_GRAY))
						.append(Component.literal("F9").withStyle(ChatFormatting.AQUA))
						.append(Component.literal(" SAFE").withStyle(ChatFormatting.DARK_GRAY))
		).build());

		gen.addEntry(e.startEnumSelector(label("Performance preset"), Preset.class, cfg.lastAppliedPreset)
				.setDefaultValue(Preset.BALANCED)
				.setTooltip(hint("Applies a full set of distances and keep rates at once."))
				.setSaveConsumer(v -> {
					cfg.lastAppliedPreset = v;
					HSNPresets.apply(cfg, v);
				}).build());

		gen.addEntry(e.startBooleanToggle(label("Show HSN status on F3"), cfg.f3ShowStatus)
				.setDefaultValue(true)
				.setTooltip(hint("Green HSN header + status lines on the debug screen."))
				.setSaveConsumer(v -> cfg.f3ShowStatus = v).build());

		gen.addEntry(e.startBooleanToggle(label("FPS overlay"), cfg.fpsOverlayEnabled)
				.setDefaultValue(false)
				.setTooltip(hint("Floating FPS counter. Toggle with F7."))
				.setSaveConsumer(v -> cfg.fpsOverlayEnabled = v).build());

		SubCategoryBuilder adaptive = e.startSubCategory(Component.literal("Adaptive culling")
				.withStyle(ChatFormatting.AQUA));
		adaptive.setExpanded(true);
		adaptive.add(e.startBooleanToggle(label("Enable adaptive culling"), cfg.adaptiveCullingEnabled)
				.setDefaultValue(true)
				.setTooltip(hint("Tightens distances automatically when FPS drops."))
				.setSaveConsumer(v -> cfg.adaptiveCullingEnabled = v).build());
		adaptive.add(e.startIntSlider(label("Target FPS"), cfg.targetFps, 20, 120)
				.setDefaultValue(60)
				.setTextGetter(v -> unit(v, "FPS"))
				.setTooltip(hint("Adaptive culling aims for this FPS."))
				.setSaveConsumer(v -> cfg.targetFps = v).build());
		gen.addEntry(adaptive.build());

		// ── PARTICLES ────────────────────────────────────────────
		ConfigCategory particles = builder.getOrCreateCategory(cat(" Particles", ChatFormatting.AQUA));

		particles.addEntry(e.startBooleanToggle(label("Enable particle culling"), cfg.particleCullingEnabled)
				.setDefaultValue(true)
				.setTooltip(hint("Master switch for particle limits."))
				.setSaveConsumer(v -> cfg.particleCullingEnabled = v).build());

		particles.addEntry(e.startIntSlider(label("Max particles"), cfg.maxParticles, 50, 2000)
				.setDefaultValue(400)
				.setTextGetter(v -> unit(v, "particles"))
				.setTooltip(hint("Soft cap. Lower = more FPS."))
				.setSaveConsumer(v -> cfg.maxParticles = v).build());

		particles.addEntry(e.startIntSlider(label("Max particle distance"), (int) cfg.maxParticleDistance, 4, 64)
				.setDefaultValue(16)
				.setTextGetter(v -> unit(v, "blocks"))
				.setSaveConsumer(v -> cfg.maxParticleDistance = v).build());

		SubCategoryBuilder keep = e.startSubCategory(Component.literal("Keep chances")
				.withStyle(ChatFormatting.AQUA));
		keep.setExpanded(true);
		keep.add(e.startIntSlider(label("Rain"), (int) Math.round(cfg.rainKeepChance * 100), 0, 100)
				.setDefaultValue(15)
				.setTextGetter(HSNConfigScreen::percent)
				.setSaveConsumer(v -> cfg.rainKeepChance = v / 100.0).build());
		keep.add(e.startIntSlider(label("Smoke / fire"), (int) Math.round(cfg.smokeKeepChance * 100), 0, 100)
				.setDefaultValue(25)
				.setTextGetter(HSNConfigScreen::percent)
				.setSaveConsumer(v -> cfg.smokeKeepChance = v / 100.0).build());
		particles.addEntry(keep.build());

		SubCategoryBuilder prio = e.startSubCategory(Component.literal("Priority system (unique)")
				.withStyle(ChatFormatting.GOLD));
		prio.setExpanded(true);
		prio.add(e.startBooleanToggle(label("Enable priority particles"), cfg.particlePriorityEnabled)
				.setDefaultValue(true)
				.setTooltip(hint("Important particles kept more often than filler ones."))
				.setSaveConsumer(v -> cfg.particlePriorityEnabled = v).build());
		prio.add(e.startIntSlider(label("High-priority keep"), (int) Math.round(cfg.highPriorityKeepChance * 100), 10, 100)
				.setDefaultValue(85)
				.setTextGetter(HSNConfigScreen::percent)
				.setSaveConsumer(v -> cfg.highPriorityKeepChance = v / 100.0).build());
		prio.add(e.startIntSlider(label("Low-priority keep"), (int) Math.round(cfg.lowPriorityKeepChance * 100), 0, 100)
				.setDefaultValue(25)
				.setTextGetter(HSNConfigScreen::percent)
				.setSaveConsumer(v -> cfg.lowPriorityKeepChance = v / 100.0).build());
		particles.addEntry(prio.build());

		// ── ENTITIES ─────────────────────────────────────────────
		ConfigCategory entities = builder.getOrCreateCategory(cat(" Entities", ChatFormatting.GREEN));

		entities.addEntry(e.startBooleanToggle(label("Enable entity culling"), cfg.entityCullingEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(v -> cfg.entityCullingEnabled = v).build());

		entities.addEntry(e.startIntSlider(label("Max entity distance"), (int) cfg.maxEntityRenderDistance, 8, 128)
				.setDefaultValue(32)
				.setTextGetter(v -> unit(v, "blocks"))
				.setSaveConsumer(v -> cfg.maxEntityRenderDistance = v).build());

		entities.addEntry(e.startIntSlider(label("Max item distance"), (int) cfg.maxItemEntityRenderDistance, 4, 64)
				.setDefaultValue(20)
				.setTextGetter(v -> unit(v, "blocks"))
				.setSaveConsumer(v -> cfg.maxItemEntityRenderDistance = v).build());

		entities.addEntry(e.startIntSlider(label("Max XP-orb distance"), (int) cfg.maxXpOrbRenderDistance, 4, 48)
				.setDefaultValue(16)
				.setTextGetter(v -> unit(v, "blocks"))
				.setSaveConsumer(v -> cfg.maxXpOrbRenderDistance = v).build());

		SubCategoryBuilder shadows = e.startSubCategory(Component.literal("Shadows & name tags")
				.withStyle(ChatFormatting.GREEN));
		shadows.setExpanded(false);
		shadows.add(e.startBooleanToggle(label("Cull shadows"), cfg.shadowCullingEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(v -> cfg.shadowCullingEnabled = v).build());
		shadows.add(e.startIntSlider(label("Max shadow distance"), (int) cfg.maxShadowDistance, 2, 32)
				.setDefaultValue(12)
				.setTextGetter(v -> unit(v, "blocks"))
				.setSaveConsumer(v -> cfg.maxShadowDistance = v).build());
		shadows.add(e.startBooleanToggle(label("Cull name tags"), cfg.nameTagCullEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(v -> cfg.nameTagCullEnabled = v).build());
		shadows.add(e.startIntSlider(label("Max name-tag distance"), (int) cfg.maxNameTagDistance, 4, 64)
				.setDefaultValue(24)
				.setTextGetter(v -> unit(v, "blocks"))
				.setSaveConsumer(v -> cfg.maxNameTagDistance = v).build());
		entities.addEntry(shadows.build());

		SubCategoryBuilder extras = e.startSubCategory(Component.literal("Extras")
				.withStyle(ChatFormatting.GREEN));
		extras.setExpanded(false);
		extras.add(e.startBooleanToggle(label("Throttle item spin under load"), cfg.itemSpinThrottleEnabled)
				.setDefaultValue(true)
				.setTooltip(hint("Freezes item rotation when FPS is low."))
				.setSaveConsumer(v -> cfg.itemSpinThrottleEnabled = v).build());
		extras.add(e.startBooleanToggle(label("Cull glow outlines"), cfg.glowOutlineCullingEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(v -> cfg.glowOutlineCullingEnabled = v).build());
		extras.add(e.startIntSlider(label("Max glow distance"), (int) cfg.maxGlowOutlineDistance, 4, 64)
				.setDefaultValue(28)
				.setTextGetter(v -> unit(v, "blocks"))
				.setSaveConsumer(v -> cfg.maxGlowOutlineDistance = v).build());
		entities.addEntry(extras.build());

		// ── BLOCK ENTITIES ───────────────────────────────────────
		ConfigCategory be = builder.getOrCreateCategory(cat(" Block Entities", ChatFormatting.LIGHT_PURPLE));

		be.addEntry(e.startBooleanToggle(label("Enable block-entity culling"), cfg.blockEntityCullingEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(v -> cfg.blockEntityCullingEnabled = v).build());

		be.addEntry(e.startIntSlider(label("Max block-entity distance"), (int) cfg.maxBlockEntityRenderDistance, 4, 64)
				.setDefaultValue(24)
				.setTextGetter(v -> unit(v, "blocks"))
				.setSaveConsumer(v -> cfg.maxBlockEntityRenderDistance = v).build());

		SubCategoryBuilder lod = e.startSubCategory(Component.literal("LOD (unique)")
				.withStyle(ChatFormatting.GOLD));
		lod.setExpanded(true);
		lod.add(e.startBooleanToggle(label("Progressive LOD"), cfg.progressiveLodEnabled)
				.setDefaultValue(true)
				.setTooltip(hint("Quality falls off near the edge of max distance. Does not change render distance."))
				.setSaveConsumer(v -> cfg.progressiveLodEnabled = v).build());
		lod.add(e.startIntSlider(label("LOD start % of max dist"), (int) (cfg.progressiveLodStart * 100), 20, 90)
				.setDefaultValue(50)
				.setTextGetter(v -> percent(v))
				.setSaveConsumer(v -> cfg.progressiveLodStart = v / 100.0).build());
		lod.add(e.startIntSlider(label("Min quality at edge %"), (int) (cfg.progressiveLodMinQuality * 100), 5, 50)
				.setDefaultValue(15)
				.setTextGetter(v -> percent(v))
				.setSaveConsumer(v -> cfg.progressiveLodMinQuality = v / 100.0).build());
		lod.add(e.startBooleanToggle(label("Entity LOD stages"), cfg.entityLodStagesEnabled)
				.setDefaultValue(true)
				.setTooltip(hint("Stage 0 full → 1 mild anim skip → 2 heavy → 3 near-cull."))
				.setSaveConsumer(v -> cfg.entityLodStagesEnabled = v).build());
		lod.add(e.startBooleanToggle(label("Particle quality curve"), cfg.particleQualityCurveEnabled)
				.setDefaultValue(true)
				.setTooltip(hint("Particle keep-chance drops smoothly with distance."))
				.setSaveConsumer(v -> cfg.particleQualityCurveEnabled = v).build());
		lod.add(e.startBooleanToggle(label("Block-entity LOD"), cfg.blockEntityLodEnabled)
				.setDefaultValue(true)
				.setTooltip(hint("Under load, cull expensive BEs earlier."))
				.setSaveConsumer(v -> cfg.blockEntityLodEnabled = v).build());
		lod.add(e.startIntSlider(label("BE LOD start distance"), (int) cfg.blockEntityLodDistance, 4, 48)
				.setDefaultValue(14)
				.setTextGetter(v -> unit(v, "blocks"))
				.setSaveConsumer(v -> cfg.blockEntityLodDistance = v).build());
		be.addEntry(lod.build());

		be.addEntry(e.startBooleanToggle(label("Cull beacon beams"), cfg.beaconBeamCullingEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(v -> cfg.beaconBeamCullingEnabled = v).build());
		be.addEntry(e.startIntSlider(label("Max beacon distance"), (int) cfg.maxBeaconBeamDistance, 8, 128)
				.setDefaultValue(48)
				.setTextGetter(v -> unit(v, "blocks"))
				.setSaveConsumer(v -> cfg.maxBeaconBeamDistance = v).build());

		// ── TEXTURES & SKY ───────────────────────────────────────
		ConfigCategory tex = builder.getOrCreateCategory(cat(" Textures & Sky", ChatFormatting.GOLD));

		SubCategoryBuilder anim = e.startSubCategory(Component.literal("Animated textures (unique)")
				.withStyle(ChatFormatting.GOLD));
		anim.setExpanded(true);
		anim.add(e.startBooleanToggle(label("Throttle animated textures"), cfg.textureAnimThrottleEnabled)
				.setDefaultValue(true)
				.setTooltip(hint("Slows water/lava/fire atlas updates under load."))
				.setSaveConsumer(v -> cfg.textureAnimThrottleEnabled = v).build());
		anim.add(e.startBooleanToggle(label("Use adaptive interval"), cfg.textureAnimUseAdaptive)
				.setDefaultValue(true)
				.setSaveConsumer(v -> cfg.textureAnimUseAdaptive = v).build());
		anim.add(e.startIntSlider(label("Base interval"), cfg.textureAnimInterval, 1, 8)
				.setDefaultValue(1)
				.setTextGetter(v -> Component.literal(v == 1 ? "every tick" : "every " + v + " ticks")
						.withStyle(ChatFormatting.AQUA))
				.setTooltip(hint("1 = full speed (no throttle)."))
				.setSaveConsumer(v -> cfg.textureAnimInterval = v).build());
		anim.add(e.startIntSlider(label("Max interval under load"), cfg.textureAnimMaxInterval, 1, 12)
				.setDefaultValue(4)
				.setTextGetter(v -> Component.literal("every " + v + " ticks").withStyle(ChatFormatting.AQUA))
				.setSaveConsumer(v -> cfg.textureAnimMaxInterval = v).build());
		tex.addEntry(anim.build());

		SubCategoryBuilder clouds = e.startSubCategory(Component.literal("Clouds")
				.withStyle(ChatFormatting.AQUA));
		clouds.setExpanded(false);
		clouds.add(e.startBooleanToggle(label("Thin / skip clouds under load"), cfg.cloudCullingEnabled)
				.setDefaultValue(false) // safer default — was causing flashes when mis-targeted
				.setTooltip(hint("Only affects real cloud renderer if present. Off by default for safety."))
				.setSaveConsumer(v -> cfg.cloudCullingEnabled = v).build());
		clouds.add(e.startIntSlider(label("Cloud density keep"), (int) Math.round(cfg.cloudDensityKeepChance * 100), 10, 100)
				.setDefaultValue(55)
				.setTextGetter(HSNConfigScreen::percent)
				.setSaveConsumer(v -> cfg.cloudDensityKeepChance = v / 100.0).build());
		tex.addEntry(clouds.build());

		// ── WEAK GPU ─────────────────────────────────────────────
		ConfigCategory weak = builder.getOrCreateCategory(cat(" Weak GPU Auto", ChatFormatting.RED));

		weak.addEntry(e.startTextDescription(
				hint("Auto-tightens settings when FPS stays low (Intel HD class GPUs).")).build());

		weak.addEntry(e.startBooleanToggle(label("Enable weak-GPU auto layer"), cfg.weakGpuAutoEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(v -> cfg.weakGpuAutoEnabled = v).build());

		weak.addEntry(e.startIntSlider(label("FPS threshold"), cfg.weakGpuFpsThreshold, 15, 60)
				.setDefaultValue(35)
				.setTextGetter(v -> unit(v, "FPS"))
				.setTooltip(hint("If smoothed FPS stays under this, extra aggression engages."))
				.setSaveConsumer(v -> cfg.weakGpuFpsThreshold = v).build());

		weak.addEntry(e.startBooleanToggle(label("Performance mode (manual)"), cfg.performanceModeEnabled)
				.setDefaultValue(false)
				.setTooltip(hint("Force aggressive settings now. Toggle with F6."))
				.setSaveConsumer(v -> cfg.performanceModeEnabled = v).build());

		// ── OTHER ────────────────────────────────────────────────
		ConfigCategory other = builder.getOrCreateCategory(cat(" Other", ChatFormatting.DARK_AQUA));

		other.addEntry(e.startBooleanToggle(label("Sound distance culling"), cfg.soundDistanceCullingEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(v -> cfg.soundDistanceCullingEnabled = v).build());
		other.addEntry(e.startIntSlider(label("Max sound distance"), (int) cfg.maxSoundDistance, 4, 64)
				.setDefaultValue(24)
				.setTextGetter(v -> unit(v, "blocks"))
				.setSaveConsumer(v -> cfg.maxSoundDistance = v).build());
		other.addEntry(e.startBooleanToggle(label("Reduce weather sounds"), cfg.weatherSoundReductionEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(v -> cfg.weatherSoundReductionEnabled = v).build());
		other.addEntry(e.startBooleanToggle(label("Merge nearby items (server)"), cfg.itemMergeEnabled)
				.setDefaultValue(true)
				.setTooltip(hint("Combines close dropped items to reduce entity count."))
				.setSaveConsumer(v -> cfg.itemMergeEnabled = v).build());

		return builder.build();
	}
}
