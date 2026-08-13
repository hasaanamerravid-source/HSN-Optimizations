package hsn.modod.client.mixin;

import hsn.modod.config.HSNConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cull name tags beyond a distance limit.
 */
@Mixin(EntityRenderer.class)
public class NameTagMixin {

	@Inject(method = {"renderNameTag", "renderLabelIfPresent", "renderName"},
			at = @At("HEAD"), cancellable = true, require = 0)
	private void hsn$nameTagCull(Entity entity, CallbackInfo ci) {
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.nameTagCullEnabled || entity == null) {
			return;
		}
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || entity == player) {
			return;
		}
		double dx = entity.getX() - player.getX();
		double dy = entity.getY() - player.getY();
		double dz = entity.getZ() - player.getZ();
		double limit = cfg.maxNameTagDistance;
		if (dx * dx + dy * dy + dz * dz > limit * limit) {
			ci.cancel();
		}
	}
}
