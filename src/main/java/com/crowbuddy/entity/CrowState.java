package com.crowbuddy.entity;

public enum CrowState {
	IDLE(0),
	SEARCHING(1),
	CARRYING(2),
	COMBAT(3),
	DISTRESS(4),
	SWARM(5),
	NESTING(6);

	private final int stateId;

	CrowState(int stateId) {
		this.stateId = stateId;
	}

	public int stateId() {
		return this.stateId;
	}

	public static CrowState fromStateId(int id) {
		for (CrowState state : values()) {
			if (state.stateId == id) {
				return state;
			}
		}
		return IDLE;
	}
}
