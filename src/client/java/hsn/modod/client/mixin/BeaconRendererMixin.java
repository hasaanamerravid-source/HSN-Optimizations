package hsn.modod.client.mixin;

import hsn.modod.client.optimize.AdaptiveCuller;
import hsn.modod.client.optimize.CullStats;
import hsn.modod.config.HSNConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Distance-culls beacon beams on Minecraft 26.2.
 * Target: net.minecraft.client.renderer.blockentity.BeaconRenderer
 * Uses shouldRender / extractRenderState / getViewDistance when available.
 */
@Mixin(targets = {
		"net.minecraft.client.renderer.blockentity.BeaconRenderer"
}, priority = 900)
public class BeaconRendererMixin {

	@Inject(method = {
			"shouldRender"
	}, at = @At("HEAD"), cancellable = true, require = 0)
	private void hsn$cullBeaconShouldRender(BlockEntity be, Vec3 cameraPos, CallbackInfoReturnable<Boolean> cir) {
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.modEnabled) {
			return;
		}
		if (!cfg.beaconBeamCullingEnabled || be == null) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null) return;

		BlockPos pos = be.getBlockPos();
		double dx = pos.getX() + 0.5 - player.getX();
		double dy = pos.getY() + 0.5 - player.getY();
		double dz = pos.getZ() + 0.5 - player.getZ();
		double distSq = dx * dx + dy * dy + dz * dz;
		double limit = cfg.maxBeaconBeamDistance * AdaptiveCuller.getScale();
		if (limit < 12.0) limit = 12.0;
		if (distSq > limit * limit
				|| hsn.modod.client.optimize.FrustumCull.outsideSphere(
						pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8.0)) {
			CullStats.entitySkip();
			cir.setReturnValue(false);
		}
	}

	@Inject(method = {
			"getViewDistance"
	}, at = @At("HEAD"), cancellable = true, require = 0)
	private void hsn$cullBeaconViewDistance(CallbackInfoReturnable<Integer> cir) {
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.modEnabled) {
			return;
		}
		if (!cfg.beaconBeamCullingEnabled) return;
		int scaled = (int) (cfg.maxBeaconBeamDistance * AdaptiveCuller.getScale());
		if (scaled < 12) scaled = 12;
		cir.setReturnValue(scaled);
	}

}
