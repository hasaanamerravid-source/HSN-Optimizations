package hsn.modod.client.mixin;

import hsn.modod.client.compat.HSNModCompat;
import hsn.modod.client.compat.SodiumCompat;
import hsn.modod.client.optimize.AdaptiveCuller;
import hsn.modod.client.optimize.CullStats;
import hsn.modod.config.HSNConfig;
import hsn.modod.optimize.NativeBridge;
import hsn.modod.optimize.PathfindingStats;
import hsn.modod.optimize.ThrottleStats;
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
	private static final String MARK = "\u00a76[HSN]\u00a7r";
	@Unique
	private static final StringBuilder LINE = new StringBuilder(96);

	@Shadow
	public abstract boolean showDebugScreen();

	@Inject(method = "extractLines", at = @At("HEAD"), require = 0)
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
			if (line != null && line.indexOf("[HSN]") >= 0) {
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
		if (cfg.performanceModeEnabled) {
			LINE.append(" \u00a7c[PERF]\u00a7r");
		}
		if (AdaptiveCuller.isWeakGpuActive()) {
			LINE.append(" \u00a76[WEAK-GPU]\u00a7r");
		}
		lines.add(LINE.toString());

		LINE.setLength(0);
		LINE.append(MARK).append(" \u00a77FPS \u00a7r").append(fpsColor).append(fps)
				.append("\u00a7r \u00a77target \u00a7b").append(cfg.targetFps)
				.append("\u00a7r \u00a77avg \u00a7b").append(avg)
				.append("\u00a7r \u00a77scale \u00a7b").append(scalePct).append("%\u00a7r");
		lines.add(LINE.toString());

		LINE.setLength(0);
		LINE.append(MARK).append(" \u00a77drop \u00a7r\u00a7dP ").append(CullStats.particlesPerSec())
				.append("/s\u00a7r \u00a7aE ");
		if (deferred) {
			LINE.append("deferred");
		} else {
			LINE.append(CullStats.entitiesPerSec()).append("/s");
		}
		LINE.append("\u00a7r \u00a7bpath ").append(PathfindingStats.skippedPerSec())
				.append("/s\u00a7r \u00a73item ").append(ThrottleStats.skippedPerSec()).append("/s\u00a7r");
		lines.add(LINE.toString());

		LINE.setLength(0);
		LINE.append(MARK).append(" \u00a77pacing \u00a7r")
				.append(cfg.framePacingFixEnabled ? "\u00a7aon" : "\u00a78off")
				.append("\u00a7r \u00a77simd \u00a7b").append(NativeBridge.activeLabel())
				.append("\u00a7r \u00a77want \u00a7e").append(cfg.simdMode)
				.append("\u00a7r \u00a77preset \u00a7e").append(cfg.lastAppliedPreset).append("\u00a7r");
		lines.add(LINE.toString());

		LINE.setLength(0);
		LINE.append(MARK).append(" \u00a77terrain \u00a7r\u00a7b")
				.append(cfg.circularRenderingEnabled ? String.valueOf(cfg.worldRenderShape) : "square")
				.append("\u00a7r \u00a77drop S \u00a7d").append(CullStats.sectionsPerSec()).append("/s\u00a7r");
		lines.add(LINE.toString());

		String compat = SodiumCompat.isPresent() ? "Sodium" : HSNModCompat.detectedModsSummary();
		if (compat == null || compat.isEmpty()) {
			compat = "vanilla";
		}

		if (!cfg.f3ShowDetails) {
			LINE.setLength(0);
			LINE.append(MARK).append(" \u00a77").append(compat).append("\u00a7r");
			lines.add(LINE.toString());
			return;
		}

		LINE.setLength(0);
		LINE.append(MARK).append(" \u00a77").append(compat)
				.append(" \u00a77dist \u00a7b").append((int) cfg.maxEntityRenderDistance)
				.append(" \u00a77part \u00a7b").append((int) cfg.maxParticleDistance)
				.append(" \u00a77shad \u00a7b").append((int) cfg.maxShadowDistance)
				.append(" \u00a77cap \u00a7b").append(cfg.maxParticles).append("\u00a7r");
		lines.add(LINE.toString());

		LINE.setLength(0);
		LINE.append(MARK).append(" \u00a77flags\u00a7r")
				.append(" cull=").append(flag(cfg.entityCullingEnabled))
				.append(" part=").append(flag(cfg.particleCullingEnabled))
				.append(" lod=").append(flag(cfg.progressiveLodEnabled))
				.append(" tex=").append(flag(cfg.blockTextureLodEnabled))
				.append(" anim=").append(flag(cfg.textureAnimThrottleEnabled))
				.append(" path=").append(flag(cfg.pathfindingThrottleEnabled))
				.append("\u00a7r");
		lines.add(LINE.toString());
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

	@Unique
	private static String flag(boolean value) {
		return value ? "\u00a7aon" : "\u00a78off";
	}
}
