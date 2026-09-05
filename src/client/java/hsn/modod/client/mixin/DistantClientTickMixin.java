package hsn.modod.client.mixin;

import hsn.modod.client.optimize.CameraSnapshot;
import hsn.modod.client.optimize.HighEndCounters;
import hsn.modod.optimize.HotPath;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class DistantClientTickMixin {

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 0)
	private void hsn$skipDistantClientTick(CallbackInfo ci) {
		if (!HotPath.flag(HotPath.CLIENT_TICK_SKIP)) {
			return;
		}
		Entity self = (Entity) (Object) this;
		if (!self.level().isClientSide()) {
			return;
		}
		if (!(self instanceof ItemEntity
				|| self instanceof ExperienceOrb
				|| self instanceof ArmorStand
				|| self instanceof ItemFrame
				|| self instanceof Painting
				|| self instanceof Display
				|| self instanceof FireworkRocketEntity)) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc != null && (self == mc.player || self == mc.getCameraEntity())) {
			return;
		}
		if (!CameraSnapshot.valid()) {
			return;
		}
		double distSq = CameraSnapshot.distSq(self.getX(), self.getY(), self.getZ());
		if (distSq <= HotPath.clientTickDistSq()) {
			return;
		}
		int interval = HotPath.clientTickInterval();
		if ((self.tickCount + self.getId()) % interval != 0) {
			self.tickCount++;
			HighEndCounters.tickSkip();
			ci.cancel();
		}
	}
}
