package com.crowbuddy.entity.ai.goal;

import com.crowbuddy.entity.CrowEntity;
import com.crowbuddy.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.LevelReader;

import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.Deque;
import java.util.EnumSet;

public class CrowNestSeekGoal extends Goal {

    private final CrowEntity crow;
    private final double speed;
    private final int timeoutTicks;
    private int ticksElapsed;
    private BlockPos targetPos;

    public CrowNestSeekGoal(CrowEntity crow, double speed, int timeoutTicks) {
        this.crow = crow;
        this.speed = speed;
        this.timeoutTicks = timeoutTicks;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!this.crow.isInMatingState()) {
            return false;
        }
        if (this.crow.isInSittingPose()) {
            return false;
        }
        this.targetPos = findNearestNest(this.crow);
        return this.targetPos != null;
    }

    @Override
    public void start() {
        PathNavigation navigation = this.crow.getNavigation();
        navigation.moveTo(this.targetPos.getX() + 0.5, this.targetPos.getY(), this.targetPos.getZ() + 0.5, this.speed);
        this.ticksElapsed = 0;
    }

    @Override
    public void tick() {
        this.ticksElapsed++;
        if (this.ticksElapsed >= this.timeoutTicks) {
            this.crow.setInMatingState(false);
            this.stop();
            return;
        }
        double distSq = this.crow.distanceToSqr(
                this.targetPos.getX() + 0.5,
                this.targetPos.getY(),
                this.targetPos.getZ() + 0.5
        );
        if (distSq < 1.0) {
            enterNest(this.targetPos);
        }
    }

    @Override
    public void stop() {
        this.crow.setInMatingState(false);
    }

    private void enterNest(BlockPos pos) {
        if (this.crow.level().getBlockState(pos).getBlock() != ModBlocks.CROW_NEST) {
            return;
        }
        var be = this.crow.level().getBlockEntity(pos);
        if (be instanceof com.crowbuddy.block.entity.CrowNestBlockEntity nestBE) {
            if (nestBE.getStage() == com.crowbuddy.block.entity.CrowNestBlockEntity.STAGE_IDLE) {
                nestBE.startIncubation();
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

    private static BlockPos findNearestNest(CrowEntity crow) {
        LevelReader reader = crow.level();
        int maxRange = 32;

        int cx = (int) crow.getX();
        int cy = (int) crow.getY();
        int cz = (int) crow.getZ();

        BlockPos start = new BlockPos(cx, cy, cz);
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);

        int grid = maxRange * 2 + 1;
        BitSet visited = new BitSet(grid * grid * grid);
        queue.add(start);

        while (!queue.isEmpty()) {
            BlockPos current = queue.pollFirst();
            int dx = current.getX() - cx;
            int dy = current.getY() - cy;
            int dz = current.getZ() - cz;
            if (Math.abs(dx) > maxRange || Math.abs(dy) > maxRange || Math.abs(dz) > maxRange) {
                continue;
            }
            int idx = (dx + maxRange) + (dy + maxRange) * grid + (dz + maxRange) * grid * grid;
            if (visited.get(idx)) {
                continue;
            }
            visited.set(idx);

            if (reader.getBlockState(current).getBlock() == ModBlocks.CROW_NEST) {
                return current;
            }

            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);
                int ndx = neighbor.getX() - cx;
                int ndy = neighbor.getY() - cy;
                int ndz = neighbor.getZ() - cz;
                if (Math.abs(ndx) <= maxRange && Math.abs(ndy) <= maxRange && Math.abs(ndz) <= maxRange) {
                    int nidx = (ndx + maxRange) + (ndy + maxRange) * grid + (ndz + maxRange) * grid * grid;
                    if (!visited.get(nidx)) {
                        queue.addLast(neighbor);
                    }
                }
            }
        }
        return null;
    }
}
