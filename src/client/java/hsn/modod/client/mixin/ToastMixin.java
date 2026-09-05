package hsn.modod.client.mixin;

import hsn.modod.config.HSNConfig;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ToastManager.class)
public class ToastMixin {

	@Unique
	private static int hsn$shown;
	@Unique
	private static long hsn$windowStart;

	@Inject(method = "addToast", at = @At("HEAD"), cancellable = true)
	private void hsn$limitToasts(Toast toast, CallbackInfo ci) {
		if (!hsn.modod.optimize.HotPath.masterOn()) {
			return;
		}
		if (!HSNConfig.get().toastLimitEnabled) {
			return;
		}
		long now = System.currentTimeMillis();
		if (now - hsn$windowStart > 2000L) {
			hsn$shown = 0;
			hsn$windowStart = now;
		}
		if (hsn$shown >= 4) {
			ci.cancel();
			return;
		}
		hsn$shown++;
	}
}
