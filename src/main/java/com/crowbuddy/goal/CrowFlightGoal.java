package com.crowbuddy.goal;

import com.crowbuddy.entity.CrowEntity;
import com.crowbuddy.entity.CrowBehaviorPolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Age-profiled flight controller with explicit takeoff, cruise, and landing phases. */
public final class CrowFlightGoal extends Goal {
    private static final int ADULT_MAX_FLIGHT_TICKS = 200;
    private static final int BABY_MAX_FLIGHT_TICKS = 120;
    private static final int ADULT_COOLDOWN_TICKS = 40;
    private static final int BABY_COOLDOWN_TICKS = 60;
    private static final int RANDOM_FLIGHT_CHANCE = 200;

    private final CrowEntity crow;
    private LivingEntity followTarget;
    private LivingEntity threat;
    private long nextFlightTick;
    private int flightTicks;
    private double takeoffY;
    private boolean landing;
    private Vec3 randomTarget;

    public CrowFlightGoal(CrowEntity crow) {
        this.crow = crow;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (this.crow.isInSittingPose()
                || this.crow.isInLove() || this.crow.isInMatingState()) return false;
        if (this.crow.level().getGameTime() < this.nextFlightTick) return false;
        this.followTarget = distantOwner();
        this.threat = recentThreat();
        if (this.followTarget != null || this.threat != null) return true;
        if (this.crow.getRandom().nextInt(RANDOM_FLIGHT_CHANCE) != 0) return false;
        this.randomTarget = randomFlightTarget();
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return this.crow.isAirborne() && !this.crow.isInSittingPose()
            && !this.crow.isInLove() && !this.crow.isInMatingState();
    }

    @Override
    public void start() {
        this.crow.getNavigation().stop();
        this.flightTicks = 0;
        this.takeoffY = this.crow.getY();
        this.landing = false;
        Vec3 motion = this.crow.getDeltaMovement();
        Vec3 horizontal = new Vec3(motion.x, 0, motion.z);
        if (horizontal.horizontalDistanceSqr() < 0.0025) {
            horizontal = Vec3.directionFromRotation(0, this.crow.getYRot()).scale(0.12);
        } else {
            horizontal = horizontal.normalize().scale(this.crow.isBaby() ? 0.12 : 0.16);
        }
        this.crow.setDeltaMovement(horizontal.x, this.crow.isBaby() ? 0.30 : 0.42, horizontal.z);
        this.crow.setAirborne(true);
        this.crow.triggerTakeoffAnimation();
    }

    @Override
    public void tick() {
        this.flightTicks++;
        int maxTicks = this.crow.isBaby() ? BABY_MAX_FLIGHT_TICKS : ADULT_MAX_FLIGHT_TICKS;
        if (!this.landing && (this.flightTicks >= maxTicks || hasArrived() || !hasValidPurpose())) {
            this.landing = true;
        }

        if (this.landing) {
            descend();
        } else if (this.threat != null) {
            steerAwayFrom(this.threat);
        } else if (this.followTarget != null) {
            steerTowards(this.followTarget.getX(), this.followTarget.getY() + 1.25, this.followTarget.getZ());
        } else if (this.randomTarget != null) {
            steerTowards(this.randomTarget.x, this.randomTarget.y, this.randomTarget.z);
        }

        if (this.crow.onGround() && this.flightTicks > 4) {
            finishLanding();
        }
    }

    @Override
    public void stop() {
        finishLanding();
        this.followTarget = null;
        this.threat = null;
        this.randomTarget = null;
        this.landing = false;
        this.nextFlightTick = this.crow.level().getGameTime()
            + (this.crow.isBaby() ? BABY_COOLDOWN_TICKS : ADULT_COOLDOWN_TICKS);
    }

    private LivingEntity distantOwner() {
        LivingEntity owner = this.crow.getOwner();
        if (owner == null || !owner.isAlive()) return null;
        double startDistanceSq = this.crow.isBaby() ? 64.0 : 144.0;
        return this.crow.distanceToSqr(owner) > startDistanceSq ? owner : null;
    }

    private LivingEntity recentThreat() {
        LivingEntity attacker = this.crow.getLastHurtByMob();
        if (attacker == null || !attacker.isAlive() || attacker == this.crow.getOwner()) return null;
        if (this.crow.tickCount - this.crow.getLastHurtByMobTimestamp() > 100) return null;
        return this.crow.distanceToSqr(attacker) <= 64.0 ? attacker : null;
    }

    private boolean hasValidPurpose() {
        if (this.threat != null && this.threat.isAlive() && this.crow.distanceToSqr(this.threat) < 144.0) return true;
        return (this.followTarget != null && this.followTarget.isAlive()) || this.randomTarget != null;
    }

    private boolean hasArrived() {
        if (this.followTarget != null) {
            double stopDistanceSq = this.crow.isBaby() ? 25.0 : 36.0;
            return this.crow.distanceToSqr(this.followTarget) <= stopDistanceSq;
        }
        return this.randomTarget != null && this.crow.position().distanceToSqr(this.randomTarget) <= 4.0;
    }

    private Vec3 randomFlightTarget() {
        BlockPos bestSurface = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = this.crow.getRandom().nextDouble() * Math.PI * 2.0;
            double distance = 8.0 + this.crow.getRandom().nextDouble() * 8.0;
            int x = net.minecraft.util.Mth.floor(this.crow.getX() + Math.cos(angle) * distance);
            int z = net.minecraft.util.Mth.floor(this.crow.getZ() + Math.sin(angle) * distance);
            int y = this.crow.level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            BlockPos surface = new BlockPos(x, y, z);
            double score = CrowBehaviorPolicy.terrainPreferenceScore(
                this.crow.level().canSeeSky(surface), y - this.crow.getY());
            if (score > bestScore) {
                bestSurface = surface;
                bestScore = score;
            }
        }
        if (bestSurface == null) {
            bestSurface = this.crow.blockPosition();
        }
        double height = (this.crow.isBaby() ? 2.0 : 3.0)
            + this.crow.getRandom().nextDouble() * (this.crow.isBaby() ? 2.0 : 3.0);
        return Vec3.atBottomCenterOf(bestSurface).add(0.0, height, 0.0);
    }

    private void steerAwayFrom(LivingEntity entity) {
        double x = this.crow.getX() + (this.crow.getX() - entity.getX());
        double z = this.crow.getZ() + (this.crow.getZ() - entity.getZ());
        steerTowards(x, this.takeoffY + relativeHeightLimit() * 0.6, z);
    }

    private void steerTowards(double x, double y, double z) {
        Vec3 delta = new Vec3(x - this.crow.getX(), y - this.crow.getY(), z - this.crow.getZ());
        if (delta.lengthSqr() < 0.01) return;
        Vec3 desired = delta.normalize().scale(this.crow.isBaby() ? 0.20 : 0.32);
        Vec3 current = this.crow.getDeltaMovement();
        Vec3 next = current.scale(0.78).add(desired.scale(0.22));
        double ceiling = this.takeoffY + relativeHeightLimit();
        if (this.crow.getY() > ceiling) next = new Vec3(next.x, Math.min(next.y, -0.04), next.z);
        this.crow.setDeltaMovement(next);
    }

    private double relativeHeightLimit() {
        return this.crow.isBaby() ? 4.0 : 8.0;
    }

    private void descend() {
        Vec3 current = this.crow.getDeltaMovement();
        this.crow.setDeltaMovement(current.x * 0.92, Math.max(current.y - 0.04, -0.22), current.z * 0.92);
    }

    private void finishLanding() {
        if (!this.crow.isAirborne()) return;
        this.crow.setAirborne(false);
        this.crow.triggerLandAnimation();
        Vec3 current = this.crow.getDeltaMovement();
        this.crow.setDeltaMovement(current.x * 0.4, current.y, current.z * 0.4);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
