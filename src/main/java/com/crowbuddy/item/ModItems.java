package com.crowbuddy.item;

import com.crowbuddy.CrowBuddy;
import net.minecraft.world.item.Item;

public class ModItems {
	public static final Item BLACK_OIL_SUNFLOWER_SEEDS = CrowBuddy.BLACK_OIL_SUNFLOWER_SEEDS;

	public static void register() {
		CrowBuddy.LOGGER.info("Registering Items for " + CrowBuddy.MOD_ID);
	}
}
