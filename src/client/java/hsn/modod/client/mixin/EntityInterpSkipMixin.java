package hsn.modod.client.mixin;

import hsn.modod.client.optimize.CameraSnapshot;
import hsn.modod.client.optimize.HighEndCounters;
import hsn.modod.optimize.HotPath;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Far entities do not need 20 Hz lerpTo / setOldPosAndRot work on a 500 FPS
 * client. Combat and the camera entity stay on the vanilla path.
 */
@Mixin(Entity.class)
public class EntityInterpSkipMixin {

	@Inject(method = {"lerpTo", "setOldPosAndRot", "updateOldPosition"}, at = @At("HEAD"), cancellable = true, require = 0)
	private void hsn$skipFarInterp(CallbackInfo ci) {
		if (!HotPath.flag(HotPath.INTERP_SKIP)) {
			return;
		}
		Entity self = (Entity) (Object) this;
		if (!self.level().isClientSide()) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc != null && (self == mc.player || self == mc.getCameraEntity() || self.isPassenger() || self.isVehicle())) {
			return;
		}
		if (self instanceof Player) {
			return;
		}
		if (self instanceof LivingEntity living && living.hurtTime > 0) {
			return;
		}
		double distSq = CameraSnapshot.valid()
				? CameraSnapshot.distSq(self.getX(), self.getY(), self.getZ())
				: 0.0;
		if (distSq > HotPath.interpDistSq()) {
			HighEndCounters.interpSkip();
			ci.cancel();
		}
	}
}
