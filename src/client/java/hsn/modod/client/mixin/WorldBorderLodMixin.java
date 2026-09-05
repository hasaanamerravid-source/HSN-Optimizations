package hsn.modod.client.mixin;

import hsn.modod.client.optimize.CameraSnapshot;
import hsn.modod.config.HSNConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = {
		"net.minecraft.client.renderer.WorldBorderRenderer"
}, priority = 900)
public class WorldBorderLodMixin {

	@Inject(method = {"render", "renderWorldBorder"}, at = @At("HEAD"), cancellable = true, require = 0)
	private void hsn$skipFarBorder(CallbackInfo ci) {
		if (!hsn.modod.optimize.HotPath.masterOn()) {
			return;
		}
		if (!HSNConfig.get().worldBorderLodEnabled) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc == null || mc.level == null || !CameraSnapshot.valid()) {
			return;
		}
		WorldBorder border = mc.level.getWorldBorder();
		double half = border.getSize() * 0.5;
		double dx = Math.abs(CameraSnapshot.x() - border.getCenterX());
		double dz = Math.abs(CameraSnapshot.z() - border.getCenterZ());
		double dist = Math.min(half - dx, half - dz);
		if (dist > 96.0) {
			ci.cancel();
		}
	}
}
