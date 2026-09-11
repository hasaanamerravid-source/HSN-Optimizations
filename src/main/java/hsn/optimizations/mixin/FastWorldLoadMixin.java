package hsn.optimizations.mixin;

import hsn.optimizations.config.HSNConfig;
import hsn.optimizations.optimize.HSNWorldOpen;
import hsn.optimizations.optimize.ServerFeatures;
import net.minecraft.server.level.ServerChunkCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerChunkCache.class)
public abstract class FastWorldLoadMixin {

	@Shadow
	public abstract boolean pollTask();

	@Inject(method = "tick", at = @At("RETURN"), require = 0)
	private void hsn$drainExtraChunkTasks(CallbackInfo ci) {
		HSNConfig cfg = HSNConfig.get();
		if (cfg == null || !cfg.modEnabled || !cfg.fastWorldLoadEnabled || !HSNWorldOpen.enabled()) {
			return;
		}
		if (!ServerFeatures.allowSimulationExtras() || !ServerFeatures.worldLoadWindowOpen()) {
			return;
		}
		int maxExtraTasks = Math.max(1, Math.min(cfg.fastWorldLoadChunkBoost, 128));
		long deadline = System.nanoTime() + HSNWorldOpen.drainBudgetNanos();
		for (int i = 0; i < maxExtraTasks; i++) {
			if (System.nanoTime() >= deadline) {
				break;
			}
			if (!this.pollTask()) {
				break;
			}
		}
	}
}
