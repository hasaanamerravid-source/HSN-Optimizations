package hsn.modod.client.mixin;

import hsn.modod.client.optimize.CameraSnapshot;
import hsn.modod.config.HSNConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Throttles in-hand/item-frame map texture updates.
 * <p>
 * Targets the {@code net.minecraft.client.gui.MapRenderer} naming. As with
 * the light texture mixins, {@link HSNMixinPlugin} only registers this
 * mixin once it has confirmed the class exists, and falls back to the
 * sibling {@code MapRendererRendererMixin} for the
 * {@code net.minecraft.client.renderer.MapRenderer} naming used by other
 * versions/mapping sets.
 */
@Mixin(targets = "net.minecraft.client.gui.MapRenderer", priority = 900)
public class MapRendererThrottleMixin {

	@Inject(method = {"tick", "update", "render"}, at = @At("HEAD"), cancellable = true, require = 0)
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
