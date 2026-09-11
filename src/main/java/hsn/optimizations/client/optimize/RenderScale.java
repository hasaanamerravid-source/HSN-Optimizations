package hsn.optimizations.client.optimize;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import hsn.optimizations.client.compat.IrisCompat;
import hsn.optimizations.client.compat.ResolutionControlCompat;
import hsn.optimizations.client.access.HSNMainTarget;
import hsn.optimizations.config.HSNConfig;
import hsn.optimizations.config.ScaleFilter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * RenderScale (Zolo101) 26.2 path.
 * Swap GameRenderer.mainRenderTarget to a scaled MainTarget for renderLevel,
 * blit back with vanilla TRACY_BLIT, HUD stays native.
 * Window size is only lied about after the swap succeeds.
 */
public final class RenderScale {

	private static volatile boolean shouldScale;
	private static volatile boolean swapLive;
	private static volatile double lastFactor = 1.0;
	private static volatile long lastResizeNs;
	private static volatile double lastNotifiedFactor = 1.0;
	private static volatile boolean lastNotifiedOn;

	private static RenderTarget scaledTarget;
	private static RenderTarget nativeTarget;
	private static RenderTarget minecraftNativeTarget;
	private static int scaledW;
	private static int scaledH;
	private static int skyPauseDepth;

	private RenderScale() {
	}

	public static boolean ownedByOtherMod() {
		return ResolutionControlCompat.present();
	}

	public static boolean enabled() {
		if (ownedByOtherMod()) {
			return false;
		}
		if (IrisCompat.shadersOn()) {
			return false;
		}
		HSNConfig cfg = HSNConfig.get();
		return cfg != null && cfg.modEnabled && cfg.renderScaleEnabled
				&& Math.abs(liveFactor(cfg) - 1.0) >= 0.01;
	}

	/** Temporarily put the native target back so SkyRenderer writes the dome there. */
	public static void pauseForSky() {
		if (!swapLive || scaledTarget == null) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return;
		}
		skyPauseDepth++;
		if (skyPauseDepth != 1) {
			return;
		}
		RenderTarget dest = nativeTarget != null ? nativeTarget : minecraftNativeTarget;
		if (dest == null || dest == scaledTarget) {
			return;
		}
		pushTarget(client, dest);
	}

	public static void resumeAfterSky() {
		if (skyPauseDepth <= 0) {
			return;
		}
		skyPauseDepth--;
		if (skyPauseDepth != 0 || !swapLive || scaledTarget == null) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client != null) {
			pushTarget(client, scaledTarget);
		}
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
		boolean on = cfg != null && cfg.modEnabled && cfg.renderScaleEnabled;
		double next = on ? liveFactor(cfg) : 1.0;
		if (on == lastNotifiedOn && Math.abs(next - lastNotifiedFactor) < 0.001) {
			return;
		}
		long now = System.nanoTime();
		if (now - lastResizeNs < 250_000_000L && lastNotifiedOn == on) {
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
		if (cfg.renderScaleAdaptive && cfg.adaptiveCullingEnabled) {
			double adapt = AdaptiveCuller.getScale();
			if (adapt < 0.25) {
				adapt = 0.25;
			} else if (adapt > 1.0) {
				adapt = 1.0;
			}
			if (s <= 1.0) {
				s = Math.max(0.25, s * adapt);
			}
		}
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
			if (!(client.gameRenderer instanceof HSNMainTarget setter)) {
				return false;
			}
			RenderTarget current = client.gameRenderer.mainRenderTarget();
			if (current == null) {
				return false;
			}
			if (nativeTarget == null || nativeTarget == scaledTarget) {
				nativeTarget = current;
			}

			int nativeW = Math.max(1, client.getWindow().getWidth());
			int nativeH = Math.max(1, client.getWindow().getHeight());
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
		} catch (Throwable ignored) {
			return false;
		}
	}

	private static void restoreNative(Minecraft client) {
		try {
			if (!(client.gameRenderer instanceof HSNMainTarget setter)) {
				return;
			}
			RenderTarget dest = nativeTarget != null ? nativeTarget
					: (minecraftNativeTarget != null ? minecraftNativeTarget : client.gameRenderer.mainRenderTarget());
			if (dest == null || scaledTarget == null || dest == scaledTarget) {
				return;
			}
			pushTarget(client, dest);
			blitVanilla(scaledTarget, dest);
			skyPauseDepth = 0;
		} catch (Throwable ignored) {
		}
	}

	private static void blitVanilla(RenderTarget input, RenderTarget output) {
		if (tryInvoke(output, "blitAndBlendToTexture", input)) {
			return;
		}
		if (tryInvoke(output, "blitToScreen", input)) {
			return;
		}
		FilterMode mode = filter() == ScaleFilter.NEAREST ? FilterMode.NEAREST : FilterMode.LINEAR;
		try {
			try (var pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
					() -> "HSN blit render target",
					output.getColorTextureView(),
					Optional.empty())) {
				pass.setPipeline(RenderPipelines.TRACY_BLIT);
				RenderSystem.bindDefaultUniforms(pass);
				Object sampler = clampSampler(mode);
				if (!bindTexture(pass, input, sampler)) {
					bindTextureFallback(pass, input);
				}
				pass.draw(3, 1, 0, 0);
			}
		} catch (Throwable ignored) {
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

	private static Object clampSampler(FilterMode mode) {
		try {
			Object cache = RenderSystem.class.getMethod("getSamplerCache").invoke(null);
			Method get = cache.getClass().getMethod("getClampToEdge", FilterMode.class);
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
					Object sampler = clampSampler(filter() == ScaleFilter.NEAREST ? FilterMode.NEAREST : FilterMode.LINEAR);
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
	 * Sky / fog / celestial passes in 26.2 sample {@code Minecraft.getMainRenderTarget()},
	 * not only {@code GameRenderer.mainRenderTarget}. Leaving Minecraft on the native
	 * buffer drew the sky there, then the scaled world blit wiped it to clear-color.
	 */
	private static void retargetMinecraft(Minecraft client, RenderTarget next) {
		if (client == null || next == null) {
			return;
		}
		try {
			Object current = invokeNoArg(client, "getMainRenderTarget");
			if (current == null) {
				current = invokeNoArg(client, "mainRenderTarget");
			}
			for (var field : client.getClass().getDeclaredFields()) {
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
		FilterMode mode = filter() == ScaleFilter.NEAREST ? FilterMode.NEAREST : FilterMode.LINEAR;
		for (String name : new String[] {"setFilterMode", "setTexFilter"}) {
			try {
				target.getClass().getMethod(name, FilterMode.class).invoke(target, mode);
				return;
			} catch (Throwable ignored) {
			}
		}
	}

}
