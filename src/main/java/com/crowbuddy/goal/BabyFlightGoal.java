package com.crowbuddy.goal;

import com.crowbuddy.entity.CrowEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

public class BabyFlightGoal extends Goal {

    private static final double TAKEOFF_VELOCITY = 0.3;
    private static final double LIFT_VELOCITY = 0.04;
    private static final double MAX_FLIGHT_HEIGHT = 8.0;
    private static final int LIFT_INTERVAL = 4;
    private static final double LAND_THRESHOLD = 1.5;
    private static final int MAX_FLIGHT_TICKS = 1200;
    private static final double OWNER_FOLLOW_DISTANCE_SQ = 64.0;

    private final CrowEntity crow;
    private LivingEntity chaseTarget;
    private int liftTimer;
    private int flightCooldown;
    private int flightStartTick;

    public BabyFlightGoal(CrowEntity crow) {
        this.crow = crow;
        this.liftTimer = 0;
        this.flightCooldown = 0;
        this.flightStartTick = -1;
    }

    @Override
    public boolean canUse() {
        if (!this.crow.isBaby()) return false;
        if (this.crow.isInSittingPose()) return false;
        if (this.flightCooldown > 0) return false;
        if (this.crow.isAirborne()) return this.shouldContinueFlight();
        return this.shouldTakeOff();
    }

    @Override
    public boolean canContinueToUse() {
        if (this.crow.isInSittingPose()) return false;
        if (!this.crow.isAirborne()) return false;
        return this.shouldContinueFlight();
    }

    @Override
    public void start() {
        this.liftTimer = 0;
        this.flightStartTick = this.crow.tickCount;
        if (!this.crow.isAirborne()) this.takeOff();
        this.findChaseTarget();
    }

    @Override
    public void stop() {
        if (this.crow.isAirborne()) this.land();
        this.chaseTarget = null;
        this.flightCooldown = reducedTickDelay(30);
        this.flightStartTick = -1;
    }

    @Override
    public void tick() {
        if (!this.crow.isAlive()) return;
        this.liftTimer++;
        if (this.crow.isAirborne()) {
            if (this.liftTimer % LIFT_INTERVAL == 0) this.applyLift();
            if (this.chaseTarget != null && this.chaseTarget.isAlive()) {
                this.navigateTowards(this.chaseTarget.getX(), this.chaseTarget.getY() + 1.5, this.chaseTarget.getZ());
            }
            if (this.crow.getY() > MAX_FLIGHT_HEIGHT) {
                Vec3 cur = this.crow.getDeltaMovement();
                this.crow.setDeltaMovement(cur.add(0.0, -0.03, 0.0));
            }
        }
        if (!this.shouldContinueFlight()) this.land();
    }

    private boolean shouldTakeOff() {
        return this.ownerMovingAway() || this.hasNearbyThreat();
    }

    private boolean shouldContinueFlight() {
        if (this.chaseTarget != null && this.chaseTarget.isAlive()) return true;
        LivingEntity owner = this.crow.getOwner();
        if (owner != null && owner.isAlive()) return this.crow.distanceToSqr(owner) > 36.0;
        if (this.flightStartTick != -1 && this.crow.tickCount - this.flightStartTick > MAX_FLIGHT_TICKS) return false;
        return false;
    }

    private boolean ownerMovingAway() {
        LivingEntity owner = this.crow.getOwner();
        if (owner == null || !owner.isAlive()) return false;
        double distSq = this.crow.distanceToSqr(owner);
        if (distSq < 36.0) return false;
        Vec3 vel = owner.getDeltaMovement();
        double speed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        return speed > 0.12 && distSq > OWNER_FOLLOW_DISTANCE_SQ;
    }

    private boolean hasNearbyThreat() {
        double rSq = 16.0;
        for (LivingEntity e : this.crow.level().getEntitiesOfClass(LivingEntity.class, this.crow.getBoundingBox().inflate(rSq, 3.0, rSq))) {
            if (e == this.crow || e == this.crow.getOwner()) continue;
            if (!this.crow.canAttack(e)) continue;
            if (this.crow.distanceToSqr(e) < rSq) {
                this.chaseTarget = e;
                return true;
            }
        }
        return false;
    }

    private void findChaseTarget() {
        LivingEntity owner = this.crow.getOwner();
        if (owner != null && owner.isAlive() && this.crow.distanceToSqr(owner) > 36.0) {
            this.chaseTarget = owner;
            return;
        }
        for (LivingEntity e : this.crow.level().getEntitiesOfClass(LivingEntity.class, this.crow.getBoundingBox().inflate(12.0, 3.0, 12.0))) {
            if (e == this.crow || e == this.crow.getOwner()) continue;
            if (this.crow.canAttack(e)) {
                this.chaseTarget = e;
                return;
            }
        }
    }

    private void takeOff() {
        Vec3 cur = this.crow.getDeltaMovement();
        double len = Math.sqrt(cur.x * cur.x + cur.z * cur.z);
        if (len < 0.01) len = 0.01;
        this.crow.setDeltaMovement((cur.x / len) * 0.15, TAKEOFF_VELOCITY, (cur.z / len) * 0.15);
        this.crow.setAirborne(true);
        this.crow.triggerTakeoffAnimation();
    }

    private void applyLift() {
        Vec3 cur = this.crow.getDeltaMovement();
        if (cur.y < 0.0) {
            this.crow.setDeltaMovement(cur.add(0.0, LIFT_VELOCITY * 2.0, 0.0));
        } else if (cur.y < LIFT_VELOCITY) {
            this.crow.setDeltaMovement(cur.add(0.0, LIFT_VELOCITY, 0.0));
        }
    }

    private void land() {
        BlockPos below = this.crow.blockPosition().below();
        if (!this.crow.level().getBlockState(below).getCollisionShape(this.crow.level(), below).isEmpty()) {
            double top = below.getY() + 1.0;
            if (Math.abs(this.crow.getY() - top) < LAND_THRESHOLD) {
                Vec3 cur = this.crow.getDeltaMovement();
                if (cur.y < 0.0) this.crow.setDeltaMovement(cur.add(0.0, 0.12, 0.0));
                this.crow.triggerLandAnimation();
                this.crow.setAirborne(false);
            }
        }
    }

    private void navigateTowards(double x, double y, double z) {
        Vec3 cur = this.crow.getDeltaMovement();
        double dx = x - this.crow.getX();
        double dy = y - this.crow.getY();
        double dz = z - this.crow.getZ();
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.1) return;
        double nx = cur.x + (dx / len) * 0.06;
        double ny = cur.y + (dy / len) * 0.03;
        double nz = cur.z + (dz / len) * 0.06;
        double hSpeed = Math.sqrt(nx * nx + nz * nz);
        if (hSpeed > 0.25) {
            double scale = 0.25 / hSpeed;
            nx *= scale;
            nz *= scale;
        }
        this.crow.setDeltaMovement(nx, ny, nz);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
