package hsn.optimizations.client.mixin;

import hsn.optimizations.client.optimize.BlockTextureLod;
import hsn.optimizations.client.optimize.GlGuard;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureAtlas.class)
public abstract class TextureAtlasLodMixin {

	@Inject(method = {"upload", "update", "reload"}, at = @At("RETURN"), require = 0)
	private void hsn$captureBlockAtlas(CallbackInfo ci) {
		try {
			Identifier id = hsn$atlasId();
			if (id != null && BlockTextureLod.isBlocksAtlas(id) && GlGuard.glUsable()) {
				BlockTextureLod.captureBoundAtlas();
			}
		} catch (Throwable ignored) {
		}
	}

	@Unique
	private Identifier hsn$atlasId() {
		for (String name : new String[]{"identifier", "location", "id", "getId"}) {
			try {
				Object value = this.getClass().getMethod(name).invoke(this);
				if (value instanceof Identifier ident) {
					return ident;
				}
			} catch (Throwable ignored) {
			}
		}
		return null;
	}
}
