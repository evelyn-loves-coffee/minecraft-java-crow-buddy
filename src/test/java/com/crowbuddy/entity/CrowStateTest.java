package com.crowbuddy.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CrowStateTest {

    @Test
    void testAllStateIds() {
        assertEquals(0, CrowState.IDLE.stateId());
        assertEquals(1, CrowState.SEARCHING.stateId());
        assertEquals(2, CrowState.CARRYING.stateId());
        assertEquals(3, CrowState.COMBAT.stateId());
        assertEquals(4, CrowState.DISTRESS.stateId());
        assertEquals(5, CrowState.SWARM.stateId());
        assertEquals(6, CrowState.NESTING.stateId());
    }

    @Test
    void testFromStateIdValid() {
        assertEquals(CrowState.IDLE, CrowState.fromStateId(0));
        assertEquals(CrowState.SEARCHING, CrowState.fromStateId(1));
        assertEquals(CrowState.CARRYING, CrowState.fromStateId(2));
        assertEquals(CrowState.COMBAT, CrowState.fromStateId(3));
        assertEquals(CrowState.DISTRESS, CrowState.fromStateId(4));
        assertEquals(CrowState.SWARM, CrowState.fromStateId(5));
        assertEquals(CrowState.NESTING, CrowState.fromStateId(6));
    }

    @Test
    void testFromStateIdInvalidReturnsIdle() {
        assertEquals(CrowState.IDLE, CrowState.fromStateId(-1));
        assertEquals(CrowState.IDLE, CrowState.fromStateId(7));
        assertEquals(CrowState.IDLE, CrowState.fromStateId(99));
        assertEquals(CrowState.IDLE, CrowState.fromStateId(Integer.MAX_VALUE));
        assertEquals(CrowState.IDLE, CrowState.fromStateId(Integer.MIN_VALUE));
    }

    @Test
    void testStateIdRoundTrip() {
        for (CrowState state : CrowState.values()) {
            assertEquals(state, CrowState.fromStateId(state.stateId()));
        }
    }

    @Test
    void testAllStateIdsUnique() {
        CrowState[] values = CrowState.values();
        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {
                assertNotEquals(values[i].stateId(), values[j].stateId(),
                    values[i] + " and " + values[j] + " share the same stateId");
            }
        }
    }
}
