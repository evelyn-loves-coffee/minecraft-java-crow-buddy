package com.crowbuddy.block.entity;

public class CrowNestStateMachine {
    public static final int STAGE_IDLE = 0;
    public static final int STAGE_EGGS = 1;
    public static final int STAGE_HATCHING = 2;
    static final int LEGACY_STAGE_FLEDGLING = 3;
    static final int LEGACY_STAGE_BABY_FLYING = 4;

    public static final int INCUBATION_TICKS = 12000;
    public static final int HATCHING_TICKS = 100;

    private int stage = STAGE_IDLE;
    private int ticksRemaining = 0;
    
    private boolean sideEffectTriggered = false;
    private SideEffectType lastSideEffect = SideEffectType.NONE;

    public enum SideEffectType {
        NONE,
        EGGS_TO_HATCHING,
        HATCH_COMPLETE
    }

    public void startIncubation() {
        if (this.stage == STAGE_IDLE) {
            this.stage = STAGE_EGGS;
            this.ticksRemaining = INCUBATION_TICKS;
            this.sideEffectTriggered = false;
        }
    }

    public void tick() {
        if (this.stage == STAGE_IDLE) return;

        if (this.ticksRemaining > 0) {
            this.ticksRemaining--;
            if (this.ticksRemaining <= 0) {
                advance();
            }
        }
    }

    private void advance() {
        switch (this.stage) {
            case STAGE_EGGS -> {
                this.stage = STAGE_HATCHING;
                this.ticksRemaining = HATCHING_TICKS;
                this.lastSideEffect = SideEffectType.EGGS_TO_HATCHING;
                this.sideEffectTriggered = true;
            }
            case STAGE_HATCHING -> {
                this.stage = STAGE_IDLE;
                this.ticksRemaining = 0;
                this.lastSideEffect = SideEffectType.HATCH_COMPLETE;
                this.sideEffectTriggered = true;
            }
            default -> {
                this.stage = STAGE_IDLE;
                this.ticksRemaining = 0;
                this.sideEffectTriggered = false;
                this.lastSideEffect = SideEffectType.NONE;
            }
        }
    }

    public int getStage() {
        return this.stage;
    }

    public void setStage(int stage) {
        this.stage = stage;
    }

    public int getTicksRemaining() {
        return this.ticksRemaining;
    }

    public void setTicksRemaining(int ticksRemaining) {
        this.ticksRemaining = ticksRemaining;
    }

    public boolean isSideEffectTriggered() {
        return this.sideEffectTriggered;
    }

    public void resetSideEffectTriggered() {
        this.sideEffectTriggered = false;
    }

    public SideEffectType getLastSideEffect() {
        return this.lastSideEffect;
    }
}
