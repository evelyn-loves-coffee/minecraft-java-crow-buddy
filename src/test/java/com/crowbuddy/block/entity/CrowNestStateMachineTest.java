package com.crowbuddy.block.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CrowNestStateMachineTest {

    @Test
    public void testFullLifecycle() {
        CrowNestStateMachine stateMachine = new CrowNestStateMachine();

        // 1. Start Incubation
        stateMachine.startIncubation();
        assertEquals(CrowNestStateMachine.STAGE_EGGS, stateMachine.getStage());
        assertEquals(CrowNestStateMachine.INCUBATION_TICKS, stateMachine.getTicksRemaining());

        // 2. Advance to Hatching
        // We simulate all ticks except the last one
        for (int i = 0; i < CrowNestStateMachine.INCUBATION_TICKS - 1; i++) {
            stateMachine.tick();
        }
        assertFalse(stateMachine.isSideEffectTriggered());
        
        stateMachine.tick(); // This should trigger the transition
        
        assertTrue(stateMachine.isSideEffectTriggered());
        assertEquals(CrowNestStateMachine.SideEffectType.EGGS_TO_HATCHING, stateMachine.getLastSideEffect());
        assertEquals(CrowNestStateMachine.STAGE_HATCHING, stateMachine.getStage());
        stateMachine.resetSideEffectTriggered();

        // 3. Advance to Fledgling
        for (int i = 0; i < CrowNestStateMachine.HATCHING_TICKS - 1; i++) {
            stateMachine.tick();
        }
        stateMachine.tick();

        assertTrue(stateMachine.isSideEffectTriggered());
        assertEquals(CrowNestStateMachine.SideEffectType.HATCHING_TO_FLEDGLING, stateMachine.getLastSideEffect());
        assertEquals(CrowNestStateMachine.STAGE_FLEDGLING, stateMachine.getStage());
        stateMachine.resetSideEffectTriggered();

        // 4. Advance to Baby Flying
        for (int i = 0; i < CrowNestStateMachine.FLEDGLING_TICKS - 1; i++) {
            stateMachine.tick();
        }
        stateMachine.tick();

        assertTrue(stateMachine.isSideEffectTriggered());
        assertEquals(CrowNestStateMachine.STAGE_BABY_FLYING, stateMachine.getStage());
        stateMachine.resetSideEffectTriggered();

        // 5. Advance to Idle
        for (int i = 0; i < CrowNestStateMachine.JR_GROWTH_TICKS - 1; i++) {
            stateMachine.tick();
        }
        stateMachine.tick();

        assertEquals(CrowNestStateMachine.STAGE_IDLE, stateMachine.getStage());
        assertEquals(0, stateMachine.getTicksRemaining());
    }

    @Test
    public void testBabySpawnedState() {
        CrowNestStateMachine stateMachine = new CrowNestStateMachine();
        stateMachine.startIncubation();
        
        assertFalse(stateMachine.isBabySpawned());
        
        // Simulate hatching
        for (int i = 0; i < CrowNestStateMachine.INCUBATION_TICKS; i++) {
            stateMachine.tick();
        }
        // Check if we are in hatching stage (which means the first transition just finished)
        // Actually, in advance(): case STAGE_EGGS -> stage = STAGE_HATCHING
        // So after INCUBATION_TICKS, we should be in STAGE_HATCHING.
        assertEquals(CrowNestStateMachine.STAGE_HATCHING, stateMachine.getStage());
        
        // Test manual setting of babySpawned
        stateMachine.setBabySpawned(true);
        assertTrue(stateMachine.isBabySpawned());
    }
}
