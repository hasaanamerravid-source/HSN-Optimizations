package hsn.modod.client.compat;

import hsn.modod.HSNOptimizations;
import net.fabricmc.loader.api.FabricLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects other optimization / rendering mods at startup.
 * Used by F3 status and to avoid stacking work that dedicated mods already do better.
 * Detection only — no invasive hooks.
 */
public final class HSNModCompat {

	private static boolean entityCullingModPresent;
	private static boolean moreCullingPresent;
	private static boolean nvidiumPresent;
	private static boolean initialized;
	private static String summary = "none";

	private HSNModCompat() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;

		FabricLoader loader = FabricLoader.getInstance();
		entityCullingModPresent = loader.isModLoaded("entityculling");
		moreCullingPresent = loader.isModLoaded("moreculling");
		nvidiumPresent = loader.isModLoaded("nvidium");

		List<String> found = new ArrayList<>();
		checkMod(loader, "sodium", found);
		checkMod(loader, "iris", found);
		checkMod(loader, "nvidium", found);
		checkMod(loader, "entityculling", found);
		checkMod(loader, "moreculling", found);
		checkMod(loader, "immediatelyfast", found);
		checkMod(loader, "lithium", found);
		checkMod(loader, "ferritecore", found);
		checkMod(loader, "boosters", found);
		checkMod(loader, "continuity", found);
		checkMod(loader, "indium", found);

		if (!found.isEmpty()) {
			summary = String.join(", ", found);
		}

		if (entityCullingModPresent || moreCullingPresent) {
			HSNOptimizations.LOGGER.info(
					"Detected dedicated culling mod(s) — HSN will defer its own entity distance culling "
							+ "when 'Defer to entity-culling mods' is enabled (default: on).");
		}
		if (nvidiumPresent) {
			HSNOptimizations.LOGGER.info("Nvidium detected — HSN leaves terrain meshing alone.");
		}
	}

	private static void checkMod(FabricLoader loader, String modId, List<String> found) {
		if (loader.isModLoaded(modId)) {
			found.add(modId);
		}
	}

	public static boolean entityCullingModPresent() {
		return entityCullingModPresent;
	}

	public static boolean moreCullingPresent() {
		return moreCullingPresent;
	}

	public static boolean nvidiumPresent() {
		return nvidiumPresent;
	}

	public static String detectedModsSummary() {
		return summary;
	}
}
