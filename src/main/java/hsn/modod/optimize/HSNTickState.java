package hsn.modod.optimize;

import hsn.modod.config.HSNConfig;

/**
 * Hot flags copied once per tick so mixins do not reread the full config object
 * on every particle / entity / sound call.
 */
public final class HSNTickState {

	public static volatile HSNConfig cfg = new HSNConfig();
	public static volatile double scale = 1.0;
	public static volatile boolean performanceMode;
	public static volatile boolean weakGpu;
	public static volatile boolean particleCulling;
	public static volatile boolean entityCulling;
	public static volatile boolean pathfindingThrottle;
	public static volatile boolean itemThrottle;
	public static volatile boolean entityLodStages;

	private HSNTickState() {
	}

	public static void refresh() {
		HSNConfig current = HSNConfig.get();
		cfg = current;
		particleCulling = current.particleCullingEnabled;
		entityCulling = current.entityCullingEnabled;
		pathfindingThrottle = current.pathfindingThrottleEnabled;
		itemThrottle = current.itemThrottleEnabled;
		entityLodStages = current.entityLodStagesEnabled;
		performanceMode = current.performanceModeEnabled;
		HotPath.rebuild(current);
		HotPath.publishScale(scale);
	}

	public static void refreshClient(double adaptiveScale, boolean weakGpuActive) {
		if (adaptiveScale < 0.05) {
			adaptiveScale = 0.05;
		} else if (adaptiveScale > 1.0) {
			adaptiveScale = 1.0;
		}
		scale = adaptiveScale;
		weakGpu = weakGpuActive;
		refresh();
	}
}
