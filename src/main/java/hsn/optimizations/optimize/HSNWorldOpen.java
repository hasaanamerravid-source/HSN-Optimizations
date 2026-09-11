package hsn.optimizations.optimize;

import hsn.optimizations.config.HSNConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;

/**
 * World-open accelerator. Prefetches region files with the C kernel,
 * drains a time-boxed amount of chunk work after start, and admits the
 * first frame when the local player already exists. View distance is
 * never rewritten.
 *
 * <p>Client-side admission logic ({@code shouldAdmitFrame}) has been moved
 * to {@code hsn.optimizations.client.optimize.HSNWorldOpenClient} so that this class
 * remains free of client-only MC classes and can compile in the common
 * ({@code main}) source set under Fabric Loom's split-environment setup.
 */
public final class HSNWorldOpen {

	private HSNWorldOpen() {
	}

	public static boolean enabled() {
		HSNConfig cfg = HSNConfig.get();
		return cfg != null && cfg.modEnabled && cfg.worldOpenEnabled;
	}

	public static void onServerStarting(MinecraftServer server) {
		if (!enabled()) {
			return;
		}
		HSNScheduler.apply();
		if (!HSNConfig.get().prefetchRegionFiles) {
			return;
		}
		try {
			Path world = server.getWorldPath(LevelResource.ROOT);
			BootKernel.prefetchWorld(world);
		} catch (Throwable ignored) {
		}
	}

	public static long drainBudgetNanos() {
		int cores = CpuTopology.visibleCores();
		long base = 4_000_000L;
		if (cores >= 8) {
			base = 8_000_000L;
		}
		if (cores >= 16) {
			base = 10_000_000L;
		}
		return base;
	}

	public static boolean isAdmissionOverlay(String className) {
		if (className == null) {
			return false;
		}
		int dot = className.lastIndexOf('.');
		String simple = dot >= 0 ? className.substring(dot + 1) : className;
		return simple.equals("ReceivingLevelScreen")
				|| simple.equals("LevelLoadingScreen")
				|| simple.equals("ReceivingLevel")
				|| simple.equals("GenericDirtMessageScreen")
				|| simple.equals("WorldOpenProgressScreen");
	}
}
