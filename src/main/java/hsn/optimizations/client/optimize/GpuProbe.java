package hsn.optimizations.client.optimize;

import hsn.optimizations.HSNOptimizations;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Finds the real GPU name on Minecraft 26.2 whether the backend is
 * OpenGL or Vulkan. Never calls GL when no context is current.
 */
public final class GpuProbe {

	private static volatile String renderer = "";
	private static volatile String vendor = "";
	private static volatile String backend = "unknown";
	private static volatile boolean vulkan;
	private static volatile boolean resolved;

	private GpuProbe() {
	}

	public static void tick() {
		if (!resolved) {
			tryResolve();
		}
	}

	public static boolean resolved() {
		return resolved;
	}

	public static boolean vulkan() {
		return vulkan;
	}

	public static String renderer() {
		return renderer;
	}

	public static String vendor() {
		return vendor;
	}

	public static String backend() {
		return backend;
	}

	public static String label() {
		if (renderer.isBlank()) {
			return backend;
		}
		return backend + " / " + renderer;
	}

	private static void tryResolve() {
		GraphicsBackend.probe();
		if (GraphicsBackend.vulkan()) {
			vulkan = true;
			backend = "vulkan";
		} else if (GraphicsBackend.directx12()) {
			vulkan = false;
			backend = "dx12";
		} else if (GraphicsBackend.opengl()) {
			vulkan = false;
			backend = "opengl";
		}
		if (fromRenderSystem()) {
			finish();
			return;
		}
		if (fromMinecraftWindow()) {
			finish();
			return;
		}
		if (GlGuard.glUsable() && fromLegacyGl()) {
			backend = "opengl";
			vulkan = false;
			finish();
		}
	}

	private static void finish() {
		if (renderer.isBlank() && vendor.isBlank()) {
			return;
		}
		resolved = true;
		LowEndTuner.noteGpu(renderer, vendor);
		try {
			GpuAutoTune.maybeApplyOnce(renderer, LowEndTuner.integratedHint());
		} catch (Throwable ignored) {
		}
		HSNOptimizations.LOGGER.info("HSN GPU probe: backend={} vulkan={} device='{}'",
				backend, vulkan, renderer);
	}

	private static boolean fromRenderSystem() {
		try {
			Class<?> rs = Class.forName("com.mojang.blaze3d.systems.RenderSystem");
			Object device = invokeFirst(rs, null, "getDevice", "getGpuDevice", "getGraphicsDevice", "getBackend");
			if (device == null) {
				return false;
			}
			String type = device.getClass().getName().toLowerCase(Locale.ROOT);
			if (type.contains("vulkan")) {
				vulkan = true;
				backend = "vulkan";
			} else if (type.contains("opengl") || type.contains("gldevice")) {
				backend = "opengl";
				vulkan = false;
			} else {
				backend = shortType(device.getClass());
				vulkan = type.contains("vk");
			}
			String name = stringFrom(device,
					"getRenderer", "renderer", "getDeviceName", "getName",
					"getImplementationInfo", "getVendor", "toString");
			String vend = stringFrom(device, "getVendor", "vendor", "getVendorName");
			if (looksLikeGpu(name)) {
				renderer = clean(name);
				if (looksLikeGpu(vend)) {
					vendor = clean(vend);
				}
				return true;
			}
			if (looksLikeGpu(vend)) {
				renderer = clean(vend);
				vendor = renderer;
				return true;
			}
		} catch (Throwable ignored) {
		}
		return false;
	}

	private static boolean fromMinecraftWindow() {
		try {
			Class<?> mc = Class.forName("net.minecraft.client.Minecraft");
			Object inst = mc.getMethod("getInstance").invoke(null);
			if (inst == null) {
				return false;
			}
			Object window = invokeFirst(inst.getClass(), inst, "getWindow", "window");
			if (window != null) {
				String title = stringFrom(window, "getWindowTitle", "getTitle", "getDebugTitle");
				if (looksLikeGpu(title)) {
					renderer = clean(title);
					return true;
				}
			}
		} catch (Throwable ignored) {
		}
		return false;
	}

	private static boolean fromLegacyGl() {
		try {
			String r = GlGuard.glUsable() ? LowEndTuner.glString(7937) : "";
			String v = GlGuard.glUsable() ? LowEndTuner.glString(7936) : "";
			if (r.isBlank() && v.isBlank()) {
				return false;
			}
			renderer = r;
			vendor = v;
			return true;
		} catch (Throwable ignored) {
			return false;
		}
	}

	private static Object invokeFirst(Class<?> type, Object target, String... names) {
		for (String name : names) {
			try {
				Method m = type.getMethod(name);
				Object v = m.invoke(target);
				if (v != null) {
					return v;
				}
			} catch (NoSuchMethodException ignored) {
			} catch (Throwable ignored) {
			}
		}
		return null;
	}

	private static String stringFrom(Object obj, String... names) {
		if (obj == null) {
			return "";
		}
		for (String name : names) {
			try {
				Object v = "toString".equals(name) ? obj.toString() : obj.getClass().getMethod(name).invoke(obj);
				if (v != null) {
					String s = v.toString().trim();
					if (!s.isEmpty() && !"null".equalsIgnoreCase(s)) {
						return s;
					}
				}
			} catch (NoSuchMethodException ignored) {
			} catch (Throwable ignored) {
			}
		}
		return "";
	}

	private static boolean looksLikeGpu(String raw) {
		if (raw == null || raw.isBlank()) {
			return false;
		}
		String s = raw.toLowerCase(Locale.ROOT);
		if (s.length() < 3 || s.contains("@") && s.contains("device")) {
			if (!(s.contains("nvidia") || s.contains("geforce") || s.contains("quadro")
					|| s.contains("radeon") || s.contains("intel") || s.contains("apple"))) {
				return false;
			}
		}
		return s.contains("nvidia") || s.contains("geforce") || s.contains("quadro")
				|| s.contains("rtx") || s.contains("gtx") || s.contains("radeon")
				|| s.contains("rx ") || s.contains("intel") || s.contains("iris")
				|| s.contains("uhd") || s.contains("arc") || s.contains("apple")
				|| s.contains("mali") || s.contains("adreno") || s.contains("llvmpipe")
				|| s.contains("vulkan") || s.contains("opengl");
	}

	private static String clean(String raw) {
		String s = raw.replace('\n', ' ').replace('\r', ' ').trim();
		if (s.length() > 160) {
			s = s.substring(0, 160);
		}
		return s;
	}

	private static String shortType(Class<?> type) {
		String n = type.getSimpleName();
		return n == null || n.isBlank() ? "device" : n;
	}
}
