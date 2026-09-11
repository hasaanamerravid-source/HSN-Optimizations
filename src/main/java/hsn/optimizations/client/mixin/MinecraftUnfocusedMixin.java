package hsn.optimizations.client.mixin;

import hsn.optimizations.client.optimize.CameraSnapshot;
import hsn.optimizations.client.optimize.UnfocusedCap;
import hsn.optimizations.optimize.HSNScheduler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftUnfocusedMixin {

	@Inject(method = "runTick", at = @At("HEAD"), require = 0)
	private void hsn$unfocusedCap(CallbackInfo ci) {
		CameraSnapshot.capture();
		UnfocusedCap.apply();
	}

	@Inject(method = "runTick", at = @At("RETURN"), require = 0)
	private void hsn$pace(CallbackInfo ci) {
		if (HSNScheduler.shouldYield()) {
			Thread.yield();
		}
	}
}
