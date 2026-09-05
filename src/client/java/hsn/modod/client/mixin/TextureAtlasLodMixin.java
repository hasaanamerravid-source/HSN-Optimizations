package hsn.modod.client.mixin;

import hsn.modod.client.optimize.BlockTextureLod;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureAtlas.class)
public abstract class TextureAtlasLodMixin {

    @Shadow
    public abstract Identifier location();

    @Inject(method = "upload", at = @At("RETURN"), require = 1)
    private void hsn$captureBlockAtlas(CallbackInfo ci) {
        try {
            Identifier id = this.location();
            if (id != null && BlockTextureLod.isBlocksAtlas(id)) {
                BlockTextureLod.captureBoundAtlas();
            }
        } catch (Throwable ignored) {
        }
    }
}