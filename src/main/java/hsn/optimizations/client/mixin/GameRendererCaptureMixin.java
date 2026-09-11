package hsn.optimizations.client.mixin;

import hsn.optimizations.client.optimize.CameraSnapshot;
import hsn.optimizations.client.optimize.HighEndCounters;
import hsn.optimizations.client.optimize.UnfocusedCap;
import hsn.optimizations.client.optimize.RenderScale;
import hsn.optimizations.client.optimize.WindowGate;
import hsn.optimizations.optimize.HSNScheduler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.renderer.GameRenderer", priority = 800)
public class GameRendererCaptureMixin {

	@Unique
	private long hsn$frameMark;

	@Inject(method = {"renderLevel", "renderWorld"}, at = @At("HEAD"), require = 0)
	private void hsn$beginScale(CallbackInfo ci) {
		RenderScale.beginWorld();
	}

	@Inject(method = {"renderLevel", "renderWorld"}, at = @At("RETURN"), require = 0)
	private void hsn$endScale(CallbackInfo ci) {
		RenderScale.endWorld();
	}

	@Inject(method = {"render", "renderLevel"}, at = @At("HEAD"), require = 0)
	private void hsn$captureCamera(CallbackInfo ci) {
		hsn$frameMark = System.nanoTime();
		WindowGate.markReady();
		CameraSnapshot.capture();
		HighEndCounters.tick();
		UnfocusedCap.apply();
	}

	@Inject(method = {"render"}, at = @At("RETURN"), require = 0)
	private void hsn$publishFrame(CallbackInfo ci) {
		if (hsn$frameMark != 0L) {
			HSNScheduler.noteFrameNanos(System.nanoTime() - hsn$frameMark);
		}
	}

	@Inject(method = {"resize", "onResized"}, at = @At("RETURN"), require = 0)
	private void hsn$scaleResize(CallbackInfo ci) {
		RenderScale.onFramebufferResized();
	}
}
