package hsn.optimizations.platform;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fabric-only platform hooks. Architectury {@code @ExpectPlatform} and the
 * NeoForge implementation were removed.
 */
public final class HSNPlatform {

	private static final FabricLoader LOADER = FabricLoader.getInstance();
	private static final Map<String, Boolean> MOD_CACHE = new ConcurrentHashMap<>();

	private HSNPlatform() {
	}

	public static Path configDir() {
		return LOADER.getConfigDir();
	}

	public static Path gameDir() {
		return LOADER.getGameDir();
	}

	public static boolean isModLoaded(String id) {
		return modPresent(id);
	}

	public static String loaderName() {
		return "fabric";
	}

	/**
	 * Cached FabricLoader lookup. Safe from mixin plugins and early boot.
	 */
	public static boolean modPresent(String id) {
		if (id == null || id.isEmpty()) {
			return false;
		}
		Boolean cached = MOD_CACHE.get(id);
		if (cached != null) {
			return cached;
		}
		boolean present;
		try {
			present = LOADER.isModLoaded(id);
		} catch (Throwable ignored) {
			present = false;
		}
		MOD_CACHE.put(id, present);
		return present;
	}
}
