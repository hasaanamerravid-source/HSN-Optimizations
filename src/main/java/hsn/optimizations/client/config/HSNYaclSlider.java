package hsn.optimizations.client.config;

import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.StateManager;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import hsn.optimizations.config.HSNConfig;
import hsn.optimizations.optimize.HotPath;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * YACL 3.9 integer slider used by {@link HSNYaclConfigScreen}.
 *
 * <p>Pattern: {@code StateManager.createInstant} +
 * {@code IntegerSliderControllerBuilder.range/step/formatValue}.
 * Values are clamped, written immediately, and flushed with {@code saveSoon}
 * so dragging does not stall the render thread. {@link HotPath} rebuilds
 * while the thumb moves so distance culls take effect before Done.
 */
public final class HSNYaclSlider {

	private HSNYaclSlider() {
	}

	public static Option<Integer> create(String name, Supplier<Integer> get, Consumer<Integer> set,
			int def, int min, int max, String unit, String what, String advice, String fps) {
		final int lo = Math.min(min, max);
		final int hi = Math.max(min, max);
		final int safeDef = clamp(def, lo, hi);
		final String unitLabel = unit == null ? "" : unit.trim();

		Consumer<Integer> apply = raw -> {
			int clamped = clamp(raw == null ? safeDef : raw, lo, hi);
			set.accept(clamped);
			try {
				HSNConfig live = HSNConfig.get();
				live.saveSoon();
				HotPath.rebuild(live);
			} catch (Throwable ignored) {
			}
		};

		Supplier<Integer> read = () -> {
			Integer cur = get.get();
			return clamp(cur == null ? safeDef : cur, lo, hi);
		};

		return Option.<Integer>createBuilder()
				.name(Component.literal(name))
				.description(description(what, advice, fps))
				.stateManager(StateManager.createInstant(safeDef, read, apply))
				.controller(opt -> IntegerSliderControllerBuilder.create(opt)
						.range(lo, hi)
						.step(1)
						.formatValue(v -> format(v, unitLabel)))
				.build();
	}

	public static Option<Integer> percent(String name, Supplier<Double> get, Consumer<Double> set,
			int def, int min, int max, String what, String advice, String fps) {
		return create(name,
				() -> (int) Math.round(get.get() * 100.0),
				v -> set.accept(v / 100.0),
				def, min, max, "%", what, advice, fps);
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static Component format(int value, String unit) {
		if (unit == null || unit.isEmpty()) {
			return Component.literal(Integer.toString(value));
		}
		return Component.literal(value + " " + unit);
	}

	private static OptionDescription description(String what, String advice, String fps) {
		Component text = Component.empty();
		String[] lines = { what, advice, fps };
		boolean first = true;
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			if (line == null || line.isEmpty()) {
				continue;
			}
			if (!first) {
				text = text.copy().append(Component.literal("\n"));
			}
			first = false;
			text = text.copy().append(Component.literal(line)
					.withStyle(i == 0 ? ChatFormatting.GRAY : ChatFormatting.DARK_GRAY));
		}
		return OptionDescription.of(text);
	}
}
