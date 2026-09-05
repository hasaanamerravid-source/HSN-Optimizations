package hsn.modod.client.mixin;

import hsn.modod.client.optimize.RenderShapeCuller;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = SectionOcclusionGraph.class, priority = 1100)
public class SectionFrustumShapeMixin {

	@Inject(method = "addSectionsInFrustum", at = @At("RETURN"), require = 0)
	private void hsn$circleFrustum(Frustum frustum,
			List<SectionRenderDispatcher.RenderSection> visible,
			List<SectionRenderDispatcher.RenderSection> nearby,
			CallbackInfo ci) {
		try {
			RenderShapeCuller.filterSections(visible, false);
			RenderShapeCuller.filterSections(nearby, true);
		} catch (Throwable ignored) {
		}
	}
}
