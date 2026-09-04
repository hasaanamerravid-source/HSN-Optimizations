package hsn.modod.client.compat;

import hsn.modod.HSNOptimizations;
import hsn.modod.config.HSNConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.util.ArrayList;
import java.util.List;

public final class HSNModCompat {

	private static boolean entityCullingModPresent;
	private static boolean moreCullingPresent;
	private static boolean nvidiumPresent;
	private static boolean shapeModPresent;
	private static boolean sodiumPresent;
	private static boolean sodiumExtraPresent;
	private static boolean lithiumPresent;
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
		shapeModPresent = loader.isModLoaded("circular-rendering");
		sodiumPresent = loader.isModLoaded("sodium");
		sodiumExtraPresent = loader.isModLoaded("sodium-extra");
		lithiumPresent = loader.isModLoaded("lithium");

		List<String> found = new ArrayList<>();
		checkMod(loader, "sodium", found);
		checkMod(loader, "sodium-extra", found);
		checkMod(loader, "iris", found);
		checkMod(loader, "nvidium", found);
		checkMod(loader, "circular-rendering", found);
		checkMod(loader, "entityculling", found);
		checkMod(loader, "moreculling", found);
		checkMod(loader, "immediatelyfast", found);
		checkMod(loader, "lithium", found);
		checkMod(loader, "ferritecore", found);
		checkMod(loader, "boosters", found);
		checkMod(loader, "lon", found);
		checkMod(loader, "laminar", found);
		checkMod(loader, "continuity", found);
		checkMod(loader, "indium", found);

		if (!found.isEmpty()) {
			summary = String.join(", ", found);
		}

		if (entityCullingModPresent || moreCullingPresent) {
			HSNOptimizations.LOGGER.info(
					"Dedicated entity-culling mod detected; HSN defers when that option is enabled.");
		}
		if (sodiumExtraPresent) {
			HSNOptimizations.LOGGER.info(
					"Sodium Extra detected; overlapping fog/toast/beacon/animation hooks can be deferred.");
		}
		if (nvidiumPresent) {
			HSNOptimizations.LOGGER.info("Nvidium detected; HSN does not touch terrain meshes.");
		}
		if (shapeModPresent) {
			HSNOptimizations.LOGGER.info("Circular Rendering detected; HSN shaped draw stays off.");
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

	public static boolean sodiumPresent() {
		if (!initialized) {
			init();
		}
		return sodiumPresent;
	}

	public static boolean sodiumExtraPresent() {
		if (!initialized) {
			init();
		}
		return sodiumExtraPresent;
	}

	public static boolean lithiumPresent() {
		return lithiumPresent;
	}

	public static boolean shapeModPresent() {
		if (!initialized) {
			init();
		}
		return shapeModPresent;
	}

	public static boolean deferFog() {
		return sodiumExtraPresent() && HSNConfig.get().deferFogToSodiumExtra;
	}

	public static boolean deferToasts() {
		return sodiumExtraPresent() && HSNConfig.get().deferToastsToSodiumExtra;
	}

	public static boolean deferBeacon() {
		return sodiumExtraPresent() && HSNConfig.get().deferBeaconToSodiumExtra;
	}

	public static boolean deferTextureAnim() {
		return sodiumExtraPresent() && HSNConfig.get().deferTextureAnimToSodiumExtra;
	}

	public static boolean deferParticles() {
		return sodiumExtraPresent() && HSNConfig.get().deferParticlesToSodiumExtra;
	}

	public static String detectedModsSummary() {
		return summary;
	}
}
