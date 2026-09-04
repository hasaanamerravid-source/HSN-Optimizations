package hsn.modod.mixin;

import hsn.modod.optimize.PathfindingStats;
import hsn.modod.optimize.PathfindingThrottle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reuses the current path only when the goal is unchanged. Never cancels tick(). */
@Mixin(PathNavigation.class)
public abstract class PathNavigationMixin {

    @Shadow
    @Final
    protected Mob mob;

    @Shadow
    public abstract Path getPath();

    @Shadow
    public abstract boolean isDone();

    @Shadow
    public abstract void setMaxVisitedNodesMultiplier(float multiplier);

    @Unique
    private PathfindingThrottle.Schedule hsn$schedule;

    @Unique
    private boolean hsn$budgetTouched;

    @Inject(method = "tick", at = @At("HEAD"))
    private void hsn$applyNodeBudget(CallbackInfo ci) {
        Mob self = this.mob;
        if (self == null || !self.isAlive() || !PathfindingThrottle.enabledFor(self)) {
            hsn$restoreBudget();
            return;
        }

        // Lazy-initialize schedule helper to prevent mixin field transformer issues
        if (this.hsn$schedule == null) {
            this.hsn$schedule = new PathfindingThrottle.Schedule();
        }

        this.hsn$schedule.interval(self);
        float budget = this.hsn$schedule.nodeBudget();

        if (budget < 1.0f) {
            this.setMaxVisitedNodesMultiplier(budget);
            this.hsn$budgetTouched = true;
        } else {
            hsn$restoreBudget();
        }
    }

    @Unique
    private void hsn$restoreBudget() {
        if (this.hsn$budgetTouched) {
            this.setMaxVisitedNodesMultiplier(1.0f);
            this.hsn$budgetTouched = false;
        }
    }

    @Inject(method = "createPath(DDDI)Lnet/minecraft/world/level/pathfinder/Path;", at = @At("HEAD"), cancellable = true, require = 0)
    private void hsn$reusePathXyz(double x, double y, double z, int reachRange, CallbackInfoReturnable<Path> cir) {
        hsn$tryReuseCurrentPath(BlockPos.containing(x, y, z), cir);
    }

    @Inject(method = "createPath(Lnet/minecraft/core/BlockPos;I)Lnet/minecraft/world/level/pathfinder/Path;", at = @At("HEAD"), cancellable = true, require = 0)
    private void hsn$reusePathBlock(BlockPos pos, int reachRange, CallbackInfoReturnable<Path> cir) {
        hsn$tryReuseCurrentPath(pos, cir);
    }

    @Inject(method = "createPath(Lnet/minecraft/world/entity/Entity;I)Lnet/minecraft/world/level/pathfinder/Path;", at = @At("HEAD"), cancellable = true, require = 0)
    private void hsn$reusePathEntity(Entity target, int reachRange, CallbackInfoReturnable<Path> cir) {
        if (target == null) {
            return;
        }
        // Use exact feet position to handle fast-moving targets accurately
        hsn$tryReuseCurrentPath(target.blockPosition(), cir);
    }

    @Unique
    private void hsn$tryReuseCurrentPath(BlockPos newGoal, CallbackInfoReturnable<Path> cir) {
        if (newGoal == null) {
            return;
        }

        Mob self = this.mob;
        if (self == null || !self.isAlive() || this.isDone()) {
            return;
        }

        Path current = this.getPath();
        if (current == null) {
            return;
        }

        if (this.hsn$schedule == null) {
            this.hsn$schedule = new PathfindingThrottle.Schedule();
        }

        int interval = this.hsn$schedule.interval(self);
        if (!PathfindingThrottle.shouldReusePath(self, current, newGoal, interval)) {
            return;
        }

        PathfindingStats.tickSkipped();
        cir.setReturnValue(current);
    }
}