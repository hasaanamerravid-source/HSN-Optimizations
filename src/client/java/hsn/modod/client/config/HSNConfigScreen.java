package hsn.modod.client.config;

import hsn.modod.config.HSNConfig;
import hsn.modod.config.HSNConfig.Preset;
import hsn.modod.config.HSNPresets;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Cloth Config screen — left-side categories, sliders, friendly labels & light colors.
 */
public final class HSNConfigScreen {

	private HSNConfigScreen() {
	}

	// ── helpers for colored / styled text ──────────────────────────
	private static MutableComponent title(String text, ChatFormatting color) {
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
				.setTitle(Component.literal("HSN Optimizations").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
				.setSavingRunnable(cfg::save);

		// Left-side category list + right-side options (globalized layout)
		builder.setGlobalized(true);
		builder.setGlobalizedExpanded(true);
		builder.setAlwaysShowTabs(false); // hide top tabs; categories live on the left

		ConfigEntryBuilder e = builder.entryBuilder();

		// ══════════════════════════════════════════════════════════
		//  GENERAL
		// ══════════════════════════════════════════════════════════
		ConfigCategory gen = builder.getOrCreateCategory(
				title(" General", ChatFormatting.YELLOW));

		gen.addEntry(e.startTextDescription(
				Component.literal("Lightweight client culling for weak hardware.")
						.withStyle(ChatFormatting.GRAY)).build());
		gen.addEntry(e.startTextDescription(
				Component.literal("Works great together with Sodium / Iris.")
						.withStyle(ChatFormatting.DARK_GRAY)).build());
		gen.addEntry(e.startTextDescription(
				Component.literal("Hotkeys:  ")
						.append(Component.literal("F7").withStyle(ChatFormatting.AQUA))
						.append(Component.literal(" = FPS overlay   ")
								.withStyle(ChatFormatting.GRAY))
						.append(Component.literal("F8").withStyle(ChatFormatting.AQUA))
						.append(Component.literal(" = ULTRA_LOW   ")
								.withStyle(ChatFormatting.GRAY))
						.append(Component.literal("F9").withStyle(ChatFormatting.AQUA))
						.append(Component.literal(" = SAFE")
								.withStyle(ChatFormatting.GRAY))).build());

		gen.addEntry(e.startEnumSelector(
						label("Performance preset"),
						Preset.class,
						cfg.lastAppliedPreset)
				.setDefaultValue(Preset.BALANCED)
				.setTooltip(hint("Instantly applies a balanced set of distances and keep rates."))
				.setSaveConsumer(v -> {
					cfg.lastAppliedPreset = v;
					HSNPresets.apply(cfg, v);
				}).build());

		gen.addEntry(e.startBooleanToggle(
						label("Show HSN status on F3 screen"),
						cfg.f3ShowStatus)
				.setDefaultValue(true)
				.setTooltip(hint("Adds a small HSN line to the debug (F3) overlay."))
				.setSaveConsumer(v -> cfg.f3ShowStatus = v).build());

		gen.addEntry(e.startBooleanToggle(
						label("FPS overlay (on-screen counter)"),
						cfg.fpsOverlayEnabled)
				.setDefaultValue(false)
				.setTooltip(hint("Shows a floating FPS counter. Toggle with F7."))
				.setSaveConsumer(v -> cfg.fpsOverlayEnabled = v).build());

		// ══════════════════════════════════════════════════════════
		//  PARTICLES
		// ══════════════════════════════════════════════════════════
		ConfigCategory particles = builder.getOrCreateCategory(
				title(" Particles", ChatFormatting.AQUA));

		particles.addEntry(e.startTextDescription(
				hint("Control how many particles are kept and how far they render.")).build());

		particles.addEntry(e.startBooleanToggle(
						label("Enable particle culling"),
						cfg.particleCullingEnabled)
				.setDefaultValue(true)
				.setTooltip(hint("Master switch for all particle limits below."))
				.setSaveConsumer(v -> cfg.particleCullingEnabled = v).build());

		particles.addEntry(e.startIntSlider(
						label("Max particles (soft limit)"),
						cfg.maxParticles, 50, 2000)
				.setDefaultValue(400)
				.setTextGetter(v -> unit(v, "particles"))
				.setTooltip(hint("Soft cap on total particles. Lower = better FPS, higher = prettier effects."))
				.setSaveConsumer(v -> cfg.maxParticles = v).build());

		particles.addEntry(e.startIntSlider(
						label("Max particle render distance"),
						(int) cfg.maxParticleDistance, 4, 64)
				.setDefaultValue(16)
				.setTextGetter(v -> unit(v, "blocks"))
				.setTooltip(hint("Particles farther than this are not drawn at all."))
				.setSaveConsumer(v -> cfg.maxParticleDistance = v).build());

		particles.addEntry(e.startIntSlider(
						label("Rain keep chance"),
						(int) Math.round(cfg.rainKeepChance * 100), 0, 100)
				.setDefaultValue(15)
				.setTextGetter(HSNConfigScreen::percent)
				.setTooltip(hint("How many rain particles are kept. 15% looks decent and saves a lot of FPS."))
				.setSaveConsumer(v -> cfg.rainKeepChance = v / 100.0).build());

		particles.addEntry(e.startIntSlider(
						label("Smoke / fire keep chance"),
						(int) Math.round(cfg.smokeKeepChance * 100), 0, 100)
				.setDefaultValue(25)
				.setTextGetter(HSNConfigScreen::percent)
				.setTooltip(hint("How many smoke, fire and similar particles are kept."))
				.setSaveConsumer(v -> cfg.smokeKeepChance = v / 100.0).build());

		// ══════════════════════════════════════════════════════════
		//  ENTITIES
		// ══════════════════════════════════════════════════════════
		ConfigCategory entities = builder.getOrCreateCategory(
				title(" Entities", ChatFormatting.GREEN));

		entities.addEntry(e.startTextDescription(
				hint("Limit how far entities, items, XP orbs, shadows and name tags are rendered.")).build());

		entities.addEntry(e.startBooleanToggle(
						label("Enable entity culling"),
						cfg.entityCullingEnabled)
				.setDefaultValue(true)
				.setTooltip(hint("Master switch for entity distance limits."))
				.setSaveConsumer(v -> cfg.entityCullingEnabled = v).build());

		entities.addEntry(e.startIntSlider(
						label("Max entity render distance"),
						(int) cfg.maxEntityRenderDistance, 8, 128)
				.setDefaultValue(32)
				.setTextGetter(v -> unit(v, "blocks"))
				.setTooltip(hint("Mobs and most entities beyond this distance are hidden."))
				.setSaveConsumer(v -> cfg.maxEntityRenderDistance = v).build());

		entities.addEntry(e.startIntSlider(
						label("Max dropped-item distance"),
						(int) cfg.maxItemEntityRenderDistance, 4, 64)
				.setDefaultValue(20)
				.setTextGetter(v -> unit(v, "blocks"))
				.setTooltip(hint("How far dropped items (item entities) are still visible."))
				.setSaveConsumer(v -> cfg.maxItemEntityRenderDistance = v).build());

		entities.addEntry(e.startIntSlider(
						label("Max XP-orb distance"),
						(int) cfg.maxXpOrbRenderDistance, 4, 48)
				.setDefaultValue(16)
				.setTextGetter(v -> unit(v, "blocks"))
				.setTooltip(hint("Experience orbs farther than this are not rendered."))
				.setSaveConsumer(v -> cfg.maxXpOrbRenderDistance = v).build());

		entities.addEntry(e.startBooleanToggle(
						label("Enable shadow culling"),
						cfg.shadowCullingEnabled)
				.setDefaultValue(true)
				.setTooltip(hint("Hides entity shadows that are far away."))
				.setSaveConsumer(v -> cfg.shadowCullingEnabled = v).build());

		entities.addEntry(e.startIntSlider(
						label("Max shadow distance"),
						(int) cfg.maxShadowDistance, 2, 32)
				.setDefaultValue(12)
				.setTextGetter(v -> unit(v, "blocks"))
				.setTooltip(hint("Entity shadows beyond this distance are skipped."))
				.setSaveConsumer(v -> cfg.maxShadowDistance = v).build());

		entities.addEntry(e.startBooleanToggle(
						label("Enable name-tag culling"),
						cfg.nameTagCullEnabled)
				.setDefaultValue(true)
				.setTooltip(hint("Hides player / mob name tags that are too far."))
				.setSaveConsumer(v -> cfg.nameTagCullEnabled = v).build());

		entities.addEntry(e.startIntSlider(
						label("Max name-tag distance"),
						(int) cfg.maxNameTagDistance, 4, 64)
				.setDefaultValue(24)
				.setTextGetter(v -> unit(v, "blocks"))
				.setTooltip(hint("Name tags beyond this distance are not drawn."))
				.setSaveConsumer(v -> cfg.maxNameTagDistance = v).build());

		// ══════════════════════════════════════════════════════════
		//  OTHER (block entities + sound)
		// ══════════════════════════════════════════════════════════
		ConfigCategory other = builder.getOrCreateCategory(
				title(" Other", ChatFormatting.LIGHT_PURPLE));

		other.addEntry(e.startTextDescription(
				hint("Block-entity rendering, sound distance and a couple of extras.")).build());

		other.addEntry(e.startBooleanToggle(
						label("Enable block-entity culling"),
						cfg.blockEntityCullingEnabled)
				.setDefaultValue(true)
				.setTooltip(hint("Hides chests, furnaces, signs, etc. that are far away."))
				.setSaveConsumer(v -> cfg.blockEntityCullingEnabled = v).build());

		other.addEntry(e.startIntSlider(
						label("Max block-entity distance"),
						(int) cfg.maxBlockEntityRenderDistance, 4, 64)
				.setDefaultValue(24)
				.setTextGetter(v -> unit(v, "blocks"))
				.setTooltip(hint("Chests, hoppers, signs and similar beyond this are not rendered."))
				.setSaveConsumer(v -> cfg.maxBlockEntityRenderDistance = v).build());

		other.addEntry(e.startBooleanToggle(
						label("Enable sound distance culling"),
						cfg.soundDistanceCullingEnabled)
				.setDefaultValue(true)
				.setTooltip(hint("Cuts off sounds that are too far from the player."))
				.setSaveConsumer(v -> cfg.soundDistanceCullingEnabled = v).build());

		other.addEntry(e.startIntSlider(
						label("Max sound distance"),
						(int) cfg.maxSoundDistance, 4, 64)
				.setDefaultValue(24)
				.setTextGetter(v -> unit(v, "blocks"))
				.setTooltip(hint("Sounds farther than this are not played."))
				.setSaveConsumer(v -> cfg.maxSoundDistance = v).build());

		other.addEntry(e.startBooleanToggle(
						label("Reduce weather sounds"),
						cfg.weatherSoundReductionEnabled)
				.setDefaultValue(true)
				.setTooltip(hint("Thins out rain / thunder ambient sounds for less noise and a tiny FPS boost."))
				.setSaveConsumer(v -> cfg.weatherSoundReductionEnabled = v).build());

		other.addEntry(e.startBooleanToggle(
						label("Merge nearby item entities (server)"),
						cfg.itemMergeEnabled)
				.setDefaultValue(true)
				.setTooltip(hint("Server-side: combines dropped items that are close together to reduce entity count."))
				.setSaveConsumer(v -> cfg.itemMergeEnabled = v).build());

		return builder.build();
	}
}