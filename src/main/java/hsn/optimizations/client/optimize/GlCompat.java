package hsn.optimizations.client.optimize;

import java.lang.reflect.Method;

/**
 * Minecraft 26.3 dropped GLFW and can run Vulkan, so OpenGL helpers are
 * optional. Resolve GlStateManager / GL11 / SDL reflectively so this module
 * compiles even when those packages are not on the compile classpath.
 */
public final class GlCompat {

	private static volatile Class<?> glState;
	private static volatile boolean glStateResolved;
	private static volatile Method getInteger;
	private static volatile Method bindTexture;
	private static volatile Method getString;
	private static volatile Method texParameterf;

	private static volatile Object filterNearest;
	private static volatile Object filterLinear;
	private static volatile Class<?> filterClass;
	private static volatile boolean filterResolved;

	private GlCompat() {
	}

	public static int getInteger(int pname) {
		Method m = integerMethod();
		if (m != null) {
			try {
				Object v = m.invoke(null, pname);
				if (v instanceof Number n) {
					return n.intValue();
				}
			} catch (Throwable ignored) {
			}
		}
		try {
			Class<?> gl = Class.forName("org.lwjgl.opengl.GL11");
			Object v = gl.getMethod("glGetInteger", int.class).invoke(null, pname);
			if (v instanceof Number n) {
				return n.intValue();
			}
		} catch (Throwable ignored) {
		}
		return 0;
	}

	public static void bindTexture(int texture) {
		Method m = bindMethod();
		if (m != null) {
			try {
				m.invoke(null, texture);
				return;
			} catch (Throwable ignored) {
			}
		}
		try {
			Class<?> gl = Class.forName("org.lwjgl.opengl.GL11");
			gl.getMethod("glBindTexture", int.class, int.class).invoke(null, 3553, texture);
		} catch (Throwable ignored) {
		}
	}

	public static String getString(int pname) {
		Method m = stringMethod();
		if (m != null) {
			try {
				Object v = m.invoke(null, pname);
				return v == null ? "" : v.toString();
			} catch (Throwable ignored) {
			}
		}
		for (String cn : new String[] {"org.lwjgl.opengl.GL11C", "org.lwjgl.opengl.GL11"}) {
			try {
				Object v = Class.forName(cn).getMethod("glGetString", int.class).invoke(null, pname);
				return v == null ? "" : v.toString();
			} catch (Throwable ignored) {
			}
		}
		return "";
	}

	public static void texParameterFloat(int target, int pname, float value) {
		Method m = texParamMethod();
		if (m != null) {
			try {
				m.invoke(null, target, pname, value);
				return;
			} catch (Throwable ignored) {
			}
		}
		for (String cn : new String[] {"org.lwjgl.opengl.GL11", "org.lwjgl.opengl.GL11C"}) {
			try {
				Class.forName(cn).getMethod("glTexParameterf", int.class, int.class, float.class)
						.invoke(null, target, pname, value);
				return;
			} catch (Throwable ignored) {
			}
		}
	}

	public static Object filterNearest() {
		resolveFilter();
		return filterNearest;
	}

	public static Object filterLinear() {
		resolveFilter();
		return filterLinear;
	}

	public static Class<?> filterClass() {
		resolveFilter();
		return filterClass;
	}

	public static long currentNativeContext() {
		// 26.3 is SDL3. Prefer SDL, then the vanilla Window handle.
		// GLFW is last and only for leftover 26.2 classpaths.
		for (String[] pair : new String[][] {
				{"org.lwjgl.sdl.SDL", "SDL_GL_GetCurrentContext"},
				{"org.lwjgl.sdl.SDL_GL", "SDL_GL_GetCurrentContext"},
				{"org.lwjgl.sdl.SDLVideo", "SDL_GL_GetCurrentContext"}
		}) {
			Long sdl = invokeStaticLong(pair[0], pair[1]);
			if (sdl != null && sdl != 0L) {
				return sdl;
			}
		}
		try {
			Class<?> mc = Class.forName("net.minecraft.client.Minecraft");
			Object client = mc.getMethod("getInstance").invoke(null);
			if (client != null) {
				Object window = null;
				try {
					window = mc.getMethod("getWindow").invoke(client);
				} catch (Throwable ignored) {
				}
				if (window != null) {
					for (String name : new String[] {"handle", "getHandle", "getWindow", "getSdlWindow"}) {
						try {
							Object h = window.getClass().getMethod(name).invoke(window);
							if (h instanceof Number n && n.longValue() != 0L) {
								return n.longValue();
							}
							if (h instanceof Long l && l != 0L) {
								return l;
							}
						} catch (Throwable ignored) {
						}
					}
				}
			}
		} catch (Throwable ignored) {
		}
		Long glfw = invokeStaticLong("org.lwjgl.glfw.GLFW", "glfwGetCurrentContext");
		if (glfw != null && glfw != 0L) {
			return glfw;
		}
		return 0L;
	}

	private static Long invokeStaticLong(String className, String method) {
		try {
			Object v = Class.forName(className).getMethod(method).invoke(null);
			if (v instanceof Number n) {
				return n.longValue();
			}
		} catch (Throwable ignored) {
		}
		return null;
	}

	private static Class<?> glState() {
		if (glStateResolved) {
			return glState;
		}
		synchronized (GlCompat.class) {
			if (!glStateResolved) {
				for (String name : new String[] {
						"com.mojang.blaze3d.opengl.GlStateManager",
						"com.mojang.blaze3d.platform.GlStateManager"
				}) {
					try {
						glState = Class.forName(name);
						break;
					} catch (Throwable ignored) {
					}
				}
				glStateResolved = true;
			}
		}
		return glState;
	}

	private static Method integerMethod() {
		if (getInteger != null) {
			return getInteger;
		}
		Class<?> type = glState();
		if (type == null) {
			return null;
		}
		for (String name : new String[] {"_getInteger", "getInteger", "_getTexLevelParameter"}) {
			try {
				getInteger = type.getMethod(name, int.class);
				return getInteger;
			} catch (Throwable ignored) {
			}
		}
		return null;
	}

	private static Method bindMethod() {
		if (bindTexture != null) {
			return bindTexture;
		}
		Class<?> type = glState();
		if (type == null) {
			return null;
		}
		for (String name : new String[] {"_bindTexture", "bindTexture"}) {
			try {
				bindTexture = type.getMethod(name, int.class);
				return bindTexture;
			} catch (Throwable ignored) {
			}
		}
		return null;
	}

	private static Method stringMethod() {
		if (getString != null) {
			return getString;
		}
		Class<?> type = glState();
		if (type == null) {
			return null;
		}
		for (String name : new String[] {"_getString", "getString"}) {
			try {
				getString = type.getMethod(name, int.class);
				return getString;
			} catch (Throwable ignored) {
			}
		}
		return null;
	}

	private static Method texParamMethod() {
		if (texParameterf != null) {
			return texParameterf;
		}
		Class<?> type = glState();
		if (type == null) {
			return null;
		}
		for (String name : new String[] {"_texParameter", "_texParameterf", "texParameter", "texParameterf"}) {
			try {
				texParameterf = type.getMethod(name, int.class, int.class, float.class);
				return texParameterf;
			} catch (Throwable ignored) {
			}
		}
		return null;
	}

	private static void resolveFilter() {
		if (filterResolved) {
			return;
		}
		synchronized (GlCompat.class) {
			if (filterResolved) {
				return;
			}
			for (String name : new String[] {
					"com.mojang.blaze3d.textures.FilterMode",
					"com.mojang.blaze3d.opengl.FilterMode"
			}) {
				try {
					Class<?> type = Class.forName(name);
					filterClass = type;
					Object[] constants = type.getEnumConstants();
					if (constants != null) {
						for (Object c : constants) {
							String n = String.valueOf(c);
							if ("NEAREST".equals(n)) {
								filterNearest = c;
							} else if ("LINEAR".equals(n)) {
								filterLinear = c;
							}
						}
					}
					if (filterNearest == null && constants != null && constants.length > 0) {
						filterNearest = constants[0];
					}
					if (filterLinear == null && constants != null && constants.length > 1) {
						filterLinear = constants[1];
					}
					break;
				} catch (Throwable ignored) {
				}
			}
			filterResolved = true;
		}
	}
}
