package com.crowbuddy.entity.ai.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

@FunctionalInterface
public interface TerrainSampler {
    boolean isPassable(Level level, BlockPos pos);
}
