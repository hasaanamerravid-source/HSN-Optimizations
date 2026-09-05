package hsn.modod.client.mixin;

import hsn.modod.client.optimize.CameraSnapshot;
import hsn.modod.client.optimize.HighEndCounters;
import hsn.modod.optimize.HotPath;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drops limb-swing / effect-particle / death-anim work for far living
 * entities. Does not cancel LivingEntity.tick — that would freeze poses.
 */
@Mixin(LivingEntity.class)
public class LivingAnimThrottleMixin {

	@Inject(method = {
			"updateWalkAnimation",
			"tickEffects",
			"tickHeadTurn",
			"tickDeath",
			"animateHurt"
	}, at = @At("HEAD"), cancellable = true, require = 0)
	private void hsn$skipFarLivingAnim(CallbackInfo ci) {
		if (!HotPath.flag(HotPath.LIVING_ANIM)) {
			return;
		}
		LivingEntity self = (LivingEntity) (Object) this;
		if (!self.level().isClientSide()) {
			return;
		}
		if (self instanceof Player) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc != null && (self == mc.player || self == mc.getCameraEntity())) {
			return;
		}
		if (self.hurtTime > 0) {
			return;
		}
		if (!CameraSnapshot.valid()) {
			return;
		}
		double distSq = CameraSnapshot.distSq(self.getX(), self.getY(), self.getZ());
		if (distSq > HotPath.livingAnimDistSq()) {
			HighEndCounters.animSkip();
			ci.cancel();
		}
	}
}
