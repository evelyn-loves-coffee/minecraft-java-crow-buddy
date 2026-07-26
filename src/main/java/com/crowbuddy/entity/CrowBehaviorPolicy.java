package com.crowbuddy.entity;

/** Pure behavior thresholds kept separate so state decisions can be unit tested. */
public final class CrowBehaviorPolicy {
    /** Vanilla Animal breeding cooldown: five minutes at 20 ticks per second. */
    public static final int BREEDING_COOLDOWN_TICKS = 6000;
    public static final int TAMING_CHANCE_DENOMINATOR = 3;
    public static final int MAX_SCAVENGE_STACK_SIZE = 8;
    public static final int MAX_SCAVENGE_TAKEOFF_TICKS = 40;
    public static final double DELIVERY_DISTANCE_SQ = 6.25;
    public static final double PAYMENT_PROMPT_RANGE = 10.0;
    public static final float SATIATION_DECAY_PER_TICK = 0.00005f;
    public static final float MIN_SCAVENGE_SATIATION = 0.05f;
    /** A seed restores 0.25 satiation, so reject it when any of that benefit would overflow. */
    public static final float MAX_SATIATION_BEFORE_FEEDING = 0.75f;
    private static final double MOVEMENT_THRESHOLD_SQ = 0.0025;
    private static final double FLIGHT_ROTATION_THRESHOLD_SQ = 1.0E-6;

    private CrowBehaviorPolicy() {}

    public static boolean isIntentionalGroundMovement(boolean navigationDone, double horizontalSpeedSq) {
        return !navigationDone && horizontalSpeedSq > MOVEMENT_THRESHOLD_SQ;
    }

    /** Minecraft yaw corresponding to a horizontal velocity vector. */
    public static float flightYawDegrees(double velocityX, double velocityZ, float currentYaw) {
        if (velocityX * velocityX + velocityZ * velocityZ <= FLIGHT_ROTATION_THRESHOLD_SQ) {
            return currentYaw;
        }
        return (float) (Math.toDegrees(Math.atan2(velocityZ, velocityX)) - 90.0);
    }

    public static double terrainPreferenceScore(boolean openSky, double elevationChange) {
        return (openSky ? 32.0 : -32.0) + elevationChange;
    }

    public static double minimumFlightSpeed(boolean baby) {
        return baby ? 0.10 : 0.14;
    }

    /** Nest-seeking flight can briefly remain ground-contacting while taking off or clearing terrain. */
    public static boolean shouldHopWhileNestSeeking(boolean mating, boolean airborne,
                                                     boolean onGround, boolean moving) {
        return mating && airborne && onGround && moving;
    }

    public static boolean canAttemptTaming(boolean baby, boolean tame, boolean tamingFood) {
        return !baby && !tame && tamingFood;
    }

    public static boolean shouldSpeedUpGrowth(boolean baby, boolean food) {
        return baby && food;
    }

    public static boolean shouldEmitGrowthParticles(boolean babyGrowth, boolean consumedFood) {
        return babyGrowth && consumedFood;
    }

    /** Positive age is vanilla's post-breeding cooldown; negative age denotes a baby. */
    public static boolean isBreedingCooldown(int age) {
        return age > 0;
    }

    public static boolean canScavenge(boolean baby, boolean sitting, boolean carrying,
                                      float satiation) {
        return !baby && !sitting && !carrying && satiation >= MIN_SCAVENGE_SATIATION;
    }

    public static int scavengeTransferCount(int carriedCount, int availableCount,
                                            boolean stackable) {
        int limit = stackable ? MAX_SCAVENGE_STACK_SIZE : 1;
        return Math.max(0, Math.min(limit - carriedCount, availableCount));
    }

    public static boolean shouldAcceptDeliveryPayment(boolean carrying, boolean tame,
                                                       boolean ownedByPlayer, boolean food) {
        return carrying && tame && ownedByPlayer && food;
    }

    public static boolean shouldShowPaymentPrompt(boolean tame, boolean playerNearby,
                                                   boolean carrying, float satiation) {
        return tame && playerNearby
            && (carrying || satiation < MIN_SCAVENGE_SATIATION);
    }

    public static boolean shouldConsumeFood(float satiation, boolean carrying) {
        return carrying || satiation <= MAX_SATIATION_BEFORE_FEEDING;
    }

    public static double scavengeFlightTargetY(int seekTicks, double horizontalDistanceSq,
                                               double flightAltitude, double itemY) {
        if (seekTicks < 12) return flightAltitude;
        if (horizontalDistanceSq > 0.64) return Math.max(itemY + 1.25, flightAltitude);
        return itemY + 0.25;
    }

    public static boolean shouldAscendBeforeScavenging(int seekTicks, double currentY,
                                                        double flightAltitude) {
        return seekTicks <= MAX_SCAVENGE_TAKEOFF_TICKS
            && currentY < flightAltitude - 0.25;
    }

    public static boolean shouldUseGroundHop(double verticalDifference,
                                             double horizontalDistanceSq) {
        return Math.abs(verticalDifference) <= 0.5 && horizontalDistanceSq <= 9.0;
    }

    public static float clampRelationship(float relationship) {
        return Math.max(-1.0f, Math.min(1.0f, relationship));
    }

    public static boolean canEnterLoveMode(boolean tame, boolean baby, int age,
                                           boolean alreadyInLove, boolean buildSiteAvailable) {
        return tame && !baby && age == 0 && !alreadyInLove && buildSiteAvailable;
    }

    public static boolean canTrampleNest(boolean steppingCarefully, boolean livingEntity,
                                         boolean crow, boolean player, boolean mobGriefing) {
        return !steppingCarefully && livingEntity && !crow && (player || mobGriefing);
    }

    public static boolean trampleRollSucceeds(int roll) {
        return roll == 0;
    }
}
