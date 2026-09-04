package hsn.modod.client.mixin;

import hsn.modod.client.optimize.RenderShapeCuller;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla path: after the renderer fills visible sections, keep only those
 * inside the view-distance circle / ellipse.
 */
@Mixin(value = LevelRenderer.class, priority = 1100)
public class RenderShapeMixin {

	@Inject(method = "cullTerrain", at = @At("RETURN"), require = 0)
	private void hsn$circleAfterCull(Camera camera, Frustum frustum, boolean spectator, CallbackInfo ci) {
		hsn$filter();
	}

	@Inject(method = "prepareChunkRenders", at = @At("HEAD"), require = 0)
	private void hsn$circleBeforeDraw(Matrix4fc matrix, CallbackInfoReturnable<ChunkSectionsToRender> cir) {
		hsn$filter();
	}

	private void hsn$filter() {
		try {
			LevelRenderer renderer = (LevelRenderer) (Object) this;
			RenderShapeCuller.filterSections(renderer.visibleSections(), false);
			RenderShapeCuller.filterSections(renderer.nearbyVisibleSections(), true);
		} catch (Throwable ignored) {
		}
	}
}
