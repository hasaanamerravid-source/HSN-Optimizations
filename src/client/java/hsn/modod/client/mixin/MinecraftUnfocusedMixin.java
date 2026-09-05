package hsn.modod.client.mixin;

import hsn.modod.client.optimize.CameraSnapshot;
import hsn.modod.client.optimize.UnfocusedCap;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftUnfocusedMixin {

	@Inject(method = {"runTick", "tick"}, at = @At("HEAD"), require = 0)
	private void hsn$unfocusedCap(CallbackInfo ci) {
		CameraSnapshot.capture();
		UnfocusedCap.apply();
	}
}
