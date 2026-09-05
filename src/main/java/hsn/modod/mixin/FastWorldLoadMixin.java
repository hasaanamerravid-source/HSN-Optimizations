package hsn.modod.mixin;

import hsn.modod.config.HSNConfig;
import hsn.modod.optimize.ServerFeatures;
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

    @Inject(
        method = "tick", 
        at = @At("RETURN"), 
        require = 0
    )
    private void hsn$drainExtraChunkTasks(CallbackInfo ci) {
        HSNConfig cfg = HSNConfig.get();
		if (!cfg.modEnabled) {
			return;
		}
        if (cfg == null || !cfg.fastWorldLoadEnabled) {
            return;
        }

        if (!ServerFeatures.allowSimulationExtras() || !ServerFeatures.worldLoadWindowOpen()) {
            return;
        }

        int maxExtraTasks = Math.max(1, Math.min(cfg.fastWorldLoadChunkBoost, 128));
        
        // Time-bounded execution budget (max 3ms) to prevent server thread starvation
        long deadline = System.nanoTime() + 3_000_000L;

        for (int i = 0; i < maxExtraTasks; i++) {
            if (System.nanoTime() >= deadline) {
                break;
            }
            // pollTask returns false when the chunk task queue is empty
            if (!this.pollTask()) {
                break;
            }
        }
    }
}
