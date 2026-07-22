package com.crowbuddy.item;

import com.crowbuddy.CrowBuddy;
import com.crowbuddy.registry.ModBlocks;
import com.crowbuddy.registry.ModEntities;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

public class ModItems {
	private static final ResourceKey<CreativeModeTab> NATURAL_BLOCKS_TAB = ResourceKey.create(
		Registries.CREATIVE_MODE_TAB,
		Identifier.fromNamespaceAndPath("minecraft", "natural_blocks")
	);
	public static final Item BLACK_OIL_SUNFLOWER_SEEDS = new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, CrowBuddy.id("black_oil_sunflower_seeds"))));
	public static final Item BLACK_FEATHER = new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, CrowBuddy.id("black_feather"))));
	public static final BlockItem CROW_NEST_ITEM = new CrowNestBlockItem(ModBlocks.CROW_NEST, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, CrowBuddy.id("crow_nest"))));
	public static final SpawnEggItem CROW_SPAWN_EGG = new SpawnEggItem(
		new Item.Properties()
			.setId(ResourceKey.create(Registries.ITEM, CrowBuddy.id("crow_spawn_egg")))
			.spawnEgg(ModEntities.CROW)
	);

	public static void register() {
		CrowBuddy.LOGGER.info("Registering Items for " + CrowBuddy.MOD_ID);
		net.minecraft.core.Registry.register(BuiltInRegistries.ITEM, CrowBuddy.id("black_oil_sunflower_seeds"), BLACK_OIL_SUNFLOWER_SEEDS);
		net.minecraft.core.Registry.register(BuiltInRegistries.ITEM, CrowBuddy.id("black_feather"), BLACK_FEATHER);
		net.minecraft.core.Registry.register(BuiltInRegistries.ITEM, CrowBuddy.id("crow_nest"), CROW_NEST_ITEM);
		net.minecraft.core.Registry.register(BuiltInRegistries.ITEM, CrowBuddy.id("crow_spawn_egg"), CROW_SPAWN_EGG);
		CreativeModeTabEvents.modifyOutputEvent(NATURAL_BLOCKS_TAB)
			.register(output -> output.accept(CROW_NEST_ITEM));
	}
}
