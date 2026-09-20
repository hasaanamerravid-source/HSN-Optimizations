package hsn.optimizations.client.mixin;

import hsn.optimizations.client.optimize.RenderShapeCuller;
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

	@Inject(
			method = "addSectionsInFrustum(Lnet/minecraft/client/renderer/culling/Frustum;Ljava/util/List;Ljava/util/List;)V",
			at = @At("RETURN"),
			require = 0
	)
	private void hsn$circleFrustum(
			Frustum frustum,
			List<SectionRenderDispatcher.RenderSection> visible,
			List<SectionRenderDispatcher.RenderSection> nearby,
			CallbackInfo ci) {
		hsn$filter(visible, nearby);
	}

	@Inject(
			method = "addSectionsInFrustum(Lnet/minecraft/client/renderer/culling/Frustum;Ljava/util/List;Ljava/util/List;Z)V",
			at = @At("RETURN"),
			require = 0
	)
	private void hsn$circleFrustum26(
			Frustum frustum,
			List<SectionRenderDispatcher.RenderSection> visible,
			List<SectionRenderDispatcher.RenderSection> nearby,
			boolean extra,
			CallbackInfo ci) {
		hsn$filter(visible, nearby);
	}

	private void hsn$filter(
			List<SectionRenderDispatcher.RenderSection> visible,
			List<SectionRenderDispatcher.RenderSection> nearby) {
		try {
			RenderShapeCuller.filterSections(visible, false);
			RenderShapeCuller.filterSections(nearby, true);
		} catch (Throwable ignored) {
		}
	}
}
