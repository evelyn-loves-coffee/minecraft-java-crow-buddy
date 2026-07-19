package com.crowbuddy.block.entity;

import com.crowbuddy.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class CrowNestBlockEntity extends BlockEntity {

    private final CrowNestStateMachine stateMachine = new CrowNestStateMachine();

    public CrowNestBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public CrowNestBlockEntity(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("stage", this.stateMachine.getStage());
        output.putInt("ticksRemaining", this.stateMachine.getTicksRemaining());
        output.putBoolean("babySpawned", this.stateMachine.isBabySpawned());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int loadedStage = input.getIntOr("stage", CrowNestStateMachine.STAGE_IDLE);
        if (loadedStage >= CrowNestStateMachine.STAGE_IDLE && loadedStage <= CrowNestStateMachine.STAGE_BABY_FLYING) {
            this.stateMachine.setStage(loadedStage);
        } else {
            this.stateMachine.setStage(CrowNestStateMachine.STAGE_IDLE);
        }
        this.stateMachine.setTicksRemaining(input.getIntOr("ticksRemaining", 0));
        this.stateMachine.setBabySpawned(input.getBooleanOr("babySpawned", false));
    }

    public void startIncubation() {
        this.stateMachine.startIncubation();
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.playSound(null, this.getBlockPos(), ModSounds.CROW_EGG_LAY,
                SoundSource.NEUTRAL, 0.5f, 1.0f);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CrowNestBlockEntity be) {
        if (be.stateMachine.getStage() == CrowNestStateMachine.STAGE_IDLE) {
            return;
        }
        if (!level.isClientSide() && be.stateMachine.getTicksRemaining() > 0) {
            be.stateMachine.tick();
            if (be.stateMachine.isSideEffectTriggered()) {
                be.advanceStage(level, pos);
                be.setChanged();
                be.stateMachine.resetSideEffectTriggered();
            }
        }
    }

    void advanceStage(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        switch (this.stateMachine.getLastSideEffect()) {
            case EGGS_TO_HATCHING -> {
                serverLevel.playSound(null, pos, ModSounds.CROW_HATCH,
                    SoundSource.NEUTRAL, 0.5f, 1.0f);
                serverLevel.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                        8, 0.3, 0.3, 0.3, 0.0
                );
            }
            case HATCHING_TO_FLEDGLING -> {
                if (!this.stateMachine.isBabySpawned()) {
                    this.spawnBabyCrow(serverLevel, pos);
                }
                serverLevel.playSound(null, pos, ModSounds.CROW_FLEDGLING,
                    SoundSource.NEUTRAL, 0.5f, 1.0f);
                serverLevel.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.CRIT,
                        pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                        12, 0.4, 0.4, 0.4, 0.1
                );
            }
            case BABY_FLYING_TO_IDLE -> {
                serverLevel.playSound(null, pos, ModSounds.CROW_BABY_FLIGHT,
                    SoundSource.NEUTRAL, 0.5f, 1.0f);
                serverLevel.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING,
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                        6, 0.3, 0.3, 0.3, 0.0
                );
            }
            default -> {}
        }
    }

    private void spawnBabyCrow(ServerLevel serverLevel, BlockPos pos) {
        com.crowbuddy.entity.CrowEntity baby = com.crowbuddy.registry.ModEntities.CROW.create(serverLevel, net.minecraft.world.entity.EntitySpawnReason.EVENT);
        if (baby == null) return;
        baby.setPos(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        baby.setBaby(true);
        baby.setAge(net.minecraft.world.entity.AgeableMob.BABY_START_AGE);
        serverLevel.levelEvent(1082, pos, 0);
        serverLevel.addFreshEntity(baby);
    }

    public int getStage() {
        return this.stateMachine.getStage();
    }
}
