package hsn.modod.client.optimize;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;

/**
 * Ranks entities so adaptive culling trims low-value entities first
 * and keeps threats/animals visible for as long as possible.
 */
public final class EntityPriority {

	private EntityPriority() {
	}

	/** Multiplier stacked on top of the adaptive scale (1.0 = unaffected, lower = culled harder). */
	public static double weight(Entity entity) {
		if (entity instanceof Enemy) {
			return 1.0; // hostile mobs stay visible longest
		}
		if (entity instanceof Mob) {
			return 0.85; // passive/neutral mobs
		}
		if (entity instanceof ItemEntity || entity instanceof ExperienceOrb) {
			return 0.55; // loot, safe to drop first
		}
		if (entity instanceof ArmorStand || entity instanceof ItemFrame) {
			return 0.6;
		}
		return 0.9;
	}
}
