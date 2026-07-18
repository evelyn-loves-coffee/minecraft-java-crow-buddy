package com.crowbuddy.block.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CrowNestStateMachineEdgeCasesTest {

    @Test
    void testTickAtIdleDoesNothing() {
        CrowNestStateMachine sm = new CrowNestStateMachine();
        for (int i = 0; i < 100; i++) {
            sm.tick();
        }
        assertEquals(CrowNestStateMachine.STAGE_IDLE, sm.getStage());
        assertEquals(0, sm.getTicksRemaining());
    }

    @Test
    void testStartIncubationFromNonIdleIsNoOp() {
        CrowNestStateMachine sm = new CrowNestStateMachine();
        sm.startIncubation();
        assertEquals(CrowNestStateMachine.STAGE_EGGS, sm.getStage());
        int savedTicks = sm.getTicksRemaining();

        sm.startIncubation();
        assertEquals(CrowNestStateMachine.STAGE_EGGS, sm.getStage());
        assertEquals(savedTicks, sm.getTicksRemaining());
    }

    @Test
    void testSideEffectReset() {
        CrowNestStateMachine sm = new CrowNestStateMachine();
        sm.startIncubation();
        for (int i = 0; i < CrowNestStateMachine.INCUBATION_TICKS; i++) {
            sm.tick();
        }
        assertTrue(sm.isSideEffectTriggered());
        sm.resetSideEffectTriggered();
        assertFalse(sm.isSideEffectTriggered());
    }

    @Test
    void testAdvanceFromInvalidStageResets() {
        CrowNestStateMachine sm = new CrowNestStateMachine();
        sm.setStage(999);
        sm.setTicksRemaining(1);
        sm.tick();
        assertEquals(CrowNestStateMachine.STAGE_IDLE, sm.getStage());
        assertEquals(0, sm.getTicksRemaining());
        assertFalse(sm.isBabySpawned());
        assertEquals(CrowNestStateMachine.SideEffectType.NONE, sm.getLastSideEffect());
    }

    @Test
    void testBabySpawnedFlagResetOnIdleTransition() {
        CrowNestStateMachine sm = new CrowNestStateMachine();
        sm.startIncubation();
        sm.setBabySpawned(true);

        for (int i = 0; i < CrowNestStateMachine.INCUBATION_TICKS; i++) sm.tick();
        sm.resetSideEffectTriggered();
        for (int i = 0; i < CrowNestStateMachine.HATCHING_TICKS; i++) sm.tick();
        sm.resetSideEffectTriggered();
        for (int i = 0; i < CrowNestStateMachine.FLEDGLING_TICKS; i++) sm.tick();
        sm.resetSideEffectTriggered();
        for (int i = 0; i < CrowNestStateMachine.JR_GROWTH_TICKS; i++) sm.tick();

        assertEquals(CrowNestStateMachine.STAGE_IDLE, sm.getStage());
        assertFalse(sm.isBabySpawned());
    }

    @Test
    void testPartialTicksDontTriggerTransition() {
        CrowNestStateMachine sm = new CrowNestStateMachine();
        sm.startIncubation();
        for (int i = 0; i < CrowNestStateMachine.INCUBATION_TICKS - 1; i++) {
            sm.tick();
        }
        assertEquals(CrowNestStateMachine.STAGE_EGGS, sm.getStage());
        assertEquals(1, sm.getTicksRemaining());
        assertFalse(sm.isSideEffectTriggered());
    }

    @Test
    void testSetStageDirectly() {
        CrowNestStateMachine sm = new CrowNestStateMachine();
        sm.setStage(CrowNestStateMachine.STAGE_HATCHING);
        assertEquals(CrowNestStateMachine.STAGE_HATCHING, sm.getStage());
    }

    @Test
    void testSetTicksRemaining() {
        CrowNestStateMachine sm = new CrowNestStateMachine();
        sm.setTicksRemaining(500);
        assertEquals(500, sm.getTicksRemaining());
    }

    @Test
    void testConstantValues() {
        assertEquals(0, CrowNestStateMachine.STAGE_IDLE);
        assertEquals(1, CrowNestStateMachine.STAGE_EGGS);
        assertEquals(2, CrowNestStateMachine.STAGE_HATCHING);
        assertEquals(3, CrowNestStateMachine.STAGE_FLEDGLING);
        assertEquals(4, CrowNestStateMachine.STAGE_BABY_FLYING);
        assertEquals(12000, CrowNestStateMachine.INCUBATION_TICKS);
        assertEquals(24000, CrowNestStateMachine.JR_GROWTH_TICKS);
        assertEquals(100, CrowNestStateMachine.HATCHING_TICKS);
        assertEquals(200, CrowNestStateMachine.FLEDGLING_TICKS);
    }

    @Test
    void testSideEffectTypes() {
        assertEquals(0, CrowNestStateMachine.SideEffectType.NONE.ordinal());
        assertEquals(1, CrowNestStateMachine.SideEffectType.EGGS_TO_HATCHING.ordinal());
        assertEquals(2, CrowNestStateMachine.SideEffectType.HATCHING_TO_FLEDGLING.ordinal());
        assertEquals(3, CrowNestStateMachine.SideEffectType.BABY_FLYING_TO_IDLE.ordinal());
    }

    @Test
    void testFledglingToBabyFlyingHasNoNamedSideEffect() {
        CrowNestStateMachine sm = new CrowNestStateMachine();
        sm.startIncubation();
        for (int i = 0; i < CrowNestStateMachine.INCUBATION_TICKS; i++) sm.tick();
        sm.resetSideEffectTriggered();
        for (int i = 0; i < CrowNestStateMachine.HATCHING_TICKS; i++) sm.tick();
        sm.resetSideEffectTriggered();
        for (int i = 0; i < CrowNestStateMachine.FLEDGLING_TICKS; i++) sm.tick();

        assertTrue(sm.isSideEffectTriggered());
        assertEquals(CrowNestStateMachine.SideEffectType.NONE, sm.getLastSideEffect());
        assertEquals(CrowNestStateMachine.STAGE_BABY_FLYING, sm.getStage());
    }
}
