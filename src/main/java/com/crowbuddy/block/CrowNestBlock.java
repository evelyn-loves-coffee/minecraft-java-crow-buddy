package com.crowbuddy.block;

import com.crowbuddy.CrowBuddy;
import com.crowbuddy.block.entity.CrowNestBlockEntity;
import com.crowbuddy.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class CrowNestBlock extends net.minecraft.world.level.block.Block implements EntityBlock {
    public static final BooleanProperty HAS_EGGS = BooleanProperty.create("has_eggs");
    public static final BooleanProperty HATCHING = BooleanProperty.create("hatching");

    public CrowNestBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(0.3f)
                .noOcclusion()
                .sound(SoundType.WOOD)
                .mapColor(MapColor.WOOD)
                .noLootTable()
                .setId(ResourceKey.create(Registries.BLOCK, CrowBuddy.id("crow_nest")))
        );
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(HAS_EGGS, false)
            .setValue(HATCHING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(HAS_EGGS, HATCHING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter viewer, BlockPos pos, CollisionContext context) {
        return net.minecraft.world.level.block.Block.box(1.0, 0.0, 1.0, 15.0, 5.0, 15.0);
    }

    @Override
    protected boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(net.minecraft.tags.BlockTags.LEAVES);
    }

    @Override
    public void stepOn(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, Entity entity) {
        this.tryTrample(level, pos, state, entity, 100);
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void fallOn(net.minecraft.world.level.Level level, BlockState state, BlockPos pos,
                       Entity entity, double fallDistance) {
        this.tryTrample(level, pos, state, entity, 3);
        super.fallOn(level, state, pos, entity, fallDistance);
    }

    private void tryTrample(net.minecraft.world.level.Level level, BlockPos pos, BlockState state,
                            Entity entity, int chance) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel) || !state.is(this)) {
            return;
        }
        boolean player = entity instanceof Player;
        boolean mobGriefing = serverLevel.getGameRules().get(
            net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING);
        if (!com.crowbuddy.entity.CrowBehaviorPolicy.canTrampleNest(
                entity.isSteppingCarefully(), entity instanceof LivingEntity,
                entity instanceof com.crowbuddy.entity.CrowEntity, player, mobGriefing)
                || !com.crowbuddy.entity.CrowBehaviorPolicy.trampleRollSucceeds(
                    level.getRandom().nextInt(chance))) {
            return;
        }
        level.playSound(null, pos, SoundEvents.TURTLE_EGG_BREAK, SoundSource.BLOCKS,
            0.7f, 0.9f + level.getRandom().nextFloat() * 0.2f);
        CrowBuddy.LOGGER.debug("Crow nest at {} trampled by {}", pos, entity.getType());
        level.destroyBlock(pos, false);
    }

    @Override
    protected BlockState updateShape(BlockState state, net.minecraft.world.level.LevelReader level,
                                     net.minecraft.world.level.ScheduledTickAccess scheduledTickAccess,
                                     BlockPos pos, net.minecraft.core.Direction direction,
                                     BlockPos neighborPos, BlockState neighborState,
                                     net.minecraft.util.RandomSource random) {
        return direction == net.minecraft.core.Direction.DOWN && !state.canSurvive(level, pos)
            ? Blocks.AIR.defaultBlockState()
            : super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrowNestBlockEntity(ModBlocks.getCrowNestBE(), pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            net.minecraft.world.level.Level level, BlockState state, BlockEntityType<T> type) {
        if (type != ModBlocks.getCrowNestBE()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) ->
            CrowNestBlockEntity.tick(tickerLevel, pos, tickerState, (CrowNestBlockEntity) blockEntity);
    }
}
