package com.crowbuddy.entity.ai.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Conservative flight clearance: foliage, fluids, and collision shapes are obstacles. */
public final class DefaultTerrainSampler implements TerrainSampler {
    @Override
    public boolean isPassable(Level level, BlockPos pos) {
        if (!level.getWorldBorder().isWithinBounds(pos) || pos.getY() < level.getMinY()
                || pos.getY() >= level.getMaxY()) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return state.getFluidState().isEmpty()
            && !state.is(BlockTags.LEAVES)
            && state.getCollisionShape(level, pos).isEmpty();
    }
}
