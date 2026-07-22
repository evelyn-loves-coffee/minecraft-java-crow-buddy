package com.crowbuddy.goal;

import com.crowbuddy.entity.CrowEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;

public class BabyNestReturnGoal extends Goal {

    private static final double RETURN_DISTANCE_SQ = 256.0;
    private static final double STOP_DISTANCE_SQ = 4.0;
    private static final double SPEED = 1.0;
    private static final int REPATH_INTERVAL = 5;

    private final CrowEntity crow;
    private int tickCounter;

    public BabyNestReturnGoal(CrowEntity crow) {
        this.crow = crow;
        this.setFlags(java.util.EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (!this.crow.isBaby()) return false;
        if (this.crow.isInSittingPose()) return false;
        if (this.crow.isAirborne()) return false;
        BlockPos nest = this.crow.getHomeNestPos();
        if (nest == null) return false;
        return this.crow.distanceToSqr(nest.getX() + 0.5, nest.getY(), nest.getZ() + 0.5) > RETURN_DISTANCE_SQ;
    }

    @Override
    public void start() {
        this.tickCounter = 0;
        BlockPos nest = this.crow.getHomeNestPos();
        if (nest == null) return;
        PathNavigation navigation = this.crow.getNavigation();
        navigation.moveTo(nest.getX() + 0.5, nest.getY(), nest.getZ() + 0.5, SPEED);
    }

    @Override
    public boolean canContinueToUse() {
        if (!this.crow.isBaby() || this.crow.isInSittingPose()
                || this.crow.isAirborne()) return false;
        BlockPos nest = this.crow.getHomeNestPos();
        return nest != null && this.crow.distanceToSqr(
            nest.getX() + 0.5, nest.getY(), nest.getZ() + 0.5) > STOP_DISTANCE_SQ;
    }

    @Override
    public void tick() {
        BlockPos nest = this.crow.getHomeNestPos();
        if (nest == null) return;
        double distSq = this.crow.distanceToSqr(nest.getX() + 0.5, nest.getY(), nest.getZ() + 0.5);
        if (distSq < STOP_DISTANCE_SQ) {
            this.stop();
            return;
        }
        this.tickCounter++;
        if (this.tickCounter >= REPATH_INTERVAL) {
            this.tickCounter = 0;
            PathNavigation navigation = this.crow.getNavigation();
            navigation.moveTo(nest.getX() + 0.5, nest.getY(), nest.getZ() + 0.5, SPEED);
        }
    }
}
