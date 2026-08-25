package hsn.modod.client;

import hsn.modod.HSNOptimizations;
import hsn.modod.client.compat.HSNModCompat;
import hsn.modod.client.compat.SodiumCompat;
import hsn.modod.client.optimize.AdaptiveCuller;
import hsn.modod.client.optimize.CullStats;
import hsn.modod.client.optimize.FpsOverlay;
import hsn.modod.config.HSNConfig;
import hsn.modod.optimize.ThrottleStats;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class HSNOptimizationsClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		HSNConfig cfg = HSNConfig.get();
		SodiumCompat.init();
		HSNModCompat.init();
		HSNKeybinds.register();
		FpsOverlay.register();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.level != null) {
				CullStats.tick();
				AdaptiveCuller.tick();
				ThrottleStats.tick();
			}
		});

		HSNOptimizations.LOGGER.info("HSN-Optimizations {} ready — preset {}",
				cfg.modVersionLabel, cfg.lastAppliedPreset);
		HSNOptimizations.LOGGER.info("Hotkeys: F6=Perf Mode · F7=FPS overlay · F8=ULTRA_LOW · F9=SAFE · Unique: texture-anim, priority-particles, weak-GPU auto");
	}
}
