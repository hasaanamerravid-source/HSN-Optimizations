package hsn.modod.client.mixin;

import hsn.modod.config.HSNConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Distance limit for block entity rendering (chests, signs, etc.).
 */
@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin {

	@Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
	private void hsnBeDistance(BlockEntity blockEntity, CallbackInfo ci) {
		if (tooFar(blockEntity)) {
			ci.cancel();
		}
	}

	@Unique
	private static boolean tooFar(BlockEntity be) {
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.blockEntityCullingEnabled || be == null) {
			return false;
		}
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return false;
		}
		double dx = be.getBlockPos().getX() + 0.5 - player.getX();
		double dy = be.getBlockPos().getY() + 0.5 - player.getY();
		double dz = be.getBlockPos().getZ() + 0.5 - player.getZ();
		double max = cfg.maxBlockEntityRenderDistance;
		return dx * dx + dy * dy + dz * dz > max * max;
	}
}
