package hsn.optimizations.client;

import hsn.optimizations.HSNOptimizations;
import hsn.optimizations.client.compat.HSNModCompat;
import hsn.optimizations.client.compat.ResolutionControlCompat;
import hsn.optimizations.client.compat.SodiumCompat;
import hsn.optimizations.client.compat.SodiumOptionsIntegration;
import hsn.optimizations.client.compat.VoxelSniperCompat;
import hsn.optimizations.client.optimize.AdaptiveCuller;
import hsn.optimizations.client.optimize.CullStats;
import hsn.optimizations.client.optimize.FpsOverlay;
import hsn.optimizations.client.optimize.HighEndCounters;
import hsn.optimizations.client.optimize.HSNWorldOpenClient;
import hsn.optimizations.client.optimize.WindowGate;
import hsn.optimizations.config.HSNConfig;
import hsn.optimizations.optimize.CpuCache;
import hsn.optimizations.optimize.CpuTopology;
import hsn.optimizations.optimize.HSNGc;
import hsn.optimizations.optimize.HSNScheduler;
import hsn.optimizations.optimize.HSNTickState;
import hsn.optimizations.optimize.NativeBridge;
import hsn.optimizations.optimize.VectorCull;
import hsn.optimizations.optimize.PathfindingStats;
import hsn.optimizations.optimize.ThrottleStats;
import hsn.optimizations.platform.HSNPlatform;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class HSNOptimizationsClient {

	private static volatile boolean initialized;
	private static boolean schedulerArmed;

	private HSNOptimizationsClient() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;

		HSNConfig cfg = HSNConfig.get();
		SodiumCompat.init();
		HSNModCompat.init();
		VoxelSniperCompat.init();
		SodiumOptionsIntegration.tryRegister();
		HSNKeybinds.register();
		FpsOverlay.register();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.getWindow() != null) {
				WindowGate.markReady();
			}
			if (!schedulerArmed && client.level != null) {
				schedulerArmed = true;
				HSNScheduler.applyOnce();
			}
			if (client.level != null) {
				VoxelSniperCompat.tick();
				CullStats.tick();
				HighEndCounters.tick();
				AdaptiveCuller.tick();
				HSNTickState.refreshClient(AdaptiveCuller.getScale(), AdaptiveCuller.isWeakGpuActive());
				ThrottleStats.tick();
				PathfindingStats.tick();
			}
			HSNWorldOpenClient.tick(client);
			HSNKeybinds.tick(client);
		});

		NativeBridge.applyConfig(cfg);
		ResolutionControlCompat.present();
		HSNOptimizations.LOGGER.info(
				"HSN {} ready on {} — preset {} — native={} cpu(avx512={},avx2={}) vector={} lanes={} l3={}KiB batchFloor={} scaleBlocked={} mode={} active={} gc={}",
				cfg.modVersionLabel,
				HSNPlatform.loaderName(),
				cfg.lastAppliedPreset,
				NativeBridge.available(),
				NativeBridge.avx512(),
				NativeBridge.avx2(),
				VectorCull.available(),
				VectorCull.laneCount(),
				CpuTopology.l3Kib(),
				CpuCache.nativeBatchFloor(),
				ResolutionControlCompat.present(),
				cfg.simdMode,
				NativeBridge.activeLabel(),
				HSNGc.collectorName());
	}
}
