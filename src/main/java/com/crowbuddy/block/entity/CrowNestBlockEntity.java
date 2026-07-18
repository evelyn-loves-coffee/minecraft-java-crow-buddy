package com.crowbuddy.block.entity;

import com.crowbuddy.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class CrowNestBlockEntity extends BlockEntity {

    static final int INCUBATION_TICKS = 12000;
    static final int JR_GROWTH_TICKS = 24000;
    static final int HATCHING_TICKS = 100;
    static final int FLEDGLING_TICKS = 200;

    public static final int STAGE_IDLE = 0;
    public static final int STAGE_EGGS = 1;
    public static final int STAGE_HATCHING = 2;
    public static final int STAGE_FLEDGLING = 3;
    public static final int STAGE_BABY_FLYING = 4;

    int stage;
    int ticksRemaining;
    boolean babySpawned;

    public CrowNestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.getCrowNestBE(), pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("stage", this.stage);
        output.putInt("ticksRemaining", this.ticksRemaining);
        output.putBoolean("babySpawned", this.babySpawned);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int loadedStage = input.getIntOr("stage", STAGE_IDLE);
        if (loadedStage >= STAGE_IDLE && loadedStage <= STAGE_BABY_FLYING) {
            this.stage = loadedStage;
        } else {
            this.stage = STAGE_IDLE;
        }
        this.ticksRemaining = input.getIntOr("ticksRemaining", 0);
        this.babySpawned = input.getBooleanOr("babySpawned", false);
    }

    public void setStage(int stage) {
        if (stage < STAGE_IDLE || stage > STAGE_BABY_FLYING) {
            this.stage = STAGE_IDLE;
            return;
        }
        if (this.stage != stage) {
            this.stage = stage;
            this.setChanged();
        }
    }

    public int getStage() {
        return this.stage;
    }

    public void startIncubation() {
        if (this.stage != STAGE_IDLE) {
            return;
        }
        this.stage = STAGE_EGGS;
        this.ticksRemaining = INCUBATION_TICKS;
        this.babySpawned = false;
        this.setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CrowNestBlockEntity be) {
        if (be.stage == STAGE_IDLE) {
            return;
        }
        if (!level.isClientSide() && be.ticksRemaining > 0) {
            be.ticksRemaining--;
            if (be.ticksRemaining <= 0) {
                be.advanceStage(level, pos);
                be.setChanged();
            }
        }
    }

    void advanceStage(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        switch (this.stage) {
            case STAGE_EGGS -> {
                this.stage = STAGE_HATCHING;
                this.ticksRemaining = HATCHING_TICKS;
                serverLevel.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                        8, 0.3, 0.3, 0.3, 0.0
                );
            }
            case STAGE_HATCHING -> {
                this.stage = STAGE_FLEDGLING;
                this.ticksRemaining = FLEDGLING_TICKS;
                if (!this.babySpawned) {
                    this.babySpawned = spawnBabyCrow(serverLevel, pos);
                }
                serverLevel.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.CRIT,
                        pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                        12, 0.4, 0.4, 0.4, 0.1
                );
            }
            case STAGE_FLEDGLING -> {
                this.stage = STAGE_BABY_FLYING;
                this.ticksRemaining = JR_GROWTH_TICKS;
            }
            case STAGE_BABY_FLYING -> {
                this.stage = STAGE_IDLE;
                this.ticksRemaining = 0;
                this.babySpawned = false;
                serverLevel.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING,
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                        6, 0.3, 0.3, 0.3, 0.0
                );
            }
            default -> {
                this.stage = STAGE_IDLE;
                this.ticksRemaining = 0;
                this.babySpawned = false;
            }
        }
    }

    private boolean spawnBabyCrow(ServerLevel serverLevel, BlockPos pos) {
        com.crowbuddy.entity.CrowEntity baby = com.crowbuddy.registry.ModEntities.CROW.create(serverLevel, net.minecraft.world.entity.EntitySpawnReason.EVENT);
        if (baby == null) return false;
        baby.setPos(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        baby.setBaby(true);
        baby.setAge(net.minecraft.world.entity.AgeableMob.BABY_START_AGE);
        serverLevel.levelEvent(1082, pos, 0);
        return serverLevel.addFreshEntity(baby);
    }
}
