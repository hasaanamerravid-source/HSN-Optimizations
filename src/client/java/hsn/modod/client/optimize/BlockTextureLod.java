package hsn.modod.client.optimize;

import com.mojang.blaze3d.opengl.GlStateManager;
import hsn.modod.HSNOptimizations;
import hsn.modod.config.HSNConfig;
import net.minecraft.resources.Identifier;

/**
 * Distant block textures: raise GL texture LOD bias on the blocks atlas while
 * the world is drawing. Hardware mipmaps already pick blurrier levels at range;
 * the bias makes that kick in sooner so the edge of render distance looks soft
 * instead of sparkling/aliased. Reset after the world pass so hotbar / inventory
 * items that share the same atlas stay sharp.
 *
 * This is not Sodium-style geometry LOD (lower-poly far chunks). That needs a
 * terrain renderer. HSN only changes texture sharpness.
 */
public final class BlockTextureLod {

	public static final int GL_TEXTURE_2D = 3553;
	public static final int GL_TEXTURE_BINDING_2D = 32873;
	public static final int GL_TEXTURE_LOD_BIAS = 34049;
	public static final int GL_TEXTURE_MAX_ANISOTROPY = 34046;

	private static int atlasGlId;
	private static float lastApplied = Float.NaN;
	private static boolean loggedMissing;
	private static boolean worldPass;

	private BlockTextureLod() {
	}

	public static boolean isBlocksAtlas(Identifier id) {
		if (id == null) {
			return false;
		}
		String path = id.getPath();
		return path.contains("blocks") && (path.contains("atlas") || path.equals("blocks"));
	}

	public static void captureBoundAtlas() {
		try {
			int bound = GlStateManager._getInteger(GL_TEXTURE_BINDING_2D);
			if (bound > 0) {
				atlasGlId = bound;
			}
		} catch (Throwable t) {
			if (!loggedMissing) {
				loggedMissing = true;
				HSNOptimizations.LOGGER.warn("Block texture LOD could not read GL texture binding: {}", t.toString());
			}
		}
	}

	public static void beginWorldPass() {
		worldPass = true;
		apply(currentBias());
	}

	public static void endWorldPass() {
		worldPass = false;
		apply(0.0f);
	}

	public static boolean inWorldPass() {
		return worldPass;
	}

	public static float currentBias() {
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.blockTextureLodEnabled) {
			return 0.0f;
		}
		double bias = cfg.blockTextureLodBias;
		if (cfg.blockTextureLodAdaptive) {
			double scale = AdaptiveCuller.getScale();
			if (cfg.performanceModeEnabled || AdaptiveCuller.isWeakGpuActive()) {
				bias += 0.75;
			} else if (scale < 0.7) {
				bias += (0.7 - scale) * 1.5;
			}
		}
		if (bias < 0.0) return 0.0f;
		if (bias > 3.0) return 3.0f;
		return (float) bias;
	}

	private static void apply(float bias) {
		if (atlasGlId <= 0) {
			return;
		}
		if (lastApplied == bias) {
			return;
		}
		try {
			int prev = 0;
			try {
				prev = GlStateManager._getInteger(GL_TEXTURE_BINDING_2D);
			} catch (Throwable ignored) {
			}
			GlStateManager._bindTexture(atlasGlId);
			GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_LOD_BIAS, (int) bias);
			if (bias > 0.0f && (HSNConfig.get().performanceModeEnabled || AdaptiveCuller.isWeakGpuActive())) {
				GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY, (int) 1.0f);
			}
			if (prev > 0) {
				GlStateManager._bindTexture(prev);
			}
			lastApplied = bias;
		} catch (Throwable t) {
			if (!loggedMissing) {
				loggedMissing = true;
				HSNOptimizations.LOGGER.warn("Block texture LOD not applied (GL hook missing): {}", t.toString());
			}
		}
	}
}
