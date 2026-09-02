package hsn.modod.optimize;

import hsn.modod.config.HSNConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Gates simulation-side extras so they do not silently change dedicated-server
 * gameplay. Lithium already owns a lot of AI/tick work; we defer when it is present.
 */
public final class ServerFeatures {

	private static final boolean LITHIUM = FabricLoader.getInstance().isModLoaded("lithium");
	private static volatile boolean lastServerDedicated;

	private ServerFeatures() {
	}

	public static void onServerStarted(MinecraftServer server) {
		lastServerDedicated = server != null && server.isDedicatedServer();
	}

	public static void onServerStopped() {
		lastServerDedicated = false;
	}

	public static boolean lithiumPresent() {
		return LITHIUM;
	}

	public static boolean allowSimulationExtras() {
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.integratedServerOnly) {
			return true;
		}
		return !lastServerDedicated;
	}

	public static boolean allowSimulationExtras(Level level) {
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.integratedServerOnly) {
			return true;
		}
		if (level == null) {
			return allowSimulationExtras();
		}
		MinecraftServer server = level.getServer();
		if (server == null) {
			return false;
		}
		return !server.isDedicatedServer();
	}

	public static boolean allowSimulationExtras(Entity entity) {
		return entity != null && allowSimulationExtras(entity.level());
	}

	public static boolean allowPathfindingThrottle(Level level) {
		if (!allowSimulationExtras(level)) {
			return false;
		}
		return !HSNConfig.get().deferPathfindingToLithium || !LITHIUM;
	}

	public static boolean worldLoadWindowOpen() {
		HSNConfig cfg = HSNConfig.get();
		long started = hsn.modod.HSNOptimizations.serverStartedAtMs;
		if (started <= 0L) {
			return false;
		}
		long windowMs = Math.max(1, cfg.fastWorldLoadWindowSeconds) * 1000L;
		return System.currentTimeMillis() - started <= windowMs;
	}
}
