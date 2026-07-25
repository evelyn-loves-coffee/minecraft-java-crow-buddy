package com.crowbuddy.goal;

import com.crowbuddy.entity.CrowEntity;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

/** Food attraction backed by the same hop/flight navigation used by custom goals. */
public final class CrowTemptGoal extends Goal {
    private static final double RANGE = 10.0;
    private static final double STOP_DISTANCE_SQ = 2.25;
    private final CrowEntity crow;
    private final double speed;
    private Player player;

    public CrowTemptGoal(CrowEntity crow, double speed) {
        this.crow = crow;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (crow.isInSittingPose() || crow.isOrderedToSit()) return false;
        player = null;
        double nearestDistanceSq = RANGE * RANGE;
        for (Player candidate : crow.level().getEntitiesOfClass(
                Player.class, crow.getBoundingBox().inflate(RANGE))) {
            double distanceSq = crow.distanceToSqr(candidate);
            if (distanceSq < nearestDistanceSq && isTempting(candidate)) {
                player = candidate;
                nearestDistanceSq = distanceSq;
            }
        }
        return player != null;
    }

    @Override
    public boolean canContinueToUse() {
        return isTempting(player) && crow.distanceToSqr(player) <= RANGE * RANGE;
    }

    @Override
    public void start() {
        if (crow.distanceToSqr(player) > STOP_DISTANCE_SQ) {
            crow.getCrowNavigator().navigateTo(crow, player, speed);
        }
    }

    @Override
    public void tick() {
        crow.getLookControl().setLookAt(player, 30.0f, crow.getMaxHeadXRot());
        if (crow.distanceToSqr(player) <= STOP_DISTANCE_SQ) {
            crow.getCrowNavigator().clear(crow);
        } else if (!crow.getCrowNavigator().hasPath()) {
            crow.getCrowNavigator().navigateTo(crow, player, speed);
        }
    }

    @Override
    public void stop() {
        crow.getCrowNavigator().clear(crow);
        player = null;
    }

    private boolean isTempting(Player candidate) {
        return candidate != null && candidate.isAlive()
            && (crow.isFood(candidate.getMainHandItem()) || crow.isFood(candidate.getOffhandItem()));
    }
}
