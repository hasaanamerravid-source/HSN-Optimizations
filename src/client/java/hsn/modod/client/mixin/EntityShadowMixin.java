package hsn.modod.client.mixin;

import hsn.modod.config.HSNConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skip entity shadows beyond a configurable distance.
 */
@Mixin(EntityRenderDispatcher.class)
public class EntityShadowMixin {

	@Inject(method = "renderShadow", at = @At("HEAD"), cancellable = true, require = 0)
	private void hsnCullShadow(Entity entity, float a, float b, CallbackInfo ci) {
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.shadowCullingEnabled || entity == null) {
			return;
		}
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || entity == player) {
			return;
		}
		double dx = entity.getX() - player.getX();
		double dy = entity.getY() - player.getY();
		double dz = entity.getZ() - player.getZ();
		double max = cfg.maxShadowDistance;
		if (dx * dx + dy * dy + dz * dz > max * max) {
			ci.cancel();
		}
	}
}
