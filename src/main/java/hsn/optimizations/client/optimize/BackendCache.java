package hsn.optimizations.client.optimize;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tiny disk cache so the next boot does not re-walk RenderSystem when the
 * device is not up yet. Live detection always wins over this file.
 */
final class BackendCache {

	private BackendCache() {
	}

	static GraphicsBackend.Kind read() {
		try {
			Path p = file();
			if (p == null || !Files.isRegularFile(p)) {
				return GraphicsBackend.Kind.UNKNOWN;
			}
			String raw = Files.readString(p, StandardCharsets.UTF_8).trim().toUpperCase();
			return GraphicsBackend.Kind.valueOf(raw);
		} catch (Throwable ignored) {
			return GraphicsBackend.Kind.UNKNOWN;
		}
	}

	static void write(GraphicsBackend.Kind kind) {
		if (kind == null || kind == GraphicsBackend.Kind.UNKNOWN) {
			return;
		}
		try {
			Path p = file();
			if (p == null) {
				return;
			}
			Files.createDirectories(p.getParent());
			Files.writeString(p, kind.name(), StandardCharsets.UTF_8);
		} catch (Throwable ignored) {
		}
	}

	private static Path file() {
		try {
			String home = System.getProperty("user.home", ".");
			return Path.of(home, ".hsn-optimizations", "backend-cache.txt");
		} catch (Throwable ignored) {
			return null;
		}
	}
}
