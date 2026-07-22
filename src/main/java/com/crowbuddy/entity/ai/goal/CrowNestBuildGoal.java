package com.crowbuddy.entity.ai.goal;

import com.crowbuddy.block.entity.CrowNestBlockEntity;
import com.crowbuddy.entity.CrowEntity;
import com.crowbuddy.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class CrowNestBuildGoal extends Goal {
    public static final int SEARCH_RADIUS = 16;
    private static final int SEARCH_INTERVAL = 100;
    private static final double ARRIVAL_DISTANCE_SQ = 1.44;

    private final CrowEntity crow;
    private final double speed;
    private final int timeoutTicks;
    private int ticksElapsed;
    private BlockPos targetPos;
    private long lastSearchTick = -SEARCH_INTERVAL;
    private boolean flying;
    private boolean finished;

    public CrowNestBuildGoal(CrowEntity crow, double speed, int timeoutTicks) {
        this.crow = crow;
        this.speed = speed;
        this.timeoutTicks = timeoutTicks;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.crow.isBaby() || !this.crow.isInMatingState()
                || this.crow.isInSittingPose() || this.crow.isOrderedToSit()) {
            return false;
        }
        this.refreshTargetIfDue();
        return this.crow.level() instanceof ServerLevel;
    }

    @Override
    public boolean canContinueToUse() {
        return this.crow.isInMatingState()
            && !this.crow.isInSittingPose()
            && !this.crow.isOrderedToSit()
            && this.ticksElapsed < this.timeoutTicks;
    }

    @Override
    public void start() {
        this.ticksElapsed = 0;
        this.finished = false;
        this.navigateToTarget();
    }

    @Override
    public void tick() {
        this.ticksElapsed++;
        if (this.ticksElapsed >= this.timeoutTicks) {
            com.crowbuddy.CrowBuddy.LOGGER.debug(
                "Crow {} could not find a valid exposed leaf nest site within {} ticks",
                this.crow.getId(), this.timeoutTicks);
            this.finished = true;
            this.crow.setInMatingState(false);
            return;
        }

        if (!isValidBuildSite(this.crow.level() instanceof ServerLevel serverLevel ? serverLevel : null,
                this.targetPos)) {
            this.targetPos = null;
            this.refreshTargetIfDue();
            if (this.targetPos == null) {
                this.crow.getNavigation().stop();
                return;
            }
            this.navigateToTarget();
        }

        if (this.flying) {
            this.flyTowardTarget();
        }
        double distSq = this.crow.distanceToSqr(
            this.targetPos.getX() + 0.5,
            this.targetPos.getY() + 0.35,
            this.targetPos.getZ() + 0.5
        );
        if (distSq < ARRIVAL_DISTANCE_SQ) {
            this.buildNest();
        }
    }

    @Override
    public void stop() {
        if (this.flying) {
            this.crow.setAirborne(false);
            this.crow.triggerLandAnimation();
            Vec3 movement = this.crow.getDeltaMovement();
            this.crow.setDeltaMovement(movement.x * 0.35, Math.min(0.0, movement.y), movement.z * 0.35);
        }
        if (this.finished) {
            this.crow.setInMatingState(false);
        }
        this.targetPos = null;
        this.lastSearchTick = -SEARCH_INTERVAL;
        this.flying = false;
    }

    private void refreshTargetIfDue() {
        long currentTick = this.crow.level().getGameTime();
        if (this.targetPos != null || currentTick - this.lastSearchTick < SEARCH_INTERVAL) {
            return;
        }
        this.targetPos = findNearestBuildSite(this.crow);
        this.lastSearchTick = currentTick;
    }

    private void navigateToTarget() {
        if (this.targetPos == null) return;
        PathNavigation navigation = this.crow.getNavigation();
        boolean hasGroundPath = navigation.moveTo(
            this.targetPos.getX() + 0.5, this.targetPos.getY(), this.targetPos.getZ() + 0.5, this.speed);
        this.flying = !hasGroundPath || this.targetPos.getY() > this.crow.getY() + 1.0;
        if (this.flying) {
            navigation.stop();
            this.crow.setAirborne(true);
            this.crow.triggerTakeoffAnimation();
        }
    }

    private void flyTowardTarget() {
        Vec3 target = Vec3.atCenterOf(this.targetPos).add(0.0, -0.15, 0.0);
        Vec3 delta = target.subtract(this.crow.position());
        if (delta.lengthSqr() < 0.01) return;
        Vec3 desired = delta.normalize().scale(0.28);
        this.crow.setDeltaMovement(this.crow.getDeltaMovement().scale(0.68).add(desired.scale(0.32)));
        this.crow.getLookControl().setLookAt(
            target.x, target.y, target.z, 12.0f, this.crow.getMaxHeadXRot());
    }

    private void buildNest() {
        if (!(this.crow.level() instanceof ServerLevel serverLevel)
                || !isValidBuildSite(serverLevel, this.targetPos)) {
            this.targetPos = null;
            return;
        }
        if (!serverLevel.setBlock(this.targetPos, ModBlocks.CROW_NEST.defaultBlockState(), Block.UPDATE_ALL)) {
            this.targetPos = null;
            return;
        }
        if (!(serverLevel.getBlockEntity(this.targetPos) instanceof CrowNestBlockEntity nest)) {
            serverLevel.removeBlock(this.targetPos, false);
            this.targetPos = null;
            return;
        }

        nest.startIncubation();
        com.crowbuddy.CrowBuddy.LOGGER.debug(
            "Crow {} constructed breeding nest at {}", this.crow.getId(), this.targetPos);
        serverLevel.sendParticles(
            net.minecraft.core.particles.ParticleTypes.HEART,
            this.targetPos.getX() + 0.5, this.targetPos.getY() + 0.8, this.targetPos.getZ() + 0.5,
            5, 0.2, 0.3, 0.2, 0.0
        );
        this.finished = true;
        this.crow.setInMatingState(false);
    }

    public static BlockPos findNearestBuildSite(CrowEntity crow) {
        if (!(crow.level() instanceof ServerLevel serverLevel)) return null;
        BlockPos center = crow.blockPosition();
        for (int radius = 0; radius <= SEARCH_RADIUS; radius++) {
            for (int x = -radius; x <= radius; x++) {
                BlockPos north = surfacePosition(serverLevel, center.getX() + x, center.getZ() - radius);
                if (isValidBuildSite(serverLevel, north)) return north;
                if (radius > 0) {
                    BlockPos south = surfacePosition(serverLevel, center.getX() + x, center.getZ() + radius);
                    if (isValidBuildSite(serverLevel, south)) return south;
                }
            }
            for (int z = -radius + 1; z < radius; z++) {
                BlockPos west = surfacePosition(serverLevel, center.getX() - radius, center.getZ() + z);
                if (isValidBuildSite(serverLevel, west)) return west;
                if (radius > 0) {
                    BlockPos east = surfacePosition(serverLevel, center.getX() + radius, center.getZ() + z);
                    if (isValidBuildSite(serverLevel, east)) return east;
                }
            }
        }
        return null;
    }

    private static BlockPos surfacePosition(ServerLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        return new BlockPos(x, y, z);
    }

    private static boolean isValidBuildSite(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null || !level.getWorldBorder().isWithinBounds(pos)) return false;
        return level.getBlockState(pos).isAir()
            && level.getBlockState(pos.below()).is(BlockTags.LEAVES)
            && level.canSeeSky(pos)
            && ModBlocks.CROW_NEST.defaultBlockState().canSurvive(level, pos);
    }
}
