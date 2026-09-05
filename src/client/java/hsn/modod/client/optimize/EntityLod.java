package hsn.modod.client.optimize;

import hsn.modod.config.HSNConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Decides when a visible entity may use a cheaper client presentation.
 * Never applies to the local player, passengers/vehicles, or hurt mobs.
 */
public final class EntityLod {

	private EntityLod() {
	}

	public static boolean shouldSimplify(Entity entity, double distSq) {
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.entityLodStagesEnabled || entity == null) {
			return false;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc != null && (entity == mc.player || entity == mc.getCameraEntity())) {
			return false;
		}
		if (entity instanceof Player) {
			return false;
		}
		if (entity.isPassenger() || entity.isVehicle()) {
			return false;
		}
		if (entity instanceof LivingEntity living) {
			if (living.hurtTime > 0 || living.deathTime > 0) {
				return false;
			}
		}
		double start = cfg.maxEntityRenderDistance * cfg.progressiveLodStart * AdaptiveCuller.getScale();
		return distSq > start * start;
	}
}
