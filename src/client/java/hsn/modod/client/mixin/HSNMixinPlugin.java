package hsn.modod.client.mixin;

import hsn.modod.config.HSNConfig;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class HSNMixinPlugin implements IMixinConfigPlugin {

	@Override
	public void onLoad(String mixinPackage) {
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
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
}
