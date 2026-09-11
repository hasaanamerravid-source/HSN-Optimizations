package hsn.optimizations.client.mixin;

import hsn.optimizations.client.optimize.HSNWorldOpenClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Minecraft 26.2 moved {@code Minecraft.setScreen} to {@code Minecraft.gui.setScreen}.
 * Instant-open only skips the overlay on integrated servers. Remote joins keep
 * vanilla {@code ReceivingLevelScreen} so "Joining world…" can close.
 */
@Mixin(targets = "net.minecraft.client.gui.Gui", priority = 900)
public class GuiSetScreenMixin {

	@Inject(method = "setScreen", at = @At("HEAD"), cancellable = true, require = 0)
	private void hsn$admitWhenReady(Screen screen, CallbackInfo ci) {
		if (HSNWorldOpenClient.shouldAdmitFrame(Minecraft.getInstance(), screen)) {
			ci.cancel();
		}
	}
}
