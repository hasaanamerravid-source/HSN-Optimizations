package hsn.modod.client.mixin;

import hsn.modod.client.optimize.RenderShapeCuller;
import hsn.modod.client.optimize.SodiumVisibleSections;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.util.collections.WriteQueue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.occlusion.OcclusionCuller", remap = false)
public abstract class SodiumCircularMixin {

	@Unique
	private RenderSection hsn$currentSection;

	@Inject(method = "findVisible", at = @At("HEAD"), require = 0)
	private void hsn$beginVisibleFrame(CallbackInfo ci) {
		SodiumVisibleSections.beginFrame();
	}

	@Inject(method = "findVisible", at = @At("RETURN"), require = 0)
	private void hsn$endVisibleFrame(CallbackInfo ci) {
		SodiumVisibleSections.endFrame();
	}

	@Inject(
			method = "visitNode(Lnet/caffeinemc/mods/sodium/client/util/collections/WriteQueue;Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSection;IZZZ)V",
			at = @At("HEAD"),
			require = 0
	)
	private void hsn$captureSection(WriteQueue<?> queue, RenderSection section, int outgoingDirection,
			boolean hasLocalPath, boolean hasRegularPath, boolean hasWidePath, CallbackInfo ci) {
		this.hsn$currentSection = section;
	}

	/**
	 * Extra circular/hex mask only. Do not replace Sodium's own testDistance —
	 * a redirect that reimplements range with the wrong units culls every
	 * section and the world never draws.
	 */
	@Inject(
			method = "testDistance(FFFLnet/caffeinemc/mods/sodium/client/util/collections/WriteQueue;Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSection;IZZZ)Z",
			at = @At("RETURN"),
			cancellable = true,
			require = 0
	)
	private void hsn$shapedDistance(float xzThreshold, float yThreshold, float maxDistance,
			WriteQueue<?> queue, RenderSection redirectSection, int outgoingDirection,
			boolean hasLocalPath, boolean hasRegularPath, boolean hasWidePath,
			CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ()) {
			return;
		}
		RenderSection section = redirectSection != null ? redirectSection : this.hsn$currentSection;
		if (section == null) {
			return;
		}
		if (RenderShapeCuller.isActive()) {
			try {
				double x = section.getOriginX() + 8.0;
				double y = section.getOriginY() + 8.0;
				double z = section.getOriginZ() + 8.0;
				if (!RenderShapeCuller.shouldDrawWorldPoint(x, y, z)) {
					cir.setReturnValue(false);
					return;
				}
			} catch (Throwable ignored) {
				try {
					double x = originCoord(section, "getOriginX", "getX") + 8.0;
					double y = originCoord(section, "getOriginY", "getY") + 8.0;
					double z = originCoord(section, "getOriginZ", "getZ") + 8.0;
					if (!RenderShapeCuller.shouldDrawWorldPoint(x, y, z)) {
						cir.setReturnValue(false);
						return;
					}
				} catch (Throwable ignoredToo) {
				}
			}
		}
		try {
			SodiumVisibleSections.markBlockOrigin(section.getOriginX(), section.getOriginY(), section.getOriginZ());
		} catch (Throwable ignored) {
		}
	}

	@Unique
	private static double originCoord(RenderSection section, String primary, String fallback) throws Exception {
		try {
			return ((Number) section.getClass().getMethod(primary).invoke(section)).doubleValue();
		} catch (NoSuchMethodException ignored) {
			return ((Number) section.getClass().getMethod(fallback).invoke(section)).doubleValue();
		}
	}
}
