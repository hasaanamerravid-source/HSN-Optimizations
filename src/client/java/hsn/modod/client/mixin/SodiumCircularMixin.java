package hsn.modod.client.mixin;

import hsn.modod.client.optimize.RenderShapeCuller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Optional Sodium hook. Compiled without Sodium on the classpath by targeting
 * the class name as a string and using Object for Sodium types.
 */
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.occlusion.OcclusionCuller", remap = false)
public abstract class SodiumCircularMixin {

	@Unique
	private Object hsn$currentSection;

	@Inject(method = "visitNode", at = @At("HEAD"), require = 0)
	private void hsn$captureSection(Object queue, Object section, int outgoingDirection,
			boolean hasLocalPath, boolean hasRegularPath, boolean hasWidePath, CallbackInfo ci) {
		this.hsn$currentSection = section;
	}

	@Redirect(method = "visitNode", at = @At(
			value = "INVOKE",
			target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/occlusion/OcclusionCuller;testDistance(FFF)Z"),
			require = 0)
	private boolean hsn$shapedDistance(Object culler, float xzThreshold, float yThreshold, float maxDistance) {
		Object section = this.hsn$currentSection;
		if (section != null && RenderShapeCuller.isActive()) {
			try {
				double x = originCoord(section, "getOriginX", "getX") + 8.0;
				double y = originCoord(section, "getOriginY", "getY") + 8.0;
				double z = originCoord(section, "getOriginZ", "getZ") + 8.0;
				if (!RenderShapeCuller.shouldDrawWorldPoint(x, y, z)) {
					return false;
				}
			} catch (Throwable ignored) {
			}
		}
		return (xzThreshold < (maxDistance * maxDistance)) && (yThreshold < maxDistance);
	}

	@Unique
	private static double originCoord(Object section, String primary, String fallback) throws Exception {
		try {
			return ((Number) section.getClass().getMethod(primary).invoke(section)).doubleValue();
		} catch (NoSuchMethodException ignored) {
			return ((Number) section.getClass().getMethod(fallback).invoke(section)).doubleValue();
		}
	}
}
