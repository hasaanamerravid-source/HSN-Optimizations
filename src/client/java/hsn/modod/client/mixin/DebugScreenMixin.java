package hsn.modod.client.mixin;

import hsn.modod.client.optimize.AdaptiveCuller;
import hsn.modod.client.optimize.CullStats;
import hsn.modod.config.HSNConfig;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Adds a short HSN status block to the F3 overlay.
 */
@Mixin(DebugScreenOverlay.class)
public class DebugScreenMixin {

	@Unique
	private static void hsnAppend(List<String> list) {
		if (list == null) return;
		try {
			HSNConfig cfg = HSNConfig.get();
			if (!cfg.f3ShowStatus) {
				return;
			}
			for (String s : list) {
				if (s != null && s.startsWith("HSN Optimizations")) return;
			}
			list.add("");
			list.add("HSN Optimizations " + cfg.modVersionLabel);
			list.add("  Preset: " + cfg.lastAppliedPreset);
			list.add("  Cull: particles=" + onOff(cfg.particleCullingEnabled)
					+ " entities=" + onOff(cfg.entityCullingEnabled)
					+ " shadows=" + onOff(cfg.shadowCullingEnabled));
			list.add("  Max dist: entity=" + (int) cfg.maxEntityRenderDistance
					+ " item=" + (int) cfg.maxItemEntityRenderDistance
					+ " particle=" + (int) cfg.maxParticleDistance);
			list.add("  Live cull/s: p-" + CullStats.particlesPerSec()
					+ " e-" + CullStats.entitiesPerSec());
			list.add("  Adaptive: " + Math.round(AdaptiveCuller.getScale() * 100) + "% @ "
					+ Math.round(AdaptiveCuller.getSmoothedFps()) + " fps"
					+ (cfg.performanceModeEnabled ? " [PERF MODE]" : ""));
			list.add("  Keys: F6=Perf Mode  F7=FPS overlay  F8=ULTRA_LOW  F9=SAFE");
		} catch (Throwable ignored) {
		}
	}

	@Unique
	private static String onOff(boolean b) {
		return b ? "ON" : "off";
	}

	@Inject(method = {"getGameInformation", "getLeft"}, at = @At("RETURN"), require = 0)
	private void hsnF3Left(CallbackInfoReturnable<List<String>> cir) {
		hsnAppend(cir.getReturnValue());
	}
}
