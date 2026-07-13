package com.crowbuddy.item;

import com.crowbuddy.CrowBuddy;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

public class ModItems {
	public static final Item BLACK_OIL_SUNFLOWER_SEEDS = new Item(new Item.Properties());

	public static void register() {
		CrowBuddy.LOGGER.info("Registering Items for " + CrowBuddy.MOD_ID);
		net.minecraft.core.Registry.register(BuiltInRegistries.ITEM, CrowBuddy.id("black_oil_sunflower_seeds"), BLACK_OIL_SUNFLOWER_SEEDS);
	}
}
