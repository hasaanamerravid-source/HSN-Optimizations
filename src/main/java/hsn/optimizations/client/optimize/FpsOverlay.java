package hsn.optimizations.client.optimize;

import hsn.optimizations.HSNOptimizations;
import hsn.optimizations.config.HSNConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Method;

/**
 * FPS line on the HUD. Fabric API 26.2 removed {@code HudRenderCallback};
 * register through {@link HudElementRegistry} instead.
 * <p>
 * Reflection for draw methods is resolved once and cached — the previous
 * implementation performed up to 8 {@code getMethod} lookups every frame.
 */
public final class FpsOverlay {

	private static final StringBuilder LINE = new StringBuilder(96);

	/** Cached draw method (graphics instance type → Method). Null until first success. */
	private static volatile Method cachedDrawMethod;
	private static volatile Class<?> cachedGraphicsClass;
	private static volatile boolean cacheFailed;

	private FpsOverlay() {
	}

	public static void register() {
		try {
			HudElementRegistry.addLast(HSNOptimizations.id("fps-overlay"), (graphics, delta) -> draw(graphics));
		} catch (Throwable t) {
			HSNOptimizations.LOGGER.warn("FPS overlay not registered: {}", t.toString());
		}
	}

	private static void draw(Object graphics) {
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.fpsOverlayEnabled) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc == null || mc.font == null) {
			return;
		}
		StringBuilder line = LINE;
		line.setLength(0);
		line.append("HSN ").append(mc.getFps()).append(" FPS | ")
				.append(cfg.lastAppliedPreset != null ? cfg.lastAppliedPreset : "CUSTOM")
				.append(" | drop ").append(CullStats.particlesPerSec()).append("p/")
				.append(CullStats.entitiesPerSec()).append("e | scale ")
				.append(Math.round(AdaptiveCuller.getScale() * 100.0)).append('%');
		if (cfg.performanceModeEnabled) {
			line.append(" | PERF");
		}
		if (AdaptiveCuller.isWeakGpuActive()) {
			line.append(" | WEAK-GPU");
		}
		String text = line.toString();
		Object font = mc.font;
		int x = cfg.fpsOverlayX;
		int y = cfg.fpsOverlayY;
		int color = 0xFFE0E0E0;

		if (tryCachedDraw(graphics, font, text, x, y, color)) {
			return;
		}
		resolveAndDraw(graphics, font, text, x, y, color);
	}

	private static boolean tryCachedDraw(Object graphics, Object font, String text, int x, int y, int color) {
		Method m = cachedDrawMethod;
		Class<?> gClass = cachedGraphicsClass;
		if (m == null || gClass == null || graphics.getClass() != gClass) {
			return false;
		}
		try {
			Class<?>[] params = m.getParameterTypes();
			if (params.length == 6) {
				m.invoke(graphics, font, text, x, y, color, true);
			} else {
				m.invoke(graphics, font, text, x, y, color);
			}
			return true;
		} catch (Throwable t) {
			// Signature or instance changed — fall through to re-resolve.
			cachedDrawMethod = null;
			cachedGraphicsClass = null;
			return false;
		}
	}

	private static void resolveAndDraw(Object graphics, Object font, String text, int x, int y, int color) {
		if (cacheFailed) {
			return;
		}
		String[] names = {"text", "drawString", "drawText", "drawShadowedString"};
		Class<?> gClass = graphics.getClass();
		Class<?> fontClass = font.getClass();
		for (String name : names) {
			try {
				Method m = gClass.getMethod(name, fontClass, String.class, int.class, int.class, int.class, boolean.class);
				m.invoke(graphics, font, text, x, y, color, true);
				cachedDrawMethod = m;
				cachedGraphicsClass = gClass;
				return;
			} catch (Throwable ignored) {
			}
			try {
				Method m = gClass.getMethod(name, fontClass, String.class, int.class, int.class, int.class);
				m.invoke(graphics, font, text, x, y, color);
				cachedDrawMethod = m;
				cachedGraphicsClass = gClass;
				return;
			} catch (Throwable ignored) {
			}
		}
		cacheFailed = true;
		HSNOptimizations.LOGGER.warn("FPS overlay: no compatible draw method found on {}", gClass.getName());
	}
}
