package hsn.modod.client.mixin;

import hsn.modod.client.compat.HSNModCompat;
import hsn.modod.client.compat.SodiumCompat;
import hsn.modod.client.optimize.AdaptiveCuller;
import hsn.modod.client.optimize.CullStats;
import hsn.modod.config.HSNConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Injects HSN status lines into the F3 debug overlay.
 * Minecraft 26.2 builds the text via private extractLines(GuiGraphicsExtractor, List, boolean).
 * Older getSystemInformation / getRightText / getGameInformation methods no longer exist.
 */
@Mixin(DebugScreenOverlay.class)
public abstract class DebugScreenMixin {

	@Shadow
	public abstract boolean showDebugScreen();

	@Unique
	private static boolean hsn$addedThisFrame = false;

	@Inject(method = "extractLines", at = @At("HEAD"), require = 1)
	private void hsn$resetExtract(GuiGraphicsExtractor extractor, List<String> lines, boolean alignLeft, CallbackInfo ci) {
		if (alignLeft) {
			hsn$addedThisFrame = false;
		}
	}

	@Inject(method = "extractLines", at = @At("RETURN"), require = 1)
	private void hsn$appendExtract(GuiGraphicsExtractor extractor, List<String> lines, boolean alignLeft, CallbackInfo ci) {
		// alignLeft == true is the left column; we append to the right column
		if (alignLeft) return;
		hsn$tryAppend(lines);
	}

	// Soft fallback for any residual older method names (never hard-require)
	@Inject(method = {"getSystemInformation", "getRightText", "getGameInformation"},
			at = @At("RETURN"), require = 0)
	private void hsn$appendList(CallbackInfoReturnable<List<String>> cir) {
		List<String> lines = cir.getReturnValue();
		if (lines != null) {
			hsn$tryAppend(lines);
		}
	}

	@Unique
	private void hsn$tryAppend(List<String> lines) {
		if (hsn$addedThisFrame || !showDebugScreen()) return;
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.f3ShowStatus) return;

		// Prevent stacking/flickering: extractLines (and fallbacks) can run
		// multiple times per frame — only append once.
		for (String existing : lines) {
			if (existing != null && existing.contains("HSN-Optimizations")) {
				hsn$addedThisFrame = true;
				return;
			}
		}

		hsn$addedThisFrame = true;

		lines.add("");
		String header = "§aHSN-Optimizations §f3.8.4";
		if (cfg.performanceModeEnabled) header += " §c[PERF]";
		if (AdaptiveCuller.isWeakGpuActive()) header += " §6[WEAK-GPU]";
		lines.add(header);

		lines.add(String.format(
				"§7Cull §f%dp §7/ §f%de §7| scale §f%d%% §7| FPS §f%.0f",
				CullStats.particlesPerSec(),
				CullStats.entitiesPerSec(),
				Math.round(AdaptiveCuller.getScale() * 100),
				AdaptiveCuller.getSmoothedFps()
		));

		StringBuilder active = new StringBuilder("§7Active: §f");
		boolean any = false;
		if (cfg.particleCullingEnabled) { active.append("particles "); any = true; }
		if (cfg.entityCullingEnabled) { active.append("entities "); any = true; }
		if (cfg.textureAnimThrottleEnabled) { active.append("tex-anim "); any = true; }
		if (cfg.particlePriorityEnabled) { active.append("prio-p "); any = true; }
		if (cfg.blockEntityLodEnabled) { active.append("be-lod "); any = true; }
		if (cfg.progressiveLodEnabled) { active.append("prog-lod "); any = true; }
		if (cfg.entityLodStagesEnabled) { active.append("ent-lod "); any = true; }
		if (cfg.particleQualityCurveEnabled) { active.append("p-curve "); any = true; }
		if (cfg.itemSpinThrottleEnabled) { active.append("item-spin "); any = true; }
		if (!any) active.append("§8(none)");
		lines.add(active.toString().trim());

		String compat = "§7Compat: §f";
		if (SodiumCompat.isPresent()) {
			compat += "Sodium ";
		} else {
			String extra = HSNModCompat.detectedModsSummary();
			if (extra != null && !extra.isEmpty() && !extra.equals("none")) {
				compat += extra;
			} else {
				compat += "§8vanilla";
			}
		}
		lines.add(compat);

		lines.add("§7Preset: §f" + cfg.lastAppliedPreset
				+ (cfg.adaptiveCullingEnabled ? " §7(adaptive on)" : " §7(adaptive off)"));
	}
}
