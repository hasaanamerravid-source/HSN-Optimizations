package hsn.modod.client.mixin;

import hsn.modod.client.optimize.CameraSnapshot;
import hsn.modod.optimize.HotPath;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Clouds are a fill-rate tax even on a 5090 when fancy clouds + shaders
 * stack. Skip the cloud pass when the camera is looking down or the
 * dimension has a ceiling.
 * <p>
 * Must NOT target {@code LevelRenderer}. In 26.2 Fabric documents
 * {@code LevelRenderer#render} as the main world pass. A cancellable
 * inject into that method leaves the title-screen panorama on screen
 * and looks like "stuck on Loading terrain".
 */
@Mixin(targets = {
		"net.minecraft.client.renderer.CloudRenderer"
}, priority = 910)
public class CloudLodMixin {

	@Inject(method = {
			"render",
			"renderClouds",
			"compileClouds"
	}, at = @At("HEAD"), cancellable = true, require = 0)
	private void hsn$skipHiddenClouds(CallbackInfo ci) {
		if (!HotPath.flag(HotPath.CLOUD_LOD)) {
			return;
		}
		if (!CameraSnapshot.valid()) {
			return;
		}
		if (CameraSnapshot.hasCeiling()) {
			ci.cancel();
			return;
		}
		if (!CameraSnapshot.lookingUp() && CameraSnapshot.y() < 80.0) {
			ci.cancel();
		}
	}
}
