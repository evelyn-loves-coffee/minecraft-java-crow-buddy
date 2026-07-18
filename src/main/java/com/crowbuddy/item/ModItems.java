package com.crowbuddy.item;

import com.crowbuddy.CrowBuddy;
import com.crowbuddy.registry.ModBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public class ModItems {
	public static final Item BLACK_OIL_SUNFLOWER_SEEDS = new Item(new Item.Properties());
	public static final Item BLACK_FEATHER = new Item(new Item.Properties());
	public static final BlockItem CROW_NEST_ITEM = new CrowNestBlockItem(ModBlocks.CROW_NEST);

	public static void register() {
		CrowBuddy.LOGGER.info("Registering Items for " + CrowBuddy.MOD_ID);
		net.minecraft.core.Registry.register(BuiltInRegistries.ITEM, CrowBuddy.id("black_oil_sunflower_seeds"), BLACK_OIL_SUNFLOWER_SEEDS);
		net.minecraft.core.Registry.register(BuiltInRegistries.ITEM, CrowBuddy.id("black_feather"), BLACK_FEATHER);
		net.minecraft.core.Registry.register(BuiltInRegistries.ITEM, CrowBuddy.id("crow_nest"), CROW_NEST_ITEM);
	}
}
