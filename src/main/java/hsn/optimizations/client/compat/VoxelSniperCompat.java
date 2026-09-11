package hsn.optimizations.client.compat;

import hsn.optimizations.HSNOptimizations;
import hsn.optimizations.config.HSNConfig;
import hsn.optimizations.platform.HSNPlatform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Soft integration with VoxelSniper (Reimagined / Fabric / FAVS) and the
 * WorldEdit wand. While the player is holding a sniper or edit tool, HSN
 * can pause horizon-Y and terrain-mask culls so the aimed block stays
 * drawn at long range.
 */
public final class VoxelSniperCompat {

	private static volatile boolean present;
	private static volatile boolean worldEditPresent;
	private static volatile boolean holdingTool;
	private static volatile boolean relaxing;
	private static volatile String label = "off";
	private static boolean initialized;

	private VoxelSniperCompat() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		present = presentAny(
				"voxelsniper",
				"voxelsniperfabric",
				"voxelsniper-reimagined",
				"voxelsniper_reimagined",
				"fastasyncvoxelsniper",
				"favs",
				"voxel_sniper",
				"voxelsniper-fabric");
		worldEditPresent = presentAny(
				"worldedit",
				"fabric-world-edit",
				"fastasyncworldedit",
				"fawe",
				"worldeditcui",
				"worldeditcui-fabric");
		if (present || worldEditPresent) {
			HSNOptimizations.LOGGER.info(
					"Editor tools detected (voxelSniper={}, worldEdit={}); HSN can pause long-range culls while a tool is held.",
					present, worldEditPresent);
		}
	}

	public static void tick() {
		if (!initialized) {
			init();
		}
		HSNConfig cfg = HSNConfig.get();
		if (cfg == null || !cfg.modEnabled || !cfg.voxelSniperCompatEnabled) {
			holdingTool = false;
			relaxing = false;
			label = "off";
			return;
		}
		boolean editor = present || (cfg.voxelSniperDetectWorldEdit && worldEditPresent);
		if (!editor && !cfg.voxelSniperAlwaysScanTools) {
			holdingTool = false;
			relaxing = false;
			label = present ? "idle" : "no-mod";
			return;
		}
		holdingTool = holdingSniperTool();
		relaxing = holdingTool;
		if (relaxing) {
			label = present ? "sniping" : "edit-tool";
		} else if (editor) {
			label = "ready";
		} else {
			label = "no-mod";
		}
	}

	public static boolean present() {
		if (!initialized) {
			init();
		}
		return present;
	}

	public static boolean worldEditPresent() {
		if (!initialized) {
			init();
		}
		return worldEditPresent;
	}

	public static boolean holdingTool() {
		return holdingTool;
	}

	public static boolean relaxing() {
		return relaxing;
	}

	public static boolean pauseHorizon() {
		return relaxing && HSNConfig.get().voxelSniperPauseHorizon;
	}

	public static boolean pauseTerrainMask() {
		return relaxing && HSNConfig.get().voxelSniperPauseTerrainMask;
	}

	public static boolean pauseEntityCull() {
		return relaxing && HSNConfig.get().voxelSniperPauseEntityCull;
	}

	public static double range() {
		double r = HSNConfig.get().voxelSniperToolRange;
		if (r < 16.0) {
			return 16.0;
		}
		if (r > 384.0) {
			return 384.0;
		}
		return r;
	}

	public static String status() {
		return label;
	}

	private static boolean holdingSniperTool() {
		try {
			Minecraft mc = Minecraft.getInstance();
			if (mc == null) {
				return false;
			}
			LocalPlayer player = mc.player;
			if (player == null) {
				return false;
			}
			return isTool(player.getMainHandItem()) || isTool(player.getOffhandItem());
		} catch (Throwable ignored) {
			return false;
		}
	}

	private static boolean isTool(Object stack) {
		if (stack == null) {
			return false;
		}
		try {
			Object empty = stack.getClass().getMethod("isEmpty").invoke(stack);
			if (Boolean.TRUE.equals(empty)) {
				return false;
			}
		} catch (Throwable ignored) {
		}
		String raw = String.valueOf(stack).toLowerCase();
		if (matchesTool(raw)) {
			return true;
		}
		try {
			Object item = stack.getClass().getMethod("getItem").invoke(stack);
			if (item != null && matchesTool(String.valueOf(item).toLowerCase())) {
				return true;
			}
		} catch (Throwable ignored) {
		}
		return false;
	}

	private static boolean matchesTool(String s) {
		if (s == null || s.isEmpty()) {
			return false;
		}
		String n = s.toLowerCase();
		int slash = Math.max(n.lastIndexOf(':'), n.lastIndexOf('/'));
		String path = slash >= 0 ? n.substring(slash + 1) : n;
		int end = path.length();
		for (int i = 0; i < path.length(); i++) {
			char c = path.charAt(i);
			if (!(c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_')) {
				end = i;
				break;
			}
		}
		path = path.substring(0, end);
		return path.equals("arrow")
				|| path.equals("spectral_arrow")
				|| path.equals("tipped_arrow")
				|| path.equals("gunpowder")
				|| path.equals("gun_powder")
				|| path.equals("wooden_axe")
				|| path.equals("wood_axe")
				|| path.equals("wand")
				|| path.contains("sniper");
	}

	private static boolean presentAny(String... ids) {
		for (String id : ids) {
			if (HSNPlatform.modPresent(id)) {
				return true;
			}
		}
		return false;
	}
}
