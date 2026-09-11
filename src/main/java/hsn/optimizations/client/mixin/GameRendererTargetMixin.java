package hsn.optimizations.client.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import hsn.optimizations.client.access.HSNMainTarget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Same accessor RenderScale 26.2 uses:
 * {@code @Mutable @Shadow @Final private RenderTarget mainRenderTarget}.
 */
@Mixin(targets = "net.minecraft.client.renderer.GameRenderer", priority = 801)
public abstract class GameRendererTargetMixin implements HSNMainTarget {

	@Mutable
	@Shadow
	@Final
	private RenderTarget mainRenderTarget;

	@Override
	public void hsn$setMainRenderTarget(Object target) {
		if (target instanceof RenderTarget renderTarget) {
			this.mainRenderTarget = renderTarget;
		}
	}
}
