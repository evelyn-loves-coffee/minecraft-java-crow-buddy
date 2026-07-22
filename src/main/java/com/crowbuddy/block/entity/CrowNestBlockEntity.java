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
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int loadedStage = input.getIntOr("stage", CrowNestStateMachine.STAGE_IDLE);
        if (loadedStage >= CrowNestStateMachine.STAGE_IDLE
                && loadedStage <= CrowNestStateMachine.LEGACY_STAGE_BABY_FLYING) {
            this.stateMachine.setStage(loadedStage);
        } else {
            this.stateMachine.setStage(CrowNestStateMachine.STAGE_IDLE);
        }
        this.stateMachine.setTicksRemaining(input.getIntOr("ticksRemaining", 0));
    }

    public void startIncubation() {
        this.stateMachine.startIncubation();
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.syncEggAppearance();
            this.level.playSound(null, this.getBlockPos(), ModSounds.CROW_EGG_LAY,
                SoundSource.NEUTRAL, 0.5f, 1.0f);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CrowNestBlockEntity be) {
        if (!level.isClientSide()
                && be.stateMachine.getStage() >= CrowNestStateMachine.LEGACY_STAGE_FLEDGLING) {
            level.removeBlock(pos, false);
            return;
        }
        if (!level.isClientSide()) {
            be.syncEggAppearance();
        }
        if (be.stateMachine.getStage() == CrowNestStateMachine.STAGE_IDLE) {
            return;
        }
        if (!level.isClientSide() && be.stateMachine.getTicksRemaining() > 0) {
            be.stateMachine.tick();
            if (be.stateMachine.isSideEffectTriggered()) {
                be.advanceStage(level, pos);
                if (level.getBlockEntity(pos) == be) {
                    be.setChanged();
                }
                be.stateMachine.resetSideEffectTriggered();
            }
        }
    }

    private void syncEggAppearance() {
        if (this.level == null) return;
        BlockState state = this.getBlockState();
        if (!state.hasProperty(com.crowbuddy.block.CrowNestBlock.HAS_EGGS)
                || !state.hasProperty(com.crowbuddy.block.CrowNestBlock.HATCHING)) return;
        boolean shouldShowEggs = this.stateMachine.getStage() == CrowNestStateMachine.STAGE_EGGS;
        boolean shouldShowCrackedEgg =
            this.stateMachine.getStage() == CrowNestStateMachine.STAGE_HATCHING;
        if (state.getValue(com.crowbuddy.block.CrowNestBlock.HAS_EGGS) != shouldShowEggs
                || state.getValue(com.crowbuddy.block.CrowNestBlock.HATCHING) != shouldShowCrackedEgg) {
            this.level.setBlock(this.getBlockPos(),
                state.setValue(com.crowbuddy.block.CrowNestBlock.HAS_EGGS, shouldShowEggs)
                    .setValue(com.crowbuddy.block.CrowNestBlock.HATCHING, shouldShowCrackedEgg),
                net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }

    void advanceStage(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        switch (this.stateMachine.getLastSideEffect()) {
            case EGGS_TO_HATCHING -> {
                this.syncEggAppearance();
                serverLevel.playSound(null, pos, ModSounds.CROW_HATCH,
                    SoundSource.NEUTRAL, 0.5f, 1.0f);
                serverLevel.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                        8, 0.3, 0.3, 0.3, 0.0
                );
            }
            case HATCH_COMPLETE -> {
                if (!this.spawnBabyCrow(serverLevel, pos)) {
                    this.stateMachine.setStage(CrowNestStateMachine.STAGE_HATCHING);
                    this.stateMachine.setTicksRemaining(1);
                    return;
                }
                serverLevel.playSound(null, pos, ModSounds.CROW_FLEDGLING,
                    SoundSource.NEUTRAL, 0.5f, 1.0f);
                serverLevel.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.CRIT,
                        pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                        12, 0.4, 0.4, 0.4, 0.1
                );
                serverLevel.removeBlock(pos, false);
            }
            default -> {}
        }
    }

    private boolean spawnBabyCrow(ServerLevel serverLevel, BlockPos pos) {
        com.crowbuddy.entity.CrowEntity baby = com.crowbuddy.registry.ModEntities.CROW.create(serverLevel, net.minecraft.world.entity.EntitySpawnReason.EVENT);
        if (baby == null) return false;
        baby.setPos(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        baby.setBaby(true);
        baby.setAge(net.minecraft.world.entity.AgeableMob.BABY_START_AGE);
        if (!serverLevel.addFreshEntity(baby)) return false;
        serverLevel.levelEvent(1082, pos, 0);
        return true;
    }

    public int getStage() {
        return this.stateMachine.getStage();
    }
}
