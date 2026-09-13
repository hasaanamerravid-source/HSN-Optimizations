package hsn.optimizations.client.optimize;

/**
 * Minecraft 26.2 can run the Vulkan backend. There is no GL context then.
 * LWJGL's {@code glGetInteger} / {@code glGetString} abort the JVM with
 * "No context is current" — a Java try/catch does not stop that.
 * Call {@link #glUsable()} before any {@code GlStateManager} / {@code GL11C} use.
 */
public final class GlGuard {

	private static final int UNKNOWN = 0;
	private static final int GL_OK = 1;
	private static final int NO_GL = -1;

	private static volatile int state = UNKNOWN;

	private GlGuard() {
	}

	public static boolean glUsable() {
		if (!WindowGate.ready()) {
			return false;
		}
		int cached = state;
		if (cached == NO_GL) {
			return false;
		}
		if (cached == GL_OK) {
			return currentContext() != 0L;
		}
		if (GraphicsBackend.peek() == GraphicsBackend.Kind.VULKAN
				|| GraphicsBackend.peek() == GraphicsBackend.Kind.DIRECTX12
				|| backendLooksLikeVulkan()) {
			state = NO_GL;
			return false;
		}
		long ctx = currentContext();
		if (ctx == 0L) {
			return false;
		}
		try {
			org.lwjgl.opengl.GL.getCapabilities();
		} catch (Throwable ignored) {
			return false;
		}
		state = GL_OK;
		return true;
	}

	private static long currentContext() {
		try {
			return org.lwjgl.glfw.GLFW.glfwGetCurrentContext();
		} catch (Throwable ignored) {
			return 0L;
		}
	}

	private static boolean backendLooksLikeVulkan() {
		try {
			Class<?> rs = Class.forName("com.mojang.blaze3d.systems.RenderSystem");
			for (String name : new String[]{"getDevice", "getGpuDevice", "getBackend", "getGraphicsDevice"}) {
				try {
					Object dev = rs.getMethod(name).invoke(null);
					if (dev != null && classNameHintsVulkan(dev.getClass())) {
						return true;
					}
				} catch (NoSuchMethodException ignored) {
				}
			}
		} catch (Throwable ignored) {
		}
		try {
			for (StackTraceElement el : Thread.currentThread().getStackTrace()) {
				String n = el.getClassName();
				if (n != null && n.toLowerCase().contains("vulkan")) {
					return true;
				}
			}
		} catch (Throwable ignored) {
		}
		return false;
	}

	private static boolean classNameHintsVulkan(Class<?> type) {
		Class<?> cursor = type;
		while (cursor != null && cursor != Object.class) {
			String n = cursor.getName();
			if (n != null && n.toLowerCase().contains("vulkan")) {
				return true;
			}
			cursor = cursor.getSuperclass();
		}
		return false;
	}
}
