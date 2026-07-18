package com.crowbuddy.block;

import com.crowbuddy.block.entity.CrowNestBlockEntity;
import com.crowbuddy.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class CrowNestBlock extends net.minecraft.world.level.block.Block implements EntityBlock {
    public CrowNestBlock() {
        super(BlockBehaviour.Properties.of()
                .noCollision()
                .strength(0.3f)
                .noOcclusion()
                .sound(SoundType.WOOD)
                .mapColor(MapColor.WOOD)
        );
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter viewer, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrowNestBlockEntity(ModBlocks.getCrowNestBE(), pos, state);
    }
}
