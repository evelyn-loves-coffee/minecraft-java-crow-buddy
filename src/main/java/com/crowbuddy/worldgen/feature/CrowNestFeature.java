package com.crowbuddy.worldgen.feature;

import com.crowbuddy.registry.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class CrowNestFeature extends Feature<NoneFeatureConfiguration> {

    public CrowNestFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();

        if (origin.getY() < 60) {
            return false;
        }

        if (!level.getBlockState(origin).is(BlockTags.LOGS)) {
            return false;
        }

        Direction randomDirection = Direction.Plane.HORIZONTAL.getRandomDirection(context.random());
        BlockPos targetPos = origin.relative(randomDirection);

        BlockState targetState = level.getBlockState(targetPos);
        if (!targetState.isAir() && !targetState.is(BlockTags.REPLACEABLE)) {
            return false;
        }

        BlockState nestState = ModBlocks.CROW_NEST.defaultBlockState();
        setBlock(level, targetPos, nestState);
        return true;
    }
}
