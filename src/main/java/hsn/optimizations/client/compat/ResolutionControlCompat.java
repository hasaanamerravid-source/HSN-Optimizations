package hsn.optimizations.client.compat;

import hsn.optimizations.HSNOptimizations;
import hsn.optimizations.platform.HSNPlatform;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Detects Resolution Control / ResolutionControl++ / RenderScale in the
 * Fabric loader <em>or</em> sitting in the user's {@code mods/} folder.
 * When present, HSN's own 3D scale path stays off so two framebuffer
 * scalers cannot fight.
 */
public final class ResolutionControlCompat {

	private static final String[] MOD_IDS = {
			"resolutioncontrol",
			"resolution-control",
			"resolution_control",
			"resolutioncontrolplus",
			"resolutioncontrolplusplus",
			"renderscale",
			"render-scale"
	};

	private static final String[] JAR_TOKENS = {
			"resolutioncontrol",
			"resolution-control",
			"resolution_control",
			"renderscale",
			"render-scale"
	};

	private static volatile Boolean cached;

	private ResolutionControlCompat() {
	}

	public static boolean present() {
		Boolean hit = cached;
		if (hit != null) {
			return hit;
		}
		boolean found = scanLoader() || scanModsFolder();
		cached = found;
		if (found) {
			HSNOptimizations.LOGGER.info(
					"HSN 3D render scale disabled — Resolution Control / RenderScale is in this instance");
		}
		return found;
	}

	public static String ownerLabel() {
		if (!present()) {
			return "";
		}
		for (String id : MOD_IDS) {
			if (HSNPlatform.modPresent(id)) {
				return id;
			}
		}
		return "mods folder (Resolution Control++)";
	}

	private static boolean scanLoader() {
		for (String id : MOD_IDS) {
			if (HSNPlatform.modPresent(id)) {
				return true;
			}
		}
		return false;
	}

	private static boolean scanModsFolder() {
		try {
			Path mods = HSNPlatform.gameDir().resolve("mods");
			if (!Files.isDirectory(mods)) {
				return false;
			}
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(mods)) {
				for (Path file : stream) {
					String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
					if (!(name.endsWith(".jar") || name.endsWith(".jar.disabled") || name.endsWith(".disabled"))) {
						continue;
					}
					for (String token : JAR_TOKENS) {
						if (name.contains(token)) {
							return true;
						}
					}
				}
			}
		} catch (Throwable ignored) {
		}
		return false;
	}
}
