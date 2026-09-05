package hsn.modod.client;

import hsn.modod.HSNOptimizations;
import hsn.modod.client.compat.HSNModCompat;
import hsn.modod.client.compat.SodiumCompat;
import hsn.modod.client.compat.SodiumOptionsIntegration;
import hsn.modod.client.optimize.AdaptiveCuller;
import hsn.modod.client.optimize.CullStats;
import hsn.modod.client.optimize.FpsOverlay;
import hsn.modod.client.optimize.HighEndCounters;
import hsn.modod.config.HSNConfig;
import hsn.modod.optimize.HSNTickState;
import hsn.modod.optimize.NativeBridge;
import hsn.modod.optimize.PathfindingStats;
import hsn.modod.optimize.ThrottleStats;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class HSNOptimizationsClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		HSNConfig cfg = HSNConfig.get();
		SodiumCompat.init();
		HSNModCompat.init();
		SodiumOptionsIntegration.tryRegister();
		HSNKeybinds.register();
		FpsOverlay.register();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.level != null) {
				CullStats.tick();
				HighEndCounters.tick();
				AdaptiveCuller.tick();
				HSNTickState.refreshClient(AdaptiveCuller.getScale(), AdaptiveCuller.isWeakGpuActive());
				ThrottleStats.tick();
				PathfindingStats.tick();
			}
		});

		NativeBridge.applyConfig(cfg);
		HSNOptimizations.LOGGER.info(
				"HSN {} ready — preset {} — native={} cpu(avx512={},avx2={}) mode={} active={}",
				cfg.modVersionLabel,
				cfg.lastAppliedPreset,
				NativeBridge.available(),
				NativeBridge.avx512(),
				NativeBridge.avx2(),
				cfg.simdMode,
				NativeBridge.activeLabel());
	}
}
