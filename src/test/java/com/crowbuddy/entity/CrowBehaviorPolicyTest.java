package com.crowbuddy.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CrowBehaviorPolicyTest {
    @Test
    void collisionDriftDoesNotSelectWalkAnimation() {
        assertFalse(CrowBehaviorPolicy.isIntentionalGroundMovement(false, 0.0025));
        assertFalse(CrowBehaviorPolicy.isIntentionalGroundMovement(true, 0.25));
        assertTrue(CrowBehaviorPolicy.isIntentionalGroundMovement(false, 0.0026));
    }

    @Test
    void flightYawTracksHorizontalVelocity() {
        assertEquals(-90.0f, CrowBehaviorPolicy.flightYawDegrees(1.0, 0.0, 45.0f), 0.001f);
        assertEquals(0.0f, CrowBehaviorPolicy.flightYawDegrees(0.0, 1.0, 45.0f), 0.001f);
        assertEquals(-45.0f, CrowBehaviorPolicy.flightYawDegrees(1.0, 1.0, 45.0f), 0.001f);
        assertEquals(45.0f, CrowBehaviorPolicy.flightYawDegrees(0.0, 0.0, 45.0f), 0.001f);
    }

    @Test
    void terrainPreferenceFavorsOpenSkyThenElevation() {
        assertTrue(CrowBehaviorPolicy.terrainPreferenceScore(true, -5.0)
            > CrowBehaviorPolicy.terrainPreferenceScore(false, 20.0));
        assertTrue(CrowBehaviorPolicy.terrainPreferenceScore(true, 4.0)
            > CrowBehaviorPolicy.terrainPreferenceScore(true, 1.0));
    }

    @Test
    void airborneCrowsHaveAgeAppropriateMinimumForwardSpeed() {
        assertEquals(0.10, CrowBehaviorPolicy.minimumFlightSpeed(true), 0.0001);
        assertEquals(0.14, CrowBehaviorPolicy.minimumFlightSpeed(false), 0.0001);
        assertTrue(CrowBehaviorPolicy.minimumFlightSpeed(false)
            > CrowBehaviorPolicy.minimumFlightSpeed(true));
    }

    @Test
    void onlyUntamedAdultsCanAttemptTaming() {
        assertTrue(CrowBehaviorPolicy.canAttemptTaming(false, false, true));
        assertFalse(CrowBehaviorPolicy.canAttemptTaming(true, false, true));
        assertFalse(CrowBehaviorPolicy.canAttemptTaming(false, true, true));
        assertFalse(CrowBehaviorPolicy.canAttemptTaming(false, false, false));
        assertEquals(3, CrowBehaviorPolicy.TAMING_CHANCE_DENOMINATOR);
    }

    @Test
    void foodSpeedsUpBabyGrowthInsteadOfTaming() {
        assertTrue(CrowBehaviorPolicy.shouldSpeedUpGrowth(true, true));
        assertFalse(CrowBehaviorPolicy.shouldSpeedUpGrowth(false, true));
        assertFalse(CrowBehaviorPolicy.shouldSpeedUpGrowth(true, false));
        assertFalse(CrowBehaviorPolicy.canAttemptTaming(true, false, true));
        assertTrue(CrowBehaviorPolicy.shouldEmitGrowthParticles(true, true));
        assertFalse(CrowBehaviorPolicy.shouldEmitGrowthParticles(true, false));
        assertFalse(CrowBehaviorPolicy.shouldEmitGrowthParticles(false, true));
    }

    @Test
    void positiveAgeIdentifiesBreedingCooldownWithoutBlockingBabies() {
        assertTrue(CrowBehaviorPolicy.isBreedingCooldown(1));
        assertTrue(CrowBehaviorPolicy.isBreedingCooldown(
            CrowBehaviorPolicy.BREEDING_COOLDOWN_TICKS));
        assertFalse(CrowBehaviorPolicy.isBreedingCooldown(0));
        assertFalse(CrowBehaviorPolicy.isBreedingCooldown(-1));
    }

    @Test
    void hungrierCrowsSearchSooner() {
        assertEquals(2400, CrowBehaviorPolicy.scavengeCooldownTicks(1.0f));
        assertEquals(1200, CrowBehaviorPolicy.scavengeCooldownTicks(0.6f));
        assertEquals(600, CrowBehaviorPolicy.scavengeCooldownTicks(0.35f));
    }

    @Test
    void relationshipIsBounded() {
        assertEquals(-1.0f, CrowBehaviorPolicy.clampRelationship(-5.0f));
        assertEquals(0.25f, CrowBehaviorPolicy.clampRelationship(0.25f));
        assertEquals(1.0f, CrowBehaviorPolicy.clampRelationship(5.0f));
    }

    @Test
    void breedingRequiresVanillaAdultReadyState() {
        assertTrue(CrowBehaviorPolicy.canEnterLoveMode(true, false, 0, false, true));
        assertFalse(CrowBehaviorPolicy.canEnterLoveMode(false, false, 0, false, true));
        assertFalse(CrowBehaviorPolicy.canEnterLoveMode(true, true, -24000, false, true));
        assertFalse(CrowBehaviorPolicy.canEnterLoveMode(true, false,
            CrowBehaviorPolicy.BREEDING_COOLDOWN_TICKS, false, true));
        assertFalse(CrowBehaviorPolicy.canEnterLoveMode(true, false, 0, true, true));
        assertFalse(CrowBehaviorPolicy.canEnterLoveMode(true, false, 0, false, false));
        assertEquals(6000, CrowBehaviorPolicy.BREEDING_COOLDOWN_TICKS);
    }

    @Test
    void nestTramplingRequiresAnEligibleEntityAndSuccessfulRoll() {
        assertTrue(CrowBehaviorPolicy.canTrampleNest(false, true, false, true, false));
        assertTrue(CrowBehaviorPolicy.canTrampleNest(false, true, false, false, true));
        assertFalse(CrowBehaviorPolicy.canTrampleNest(true, true, false, true, true));
        assertFalse(CrowBehaviorPolicy.canTrampleNest(false, true, true, false, true));
        assertFalse(CrowBehaviorPolicy.canTrampleNest(false, true, false, false, false));
        assertFalse(CrowBehaviorPolicy.canTrampleNest(false, false, false, false, true));
        assertTrue(CrowBehaviorPolicy.trampleRollSucceeds(0));
        assertFalse(CrowBehaviorPolicy.trampleRollSucceeds(1));
    }
}
