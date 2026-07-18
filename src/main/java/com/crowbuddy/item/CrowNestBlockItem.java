package com.crowbuddy.item;

import com.crowbuddy.block.CrowNestBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;

public class CrowNestBlockItem extends BlockItem {
    public CrowNestBlockItem(CrowNestBlock block) {
        super(block, new Item.Properties());
    }

    @Override
    protected boolean canPlace(BlockPlaceContext context, BlockState state) {
        if (!super.canPlace(context, state)) {
            return false;
        }
        BlockPos targetPos = context.getClickedPos().relative(context.getNearestLookingDirection());
        for (Direction dir : Direction.values()) {
            if (context.getLevel().getBlockState(targetPos.relative(dir)).is(BlockTags.LOGS)) {
                return true;
            }
        }
        return false;
    }
}
