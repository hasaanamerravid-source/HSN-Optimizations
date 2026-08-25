package hsn.modod.client.mixin;

import hsn.modod.config.HSNConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Distance + weather + burst sound culling. Returns PlayResult.NOT_STARTED so
 * the engine treats the sound as skipped instead of a null crash.
 */
@Mixin(SoundEngine.class)
public class SoundEngineMixin {

	@Unique
	private static int soundsThisWindow = 0;
	@Unique
	private static long soundWindowStartMs = 0L;

	@Inject(method = "play", at = @At("HEAD"), cancellable = true)
	private void hsn$soundCull(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.soundDistanceCullingEnabled && !cfg.weatherSoundReductionEnabled && !cfg.soundBurstLimitEnabled) {
			return;
		}
		if (sound == null) {
			return;
		}

		if (cfg.soundBurstLimitEnabled) {
			long now = System.currentTimeMillis();
			if (now - soundWindowStartMs > 50L) {
				soundsThisWindow = 0;
				soundWindowStartMs = now;
			}
			if (++soundsThisWindow > Math.max(1, cfg.maxNewSoundsPerTick)) {
				cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
				return;
			}
		}

		String path = hsn$pathOf(sound);
		if (path.contains("ui") || path.contains("music") || path.contains("menu")
				|| path.contains("click") || path.contains("note")) {
			return;
		}

		boolean weather = path.contains("rain") || path.contains("thunder") || path.contains("weather");
		if (weather && cfg.weatherSoundReductionEnabled && Math.random() > cfg.weatherSoundKeepChance) {
			cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
			return;
		}

		if (!cfg.soundDistanceCullingEnabled) {
			return;
		}

		if (sound.isRelative()) {
			return;
		}

		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}

		double dx = sound.getX() - player.getX();
		double dy = sound.getY() - player.getY();
		double dz = sound.getZ() - player.getZ();
		double max = cfg.maxSoundDistance;
		if (dx * dx + dy * dy + dz * dz > max * max) {
			cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
		}
	}

	@Unique
	private static String hsn$pathOf(SoundInstance sound) {
		try {
			// Try getId() first (newer versions), fallback to getLocation()
			Identifier id = null;
			try {
				id = (Identifier) sound.getClass().getMethod("getId").invoke(sound);
			} catch (NoSuchMethodException e) {
				id = (Identifier) sound.getClass().getMethod("getLocation").invoke(sound);
			}
			return id != null ? id.getPath().toLowerCase() : "";
		} catch (Throwable t) {
			return "";
		}
	}
}
