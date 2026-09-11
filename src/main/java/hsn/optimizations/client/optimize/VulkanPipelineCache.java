package hsn.optimizations.client.optimize;

import hsn.optimizations.HSNOptimizations;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Warm Minecraft / LWJGL Vulkan pipeline cache if the game exposes one.
 * We never create a second {@code VkDevice}. Missing APIs stay no-ops.
 */
public final class VulkanPipelineCache {

	private static volatile boolean warmed;
	private static volatile String status = "idle";

	private VulkanPipelineCache() {
	}

	public static String status() {
		return status;
	}

	public static void warm(GraphicsBackend.Kind kind) {
		if (warmed) {
			return;
		}
		warmed = true;
		if (kind != GraphicsBackend.Kind.VULKAN) {
			status = "n/a-" + kind.name().toLowerCase();
			return;
		}
		Path blob = cacheFile();
		if (blob != null) {
			try {
				Files.createDirectories(blob.getParent());
				if (!Files.exists(blob)) {
					Files.createFile(blob);
				}
			} catch (Throwable ignored) {
			}
		}
		if (hintDevice(blob)) {
			status = "device-hook";
			HSNOptimizations.LOGGER.info("HSN Vulkan pipeline cache hooked ({})", blob);
			return;
		}
		status = blob != null ? "file-ready" : "no-path";
	}

	private static boolean hintDevice(Path blob) {
		try {
			Class<?> rs = Class.forName("com.mojang.blaze3d.systems.RenderSystem");
			Object device = null;
			for (String name : new String[]{"getDevice", "getGpuDevice", "getGraphicsDevice"}) {
				try {
					device = rs.getMethod(name).invoke(null);
					if (device != null) {
						break;
					}
				} catch (NoSuchMethodException ignored) {
				}
			}
			if (device == null) {
				return false;
			}
			String[] hooks = {
					"setPipelineCachePath", "setPipelineCache", "enablePipelineCache",
					"loadPipelineCache", "getPipelineCache"
			};
			for (String hook : hooks) {
				if (tryHook(device, hook, blob)) {
					return true;
				}
			}
		} catch (Throwable ignored) {
		}
		return false;
	}

	private static boolean tryHook(Object device, String hook, Path blob) {
		for (Method m : device.getClass().getMethods()) {
			if (!m.getName().equals(hook)) {
				continue;
			}
			Class<?>[] p = m.getParameterTypes();
			try {
				if (p.length == 0) {
					m.invoke(device);
					return true;
				}
				if (p.length == 1 && blob != null) {
					if (p[0] == Path.class) {
						m.invoke(device, blob);
						return true;
					}
					if (p[0] == String.class) {
						m.invoke(device, blob.toString());
						return true;
					}
					if (p[0] == boolean.class || p[0] == Boolean.class) {
						m.invoke(device, Boolean.TRUE);
						return true;
					}
				}
			} catch (Throwable ignored) {
			}
		}
		return false;
	}

	private static Path cacheFile() {
		try {
			String home = System.getProperty("user.home", ".");
			return Path.of(home, ".hsn-optimizations", "vk_pipeline.bin");
		} catch (Throwable ignored) {
			return null;
		}
	}
}
