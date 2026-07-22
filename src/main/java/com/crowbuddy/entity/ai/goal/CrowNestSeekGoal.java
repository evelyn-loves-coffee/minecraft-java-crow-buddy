package com.crowbuddy.entity.ai.goal;

import com.crowbuddy.entity.CrowEntity;
import com.crowbuddy.block.entity.CrowNestBlockEntity;
import com.crowbuddy.block.entity.CrowNestStateMachine;
import com.crowbuddy.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class CrowNestSeekGoal extends Goal {

    private static final int SEARCH_INTERVAL = 100;
    private static final double ARRIVAL_DISTANCE_SQ = 1.44;

    private final CrowEntity crow;
    private final double speed;
    private final int timeoutTicks;
    private int ticksElapsed;
    private BlockPos targetPos;
    private long lastSearchTick = -100;
    private boolean flying;
    private boolean finished;

    public CrowNestSeekGoal(CrowEntity crow, double speed, int timeoutTicks) {
        this.crow = crow;
        this.speed = speed;
        this.timeoutTicks = timeoutTicks;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.crow.isBaby()) {
            return false;
        }
        if (!this.crow.isInMatingState()) {
            return false;
        }
        if (this.crow.isInSittingPose()) {
            return false;
        }
        long currentTick = this.crow.level().getGameTime();
        if (this.targetPos == null || (currentTick - this.lastSearchTick) >= SEARCH_INTERVAL) {
            this.targetPos = findNearestAvailableNest(this.crow);
            this.lastSearchTick = currentTick;
        }
        return this.targetPos != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.crow.isInMatingState() && !this.crow.isInSittingPose()
            && this.targetPos != null
            && this.ticksElapsed < this.timeoutTicks;
    }

    @Override
    public void start() {
        PathNavigation navigation = this.crow.getNavigation();
        boolean hasGroundPath = navigation.moveTo(
            this.targetPos.getX() + 0.5, this.targetPos.getY(), this.targetPos.getZ() + 0.5, this.speed);
        this.flying = !hasGroundPath || this.targetPos.getY() > this.crow.getY() + 1.0;
        if (this.flying) {
            navigation.stop();
            this.crow.setAirborne(true);
            this.crow.triggerTakeoffAnimation();
        }
        this.ticksElapsed = 0;
        this.finished = false;
    }

    @Override
    public void tick() {
        this.ticksElapsed++;
        if (this.ticksElapsed >= this.timeoutTicks) {
            this.crow.setInMatingState(false);
            this.finished = true;
            this.stop();
            return;
        }
        if (!isAvailableNest(this.crow.level(), this.targetPos)) {
            this.targetPos = findNearestAvailableNest(this.crow);
            if (this.targetPos == null) {
                this.crow.setInMatingState(false);
                this.finished = true;
                return;
            }
            if (!this.flying && this.targetPos.getY() > this.crow.getY() + 1.0) {
                this.crow.getNavigation().stop();
                this.flying = true;
                this.crow.setAirborne(true);
                this.crow.triggerTakeoffAnimation();
            }
        }
        if (this.flying) {
            flyTowardNest();
        }
        double distSq = this.crow.distanceToSqr(
                this.targetPos.getX() + 0.5,
                this.targetPos.getY() + 0.35,
                this.targetPos.getZ() + 0.5
        );
        if (distSq < ARRIVAL_DISTANCE_SQ) {
            enterNest(this.targetPos);
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
        this.lastSearchTick = -100;
        this.flying = false;
    }

    private void flyTowardNest() {
        Vec3 target = new Vec3(
            this.targetPos.getX() + 0.5,
            this.targetPos.getY() + 0.35,
            this.targetPos.getZ() + 0.5);
        Vec3 delta = target.subtract(this.crow.position());
        if (delta.lengthSqr() < 0.01) return;
        Vec3 desired = delta.normalize().scale(0.28);
        Vec3 next = this.crow.getDeltaMovement().scale(0.68).add(desired.scale(0.32));
        this.crow.setDeltaMovement(next);
        this.crow.getLookControl().setLookAt(target.x, target.y, target.z, 12.0f, this.crow.getMaxHeadXRot());
    }

    private void enterNest(BlockPos pos) {
        if (this.crow.level().getBlockState(pos).getBlock() != ModBlocks.CROW_NEST) {
            return;
        }
        var be = this.crow.level().getBlockEntity(pos);
        if (be instanceof com.crowbuddy.block.entity.CrowNestBlockEntity nestBE) {
            if (nestBE.getStage() == CrowNestStateMachine.STAGE_IDLE) {
                nestBE.startIncubation();
                this.finished = true;
            }
        }
        this.crow.setInMatingState(false);
        if (this.crow.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.HEART,
                    pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                    5, 0.2, 0.3, 0.2, 0.0
            );
        }
    }

    public static BlockPos findNearestAvailableNest(CrowEntity crow) {
        LevelReader reader = crow.level();
        return BlockPos.findClosestMatch(
            crow.blockPosition(), 32, 20,
            pos -> isAvailableNest(reader, pos)
        ).orElse(null);
    }

    private static boolean isAvailableNest(LevelReader reader, BlockPos pos) {
        return reader.getBlockState(pos).getBlock() == ModBlocks.CROW_NEST
            && reader.getBlockEntity(pos) instanceof CrowNestBlockEntity nest
            && nest.getStage() == CrowNestStateMachine.STAGE_IDLE;
    }
}
