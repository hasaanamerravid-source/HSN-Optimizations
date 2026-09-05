package hsn.modod;

import hsn.modod.config.HSNConfig;
import hsn.modod.optimize.HSNTickState;
import hsn.modod.optimize.LocateCache;
import hsn.modod.optimize.PathfindingStats;
import hsn.modod.optimize.ServerFeatures;
import hsn.modod.optimize.ThrottleStats;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HSNOptimizations implements ModInitializer {

    public static final String MOD_ID = "hsn-optimizations";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Wall-clock time of the last server start (integrated or dedicated). */
    public static volatile long serverStartedAtMs;

    @Override
    public void onInitialize() {
        try {
            HSNConfig.load();
            HSNTickState.refresh();
        } catch (Throwable t) {
            LOGGER.error("Failed to load HSNConfig during initialization, using defaults", t);
        }

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            serverStartedAtMs = System.currentTimeMillis();
            try {
                ServerFeatures.onServerStarted(server);
                HSNTickState.refresh();
                LOGGER.info("HSN server window opened (fast-load extras last {}s)",
                        HSNConfig.get().fastWorldLoadWindowSeconds);
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

        LOGGER.info("HSN {} initialized successfully", HSNConfig.modVersionLabel);
    }

    /**
     * Creates an Identifier for this mod on Minecraft 26.2.
     */
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
