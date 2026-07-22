package com.crowbuddy.block.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CrowNestStateMachineTest {
    @Test
    void fullLifecycleEndsWithHatchCompletion() {
        CrowNestStateMachine stateMachine = new CrowNestStateMachine();
        stateMachine.startIncubation();

        for (int i = 0; i < CrowNestStateMachine.INCUBATION_TICKS; i++) stateMachine.tick();
        assertEquals(CrowNestStateMachine.STAGE_HATCHING, stateMachine.getStage());
        assertEquals(CrowNestStateMachine.SideEffectType.EGGS_TO_HATCHING,
            stateMachine.getLastSideEffect());

        stateMachine.resetSideEffectTriggered();
        for (int i = 0; i < CrowNestStateMachine.HATCHING_TICKS; i++) stateMachine.tick();
        assertEquals(CrowNestStateMachine.STAGE_IDLE, stateMachine.getStage());
        assertEquals(0, stateMachine.getTicksRemaining());
        assertTrue(stateMachine.isSideEffectTriggered());
        assertEquals(CrowNestStateMachine.SideEffectType.HATCH_COMPLETE,
            stateMachine.getLastSideEffect());
    }

    @Test
    void incubationCannotRestartWhileActive() {
        CrowNestStateMachine stateMachine = new CrowNestStateMachine();
        stateMachine.startIncubation();
        stateMachine.tick();
        int remaining = stateMachine.getTicksRemaining();
        stateMachine.startIncubation();
        assertEquals(remaining, stateMachine.getTicksRemaining());
    }
}
