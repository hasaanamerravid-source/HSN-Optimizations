package hsn.optimizations.optimize;

import hsn.optimizations.HSNOptimizations;
import hsn.optimizations.platform.HSNPlatform;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * ZGC cannot be switched on after the JVM has started. This class detects
 * the live collector and writes a launcher snippet the player can paste.
 */
public final class HSNGc {

	private static final String HINT = """
			# HSN recommended JVM arguments (Java 25+)
			# Paste into your launcher → Installation → JVM arguments.
			# ZGC is selected at process start. A running game cannot change it.
			#
			-XX:+UseZGC
			-XX:+AlwaysPreTouch
			-XX:SoftMaxHeapSize=4G
			""";

	private HSNGc() {
	}

	private static volatile String cachedCollector;

	public static String collectorName() {
		String cached = cachedCollector;
		if (cached != null) {
			return cached;
		}
		StringBuilder sb = new StringBuilder();
		for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
			if (sb.length() > 0) {
				sb.append('+');
			}
			sb.append(bean.getName());
		}
		cached = sb.length() == 0 ? "unknown" : sb.toString();
		cachedCollector = cached;
		return cached;
	}

	public static boolean zgcActive() {
		return collectorName().toLowerCase(Locale.ROOT).contains("zgc");
	}

	public static void publish() {
		String name = collectorName();
		Path file;
		try {
			file = HSNPlatform.gameDir().resolve("hsn-recommended-jvm.txt");
		} catch (Throwable ignored) {
			file = Path.of("hsn-recommended-jvm.txt");
		}
		try {
			Files.writeString(file, HINT);
		} catch (Exception ignored) {
		}
		if (zgcActive()) {
			HSNOptimizations.LOGGER.info("HSN GC: {} (generational ZGC path is live)", name);
		} else {
			HSNOptimizations.LOGGER.info(
					"HSN GC: {} — add -XX:+UseZGC to the launcher JVM args for pause-less collections. Wrote {}",
					name, file.toAbsolutePath());
		}
	}
}
