package hsn.optimizations.client.compat;

import hsn.optimizations.config.HSNConfig;
import hsn.optimizations.platform.HSNPlatform;

/**
 * Iris / Oculus shader-pack detection. HSN does not ship a shader pipeline.
 * When a pack is bound, framebuffer swaps and lightmap freezes fight the
 * pack (BSL night looking like day, shadows sitting in the wrong place).
 */
public final class IrisCompat {

	private static final boolean IRIS = HSNPlatform.modPresent("iris")
			|| HSNPlatform.modPresent("oculus")
			|| HSNPlatform.modPresent("iris-shaders");

	private IrisCompat() {
	}

	public static boolean loaded() {
		return IRIS;
	}

	public static boolean shadersOn() {
		if (!IRIS) {
			return false;
		}
		HSNConfig cfg = HSNConfig.get();
		if (cfg != null && !cfg.shaderSafeMode) {
			return false;
		}
		Boolean api = probeApi();
		if (api != null) {
			return api;
		}
		Boolean pack = probeCurrentPack();
		if (pack != null) {
			return pack;
		}
		// Iris is installed but the pack state is hidden. Stay conservative.
		return true;
	}

	public static String statusLine() {
		if (!IRIS) {
			return "no shader mod";
		}
		if (shadersOn()) {
			return "shader pack ON — 3D scale, lightmap cache, fog scale, cloud/sky skips stay off";
		}
		return "Iris loaded, no pack bound";
	}

	private static Boolean probeApi() {
		String[] names = {
				"net.irisshaders.iris.api.v0.IrisApi",
				"net.coderbot.iris.api.v0.IrisApi"
		};
		for (String name : names) {
			try {
				Class<?> api = Class.forName(name);
				Object inst = api.getMethod("getInstance").invoke(null);
				if (inst == null) {
					continue;
				}
				Object used = inst.getClass().getMethod("isShaderPackInUse").invoke(inst);
				if (used instanceof Boolean b) {
					return b;
				}
			} catch (Throwable ignored) {
			}
		}
		return null;
	}

	private static Boolean probeCurrentPack() {
		String[] names = {
				"net.irisshaders.iris.Iris",
				"net.coderbot.iris.Iris"
		};
		for (String name : names) {
			try {
				Class<?> iris = Class.forName(name);
				try {
					Object cfg = iris.getMethod("getIrisConfig").invoke(null);
					if (cfg != null) {
						Object on = cfg.getClass().getMethod("areShadersEnabled").invoke(cfg);
						if (on instanceof Boolean b && !b) {
							return false;
						}
					}
				} catch (Throwable ignored) {
				}
				Object pack = iris.getMethod("getCurrentPack").invoke(null);
				if (pack instanceof java.util.Optional<?> opt) {
					return opt.isPresent();
				}
			} catch (Throwable ignored) {
			}
		}
		return null;
	}
}
