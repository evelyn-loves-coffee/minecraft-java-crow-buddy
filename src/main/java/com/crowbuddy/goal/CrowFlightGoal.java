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
        if (this.crow.isInSittingPose() || this.crow.isOrderedToSit()
                || this.crow.isInLove() || this.crow.isInMatingState()) return false;
        if (this.crow.level().getGameTime() < this.nextFlightTick) return false;
        this.followTarget = distantOwner();
        this.threat = recentThreat();
        if (this.followTarget != null || this.threat != null) return true;
        if (!this.crow.getCarriedItem().isEmpty()) return false;
        if (this.crow.getRandom().nextInt(RANDOM_FLIGHT_CHANCE) != 0) return false;
        this.randomTarget = randomFlightTarget();
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return this.crow.isAirborne() && !this.crow.isInSittingPose() && !this.crow.isOrderedToSit()
            && !this.crow.isInLove() && !this.crow.isInMatingState();
    }

    @Override
    public void start() {
        this.flightTicks = 0;
        this.takeoffY = this.crow.getY();
        this.landing = false;
        if (this.threat != null) {
            double x = this.crow.getX() + (this.crow.getX() - this.threat.getX());
            double z = this.crow.getZ() + (this.crow.getZ() - this.threat.getZ());
            this.crow.getCrowNavigator().navigateTo(this.crow,
                new Vec3(x, this.takeoffY + relativeHeightLimit() * 0.6, z), 1.0);
        } else if (this.followTarget != null) {
            this.crow.getCrowNavigator().navigateTo(this.crow, this.followTarget, 1.0);
        } else if (this.randomTarget != null) {
            this.crow.getCrowNavigator().navigateTo(this.crow, this.randomTarget, 1.0);
        }
    }

    @Override
    public void tick() {
        this.flightTicks++;
        int maxTicks = this.crow.isBaby() ? BABY_MAX_FLIGHT_TICKS : ADULT_MAX_FLIGHT_TICKS;
        if (!this.landing && (this.flightTicks >= maxTicks || hasArrived() || !hasValidPurpose())) {
            this.landing = true;
            this.crow.getCrowNavigator().clear(this.crow);
        }

        if (this.landing) {
            descend();
        }

        if (this.crow.onGround() && this.flightTicks > 4) {
            finishLanding();
        }
    }

    @Override
    public void stop() {
        this.crow.getCrowNavigator().clear(this.crow);
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
        double startDistanceSq = !this.crow.getCarriedItem().isEmpty()
            ? CrowBehaviorPolicy.DELIVERY_DISTANCE_SQ
            : (this.crow.isBaby() ? 64.0 : 144.0);
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
            double stopDistanceSq = !this.crow.getCarriedItem().isEmpty()
                ? CrowBehaviorPolicy.DELIVERY_DISTANCE_SQ
                : (this.crow.isBaby() ? 25.0 : 36.0);
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

    private double relativeHeightLimit() {
        return this.crow.isBaby() ? 4.0 : 8.0;
    }

    private void descend() {
        Vec3 current = this.crow.getDeltaMovement();
        this.crow.setDeltaMovement(current.x * 0.92, Math.max(current.y - 0.04, -0.22), current.z * 0.92);
    }

    private void finishLanding() {
        if (!this.crow.isAirborne()) return;
        this.crow.getCrowNavigator().clear(this.crow);
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
