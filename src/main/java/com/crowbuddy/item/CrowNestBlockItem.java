package com.crowbuddy.item;

import com.crowbuddy.block.CrowNestBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;

public class CrowNestBlockItem extends BlockItem {
    public CrowNestBlockItem(CrowNestBlock block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPos clickedPos = context.getClickedPos();
        if (context.getLevel().getBlockState(clickedPos).is(BlockTags.LOGS)) {
            // Retarget from the original hit, never from BlockPlaceContext's derived position.
            BlockPlaceContext baseContext = new BlockPlaceContext(context);
            return this.place(BlockPlaceContext.at(baseContext, clickedPos, Direction.UP));
        }
        return super.useOn(context);
    }
}
