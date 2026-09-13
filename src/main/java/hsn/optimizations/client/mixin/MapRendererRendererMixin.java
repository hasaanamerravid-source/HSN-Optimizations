package hsn.optimizations.client.mixin;

import hsn.optimizations.client.optimize.CameraSnapshot;
import hsn.optimizations.config.HSNConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Same optimization as {@link MapRendererThrottleMixin}, targeting the
 * {@code net.minecraft.client.renderer.MapRenderer} naming used by other
 * versions/mapping sets. Only registered by {@link HSNMixinPlugin} when
 * that class's bytecode is present (resource probe — never {@code Class.forName}).
 */
@Mixin(targets = "net.minecraft.client.renderer.MapRenderer", priority = 900)
public class MapRendererRendererMixin {

	@Inject(method = {"tick", "update"}, at = @At("HEAD"), cancellable = true, require = 0)
	private void hsn$throttleMaps(CallbackInfo ci) {
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.modEnabled) {
			return;
		}
		if (!cfg.mapRendererThrottleEnabled) {
			return;
		}
		long t = CameraSnapshot.gameTime();
		int interval = Math.max(1, cfg.mapRendererInterval);
		if ((t % interval) != 0L) {
			ci.cancel();
		}
	}
}
