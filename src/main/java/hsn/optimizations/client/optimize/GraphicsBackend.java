package hsn.optimizations.client.optimize;

import hsn.optimizations.HSNOptimizations;

/**
 * One place that knows whether Minecraft 26.2 is on OpenGL or Vulkan.
 * GL-only features (texture LOD bias, {@code glGetString}) run only on
 * OpenGL. Vulkan keeps CPU culling and skips those hooks. Missing APIs
 * fail open — the game still runs.
 */
public final class GraphicsBackend {

	public enum Kind {
		DIRECTX12,
		VULKAN,
		OPENGL,
		UNKNOWN
	}

	private static volatile Kind kind = Kind.UNKNOWN;
	private static volatile boolean locked;

	private GraphicsBackend() {
	}

	/** Cached kind only — does not probe. Safe from {@link GlGuard}. */
	public static Kind peek() {
		return kind;
	}

	public static Kind kind() {
		if (!locked) {
			probe();
		}
		return kind;
	}

	public static boolean vulkan() {
		return kind() == Kind.VULKAN;
	}

	public static boolean directx12() {
		return kind() == Kind.DIRECTX12;
	}

	public static boolean opengl() {
		return kind() == Kind.OPENGL;
	}

	/** Backends that have no GL context. GL hooks would abort the JVM. */
	public static boolean modernNonGl() {
		Kind k = kind();
		return k == Kind.VULKAN || k == Kind.DIRECTX12;
	}

	public static boolean glHooksSafe() {
		return opengl() && GlGuard.glUsable();
	}

	/** Texture LOD bias is an OpenGL sampler knob. No-op on Vulkan. */
	public static boolean textureLodSupported() {
		return glHooksSafe();
	}

	public static String label() {
		return switch (kind()) {
			case DIRECTX12 -> "dx12";
			case VULKAN -> "vulkan";
			case OPENGL -> "opengl";
			case UNKNOWN -> "unknown";
		};
	}

	public static void probe() {
		if (locked) {
			return;
		}
		Kind found = detect();
		if (found == Kind.UNKNOWN) {
			return;
		}
		kind = found;
		locked = true;
		HSNOptimizations.LOGGER.info("HSN graphics backend: {} (fallback chain dx12 -> vulkan -> opengl -> cpu)",
				found.name().toLowerCase());
		VulkanPipelineCache.warm(found);
	}

	private static Kind detect() {
		Kind live = fromRenderSystem();
		if (live != Kind.UNKNOWN) {
			BackendCache.write(live);
			return live;
		}
		if (!WindowGate.ready()) {
			return Kind.UNKNOWN;
		}
		if (GlGuard.glUsable()) {
			BackendCache.write(Kind.OPENGL);
			return Kind.OPENGL;
		}
		return Kind.UNKNOWN;
	}

	private static Kind fromRenderSystem() {
		try {
			Class<?> rs = Class.forName("com.mojang.blaze3d.systems.RenderSystem");
			for (String name : new String[]{"getDevice", "getGpuDevice", "getGraphicsDevice", "getBackend"}) {
				try {
					Object dev = rs.getMethod(name).invoke(null);
					if (dev == null) {
						continue;
					}
					String n = typeChain(dev.getClass());
					if (n.contains("directx") || n.contains("d3d12") || n.contains("dx12") || n.contains(".dx.")) {
						return Kind.DIRECTX12;
					}
					if (n.contains("vulkan") || n.contains(".vk")) {
						return Kind.VULKAN;
					}
					if (n.contains("opengl") || n.contains("gldevice") || n.contains(".gl.")) {
						return Kind.OPENGL;
					}
				} catch (NoSuchMethodException ignored) {
				}
			}
		} catch (Throwable ignored) {
		}
		return Kind.UNKNOWN;
	}

	private static boolean lwjglDx12Live() {
		try {
			for (String name : new String[]{
					"org.lwjgl.directx.DX12",
					"org.lwjgl.d3d12.D3D12",
					"com.mojang.blaze3d.systems.Dx12Device",
					"com.mojang.blaze3d.dx12.Dx12Device"
			}) {
				try {
					Class.forName(name);
					return true;
				} catch (ClassNotFoundException ignored) {
				}
			}
		} catch (Throwable ignored) {
		}
		return false;
	}

	private static boolean lwjglVulkanLive() {
		try {
			Class<?> vk = Class.forName("org.lwjgl.vulkan.VK");
			try {
				Object inst = vk.getMethod("getInstance").invoke(null);
				if (inst != null) {
					return true;
				}
			} catch (NoSuchMethodException ignored) {
			}
			try {
				Object cap = vk.getMethod("getCapabilities").invoke(null);
				return cap != null;
			} catch (NoSuchMethodException ignored) {
			}
		} catch (Throwable ignored) {
		}
		return false;
	}

	private static String typeChain(Class<?> type) {
		StringBuilder sb = new StringBuilder();
		Class<?> c = type;
		while (c != null && c != Object.class) {
			sb.append(c.getName()).append(' ');
			c = c.getSuperclass();
		}
		return sb.toString().toLowerCase();
	}
}
