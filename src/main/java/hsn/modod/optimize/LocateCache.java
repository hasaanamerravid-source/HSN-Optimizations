package hsn.modod.optimize;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.LinkedHashMap;
import java.util.Map;

public final class LocateCache {

	private static final int CAP = 48;

	private static final Map<Key, Hit> CACHE = new LinkedHashMap<>(CAP, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<Key, Hit> eldest) {
			return size() > CAP;
		}
	};

	private LocateCache() {
	}

	public static synchronized Hit get(ResourceKey<Level> dim, HolderSet<Structure> structures,
									   BlockPos origin, int radius) {
		Hit hit = CACHE.get(new Key(dim, keyOf(structures), cell(origin), radius));
		if (hit == null) {
			return null;
		}
		if (System.currentTimeMillis() > hit.expiresAt) {
			CACHE.remove(new Key(dim, keyOf(structures), cell(origin), radius));
			return null;
		}
		return hit;
	}

	public static synchronized void put(ResourceKey<Level> dim, HolderSet<Structure> structures,
										BlockPos origin, int radius, BlockPos found, Holder<Structure> type, long ttlMs) {
		if (dim == null || origin == null || found == null) {
			return;
		}
		CACHE.put(
				new Key(dim, keyOf(structures), cell(origin), radius),
				new Hit(found, type, System.currentTimeMillis() + Math.max(1_000L, ttlMs)));
	}

	public static synchronized void clear() {
		CACHE.clear();
	}


	public static boolean structuresEnabled(MinecraftServer server) {
		// 26.2 WorldData has no worldGenOptions(). Vanilla locate already
		// returns null when structures are disabled.
		return server != null;
	}

	public static String keyOf(HolderSet<Structure> structures) {
		if (structures == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder(32);
		try {
			for (Holder<Structure> holder : structures) {
				sb.append(holder.unwrapKey().map(Object::toString).orElse("?")).append(',');
			}
		} catch (Throwable t) {
			return Integer.toHexString(System.identityHashCode(structures));
		}
		return sb.toString();
	}

	private static BlockPos cell(BlockPos pos) {
		return new BlockPos(pos.getX() >> 7, 0, pos.getZ() >> 7);
	}

	public record Key(ResourceKey<Level> dim, String structures, BlockPos cell, int radius) {
	}

	public record Hit(BlockPos pos, Holder<Structure> structure, long expiresAt) {
	}
}
