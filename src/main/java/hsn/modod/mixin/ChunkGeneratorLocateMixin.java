package hsn.modod.mixin;

import com.mojang.datafixers.util.Pair;
import hsn.modod.config.HSNConfig;
import hsn.modod.optimize.LocateCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorLocateMixin {

	@Inject(method = "findNearestMapStructure", at = @At("HEAD"), cancellable = true, require = 0)
	private void hsn$locateHead(ServerLevel level, HolderSet<Structure> structures, BlockPos origin,
								int radius, boolean findUnexplored,
								CallbackInfoReturnable<Pair<BlockPos, Holder<Structure>>> cir) {
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.modEnabled) {
			return;
		}
		if (!cfg.locateOptimizeEnabled || level == null) {
			return;
		}
		if (level.getServer() != null && !LocateCache.structuresEnabled(level.getServer())) {
			cir.setReturnValue(null);
			return;
		}
		if (findUnexplored) {
			return;
		}
		LocateCache.Hit hit = LocateCache.get(level.dimension(), structures, origin, radius);
		if (hit != null && hit.pos() != null && hit.structure() != null) {
			cir.setReturnValue(Pair.of(hit.pos(), hit.structure()));
		}
	}

	@Inject(method = "findNearestMapStructure", at = @At("RETURN"), require = 0)
	private void hsn$locateTail(ServerLevel level, HolderSet<Structure> structures, BlockPos origin,
								int radius, boolean findUnexplored,
								CallbackInfoReturnable<Pair<BlockPos, Holder<Structure>>> cir) {
		HSNConfig cfg = HSNConfig.get();
		if (!cfg.modEnabled) {
			return;
		}
		if (!cfg.locateOptimizeEnabled || findUnexplored || level == null) {
			return;
		}
		Pair<BlockPos, Holder<Structure>> value = cir.getReturnValue();
		if (value == null || value.getFirst() == null) {
			return;
		}
		LocateCache.put(level.dimension(), structures, origin, radius,
				value.getFirst(), value.getSecond(), cfg.locateCacheTtlSeconds * 1000L);
	}
}
