package hsn.modod.client.optimize;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;

/**
 * Ranks living entities so adaptive culling trims low-value mobs first
 * and keeps threats/animals visible for as long as possible.
 */
public final class EntityPriority {

	private EntityPriority() {
	}

	/**
	 * Multiplier stacked on top of the living-entity draw distance
	 * (1.0 = unaffected, lower = culled harder).
	 * <p>
	 * Items, XP orbs, armor stands and item frames return 1.0 because they
	 * already have dedicated distance sliders. Applying 0.55 / 0.60 here
	 * used to squash those sliders (20 blocks became ~11).
	 */
	public static double weight(Entity entity) {
		if (entity instanceof ItemEntity || entity instanceof ExperienceOrb
				|| entity instanceof ArmorStand || entity instanceof ItemFrame) {
			return 1.0;
		}
		if (entity instanceof Enemy) {
			return 1.0; // hostile mobs stay visible longest
		}
		if (entity instanceof Mob) {
			return 0.85; // passive/neutral mobs
		}
		return 0.9;
	}
}
