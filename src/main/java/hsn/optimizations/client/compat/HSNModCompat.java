package hsn.optimizations.client.compat;

import hsn.optimizations.HSNOptimizations;
import hsn.optimizations.platform.HSNPlatform;

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

		entityCullingModPresent = present("entityculling", "entity_culling");
		moreCullingPresent = present("moreculling", "more_culling");
		nvidiumPresent = present("nvidium");
		shapeModPresent = present("circular-rendering", "circular_rendering", "circularrendering");
		sodiumPresent = present("sodium");
		sodiumExtraPresent = present("sodium-extra", "sodium_extra", "sodiumextra");
		lithiumPresent = present("lithium");

		List<String> found = new ArrayList<>();
		for (String id : new String[]{
				"sodium", "sodium-extra", "iris", "nvidium", "circular-rendering",
				"entityculling", "moreculling", "immediatelyfast", "lithium",
				"ferritecore", "embeddium", "rubidium", "particle_core",
				"asyncparticles", "badoptimizations", "scalablelux",
				"voxelsniper", "voxelsniperfabric", "fastasyncvoxelsniper",
				"worldedit", "fastasyncworldedit"
		}) {
			if (HSNPlatform.modPresent(id)) {
				found.add(id);
			}
		}
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

	public static boolean entityCullingModPresent() {
		return entityCullingModPresent;
	}

	public static boolean moreCullingPresent() {
		return moreCullingPresent;
	}

	public static boolean nvidiumPresent() {
		return nvidiumPresent;
	}

	public static boolean shapeModPresent() {
		return shapeModPresent;
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

	public static boolean deferFog() {
		return sodiumExtraPresent() && hsn.optimizations.config.HSNConfig.get().deferFogToSodiumExtra;
	}

	public static boolean deferToasts() {
		return sodiumExtraPresent() && hsn.optimizations.config.HSNConfig.get().deferToastsToSodiumExtra;
	}

	public static boolean deferBeacon() {
		return sodiumExtraPresent() && hsn.optimizations.config.HSNConfig.get().deferBeaconToSodiumExtra;
	}

	public static boolean deferTextureAnim() {
		return sodiumExtraPresent() && hsn.optimizations.config.HSNConfig.get().deferTextureAnimToSodiumExtra;
	}

	public static boolean deferParticles() {
		return sodiumExtraPresent() && hsn.optimizations.config.HSNConfig.get().deferParticlesToSodiumExtra;
	}

	public static String detectedModsSummary() {
		return summary;
	}


	private static boolean present(String... ids) {
		for (String id : ids) {
			if (HSNPlatform.modPresent(id)) {
				return true;
			}
		}
		return false;
	}

	public static String summary() {
		return summary;
	}
}
