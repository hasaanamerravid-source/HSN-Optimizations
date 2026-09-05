package hsn.modod.client.optimize;

import hsn.modod.optimize.HotPath;

/**
 * LightTexture.updateLightTexture walks a 16x16 LUT and uploads it every
 * frame. On a 360–500 FPS client that is wasted bandwidth: gamma, night vision
 * and dimension rarely change. Keep the last signature and skip the rebuild
 * when nothing that affects the LUT moved.
 */
public final class LightmapGate {

	private static int lastSignature = Integer.MIN_VALUE;
	private static int freezeFrames;
	private static int skipped;
	private static int ran;

	private LightmapGate() {
	}

	public static boolean shouldSkip() {
		if (!HotPath.flag(HotPath.LIGHTMAP_CACHE)) {
			return false;
		}
		if (!CameraSnapshot.valid()) {
			return false;
		}
		int sig = CameraSnapshot.lightSignature();
		if (sig != lastSignature) {
			lastSignature = sig;
			freezeFrames = 0;
			ran++;
			return false;
		}
		// Still rebuild every 20 frames so lightning / boss-bar flashes cannot stick.
		freezeFrames++;
		if (freezeFrames >= 20) {
			freezeFrames = 0;
			ran++;
			return false;
		}
		skipped++;
		return true;
	}

	public static int skipped() {
		return skipped;
	}

	public static int ran() {
		return ran;
	}
}
