package hsn.optimizations.client.mixin;

import hsn.optimizations.HSNOptimizations;
import hsn.optimizations.client.compat.ResolutionControlCompat;
import hsn.optimizations.config.HSNConfig;
import hsn.optimizations.platform.HSNPlatform;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class HSNMixinPlugin implements IMixinConfigPlugin {

	/**
	 * Renderer classes that have been renamed across Minecraft versions and
	 * mapping sets. Rather than statically declaring every historical name
	 * as a soft {@code @Mixin(targets = ...)} entry - which makes Sponge
	 * Mixin log a WARN for every candidate that doesn't resolve on the
	 * running version - each candidate name lives in its own single-target
	 * mixin class.
	 * <p>
	 * {@link #onLoad(String)} probes each candidate with a <em>resource</em>
	 * lookup ({@code ClassLoader.getResource}) so the class is never defined
	 * in the Knot loader. Using {@code Class.forName} here used to load
	 * {@code MapRenderer} (and the light-texture classes) during mixin
	 * prepare, which then crashed Fabric at Sodium's {@code preLaunch}
	 * entrypoint with {@code MixinTargetAlreadyLoadedException}.
	 * {@link #getMixins()} only hands Sponge Mixin the candidates whose
	 * bytecode is present on this Minecraft version.
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
	private boolean sodium;
	private boolean extra;
	private boolean framepace;
	private boolean particleCore;
	private boolean asyncParticles;
	private boolean badOptimizations;
	private boolean immediatelyFast;
	private boolean entityCulling;
	private boolean moreCulling;
	private boolean blockEntityRd;
	private boolean circularRendering;
	private boolean nvidium;
	private HSNConfig cachedCfg;

	@Override
	public void onLoad(String mixinPackage) {
		sodium = any("sodium");
		extra = any("sodium-extra", "sodium_extra", "sodiumextra");
		framepace = any("framepace", "frame-pace");
		particleCore = any("particle_core", "particlecore", "particle-core");
		asyncParticles = any("asyncparticles", "async_particles");
		badOptimizations = any("badoptimizations", "bad_optimizations");
		immediatelyFast = any("immediatelyfast", "immediately_fast");
		entityCulling = any("entityculling", "entity_culling");
		moreCulling = any("moreculling", "more_culling");
		blockEntityRd = any("blockentityrd", "block_entity_rd", "berd");
		circularRendering = any("circular-rendering", "circular_rendering", "circularrendering");
		nvidium = any("nvidium");
		cachedCfg = safeConfig();
		ClassLoader cl = HSNMixinPlugin.class.getClassLoader();
		for (Map.Entry<String, String[]> entry : VERSION_VARIANT_MIXINS.entrySet()) {
			for (String candidate : entry.getValue()) {
				if (!classBytecodePresent(cl, candidate)) {
					continue;
				}
				resolvedVariantMixins.add(entry.getKey());
				break;
			}
		}
		if (resolvedVariantMixins.isEmpty()) {
			HSNOptimizations.LOGGER.info(
					"HSN: no known light-texture/map-renderer target matched this Minecraft version; "
							+ "the lightmap-cache and map-throttle optimizations are inactive (everything else is unaffected).");
		} else {
			HSNOptimizations.LOGGER.debug("HSN: enabling version-variant mixins {}", resolvedVariantMixins);
		}
	}

	/**
	 * True when the {@code .class} resource exists. Does not define the class,
	 * so Mixin can still transform it later.
	 */
	private static boolean classBytecodePresent(ClassLoader cl, String fqcn) {
		String path = fqcn.replace('.', '/') + ".class";
		try {
			if (cl != null && cl.getResource(path) != null) {
				return true;
			}
		} catch (Throwable ignored) {
		}
		try {
			ClassLoader ctx = Thread.currentThread().getContextClassLoader();
			if (ctx != null && ctx != cl && ctx.getResource(path) != null) {
				return true;
			}
		} catch (Throwable ignored) {
		}
		try {
			return ClassLoader.getSystemResource(path) != null;
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
		HSNConfig cfg = cachedCfg;

		// Only skip when the *target class is absent* or another mod owns the
		// exact same GL hook. HSN stays on next to Sodium / Entity Culling /
		// Particle Core / More Culling / BadOptimizations.
		if (mixinClassName.endsWith("WindowScaleMixin")) {
			return !ResolutionControlCompat.present();
		}
		if (mixinClassName.endsWith("BufferStorageMixin")) {
			return !framepace;
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

	private static boolean any(String... ids) {
		for (String id : ids) {
			if (HSNPlatform.modPresent(id)) {
				return true;
			}
		}
		return false;
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
