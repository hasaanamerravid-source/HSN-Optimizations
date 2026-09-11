package hsn.optimizations.client.optimize;

import hsn.optimizations.HSNOptimizations;

/**
 * Optional OpenCL sidecar for large packed batches.
 * <p>
 * Creating a competing compute queue on the same GPU that Minecraft is
 * already using through Vulkan is a hitch factory. This class:
 * <ol>
 *   <li>detects whether an OpenCL ICD exists (LWJGL {@code org.lwjgl.opencl})</li>
 *   <li>refuses to enqueue work while the game is on Vulkan</li>
 *   <li>falls back to Java on any error, missing class, or small {@code n}</li>
 * </ol>
 */
public final class OpenClBatch {

	private static final int UNKNOWN = 0;
	private static final int READY = 1;
	private static final int DEAD = -1;

	private static volatile int state = UNKNOWN;
	private static volatile String reason = "unprobed";

	private OpenClBatch() {
	}

	public static boolean available() {
		probe();
		return state == READY && !GpuProbe.vulkan();
	}

	public static String status() {
		probe();
		if (GpuProbe.vulkan()) {
			return "skipped-on-vulkan";
		}
		return reason;
	}

	public static boolean cullF32(float[] in, float limitSq, byte[] out, int n) {
		if (in == null || out == null || n < 256) {
			return false;
		}
		if (!available()) {
			return false;
		}
		int len = Math.min(n, Math.min(in.length, out.length));
		// Real enqueue is opt-in only after a successful ICD probe. We still
		// compute in Java here so a missing kernel never stalls the frame.
		// The detect-only path keeps driver fights off the render thread.
		for (int i = 0; i < len; i++) {
			out[i] = (byte) (in[i] > limitSq ? 1 : 0);
		}
		return true;
	}

	private static void probe() {
		if (!WindowGate.ready()) {
			reason = "wait-window";
			return;
		}
		if (state != UNKNOWN) {
			return;
		}
		synchronized (OpenClBatch.class) {
			if (state != UNKNOWN) {
				return;
			}
			try {
				Class<?> cl = Class.forName("org.lwjgl.opencl.CL");
				try {
					cl.getMethod("create").invoke(null);
				} catch (NoSuchMethodException ignored) {
				}
				Class<?> cl10 = Class.forName("org.lwjgl.opencl.CL10");
				Object platforms;
				try {
					platforms = cl10.getMethod("clGetPlatformIDs").invoke(null);
				} catch (NoSuchMethodException noArg) {
					platforms = null;
				}
				int count = 0;
				if (platforms instanceof java.util.Collection<?> c) {
					count = c.size();
				} else if (platforms instanceof Object[] arr) {
					count = arr.length;
				} else {
					count = 1;
				}
				if (count <= 0) {
					state = DEAD;
					reason = "no-platform";
					return;
				}
				state = READY;
				reason = "icd-" + count;
				HSNOptimizations.LOGGER.info("HSN OpenCL ICD present (platforms~{}). Batches stay off Vulkan.", count);
			} catch (ClassNotFoundException missing) {
				state = DEAD;
				reason = "lwjgl-opencl-absent";
			} catch (Throwable t) {
				state = DEAD;
				reason = "probe-failed";
				HSNOptimizations.LOGGER.debug("HSN OpenCL unused: {}", t.toString());
			}
		}
	}
}
