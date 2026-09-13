package hsn.optimizations;

import hsn.optimizations.config.HSNConfig;
import hsn.optimizations.optimize.HSNGc;
import hsn.optimizations.optimize.HSNScheduler;
import hsn.optimizations.optimize.HSNTickState;
import hsn.optimizations.optimize.HSNWorldOpen;
import hsn.optimizations.optimize.LocateCache;
import hsn.optimizations.optimize.PathfindingStats;
import hsn.optimizations.optimize.ServerFeatures;
import hsn.optimizations.optimize.ThrottleStats;
import hsn.optimizations.platform.HSNPlatform;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HSNOptimizations {

	public static final String MOD_ID = "hsn-optimizations";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** Wall-clock time of the last server start (integrated or dedicated). */
	public static volatile long serverStartedAtMs;

	private static volatile boolean initialized;

	private HSNOptimizations() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;

		try {
			HSNConfig.load();
			HSNTickState.refresh();
		} catch (Throwable t) {
			LOGGER.error("Failed to load HSNConfig during initialization, using defaults", t);
		}

		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			try {
				HSNWorldOpen.onServerStarting(server);
			} catch (Throwable t) {
				LOGGER.debug("HSN world-open: {}", t.toString());
			}
		});

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			serverStartedAtMs = System.currentTimeMillis();
			try {
				ServerFeatures.onServerStarted(server);
				HSNTickState.refresh();
				HSNScheduler.applyOnce();
				LOGGER.info("HSN server window opened ({}s drain, worldOpen={}, gc={}, loader={})",
						HSNConfig.get().fastWorldLoadWindowSeconds,
						HSNConfig.get().worldOpenEnabled,
						HSNGc.collectorName(),
						HSNPlatform.loaderName());
			} catch (Throwable t) {
				LOGGER.error("Error executing HSN SERVER_STARTED hook", t);
			}
		});

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			serverStartedAtMs = 0L;
			try {
				ServerFeatures.onServerStopped();
				LocateCache.clear();
			} catch (Throwable t) {
				LOGGER.error("Error executing HSN SERVER_STOPPED hook", t);
			}
		});

		ServerTickEvents.START_SERVER_TICK.register(server -> {
			try {
				HSNTickState.refresh();
			} catch (Throwable t) {
				LOGGER.error("Error during HSN SERVER_PRE tick", t);
			}
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			try {
				PathfindingStats.tick();
				ThrottleStats.tick();
			} catch (Throwable t) {
				LOGGER.error("Error during HSN SERVER_POST tick", t);
			}
		});

		LOGGER.info("HSN {} initialized on {}", HSNConfig.modVersionLabel, HSNPlatform.loaderName());
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
