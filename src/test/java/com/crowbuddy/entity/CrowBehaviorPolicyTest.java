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
    void groundedNestSeekingFlightUsesHopAnimationOnlyWhileMoving() {
        assertTrue(CrowBehaviorPolicy.shouldHopWhileNestSeeking(true, true, true, true));
        assertFalse(CrowBehaviorPolicy.shouldHopWhileNestSeeking(false, true, true, true));
        assertFalse(CrowBehaviorPolicy.shouldHopWhileNestSeeking(true, false, true, true));
        assertFalse(CrowBehaviorPolicy.shouldHopWhileNestSeeking(true, true, false, true));
        assertFalse(CrowBehaviorPolicy.shouldHopWhileNestSeeking(true, true, true, false));
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
    void breedingCooldownDoesNotPreventOtherwiseEligibleScavenging() {
        assertTrue(CrowBehaviorPolicy.isBreedingCooldown(
            CrowBehaviorPolicy.BREEDING_COOLDOWN_TICKS));
        assertTrue(CrowBehaviorPolicy.canScavenge(false, false, false, 0.05f));
        assertFalse(CrowBehaviorPolicy.canScavenge(true, false, false, 1.0f));
        assertFalse(CrowBehaviorPolicy.canScavenge(false, true, false, 1.0f));
        assertFalse(CrowBehaviorPolicy.canScavenge(false, false, true, 1.0f));
        assertFalse(CrowBehaviorPolicy.canScavenge(false, false, false, 0.049f));
        assertEquals(0.00005f, CrowBehaviorPolicy.SATIATION_DECAY_PER_TICK, 0.000001f);
        assertEquals(0.05f, CrowBehaviorPolicy.MIN_SCAVENGE_SATIATION, 0.0001f);
    }

    @Test
    void carriedItemPaymentRequiresFoodFromTheOwner() {
        assertTrue(CrowBehaviorPolicy.shouldAcceptDeliveryPayment(true, true, true, true));
        assertFalse(CrowBehaviorPolicy.shouldAcceptDeliveryPayment(false, true, true, true));
        assertFalse(CrowBehaviorPolicy.shouldAcceptDeliveryPayment(true, false, true, true));
        assertFalse(CrowBehaviorPolicy.shouldAcceptDeliveryPayment(true, true, false, true));
        assertFalse(CrowBehaviorPolicy.shouldAcceptDeliveryPayment(true, true, true, false));
    }

    @Test
    void paymentPromptRequiresTamedCrowAndNearbyPlayer() {
        assertTrue(CrowBehaviorPolicy.shouldShowPaymentPrompt(true, true, true, 1.0f));
        assertTrue(CrowBehaviorPolicy.shouldShowPaymentPrompt(true, true, false, 0.049f));
        assertFalse(CrowBehaviorPolicy.shouldShowPaymentPrompt(false, true, true, 0.0f));
        assertFalse(CrowBehaviorPolicy.shouldShowPaymentPrompt(true, false, true, 0.0f));
        assertFalse(CrowBehaviorPolicy.shouldShowPaymentPrompt(true, true, false, 0.05f));
        assertEquals(10.0, CrowBehaviorPolicy.PAYMENT_PROMPT_RANGE, 0.0001);
    }

    @Test
    void fullCrowsOnlyAcceptFoodToReleaseACarriedItem() {
        assertFalse(CrowBehaviorPolicy.shouldConsumeFood(1.0f, false));
        assertFalse(CrowBehaviorPolicy.shouldConsumeFood(0.999f, false));
        assertFalse(CrowBehaviorPolicy.shouldConsumeFood(0.751f, false));
        assertTrue(CrowBehaviorPolicy.shouldConsumeFood(0.75f, false));
        assertTrue(CrowBehaviorPolicy.shouldConsumeFood(1.0f, true));
        assertEquals(6.25, CrowBehaviorPolicy.DELIVERY_DISTANCE_SQ, 0.0001);
    }

    @Test
    void scavengingAggregatesStackableItemsWithoutExceedingEight() {
        assertEquals(8, CrowBehaviorPolicy.MAX_SCAVENGE_STACK_SIZE);
        assertEquals(2, CrowBehaviorPolicy.scavengeTransferCount(0, 2, true));
        assertEquals(6, CrowBehaviorPolicy.scavengeTransferCount(2, 10, true));
        assertEquals(0, CrowBehaviorPolicy.scavengeTransferCount(8, 10, true));
        assertEquals(1, CrowBehaviorPolicy.scavengeTransferCount(0, 8, false));
        assertEquals(0, CrowBehaviorPolicy.scavengeTransferCount(1, 8, false));
    }

    @Test
    void scavengingFlightTakesOffCruisesThenDescendsToTheItem() {
        assertEquals(66.5, CrowBehaviorPolicy.scavengeFlightTargetY(0, 25.0, 66.5, 64.0));
        assertEquals(66.5, CrowBehaviorPolicy.scavengeFlightTargetY(12, 25.0, 66.5, 64.0));
        assertEquals(64.25, CrowBehaviorPolicy.scavengeFlightTargetY(12, 0.5, 66.5, 64.0));
        assertTrue(CrowBehaviorPolicy.shouldAscendBeforeScavenging(1, 64.0, 66.25));
        assertTrue(CrowBehaviorPolicy.shouldAscendBeforeScavenging(40, 64.0, 66.25));
        assertFalse(CrowBehaviorPolicy.shouldAscendBeforeScavenging(41, 64.0, 66.25));
        assertFalse(CrowBehaviorPolicy.shouldAscendBeforeScavenging(1, 66.0, 66.25));
        assertTrue(CrowBehaviorPolicy.shouldUseGroundHop(0.5, 9.0));
        assertFalse(CrowBehaviorPolicy.shouldUseGroundHop(0.51, 1.0));
        assertFalse(CrowBehaviorPolicy.shouldUseGroundHop(0.0, 9.01));
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
    void nestConstructionRequiresATamedAdultInTheBreedingFlow() {
        assertTrue(CrowBehaviorPolicy.canBuildNest(true, false, true, false));
        assertFalse(CrowBehaviorPolicy.canBuildNest(false, false, true, false));
        assertFalse(CrowBehaviorPolicy.canBuildNest(true, true, true, false));
        assertFalse(CrowBehaviorPolicy.canBuildNest(true, false, false, false));
        assertFalse(CrowBehaviorPolicy.canBuildNest(true, false, true, true));
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
