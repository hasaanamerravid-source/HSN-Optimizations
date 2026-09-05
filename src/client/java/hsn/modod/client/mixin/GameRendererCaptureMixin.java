package hsn.modod.client.mixin;

import hsn.modod.client.optimize.CameraSnapshot;
import hsn.modod.client.optimize.HighEndCounters;
import hsn.modod.client.optimize.UnfocusedCap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.renderer.GameRenderer", priority = 800)
public class GameRendererCaptureMixin {

	@Inject(method = {"render", "renderLevel"}, at = @At("HEAD"), require = 0)
	private void hsn$captureCamera(CallbackInfo ci) {
		CameraSnapshot.capture();
		HighEndCounters.tick();
		UnfocusedCap.apply();
	}
}
