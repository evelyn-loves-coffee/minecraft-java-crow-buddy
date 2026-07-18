package com.crowbuddy.registry;

import com.crowbuddy.CrowBuddy;
import com.crowbuddy.block.CrowNestBlock;
import com.crowbuddy.block.entity.CrowNestBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Set;

public class ModBlocks {
    public static final CrowNestBlock CROW_NEST = new CrowNestBlock();

    private static BlockEntityType<CrowNestBlockEntity> crowNestBE;

    public static BlockEntityType<CrowNestBlockEntity> getCrowNestBE() {
        if (crowNestBE == null) {
            crowNestBE = new BlockEntityType<>(CrowNestBlockEntity::new, Set.of(CROW_NEST));
        }
        return crowNestBE;
    }

    public static void register() {
        CrowBuddy.LOGGER.info("Registering Blocks for " + CrowBuddy.MOD_ID);
        net.minecraft.core.Registry.register(BuiltInRegistries.BLOCK, CrowBuddy.id("crow_nest"), CROW_NEST);
        net.minecraft.core.Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, CrowBuddy.id("crow_nest"), getCrowNestBE());
    }
}
