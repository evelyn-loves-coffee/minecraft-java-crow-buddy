package com.crowbuddy.entity;

/** Pure behavior thresholds kept separate so state decisions can be unit tested. */
public final class CrowBehaviorPolicy {
    /** Vanilla Animal breeding cooldown: five minutes at 20 ticks per second. */
    public static final int BREEDING_COOLDOWN_TICKS = 6000;
    public static final int TAMING_CHANCE_DENOMINATOR = 3;
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

    public static int scavengeCooldownTicks(float satiation) {
        if (satiation >= 0.8f) return 2400;
        if (satiation >= 0.5f) return 1200;
        return 600;
    }

    public static float clampRelationship(float relationship) {
        return Math.max(-1.0f, Math.min(1.0f, relationship));
    }

    public static boolean canEnterLoveMode(boolean tame, boolean baby, int age,
                                           boolean alreadyInLove, boolean availableNest) {
        return tame && !baby && age == 0 && !alreadyInLove && availableNest;
    }
}
