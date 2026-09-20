package hsn.optimizations.client.config;

import hsn.optimizations.config.HSNConfig;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;
import java.util.function.IntFunction;

/**
 * Drag-to-set integer slider for the built-in settings screen.
 * Uses vanilla {@link AbstractSliderButton} so 26.3 SDL mouse events work
 * the same way as the video-settings sliders.
 */
public final class HSNIntSlider extends AbstractSliderButton {

	private final int min;
	private final int max;
	private final int step;
	private final IntConsumer apply;
	private final IntFunction<String> format;
	private int current;
	private boolean applying;

	public HSNIntSlider(int x, int y, int width, int height, String name, int value,
			int min, int max, int step, IntFunction<String> format, IntConsumer apply) {
		super(x, y, width, height, Component.literal(format.apply(clamp(value, min, max))),
				toSlider(clamp(value, min, max), min, max));
		this.min = min;
		this.max = max;
		this.step = Math.max(1, step);
		this.format = format;
		this.apply = apply;
		this.current = clamp(value, min, max);
		this.setTooltip(Tooltip.create(Component.literal(
				name + " — drag, click, or use the mouse wheel")));
	}

	@Override
	protected void updateMessage() {
		this.setMessage(Component.literal(format.apply(current)));
	}

	@Override
	protected void applyValue() {
		if (applying) {
			return;
		}
		int next = fromSlider(this.value, min, max, step);
		if (next == current) {
			return;
		}
		current = next;
		applying = true;
		try {
			apply.accept(current);
			HSNConfig cfg = HSNConfig.get();
			cfg.saveSoon();
		} catch (Throwable ignored) {
		} finally {
			applying = false;
			updateMessage();
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (!this.isMouseOver(mouseX, mouseY) || scrollY == 0.0) {
			return false;
		}
		int delta = scrollY > 0.0 ? step : -step;
		int next = clamp(current + delta, min, max);
		if (next == current) {
			return true;
		}
		current = next;
		this.value = toSlider(current, min, max);
		applyValue();
		return true;
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static double toSlider(int value, int min, int max) {
		if (max <= min) {
			return 0.0;
		}
		return (value - min) / (double) (max - min);
	}

	private static int fromSlider(double slider, int min, int max, int step) {
		if (max <= min) {
			return min;
		}
		int raw = (int) Math.round(min + slider * (max - min));
		int snapped = min + (int) Math.round((raw - min) / (double) step) * step;
		return clamp(snapped, min, max);
	}
}
