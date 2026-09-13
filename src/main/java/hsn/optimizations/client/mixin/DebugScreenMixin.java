package hsn.optimizations.client.mixin;

import hsn.optimizations.client.compat.HSNModCompat;
import hsn.optimizations.client.compat.SodiumCompat;
import hsn.optimizations.client.compat.VoxelSniperCompat;
import hsn.optimizations.client.optimize.AdaptiveCuller;
import hsn.optimizations.client.optimize.CullStats;
import hsn.optimizations.client.optimize.HighEndCounters;
import hsn.optimizations.client.optimize.HorizonYCull;
import hsn.optimizations.client.optimize.RenderScale;
import hsn.optimizations.config.HSNConfig;
import hsn.optimizations.optimize.HotPath;
import hsn.optimizations.optimize.NativeBridge;
import hsn.optimizations.optimize.PathfindingStats;
import hsn.optimizations.optimize.ThrottleStats;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(DebugScreenOverlay.class)
public abstract class DebugScreenMixin {

	@Unique
	private static final String MARK = "\u00a76HSN\u00a7r";
	@Unique
	private static final StringBuilder LINE = new StringBuilder(160);

	@Shadow
	public abstract boolean showDebugScreen();

	// 26.2 extractLines / drawLines / collectLines are all void
	// (GuiGraphicsExtractor, List, boolean). CIR here crashes apply.
	@Inject(method = {"extractLines", "drawLines", "collectLines"}, at = @At("HEAD"), require = 0)
	private void hsn$appendStatus(GuiGraphicsExtractor extractor, List<String> lines, boolean isLeftColumn,
			CallbackInfo ci) {
		try {
			hsn$fill(lines, isLeftColumn);
		} catch (Throwable ignored) {
		}
	}

	@Unique
	private void hsn$fill(List<String> lines, boolean isLeftColumn) {
		if (isLeftColumn || !showDebugScreen() || !HSNConfig.get().f3ShowStatus) {
			return;
		}
		if (lines == null) {
			return;
		}
		for (int i = 0, n = lines.size(); i < n; i++) {
			String line = lines.get(i);
			if (line != null && (line.indexOf("[HSN]") >= 0 || line.startsWith("\u00a76HSN"))) {
				return;
			}
		}

		CullStats.tick();
		HSNConfig cfg = HSNConfig.get();
		boolean deferred = cfg.deferToDedicatedEntityCullingMods && HSNModCompat.entityCullingModPresent();
		int fps = AdaptiveCuller.getInstantFps();
		int avg = (int) Math.round(AdaptiveCuller.getSmoothedFps());
		int scalePct = (int) Math.round(AdaptiveCuller.getScale() * 100.0);
		String fpsColor = fpsColor(fps, cfg.targetFps);

		lines.add("");

		LINE.setLength(0);
		LINE.append(MARK).append(' ').append("\u00a7e").append(HSNConfig.modVersionLabel).append("\u00a7r");
		LINE.append("  ").append(fpsColor).append(fps).append("\u00a77/\u00a7b").append(cfg.targetFps)
				.append("\u00a77 fps  avg \u00a7b").append(avg).append("\u00a7r");
		LINE.append("  \u00a77cull \u00a7b").append(scalePct).append("%\u00a7r");
		LINE.append("  \u00a77rs \u00a7b").append(RenderScale.statusLine()).append("\u00a7r");
		if (cfg.performanceModeEnabled) {
			LINE.append(" \u00a7cPERF\u00a7r");
		}
		if (AdaptiveCuller.isWeakGpuActive()) {
			LINE.append(" \u00a76WEAK\u00a7r");
		}
		if (!cfg.modEnabled) {
			LINE.append(" \u00a78OFF\u00a7r");
		}
		lines.add(LINE.toString());

		LINE.setLength(0);
		LINE.append(MARK).append(" \u00a77drop\u00a7r \u00a7dP").append(CullStats.particlesPerSec())
				.append("\u00a7r \u00a7aE");
		if (deferred) {
			LINE.append("def");
		} else {
			LINE.append(CullStats.entitiesPerSec());
		}
		LINE.append("\u00a7r \u00a7dS").append(CullStats.sectionsPerSec())
				.append("\u00a7r \u00a7bpath ").append(PathfindingStats.skippedPerSec())
				.append("\u00a7r \u00a73item ").append(ThrottleStats.skippedPerSec()).append("\u00a7r");
		if (cfg.horizonYCullEnabled) {
			LINE.append("  \u00a77Y\u00a7b");
			if (HorizonYCull.active()) {
				LINE.append((int) Math.floor(HorizonYCull.minVisibleY()));
			} else {
				LINE.append(VoxelSniperCompat.pauseHorizon() ? "pause" : "off");
			}
			LINE.append("\u00a7r");
		}
		LINE.append("  \u00a77VS \u00a7b").append(VoxelSniperCompat.status()).append("\u00a7r");
		lines.add(LINE.toString());

		if (cfg.f3Compact && !cfg.f3ShowDetails) {
			return;
		}

		LINE.setLength(0);
		LINE.append(MARK).append(" \u00a77").append(cfg.lastAppliedPreset)
				.append("  simd \u00a7b").append(NativeBridge.activeLabel())
				.append("\u00a7r \u00a77mask \u00a7b")
				.append(cfg.circularRenderingEnabled ? String.valueOf(cfg.worldRenderShape) : "square")
				.append("\u00a7r");
		if (SodiumCompat.isPresent()) {
			LINE.append("  \u00a7aSodium\u00a7r");
		}
		lines.add(LINE.toString());

		if (!cfg.f3ShowDetails) {
			return;
		}

		LINE.setLength(0);
		LINE.append(MARK).append(" \u00a77dist\u00a7r e\u00a7a").append((int) Math.round(HotPath.entityDist()))
				.append("\u00a7r p\u00a7a").append((int) Math.round(HotPath.particleDist()))
				.append("\u00a7r cap\u00a7b").append(cfg.maxParticles)
				.append("\u00a7r  L").append(HighEndCounters.lightPerSec())
				.append(" i").append(HighEndCounters.interpPerSec())
				.append(" t").append(HighEndCounters.tickPerSec())
				.append(" a").append(HighEndCounters.animPerSec());
		lines.add(LINE.toString());

		LINE.setLength(0);
		LINE.append(MARK).append(" \u00a77on\u00a7r")
				.append(bit("E", cfg.entityCullingEnabled))
				.append(bit("P", cfg.particleCullingEnabled))
				.append(bit("H", cfg.horizonYCullEnabled))
				.append(bit("M", cfg.circularRenderingEnabled))
				.append(bit("R", cfg.renderScaleEnabled))
				.append(bit("A", cfg.adaptiveCullingEnabled))
				.append(bit("N", cfg.nativeHotpathEnabled))
				.append(bit("VS", cfg.voxelSniperCompatEnabled));
		String compat = HSNModCompat.detectedModsSummary();
		if (compat != null && !compat.isEmpty() && !"none".equals(compat)) {
			LINE.append("  \u00a78").append(compat).append("\u00a7r");
		}
		lines.add(LINE.toString());
	}

	@Unique
	private static String bit(String name, boolean on) {
		return on ? " \u00a7a" + name + "\u00a7r" : " \u00a78" + name + "\u00a7r";
	}

	@Unique
	private static String fpsColor(int fps, int target) {
		if (fps >= target) {
			return "\u00a7a";
		}
		if (fps >= target * 0.7) {
			return "\u00a7e";
		}
		return "\u00a7c";
	}
}
