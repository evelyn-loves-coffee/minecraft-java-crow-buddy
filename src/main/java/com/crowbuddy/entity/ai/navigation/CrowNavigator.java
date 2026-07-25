package com.crowbuddy.entity.ai.navigation;

import com.crowbuddy.entity.CrowBehaviorPolicy;
import com.crowbuddy.entity.CrowEntity;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Shared movement controller. Goals own intent; this class owns planning and steering. */
public final class CrowNavigator {
    private static final double WAYPOINT_DISTANCE_SQ = 1.0;
    private static final int STALL_TICKS = 20;
    private final FlightNavigator flightPlanner;
    private final CrowPathCache cache = new CrowPathCache();
    private Vec3 target;
    private Entity trackedTarget;
    private List<Vec3> path = List.of();
    private int waypoint;
    private int stalledTicks;
    private double lastTargetDistanceSq = Double.POSITIVE_INFINITY;
    private double speed = 1.0;
    private MovementMode mode = MovementMode.HOP;

    public CrowNavigator(FlightNavigator flightPlanner) { this.flightPlanner = flightPlanner; }

    public boolean navigateTo(CrowEntity crow, Vec3 destination, double speed) {
        this.trackedTarget = null;
        return setTarget(crow, destination, speed);
    }

    public boolean navigateTo(CrowEntity crow, Entity destination, double speed) {
        this.trackedTarget = destination;
        return setTarget(crow, destination.position().add(0.0, destination.getBbHeight() * 0.5, 0.0), speed);
    }

    private boolean setTarget(CrowEntity crow, Vec3 destination, double speed) {
        this.target = destination;
        this.speed = speed;
        double dx = destination.x - crow.getX(), dz = destination.z - crow.getZ();
        this.mode = CrowBehaviorPolicy.shouldUseGroundHop(destination.y - crow.getY(), dx * dx + dz * dz)
            ? MovementMode.HOP : MovementMode.FLY;
        this.waypoint = 0;
        this.stalledTicks = 0;
        this.lastTargetDistanceSq = crow.position().distanceToSqr(destination);
        if (mode == MovementMode.HOP) {
            cache.clear();
            path = List.of();
            crow.setAirborne(false);
            return crow.getNavigation().moveTo(destination.x, destination.y, destination.z, speed);
        }
        crow.getNavigation().stop();
        long tick = crow.level().getGameTime();
        path = cache.matches(destination, tick) ? cache.path() : flightPlanner.findPath(crow.level(), crow.position(), destination);
        if (path.isEmpty()) path = List.of(destination); // bounded planner fallback retains liveness
        else cache.store(destination, path, tick);
        if (!crow.isAirborne()) crow.launchToward(path.getFirst(), 0.16);
        return true;
    }

    public void tick(CrowEntity crow) {
        if (target == null) return;
        if (trackedTarget != null) {
            if (!trackedTarget.isAlive()) { clear(crow); return; }
            Vec3 moved = trackedTarget.position().add(0.0, trackedTarget.getBbHeight() * 0.5, 0.0);
            if (moved.distanceToSqr(target) > 1.0) setTarget(crow, moved, speed);
        }
        if (mode == MovementMode.HOP) return;
        if (crow.position().distanceToSqr(target) <= WAYPOINT_DISTANCE_SQ) {
            crow.setDeltaMovement(crow.getDeltaMovement().scale(0.35));
            if (crow.isAirborne()) {
                crow.setAirborne(false);
                crow.triggerLandAnimation();
            }
            return;
        }
        while (waypoint < path.size() - 1 && crow.position().distanceToSqr(path.get(waypoint)) <= WAYPOINT_DISTANCE_SQ) waypoint++;
        Vec3 aim = path.get(Math.min(waypoint, path.size() - 1));
        Vec3 delta = aim.subtract(crow.position());
        if (delta.lengthSqr() > 0.01) {
            Vec3 desired = delta.normalize().scale((crow.isBaby() ? 0.20 : 0.30) * speed);
            Vec3 movement = crow.getDeltaMovement().scale(0.72).add(desired.scale(0.28));
            if (crow.onGround()) movement = new Vec3(movement.x, Math.max(0.42, movement.y), movement.z);
            crow.setDeltaMovement(movement);
            crow.getLookControl().setLookAt(aim.x, aim.y, aim.z, 12.0f, crow.getMaxHeadXRot());
        }
        double distance = crow.position().distanceToSqr(target);
        stalledTicks = distance + 0.04 < lastTargetDistanceSq ? 0 : stalledTicks + 1;
        lastTargetDistanceSq = distance;
        if (stalledTicks >= STALL_TICKS) { cache.clear(); setTarget(crow, target, speed); }
    }

    public boolean hasReachedTarget(CrowEntity crow, double distanceSq) { return target != null && crow.position().distanceToSqr(target) <= distanceSq; }
    public boolean hasPath() { return target != null; }
    public MovementMode getMovementMode() { return mode; }
    public void invalidatePath() { cache.clear(); path = List.of(); }
    public void clear(CrowEntity crow) {
        crow.getNavigation().stop();
        if (crow.onGround() && crow.isAirborne()) {
            crow.setAirborne(false);
            crow.triggerLandAnimation();
        }
        target = null; trackedTarget = null; path = List.of(); cache.clear();
    }
}
