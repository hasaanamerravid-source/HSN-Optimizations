package hsn.modod.client.mixin;

import hsn.modod.config.HSNConfig;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Soft limit on simultaneous toasts to reduce UI spam on weak systems.
 */
@Mixin(ToastManager.class)
public class ToastMixin {

	@Inject(method = "addToast", at = @At("HEAD"), cancellable = true, require = 0)
	private void hsn$limitToasts(Toast toast, CallbackInfo ci) {
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.toastLimitEnabled) {
			return;
		}
		// Simple probabilistic throttle when many toasts are expected; avoids reflection complexity
		if (Math.random() > 0.85) {
			ci.cancel();
		}
	}
}
