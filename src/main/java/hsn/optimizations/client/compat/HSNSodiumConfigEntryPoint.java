package hsn.optimizations.client.compat;

import hsn.optimizations.HSNOptimizations;
import hsn.optimizations.client.compat.HSNConfigEntry;
import hsn.optimizations.config.HSNConfig;
import hsn.optimizations.optimize.HotPath;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.structure.BooleanOptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Small Sodium Video Settings page. Full menu is the built-in HSN screen.
 */
public class HSNSodiumConfigEntryPoint implements ConfigEntryPoint {

	private static final StorageEventHandler SAVE = () -> HSNConfig.get().save();

	@Override
	public void registerConfigLate(ConfigBuilder builder) {
		try {
			HSNConfig cfg = HSNConfig.get();
			var group = builder.createOptionGroup()
					.setName(Component.literal("HSN-Optimizations 4.0"))
					.addOption(bool(builder, "mod_enabled",
							"Enable HSN",
							"Master switch.",
							() -> cfg.modEnabled,
							v -> {
								cfg.modEnabled = v;
								HotPath.rebuild(cfg);
							},
							true))
					.addOption(builder.createExternalButtonOption(id("full_settings"))
							.setName(Component.literal("Open HSN settings"))
							.setTooltip(Component.literal("Full HSN settings (YACL if installed, else built-in)."))
							.setScreenConsumer(parent -> ClientScreens.open(HSNConfigEntry.createScreen(parent))));
			var mod = builder.registerOwnModOptions()
					.setName("HSN-Optimizations 4.0")
					.setVersion(HSNConfig.modVersionLabel);
			try {
				mod.setIcon(Identifier.fromNamespaceAndPath("hsn-optimizations", "icon.png"));
			} catch (Throwable ignored) {
			}
			mod.addPage(builder.createOptionPage()
					.setName(Component.literal("HSN-Optimizations 4.0"))
					.addOptionGroup(group));
			HSNOptimizations.LOGGER.info("HSN Sodium tab registered");
		} catch (Throwable t) {
			HSNOptimizations.LOGGER.warn("Sodium config tab skipped: {}", t.toString());
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

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath("hsn-optimizations", path);
	}
}
