package hsn.modod.client.mixin;

import hsn.modod.config.HSNConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Distance + weather sound reduction for low-end systems.
 */
@Mixin(SoundEngine.class)
public class SoundEngineMixin {

	@Inject(method = "play", at = @At("HEAD"), cancellable = true, require = 0)
	private void hsn$soundCull(SoundInstance sound, CallbackInfoReturnable<?> cir) {
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.soundDistanceCullingEnabled && !cfg.weatherSoundReductionEnabled) {
			return;
		}
		if (sound == null) {
			return;
		}

		String path = hsn$pathOf(sound);
		if (path.contains("ui") || path.contains("music") || path.contains("menu")
				|| path.contains("click") || path.contains("note")) {
			return;
		}

		boolean weather = path.contains("rain") || path.contains("thunder") || path.contains("weather");
		if (weather && cfg.weatherSoundReductionEnabled && Math.random() > cfg.weatherSoundKeepChance) {
			cir.setReturnValue(null);
			return;
		}

		if (!cfg.soundDistanceCullingEnabled) {
			return;
		}

		try {
			if (sound.isRelative()) {
				return;
			}
		} catch (Throwable ignored) {
		}

		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}

		double[] pos = hsn$posOf(sound);
		if (pos == null) {
			return;
		}

		double dx = pos[0] - player.getX();
		double dy = pos[1] - player.getY();
		double dz = pos[2] - player.getZ();
		double max = cfg.maxSoundDistance;
		if (dx * dx + dy * dy + dz * dz > max * max) {
			cir.setReturnValue(null);
		}
	}

	@Unique
	private static String hsn$pathOf(SoundInstance sound) {
		try {
			for (String m : new String[]{"getLocation", "getIdentifier", "location", "getSoundLocation", "getSoundEvent"}) {
				try {
					Object id = sound.getClass().getMethod(m).invoke(sound);
					if (id == null) continue;
					try {
						Object p = id.getClass().getMethod("getPath").invoke(id);
						if (p != null) return p.toString().toLowerCase();
					} catch (Throwable ignored) {
					}
					return id.toString().toLowerCase();
				} catch (Throwable ignored) {
				}
			}
		} catch (Throwable ignored) {
		}
		return "";
	}

	@Unique
	private static double[] hsn$posOf(SoundInstance sound) {
		try {
			return new double[]{sound.getX(), sound.getY(), sound.getZ()};
		} catch (Throwable t) {
			return null;
		}
	}
}
