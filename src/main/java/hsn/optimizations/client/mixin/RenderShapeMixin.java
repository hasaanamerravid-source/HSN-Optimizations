package hsn.optimizations.client.mixin;

import hsn.optimizations.client.optimize.RenderShapeCuller;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla path: after the renderer fills visible sections, keep only those
 * inside the view-distance circle / ellipse.
 * <p>
 * 26.3 changed {@code prepareChunkRenders} to {@code (Matrix4fc, boolean)}.
 * Each inject uses a full JVM descriptor so Mixin cannot bind the old
 * handler to the new method and crash on apply.
 */
@Mixin(value = LevelRenderer.class, priority = 1100)
public class RenderShapeMixin {

	@Inject(method = "cullTerrain", at = @At("RETURN"), require = 0)
	private void hsn$circleAfterCull(CallbackInfo ci) {
		hsn$filter();
	}

	@Inject(
			method = "prepareChunkRenders(Lorg/joml/Matrix4fc;Z)Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;",
			at = @At("HEAD"),
			require = 0
	)
	private void hsn$circleBeforeDraw26(
			Matrix4fc matrix,
			boolean flag,
			CallbackInfoReturnable<ChunkSectionsToRender> cir) {
		hsn$filter();
	}

	@Inject(
			method = "prepareChunkRenders(Lorg/joml/Matrix4fc;)Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;",
			at = @At("HEAD"),
			require = 0
	)
	private void hsn$circleBeforeDrawOld(
			Matrix4fc matrix,
			CallbackInfoReturnable<ChunkSectionsToRender> cir) {
		hsn$filter();
	}

	@Inject(
			method = "prepareChunkRenders(Lorg/joml/Matrix4fc;DDD)Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;",
			at = @At("HEAD"),
			require = 0
	)
	private void hsn$circleBeforeDrawPos(
			Matrix4fc matrix,
			double x,
			double y,
			double z,
			CallbackInfoReturnable<ChunkSectionsToRender> cir) {
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
