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

@Mixin(SoundEngine.class)
public class SoundEngineMixin {

	@Unique
	private static int soundsThisTick = 0;
	@Unique
	private static long soundTick = Long.MIN_VALUE;

	@Inject(method = "play", at = @At("HEAD"), cancellable = true)
	private void hsn$soundCull(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.soundDistanceCullingEnabled && !cfg.weatherSoundReductionEnabled && !cfg.soundBurstLimitEnabled) {
			return;
		}
		if (sound == null) {
			return;
		}

		if (sound.isRelative()) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		long gt = mc.level != null ? mc.level.getGameTime() : 0L;
		if (gt != soundTick) {
			soundsThisTick = 0;
			soundTick = gt;
		}

		if (cfg.soundDistanceCullingEnabled) {
			LocalPlayer player = mc.player;
			if (player != null) {
				double dx = sound.getX() - player.getX();
				double dy = sound.getY() - player.getY();
				double dz = sound.getZ() - player.getZ();
				double max = cfg.maxSoundDistance;
				if (dx * dx + dy * dy + dz * dz > max * max) {
					cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
					return;
				}
			}
		}

		if (cfg.soundBurstLimitEnabled) {
			if (++soundsThisTick > Math.max(1, cfg.maxNewSoundsPerTick)) {
				cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
				return;
			}
		}

		if (cfg.weatherSoundReductionEnabled && cfg.weatherSoundKeepChance < 1.0) {
			String path = hsn$pathOf(sound);
			if (path.indexOf("rain") >= 0 || path.indexOf("thunder") >= 0 || path.indexOf("weather") >= 0) {
				// Deterministic keep so loops do not flicker every frame.
				int key = path.hashCode() ^ (int) gt;
				double keep = cfg.weatherSoundKeepChance;
				if ((key & 0xFFFF) / 65536.0 > keep) {
					cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
				}
			}
		}
	}

	@Unique
	private static String hsn$pathOf(SoundInstance sound) {
		Identifier id;
		try {
			id = sound.getIdentifier();
		} catch (Throwable ignored) {
			try {
				id = ((SoundInstanceAccess) sound).hsn$getIdentifier();
			} catch (Throwable ignoredAgain) {
				return "";
			}
		}
		return id != null ? id.getPath() : "";
	}
}
