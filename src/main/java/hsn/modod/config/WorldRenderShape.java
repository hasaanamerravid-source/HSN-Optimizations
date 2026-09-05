package hsn.modod.config;

public enum WorldRenderShape {
	OFF("Square"),
	CIRCLE("Circle"),
	HEXAGON("Hexagon"),
	SEMICIRCLE("Front half");

	private final String displayName;

	WorldRenderShape(String displayName) {
		this.displayName = displayName;
	}

	public String displayName() {
		return displayName;
	}

	@Override
	public String toString() {
		return displayName;
	}

	public static WorldRenderShape fromOrdinal(int ordinal) {
		WorldRenderShape[] values = values();
		if (ordinal < 0) {
			return OFF;
		}
		if (ordinal >= values.length) {
			return values[values.length - 1];
		}
		return values[ordinal];
	}

	public static int maxOrdinal() {
		return values().length - 1;
	}
}
