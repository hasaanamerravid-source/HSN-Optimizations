package hsn.optimizations.client.mixin;

import hsn.optimizations.config.HSNConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

/**
 * Skip boss-bar layout when no events are active.
 * Targets both official and Yarn names; fails open if the event map is missing.
 */
@Mixin(targets = {
		"net.minecraft.client.gui.components.BossHealthOverlay",
		"net.minecraft.client.gui.hud.BossBarHud"
}, remap = true)
public class BossOverlayMixin {

	@Inject(method = {"render", "renderOverlay"}, at = @At("HEAD"), cancellable = true, require = 0)
	private void hsn$skipEmpty(CallbackInfo ci) {
		HSNConfig cfg = HSNConfig.get();
		if (cfg == null || !cfg.modEnabled || !cfg.skipEmptyBossOverlayEnabled) {
			return;
		}
		try {
			for (Field field : this.getClass().getDeclaredFields()) {
				Class<?> type = field.getType();
				if (!Map.class.isAssignableFrom(type) && !Collection.class.isAssignableFrom(type)) {
					continue;
				}
				field.setAccessible(true);
				Object value = field.get(this);
				if (value instanceof Map<?, ?> map && map.isEmpty()) {
					ci.cancel();
					return;
				}
				if (value instanceof Collection<?> col && col.isEmpty()) {
					ci.cancel();
					return;
				}
			}
		} catch (Throwable ignored) {
		}
	}
}
