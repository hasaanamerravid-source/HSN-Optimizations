package hsn.modod.client.mixin;

import hsn.modod.client.optimize.RenderShapeCuller;
import hsn.modod.client.optimize.SodiumVisibleSections;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.util.collections.WriteQueue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

	@Redirect(
			method = "visitNode(Lnet/caffeinemc/mods/sodium/client/util/collections/WriteQueue;Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSection;IZZZ)V",
			at = @At(
					value = "INVOKE",
					target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/occlusion/OcclusionCuller;testDistance(FFFLnet/caffeinemc/mods/sodium/client/util/collections/WriteQueue;Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSection;IZZZ)Z"
			),
			require = 0
	)
	private boolean hsn$shapedDistance(float xzThreshold, float yThreshold, float maxDistance,
			WriteQueue<?> queue, RenderSection redirectSection, int outgoingDirection,
			boolean hasLocalPath, boolean hasRegularPath, boolean hasWidePath) {
		// Mixin @Redirect on a call made from inside the target class itself uses the
		// mixed-in instance as the implicit receiver, so no separate "culler" argument
		// is needed here — the parameter list must mirror testDistance(...) exactly.
		// Sodium 0.9.x's testDistance() carries the section directly, which is more
		// reliable than the @Inject-captured field below, so prefer it when present.
		RenderSection section = redirectSection != null ? redirectSection : this.hsn$currentSection;
		boolean inRange = (xzThreshold < (maxDistance * maxDistance)) && (yThreshold < maxDistance);
		if (section != null && RenderShapeCuller.isActive()) {
			try {
				double x = section.getOriginX() + 8.0;
				double y = section.getOriginY() + 8.0;
				double z = section.getOriginZ() + 8.0;
				if (!RenderShapeCuller.shouldDrawWorldPoint(x, y, z)) {
					inRange = false;
				}
			} catch (Throwable ignored) {
				try {
					double x = originCoord(section, "getOriginX", "getX") + 8.0;
					double y = originCoord(section, "getOriginY", "getY") + 8.0;
					double z = originCoord(section, "getOriginZ", "getZ") + 8.0;
					if (!RenderShapeCuller.shouldDrawWorldPoint(x, y, z)) {
						inRange = false;
					}
				} catch (Throwable ignoredToo) {
				}
			}
		}
		if (inRange && section != null) {
			try {
				SodiumVisibleSections.markBlockOrigin(section.getOriginX(), section.getOriginY(), section.getOriginZ());
			} catch (Throwable ignored) {
			}
		}
		return inRange;
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
