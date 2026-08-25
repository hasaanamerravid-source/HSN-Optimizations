package hsn.modod.client.mixin;

import hsn.modod.client.optimize.AdaptiveCuller;
import hsn.modod.client.optimize.DistanceLod;
import hsn.modod.config.HSNConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client-only animation relief for distant living entities.
 * Skips {@code aiStep} on remote entities (not the local player) so movement
 * packets still apply via {@code tick}. Never cancels the full entity tick.
 */
@Mixin(LivingEntity.class)
public abstract class EntityAnimMixin {

	@Unique
	private int hsn$animSkipCounter = 0;

	@Inject(method = "aiStep", at = @At("HEAD"), cancellable = true, require = 0)
	private void hsn$maybeSkipDistantAnim(CallbackInfo ci) {
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.entityCullingEnabled || !cfg.entityLodStagesEnabled) {
			return;
		}

		LivingEntity self = (LivingEntity) (Object) this;
		Minecraft mc = Minecraft.getInstance();
		if (mc == null || !mc.isSameThread()) {
			return;
		}
		LocalPlayer player = mc.player;
		if (player == null || self == player) {
			return;
		}

		double distSq = player.distanceToSqr(self);
		double maxDist = cfg.maxEntityRenderDistance * AdaptiveCuller.getScale();
		int stage = DistanceLod.entityStage(distSq, maxDist);
		if (stage <= 0) {
			return;
		}

		int interval = stage + 1;
		if (AdaptiveCuller.getScale() < 0.65) {
			interval += 1;
		}

		hsn$animSkipCounter++;
		if (hsn$animSkipCounter < interval) {
			ci.cancel();
		} else {
			hsn$animSkipCounter = 0;
		}
	}
}
