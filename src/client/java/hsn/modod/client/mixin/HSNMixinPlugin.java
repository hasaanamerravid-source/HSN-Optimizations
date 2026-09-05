package hsn.modod.client.mixin;

import hsn.modod.HSNOptimizations;
import hsn.modod.config.HSNConfig;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HSNMixinPlugin implements IMixinConfigPlugin {

	/**
	 * Renderer classes that have been renamed across Minecraft versions and
	 * mapping sets. Rather than statically declaring every historical name
	 * as a soft {@code @Mixin(targets = ...)} entry - which makes Sponge
	 * Mixin log a WARN for every candidate that doesn't resolve on the
	 * running version - each candidate name lives in its own single-target
	 * mixin class. {@link #onLoad(String)} probes each candidate with a
	 * plain {@code Class.forName} check (no Mixin involvement, so a miss is
	 * silent) and {@link #getMixins()} only hands Sponge Mixin the ones
	 * that are actually present. Whichever candidate exists for the running
	 * version gets the optimization applied; the others are never touched.
	 */
	private static final Map<String, String[]> VERSION_VARIANT_MIXINS = buildVariantMap();

	private static Map<String, String[]> buildVariantMap() {
		Map<String, String[]> map = new LinkedHashMap<>();
		map.put("LightTextureMixin", new String[]{
				"net.minecraft.client.renderer.LightTexture"
		});
		map.put("LightmapTextureManagerMixin", new String[]{
				"net.minecraft.client.renderer.lightmap.LightmapTextureManager"
		});
		map.put("LightmapTextureManagerYarnMixin", new String[]{
				"net.minecraft.client.render.LightmapTextureManager"
		});
		map.put("MapRendererThrottleMixin", new String[]{
				"net.minecraft.client.gui.MapRenderer"
		});
		map.put("MapRendererRendererMixin", new String[]{
				"net.minecraft.client.renderer.MapRenderer"
		});
		return map;
	}

	private final List<String> resolvedVariantMixins = new ArrayList<>();

	@Override
	public void onLoad(String mixinPackage) {
		ClassLoader cl = HSNMixinPlugin.class.getClassLoader();
		for (Map.Entry<String, String[]> entry : VERSION_VARIANT_MIXINS.entrySet()) {
			for (String candidate : entry.getValue()) {
				if (classExists(cl, candidate)) {
					resolvedVariantMixins.add(entry.getKey());
					break;
				}
			}
		}
		if (resolvedVariantMixins.isEmpty()) {
			HSNOptimizations.LOGGER.info(
					"HSN: no known light-texture/map-renderer target matched this Minecraft version; "
							+ "the lightmap-cache and map-throttle optimizations are inactive (everything else is unaffected).");
		}
	}

	private static boolean classExists(ClassLoader cl, String fqcn) {
		try {
			Class.forName(fqcn, false, cl);
			return true;
		} catch (Throwable ignored) {
			return false;
		}
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		FabricLoader loader = FabricLoader.getInstance();
		boolean sodium = loader.isModLoaded("sodium");
		boolean extra = loader.isModLoaded("sodium-extra");
		HSNConfig cfg = safeConfig();

		if (mixinClassName.endsWith("BufferStorageMixin")) {
			return !loader.isModLoaded("framepace");
		}
		if (mixinClassName.endsWith("SodiumCircularMixin")) {
			return sodium;
		}
		if (mixinClassName.endsWith("RenderShapeMixin") || mixinClassName.endsWith("SectionFrustumShapeMixin")) {
			return !sodium;
		}
		if (extra && cfg != null) {
			if (mixinClassName.endsWith("FogRendererMixin") && cfg.deferFogToSodiumExtra) {
				return false;
			}
			if (mixinClassName.endsWith("ToastMixin") && cfg.deferToastsToSodiumExtra) {
				return false;
			}
			if (mixinClassName.endsWith("BeaconRendererMixin") && cfg.deferBeaconToSodiumExtra) {
				return false;
			}
			if (mixinClassName.endsWith("TextureAnimationMixin") && cfg.deferTextureAnimToSodiumExtra) {
				return false;
			}
			if ((mixinClassName.endsWith("ParticleManagerMixin") || mixinClassName.endsWith("ParticleTickMixin"))
					&& cfg.deferParticlesToSodiumExtra) {
				return false;
			}
		}
		return true;
	}

	private static HSNConfig safeConfig() {
		try {
			return HSNConfig.get();
		} catch (Throwable ignored) {
			return null;
		}
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return resolvedVariantMixins.isEmpty() ? null : resolvedVariantMixins;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
}
