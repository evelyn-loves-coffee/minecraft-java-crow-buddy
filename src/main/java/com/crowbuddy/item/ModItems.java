package com.crowbuddy.item;

import com.crowbuddy.CrowBuddy;
import com.crowbuddy.registry.ModEntities;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

public class ModItems {
	public static final Item BLACK_OIL_SUNFLOWER_SEEDS = new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, CrowBuddy.id("black_oil_sunflower_seeds"))));
	public static final Item BLACK_FEATHER = new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, CrowBuddy.id("black_feather"))));
	public static final SpawnEggItem CROW_SPAWN_EGG = new SpawnEggItem(
		new Item.Properties()
			.setId(ResourceKey.create(Registries.ITEM, CrowBuddy.id("crow_spawn_egg")))
			.spawnEgg(ModEntities.CROW)
	);

	public static void register() {
		CrowBuddy.LOGGER.info("Registering Items for " + CrowBuddy.MOD_ID);
		net.minecraft.core.Registry.register(BuiltInRegistries.ITEM, CrowBuddy.id("black_oil_sunflower_seeds"), BLACK_OIL_SUNFLOWER_SEEDS);
		net.minecraft.core.Registry.register(BuiltInRegistries.ITEM, CrowBuddy.id("black_feather"), BLACK_FEATHER);
		net.minecraft.core.Registry.register(BuiltInRegistries.ITEM, CrowBuddy.id("crow_spawn_egg"), CROW_SPAWN_EGG);
	}
}
