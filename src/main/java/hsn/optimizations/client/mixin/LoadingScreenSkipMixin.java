package hsn.optimizations.client.mixin;

import hsn.optimizations.client.optimize.HSNWorldOpenClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fallback if a mapping set still exposes {@code Minecraft.setScreen}.
 * Minecraft 26.2 moved the live call to {@link GuiSetScreenMixin}.
 */
@Mixin(Minecraft.class)
public class LoadingScreenSkipMixin {

	@Inject(method = "setScreen", at = @At("HEAD"), cancellable = true, require = 0)
	private void hsn$admitWhenReady(Screen screen, CallbackInfo ci) {
		Minecraft mc = (Minecraft) (Object) this;
		if (HSNWorldOpenClient.shouldAdmitFrame(mc, screen)) {
			ci.cancel();
		}
	}
}
