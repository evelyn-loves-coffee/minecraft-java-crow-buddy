package com.crowbuddy.entity.ai.goal;

import com.crowbuddy.block.entity.CrowNestBlockEntity;
import com.crowbuddy.entity.CrowEntity;
import com.crowbuddy.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class CrowNestBuildGoal extends Goal {
    public static final int SEARCH_RADIUS = 48;
    private static final int SEARCH_INTERVAL = 100;
    private static final double ARRIVAL_DISTANCE_SQ = 1.44;
    private static final double TERRAIN_CLEARANCE = 2.0;
    private static final int CLEARANCE_SEARCH_RADIUS = 8;
    private static final double GROUNDED_HOP_VELOCITY = 0.42;
    private static final int[][] CLEARANCE_DIRECTIONS = {
        {1, 0}, {-1, 0}, {0, 1}, {0, -1},
        {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    private enum FlightPhase {
        DIRECT,
        CLEAR_UNDERSIDE,
        CLIMB,
        CRUISE
    }

    private final CrowEntity crow;
    private final double speed;
    private final int timeoutTicks;
    private int ticksElapsed;
    private BlockPos targetPos;
    private long lastSearchTick = -SEARCH_INTERVAL;
    private boolean flying;
    private boolean finished;
    private FlightPhase flightPhase = FlightPhase.DIRECT;
    private Vec3 clearanceWaypoint;
    private double cruiseAltitude;
    private Vec3 lastProgressPosition;
    private int stalledTicks;

    public CrowNestBuildGoal(CrowEntity crow, double speed, int timeoutTicks) {
        this.crow = crow;
        this.speed = speed;
        this.timeoutTicks = timeoutTicks;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    public void assignTarget(BlockPos targetPos) {
        this.targetPos = targetPos;
        this.lastSearchTick = this.crow.level().getGameTime();
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
        this.lastProgressPosition = this.crow.position();
        this.stalledTicks = 0;
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
            // A nearby pair can claim the same exposed leaf between selection and
            // arrival. Bypass the normal polling throttle so this crow immediately
            // chooses the next valid canopy position instead of appearing to abort.
            this.lastSearchTick = -SEARCH_INTERVAL;
            this.refreshTargetIfDue();
            if (this.targetPos == null) {
                this.crow.getNavigation().stop();
                return;
            }
            this.navigateToTarget();
        }

        if (this.flying) {
            this.flyTowardTarget();
            this.recoverIfStalled();
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
        this.flightPhase = FlightPhase.DIRECT;
        this.clearanceWaypoint = null;
        this.cruiseAltitude = 0.0;
        this.lastProgressPosition = null;
        this.stalledTicks = 0;
    }

    private void refreshTargetIfDue() {
        long currentTick = this.crow.level().getGameTime();
        if (this.targetPos != null || currentTick - this.lastSearchTick < SEARCH_INTERVAL) {
            return;
        }
        this.targetPos = findNearestBuildSite(this.crow);
        this.lastSearchTick = currentTick;
        if (this.targetPos != null) {
            com.crowbuddy.CrowBuddy.LOGGER.debug(
                "Crow {} selected exposed leaf nest site at {} within {}-block search radius",
                this.crow.getId(), this.targetPos, SEARCH_RADIUS);
        }
    }

    private void navigateToTarget() {
        if (this.targetPos == null) return;
        this.flying = true;
        this.planFlightApproach();
        Vec3 launchTarget = this.clearanceWaypoint != null
            ? this.clearanceWaypoint
            : Vec3.atCenterOf(this.targetPos);
        this.crow.launchToward(launchTarget, 0.16);
    }

    private void flyTowardTarget() {
        Vec3 target;
        if (this.flightPhase == FlightPhase.CLEAR_UNDERSIDE && this.clearanceWaypoint != null) {
            // Maintain lift while moving out from under a canopy. The old downward
            // clamp turned this phase into ground sliding.
            target = this.clearanceWaypoint.add(0.0, 0.75, 0.0);
            if (horizontalDistanceSqr(this.crow.position(), target) <= 0.36) {
                this.flightPhase = FlightPhase.CLIMB;
                target = new Vec3(target.x, this.cruiseAltitude, target.z);
            }
        } else if (this.flightPhase == FlightPhase.CLIMB && this.clearanceWaypoint != null) {
            target = new Vec3(this.clearanceWaypoint.x, this.cruiseAltitude, this.clearanceWaypoint.z);
            if (this.crow.getY() >= this.cruiseAltitude - 0.25) {
                this.flightPhase = FlightPhase.CRUISE;
                target = new Vec3(
                    this.targetPos.getX() + 0.5, this.cruiseAltitude, this.targetPos.getZ() + 0.5);
            }
        } else if (this.flightPhase == FlightPhase.CRUISE) {
            target = new Vec3(
                this.targetPos.getX() + 0.5, this.cruiseAltitude, this.targetPos.getZ() + 0.5);
            if (horizontalDistanceSqr(this.crow.position(), target) <= 0.64) {
                this.flightPhase = FlightPhase.DIRECT;
                target = Vec3.atCenterOf(this.targetPos).add(0.0, -0.15, 0.0);
            }
        } else {
            target = Vec3.atCenterOf(this.targetPos).add(0.0, -0.15, 0.0);
        }
        Vec3 delta = target.subtract(this.crow.position());
        if (delta.lengthSqr() < 0.01) return;
        Vec3 desired = delta.normalize().scale(0.28);
        Vec3 movement = this.crow.getDeltaMovement().scale(0.68).add(desired.scale(0.32));
        // Manual flight steering does not use vanilla path navigation, so it cannot
        // step over even a one-block obstacle by itself. A standard jump-strength
        // impulse gets a ground-contacting crow airborne while flight remains the
        // preferred travel mode.
        if (this.crow.onGround()) {
            movement = new Vec3(movement.x, Math.max(GROUNDED_HOP_VELOCITY, movement.y), movement.z);
        }
        this.crow.setDeltaMovement(movement);
        this.crow.getLookControl().setLookAt(
            target.x, target.y, target.z, 12.0f, this.crow.getMaxHeadXRot());
    }

    private void recoverIfStalled() {
        if (this.lastProgressPosition == null
                || this.crow.position().distanceToSqr(this.lastProgressPosition) >= 0.04) {
            this.lastProgressPosition = this.crow.position();
            this.stalledTicks = 0;
            return;
        }
        if (++this.stalledTicks < 20) return;
        this.stalledTicks = 0;
        this.lastProgressPosition = this.crow.position();
        this.planFlightApproach();
        Vec3 movement = this.crow.getDeltaMovement();
        this.crow.setDeltaMovement(movement.x, Math.max(GROUNDED_HOP_VELOCITY, movement.y), movement.z);
        com.crowbuddy.CrowBuddy.LOGGER.debug(
            "Crow {} replanned a stalled nest approach to {}", this.crow.getId(), this.targetPos);
    }

    private void planFlightApproach() {
        this.flightPhase = FlightPhase.DIRECT;
        this.clearanceWaypoint = null;
        Vec3 currentColumn = this.crow.position();
        this.cruiseAltitude = this.terrainSafeCruiseAltitude(currentColumn);
        if (this.crow.getY() >= this.cruiseAltitude - 0.25) {
            this.flightPhase = FlightPhase.CRUISE;
            this.logPlannedFlight();
            return;
        }
        if (this.isVerticalColumnClear(currentColumn, this.cruiseAltitude)) {
            this.clearanceWaypoint = currentColumn;
            this.flightPhase = FlightPhase.CLIMB;
            this.logPlannedFlight();
            return;
        }

        this.clearanceWaypoint = this.findOpenClimbWaypoint();
        if (this.clearanceWaypoint != null) {
            this.flightPhase = FlightPhase.CLEAR_UNDERSIDE;
            Vec3 movement = this.crow.getDeltaMovement();
            this.crow.setDeltaMovement(movement.x, Math.min(0.0, movement.y), movement.z);
            this.logPlannedFlight();
            return;
        }

        com.crowbuddy.CrowBuddy.LOGGER.debug(
            "Crow {} found no collision-free climb column within {} blocks; using direct nest approach",
            this.crow.getId(), CLEARANCE_SEARCH_RADIUS);
    }

    private void logPlannedFlight() {
        com.crowbuddy.CrowBuddy.LOGGER.debug(
            "Crow {} planned {} nest approach via {} at cruise altitude {}",
            this.crow.getId(), this.flightPhase, this.clearanceWaypoint,
            this.cruiseAltitude);
    }

    private Vec3 findOpenClimbWaypoint() {
        Vec3 origin = this.crow.position();
        for (int distance = 1; distance <= CLEARANCE_SEARCH_RADIUS; distance++) {
            Vec3 best = null;
            double bestTargetDistance = Double.POSITIVE_INFINITY;
            double bestCruiseAltitude = 0.0;
            for (int[] direction : CLEARANCE_DIRECTIONS) {
                Vec3 candidate = origin.add(
                    direction[0] * distance, 0.0, direction[1] * distance);
                double candidateCruiseAltitude = this.terrainSafeCruiseAltitude(candidate);
                if (!this.isHorizontalPathClear(origin, candidate)
                        || !this.isVerticalColumnClear(candidate, candidateCruiseAltitude)) {
                    continue;
                }
                double targetDistance = candidate.distanceToSqr(Vec3.atCenterOf(this.targetPos));
                if (targetDistance < bestTargetDistance) {
                    best = candidate;
                    bestTargetDistance = targetDistance;
                    bestCruiseAltitude = candidateCruiseAltitude;
                }
            }
            if (best != null) {
                this.cruiseAltitude = bestCruiseAltitude;
                return best;
            }
        }
        return null;
    }

    private double terrainSafeCruiseAltitude(Vec3 from) {
        Vec3 destination = Vec3.atCenterOf(this.targetPos);
        Vec3 delta = destination.subtract(from);
        int samples = Math.max(1, (int) Math.ceil(delta.horizontalDistance() * 2.0));
        int highestSurface = this.targetPos.getY();
        for (int sample = 0; sample <= samples; sample++) {
            double progress = (double) sample / samples;
            int x = Mth.floor(from.x + delta.x * progress);
            int z = Mth.floor(from.z + delta.z * progress);
            highestSurface = Math.max(highestSurface,
                this.crow.level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z));
        }
        return highestSurface + TERRAIN_CLEARANCE;
    }

    private boolean isHorizontalPathClear(Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        int samples = Math.max(1, (int) Math.ceil(delta.horizontalDistance() * 2.0));
        AABB bounds = this.crow.getBoundingBox();
        for (int sample = 1; sample <= samples; sample++) {
            Vec3 offset = delta.scale((double) sample / samples);
            if (!this.crow.level().noCollision(this.crow, bounds.move(offset))) return false;
        }
        return true;
    }

    private boolean isVerticalColumnClear(Vec3 base, double approachY) {
        Vec3 offset = base.subtract(this.crow.position());
        AABB bounds = this.crow.getBoundingBox().move(offset);
        double rise = Math.max(0.0, approachY - base.y);
        return this.crow.level().noCollision(this.crow, bounds.expandTowards(0.0, rise, 0.0));
    }

    private static double horizontalDistanceSqr(Vec3 first, Vec3 second) {
        double dx = first.x - second.x;
        double dz = first.z - second.z;
        return dx * dx + dz * dz;
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
