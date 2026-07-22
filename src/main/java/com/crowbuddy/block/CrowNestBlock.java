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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class CrowNestBlock extends net.minecraft.world.level.block.Block implements EntityBlock {
    public static final BooleanProperty HAS_EGGS = BooleanProperty.create("has_eggs");

    public CrowNestBlock() {
        super(BlockBehaviour.Properties.of()
                .noCollision()
                .strength(0.3f)
                .noOcclusion()
                .sound(SoundType.WOOD)
                .mapColor(MapColor.WOOD)
                .noLootTable()
                .setId(ResourceKey.create(Registries.BLOCK, CrowBuddy.id("crow_nest")))
        );
        this.registerDefaultState(this.stateDefinition.any().setValue(HAS_EGGS, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(HAS_EGGS);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter viewer, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(net.minecraft.tags.BlockTags.LOGS);
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
