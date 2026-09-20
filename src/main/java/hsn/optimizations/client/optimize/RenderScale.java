package hsn.optimizations.client.optimize;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import hsn.optimizations.HSNOptimizations;
import hsn.optimizations.client.compat.IrisCompat;
import hsn.optimizations.client.compat.ResolutionControlCompat;
import hsn.optimizations.client.access.HSNMainTarget;
import hsn.optimizations.config.HSNConfig;
import hsn.optimizations.config.ScaleFilter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * World-pass framebuffer scale for 26.3.
 * <p>
 * The world is drawn into a smaller (or larger) {@link MainTarget}. After
 * {@code renderLevel} the scaled color buffer is blitted back onto the native
 * target. The HUD stays at window resolution.
 * <p>
 * Variable scale: when adaptive culling is on, the configured factor is
 * multiplied by {@link AdaptiveCuller#getScale()} so resolution eases down
 * under load and recovers when FPS is healthy.
 */
public final class RenderScale {

	private static volatile boolean shouldScale;
	private static volatile boolean swapLive;
	private static volatile boolean blitBroken;
	private static volatile double lastFactor = 1.0;
	private static volatile long lastResizeNs;
	private static volatile double lastNotifiedFactor = 1.0;
	private static volatile boolean lastNotifiedOn;

	private static RenderTarget scaledTarget;
	private static RenderTarget nativeTarget;
	private static RenderTarget minecraftNativeTarget;
	private static Field minecraftTargetField;
	private static int scaledW;
	private static int scaledH;
	private static boolean blitWarned;

	private RenderScale() {
	}

	public static boolean ownedByOtherMod() {
		return ResolutionControlCompat.present();
	}

	public static boolean enabled() {
		if (blitBroken) {
			return false;
		}
		if (ownedByOtherMod()) {
			return false;
		}
		if (IrisCompat.shadersOn()) {
			return false;
		}
		HSNConfig cfg = HSNConfig.get();
		return cfg != null && cfg.modEnabled && scaleRequested(cfg)
				&& Math.abs(liveFactor(cfg) - 1.0) >= 0.01;
	}

	private static boolean scaleRequested(HSNConfig cfg) {
		return cfg.renderScaleEnabled || cfg.renderScaleAdaptive;
	}

	/**
	 * Sky is part of the world pass. Keep it on the scaled target so the
	 * later blit composites a complete frame. Pausing to native used to
	 * wipe the dome when the blit was opaque.
	 */
	public static void pauseForSky() {
		// no-op — sky draws onto the scaled world target
	}

	public static void resumeAfterSky() {
		// no-op
	}

	public static void beginWorld() {
		setShouldScale(true);
	}

	public static void endWorld() {
		setShouldScale(false);
	}

	public static boolean worldPassActive() {
		return shouldScale && swapLive;
	}

	public static double factor() {
		return worldPassActive() ? lastFactor : 1.0;
	}

	public static double currentScaleFactor() {
		return worldPassActive() ? lastFactor : 1.0;
	}

	public static int glFilter() {
		return filter() == ScaleFilter.NEAREST ? 9728 : 9729;
	}

	public static ScaleFilter filter() {
		HSNConfig cfg = HSNConfig.get();
		if (cfg == null || cfg.renderScaleFilter == null) {
			return ScaleFilter.LINEAR;
		}
		return cfg.renderScaleFilter;
	}

	public static int scaled(int nativePx) {
		if (!worldPassActive() || nativePx <= 0) {
			return nativePx;
		}
		return Math.max(1, (int) Math.round(nativePx * lastFactor));
	}

	public static void onFramebufferResized() {
		scaledTarget = null;
		scaledW = 0;
		scaledH = 0;
	}

	public static void notifyChanged() {
		if (ownedByOtherMod()) {
			return;
		}
		HSNConfig cfg = HSNConfig.get();
		boolean on = cfg != null && cfg.modEnabled && scaleRequested(cfg)
				&& Math.abs(liveFactor(cfg) - 1.0) >= 0.01;
		double next = on ? liveFactor(cfg) : 1.0;
		if (on == lastNotifiedOn && Math.abs(next - lastNotifiedFactor) < 0.001) {
			return;
		}
		long now = System.nanoTime();
		if (now - lastResizeNs < 80_000_000L && lastNotifiedOn == on
				&& Math.abs(next - lastNotifiedFactor) < 0.05) {
			return;
		}
		lastResizeNs = now;
		lastNotifiedOn = on;
		lastNotifiedFactor = next;
		onFramebufferResized();
	}

	public static String statusLine() {
		if (ownedByOtherMod()) {
			return "off (Resolution Control owns scale)";
		}
		if (IrisCompat.shadersOn()) {
			return "off (shader pack owns the framebuffer)";
		}
		HSNConfig cfg = HSNConfig.get();
		if (cfg == null || !cfg.modEnabled || !cfg.renderScaleEnabled) {
			return "1.00x native";
		}
		double live = liveFactor(cfg);
		if (Math.abs(live - 1.0) < 0.01) {
			return "1.00x native";
		}
		return String.format(java.util.Locale.ROOT, "%.2fx %s", live, filter().title());
	}

	private static double liveFactor(HSNConfig cfg) {
		if (cfg == null) {
			return 1.0;
		}
		double s = cfg.renderScale;
		if (s < 0.25) {
			s = 0.25;
		} else if (s > 2.0) {
			s = 2.0;
		}
		double always = cfg.renderScaleEnabled ? s : 1.0;
		double adaptive = 1.0;
		if (cfg.renderScaleAdaptive) {
			double fps = AdaptiveCuller.getSmoothedFps();
			int gate = Math.max(15, cfg.adaptiveRenderScaleFps);
			double dropTo = s < 0.99 ? s : 0.70;
			if (dropTo < 0.25) {
				dropTo = 0.25;
			}
			if (fps > 0.0 && fps <= gate) {
				double span = 15.0;
				double t = (gate - fps) / span;
				if (t < 0.0) {
					t = 0.0;
				} else if (t > 1.0) {
					t = 1.0;
				}
				adaptive = 1.0 + (dropTo - 1.0) * t;
			}
		}
		s = Math.min(always, adaptive);
		s = Math.round(s * 20.0) / 20.0;
		if (s < 0.25) {
			s = 0.25;
		}
		return s;
	}

	private static void setShouldScale(boolean next) {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.gameRenderer == null) {
			shouldScale = false;
			swapLive = false;
			return;
		}
		if (next) {
			if (!enabled()) {
				shouldScale = false;
				swapLive = false;
				return;
			}
			lastFactor = liveFactor(HSNConfig.get());
			if (!bindScaled(client)) {
				shouldScale = false;
				swapLive = false;
				return;
			}
			shouldScale = true;
			swapLive = true;
			return;
		}
		if (swapLive) {
			restoreNative(client);
		}
		shouldScale = false;
		swapLive = false;
	}

	private static boolean bindScaled(Minecraft client) {
		try {
			if (!(client.gameRenderer instanceof HSNMainTarget)) {
				return false;
			}
			RenderTarget current = client.gameRenderer.mainRenderTarget();
			if (current == null) {
				return false;
			}
			if (nativeTarget == null || nativeTarget == scaledTarget) {
				nativeTarget = current;
			}

			int nativeW = Math.max(1, windowPx(client, true));
			int nativeH = Math.max(1, windowPx(client, false));
			int wantW = Math.max(1, (int) Math.round(nativeW * lastFactor));
			int wantH = Math.max(1, (int) Math.round(nativeH * lastFactor));

			if (scaledTarget == null) {
				scaledTarget = new MainTarget(wantW, wantH);
				scaledW = wantW;
				scaledH = wantH;
			} else if (scaledW != wantW || scaledH != wantH) {
				scaledTarget.resize(wantW, wantH);
				scaledW = wantW;
				scaledH = wantH;
			}

			pushTarget(client, scaledTarget);
			clearTransparent(scaledTarget);
			applyFilter(scaledTarget);
			return true;
		} catch (Throwable t) {
			HSNOptimizations.LOGGER.debug("Render scale bind failed: {}", t.toString());
			return false;
		}
	}

	private static int windowPx(Minecraft client, boolean width) {
		try {
			var window = client.getWindow();
			if (window != null) {
				if (width) {
					int fb = invokeInt(window, "getFramebufferWidth", "getWidth");
					return fb > 0 ? fb : Math.max(1, window.getWidth());
				}
				int fb = invokeInt(window, "getFramebufferHeight", "getHeight");
				return fb > 0 ? fb : Math.max(1, window.getHeight());
			}
		} catch (Throwable ignored) {
		}
		return width ? 854 : 480;
	}

	private static int invokeInt(Object owner, String... names) {
		for (String name : names) {
			try {
				Object v = owner.getClass().getMethod(name).invoke(owner);
				if (v instanceof Number n) {
					return n.intValue();
				}
			} catch (Throwable ignored) {
			}
		}
		return 0;
	}

	private static void restoreNative(Minecraft client) {
		try {
			if (!(client.gameRenderer instanceof HSNMainTarget)) {
				return;
			}
			RenderTarget dest = nativeTarget != null ? nativeTarget
					: (minecraftNativeTarget != null ? minecraftNativeTarget : client.gameRenderer.mainRenderTarget());
			if (dest == null || scaledTarget == null || dest == scaledTarget) {
				return;
			}
			pushTarget(client, dest);
			if (!blitVanilla(scaledTarget, dest)) {
				blitBroken = true;
				if (!blitWarned) {
					blitWarned = true;
					HSNOptimizations.LOGGER.warn("Render scale blit failed; world will draw at native resolution.");
				}
				try {
					HSNConfig cfg = HSNConfig.get();
					if (cfg != null) {
						cfg.renderScaleEnabled = false;
					}
				} catch (Throwable ignored) {
				}
			}
		} catch (Throwable t) {
			HSNOptimizations.LOGGER.debug("Render scale restore failed: {}", t.toString());
		}
	}

	private static boolean blitVanilla(RenderTarget input, RenderTarget output) {
		if (tryInvoke(output, "blitAndBlendToTexture", input)) {
			return true;
		}
		if (tryInvoke(output, "blitToScreen", input)) {
			return true;
		}
		if (tryInvoke(output, "blitTo", input)) {
			return true;
		}
		Object mode = filter() == ScaleFilter.NEAREST ? GlCompat.filterNearest() : GlCompat.filterLinear();
		try {
			try (var pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
					() -> "HSN blit render target",
					output.getColorTextureView(),
					Optional.empty())) {
				setBlitPipeline(pass);
				RenderSystem.bindDefaultUniforms(pass);
				Object sampler = clampSampler(mode);
				if (!bindTexture(pass, input, sampler)) {
					bindTextureFallback(pass, input);
				}
				pass.draw(3, 1, 0, 0);
			}
			return true;
		} catch (Throwable t) {
			HSNOptimizations.LOGGER.debug("Render scale GPU blit failed: {}", t.toString());
			return false;
		}
	}

	private static boolean tryInvoke(RenderTarget output, String name, RenderTarget input) {
		try {
			Object view = input.getColorTextureView();
			for (Method method : output.getClass().getMethods()) {
				if (!name.equals(method.getName()) || method.getParameterCount() != 1) {
					continue;
				}
				if (method.getParameterTypes()[0].isInstance(view) || method.getParameterTypes()[0].isInstance(input)) {
					Object arg = method.getParameterTypes()[0].isInstance(view) ? view : input;
					method.invoke(output, arg);
					return true;
				}
			}
		} catch (Throwable ignored) {
		}
		return false;
	}

	private static void pushTarget(Minecraft client, RenderTarget next) {
		if (next == null) {
			return;
		}
		if (client.gameRenderer instanceof HSNMainTarget setter) {
			setter.hsn$setMainRenderTarget(next);
		}
		retargetMinecraft(client, next);
	}

	private static void clearTransparent(RenderTarget target) {
		if (target == null) {
			return;
		}
		try {
			target.getClass().getMethod("setClearColor", float.class, float.class, float.class, float.class)
					.invoke(target, 0.0f, 0.0f, 0.0f, 0.0f);
		} catch (Throwable ignored) {
		}
		for (String name : new String[] {"clear", "clearFramebuffer"}) {
			try {
				target.getClass().getMethod(name).invoke(target);
				return;
			} catch (Throwable ignored) {
			}
		}
	}

	private static void setBlitPipeline(Object pass) {
		if (pass == null) {
			return;
		}
		Object pipeline = null;
		for (String field : new String[] {"TRACY_BLIT", "GUI_TEXTURED", "GUI_OPAQUE_TEXTURED_BACKGROUND", "ENTITY_SOLID"}) {
			try {
				pipeline = RenderPipelines.class.getField(field).get(null);
				if (pipeline != null) {
					break;
				}
			} catch (Throwable ignored) {
			}
		}
		if (pipeline == null) {
			return;
		}
		for (Method method : pass.getClass().getMethods()) {
			if (!"setPipeline".equals(method.getName()) || method.getParameterCount() != 1) {
				continue;
			}
			try {
				if (method.getParameterTypes()[0].isInstance(pipeline)) {
					method.invoke(pass, pipeline);
					return;
				}
			} catch (Throwable ignored) {
			}
		}
	}

	private static Object clampSampler(Object mode) {
		if (mode == null) {
			return null;
		}
		try {
			Object cache = RenderSystem.class.getMethod("getSamplerCache").invoke(null);
			Class<?> filterType = GlCompat.filterClass() != null ? GlCompat.filterClass() : mode.getClass();
			Method get = cache.getClass().getMethod("getClampToEdge", filterType);
			return get.invoke(cache, mode);
		} catch (Throwable ignored) {
			return null;
		}
	}

	private static boolean bindTexture(Object pass, RenderTarget input, Object sampler) {
		if (sampler == null) {
			return false;
		}
		try {
			Object view = input.getColorTextureView();
			for (Method method : pass.getClass().getMethods()) {
				if (!"bindTexture".equals(method.getName()) || method.getParameterCount() != 3) {
					continue;
				}
				Class<?>[] types = method.getParameterTypes();
				if (types[0] == String.class && types[2].isInstance(sampler)) {
					method.invoke(pass, "InSampler", view, sampler);
					return true;
				}
			}
		} catch (Throwable ignored) {
		}
		return false;
	}

	private static void bindTextureFallback(Object pass, RenderTarget input) {
		try {
			Object view = input.getColorTextureView();
			for (Method method : pass.getClass().getMethods()) {
				if (!method.getName().equals("bindTexture") || method.getParameterCount() < 2) {
					continue;
				}
				Class<?>[] types = method.getParameterTypes();
				if (types.length == 3 && types[0] == String.class) {
					Object sampler = clampSampler(filter() == ScaleFilter.NEAREST ? GlCompat.filterNearest() : GlCompat.filterLinear());
					method.invoke(pass, "InSampler", view, sampler);
					return;
				}
				if (types.length == 2 && types[0] == String.class) {
					method.invoke(pass, "InSampler", view);
					return;
				}
			}
		} catch (Throwable ignored) {
		}
	}

	/**
	 * 26.2+ sky / fog / celestial passes sample {@code Minecraft.getMainRenderTarget()}
	 * as well as {@code GameRenderer.mainRenderTarget}. Both must follow the swap.
	 */
	private static void retargetMinecraft(Minecraft client, RenderTarget next) {
		if (client == null || next == null) {
			return;
		}
		try {
			if (minecraftTargetField != null) {
				Object value = minecraftTargetField.get(client);
				if (value instanceof RenderTarget rt && rt != scaledTarget) {
					minecraftNativeTarget = rt;
				}
				minecraftTargetField.set(client, next);
				return;
			}
			Object current = invokeNoArg(client, "getMainRenderTarget");
			if (current == null) {
				current = invokeNoArg(client, "mainRenderTarget");
			}
			for (Field field : client.getClass().getDeclaredFields()) {
				if (!RenderTarget.class.isAssignableFrom(field.getType())) {
					continue;
				}
				field.setAccessible(true);
				Object value = field.get(client);
				boolean mainName = field.getName().toLowerCase(java.util.Locale.ROOT).contains("main");
				if (value != current && !mainName) {
					continue;
				}
				if (value instanceof RenderTarget rt && rt != scaledTarget) {
					minecraftNativeTarget = rt;
				}
				field.set(client, next);
				minecraftTargetField = field;
				return;
			}
		} catch (Throwable ignored) {
		}
	}

	private static Object invokeNoArg(Object owner, String name) {
		try {
			return owner.getClass().getMethod(name).invoke(owner);
		} catch (Throwable ignored) {
			return null;
		}
	}

	private static void applyFilter(RenderTarget target) {
		if (target == null) {
			return;
		}
		Object mode = filter() == ScaleFilter.NEAREST ? GlCompat.filterNearest() : GlCompat.filterLinear();
		Class<?> filterType = GlCompat.filterClass();
		for (String name : new String[] {"setFilterMode", "setTexFilter"}) {
			try {
				if (filterType != null) {
					target.getClass().getMethod(name, filterType).invoke(target, mode);
					return;
				}
			} catch (Throwable ignored) {
			}
			try {
				target.getClass().getMethod(name, int.class).invoke(target, filter() == ScaleFilter.NEAREST ? 9728 : 9729);
				return;
			} catch (Throwable ignored) {
			}
		}
	}

}
