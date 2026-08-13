package hsn.modod.optimize;

import hsn.modod.config.HSNConfig;
import java.util.HashSet;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

public final class ItemEntityOptimizer {
    private static int tickCounter = 0;

    private ItemEntityOptimizer() {
    }

    public static void init() {
        ServerTickEvents.END_LEVEL_TICK.register(ItemEntityOptimizer::onLevelTick);
    }

    private static void onLevelTick(ServerLevel level) {
        HSNConfig cfg = HSNConfig.get();
        if (!cfg.itemMergeEnabled) {
            return;
        }
        if (++tickCounter < Math.max(10, cfg.itemMergeIntervalTicks)) {
            return;
        }
        tickCounter = 0;
        double radius = cfg.itemMergeRadius;
        double scanRange = 48.0;
        HashSet<ItemEntity> visited = new HashSet<ItemEntity>();
        for (ServerPlayer player : level.players()) {
            AABB playerBox = player.getBoundingBox().inflate(scanRange);
            List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, playerBox, e -> e.isAlive() && !e.getItem().isEmpty());
            block1: for (ItemEntity a : items) {
                ItemStack stackA;
                if (!visited.add(a) || !a.isAlive() || (stackA = a.getItem()).getCount() >= stackA.getMaxStackSize()) continue;
                AABB box = a.getBoundingBox().inflate(radius);
                List<ItemEntity> nearby = level.getEntitiesOfClass(ItemEntity.class, box, b -> b != a && b.isAlive() && !b.getItem().isEmpty());
                for (ItemEntity b2 : nearby) {
                    if (!a.isAlive()) continue block1;
                    ItemStack stackB = b2.getItem();
                    if (!ItemStack.isSameItemSameComponents((ItemStack)stackA, (ItemStack)stackB)) continue;
                    int space = stackA.getMaxStackSize() - stackA.getCount();
                    if (space <= 0) continue block1;
                    int move = Math.min(space, stackB.getCount());
                    stackA.grow(move);
                    a.setItem(stackA);
                    stackB.shrink(move);
                    if (stackB.isEmpty()) {
                        b2.discard();
                        continue;
                    }
                    b2.setItem(stackB);
                }
            }
        }
    }
}

