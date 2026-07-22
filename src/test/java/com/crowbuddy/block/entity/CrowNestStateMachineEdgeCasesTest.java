package com.crowbuddy.block.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CrowNestStateMachineEdgeCasesTest {
    @Test
    void idleTicksDoNothing() {
        CrowNestStateMachine stateMachine = new CrowNestStateMachine();
        for (int i = 0; i < 100; i++) stateMachine.tick();
        assertEquals(CrowNestStateMachine.STAGE_IDLE, stateMachine.getStage());
        assertEquals(0, stateMachine.getTicksRemaining());
    }

    @Test
    void partialIncubationDoesNotTransition() {
        CrowNestStateMachine stateMachine = new CrowNestStateMachine();
        stateMachine.startIncubation();
        for (int i = 0; i < CrowNestStateMachine.INCUBATION_TICKS - 1; i++) stateMachine.tick();
        assertEquals(CrowNestStateMachine.STAGE_EGGS, stateMachine.getStage());
        assertEquals(1, stateMachine.getTicksRemaining());
        assertFalse(stateMachine.isSideEffectTriggered());
    }

    @Test
    void invalidActiveStageResetsSafely() {
        CrowNestStateMachine stateMachine = new CrowNestStateMachine();
        stateMachine.setStage(999);
        stateMachine.setTicksRemaining(1);
        stateMachine.tick();
        assertEquals(CrowNestStateMachine.STAGE_IDLE, stateMachine.getStage());
        assertEquals(CrowNestStateMachine.SideEffectType.NONE, stateMachine.getLastSideEffect());
    }

    @Test
    void sideEffectCanBeAcknowledged() {
        CrowNestStateMachine stateMachine = new CrowNestStateMachine();
        stateMachine.startIncubation();
        for (int i = 0; i < CrowNestStateMachine.INCUBATION_TICKS; i++) stateMachine.tick();
        assertTrue(stateMachine.isSideEffectTriggered());
        stateMachine.resetSideEffectTriggered();
        assertFalse(stateMachine.isSideEffectTriggered());
    }

    @Test
    void activeConstantsRemainStableForSavedData() {
        assertEquals(0, CrowNestStateMachine.STAGE_IDLE);
        assertEquals(1, CrowNestStateMachine.STAGE_EGGS);
        assertEquals(2, CrowNestStateMachine.STAGE_HATCHING);
        assertEquals(12000, CrowNestStateMachine.INCUBATION_TICKS);
        assertEquals(100, CrowNestStateMachine.HATCHING_TICKS);
    }
}
