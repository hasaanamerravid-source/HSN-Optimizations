package hsn.optimizations.client.mixin;

import hsn.optimizations.client.optimize.CameraSnapshot;
import hsn.optimizations.client.optimize.HighEndCounters;
import hsn.optimizations.optimize.HotPath;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla {@code ClientLevel.animateTick} walks a cube around the player every
 * client tick and rolls random block ambience (torch flame, drip, spores,
 * furnace crackle, portal wisps). That loop is one of the fattest client
 * CPU costs after entity rendering.
 * <p>
 * Interval skip + range cap. Combat, block updates, and redstone are untouched.
 */
@Mixin(targets = "net.minecraft.client.multiplayer.ClientLevel", priority = 910)
public class ClientAmbientTickMixin {

	@Inject(method = {"animateTick", "doAnimateTick"}, at = @At("HEAD"), cancellable = true, require = 0)
	private void hsn$skipAmbientTick(CallbackInfo ci) {
		if (!HotPath.flag(HotPath.AMBIENT_TICK)) {
			return;
		}
		int interval = HotPath.ambientInterval();
		if (HotPath.flag(HotPath.PERF_MODE) && interval < 3) {
			interval = 3;
		}
		if (interval <= 1) {
			return;
		}
		long time = CameraSnapshot.valid() ? CameraSnapshot.gameTime() : 0L;
		if (Math.floorMod(time, interval) != 0) {
			HighEndCounters.ambientSkip();
			ci.cancel();
		}
	}

	@ModifyVariable(
			method = {"doAnimateTick"},
			at = @At("HEAD"),
			argsOnly = true,
			ordinal = 3,
			require = 0
	)
	private int hsn$capAmbientRange(int range) {
		if (!HotPath.flag(HotPath.AMBIENT_TICK)) {
			return range;
		}
		int cap = HotPath.ambientRange();
		if (HotPath.flag(HotPath.PERF_MODE) && cap > 8) {
			cap = 8;
		}
		if (cap > 0 && range > cap) {
			return cap;
		}
		return range;
	}
}
