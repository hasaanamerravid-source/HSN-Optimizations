package hsn.optimizations.config;

/**
 * How the scaled 3D buffer is sampled back onto the native HUD framebuffer.
 * Linear is the cheap SSAA / downsample filter. Nearest keeps a pixel look
 * when rendering below native resolution.
 */
public enum ScaleFilter {
	LINEAR("Linear"),
	NEAREST("Nearest");

	private final String title;

	ScaleFilter(String title) {
		this.title = title;
	}

	public String title() {
		return title;
	}

	@Override
	public String toString() {
		return title;
	}
}
