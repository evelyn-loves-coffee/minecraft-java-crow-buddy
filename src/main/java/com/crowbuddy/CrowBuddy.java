package com.crowbuddy;

import com.crowbuddy.item.ModItems;
import com.crowbuddy.registry.ModEntities;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrowBuddy implements ModInitializer {
	public static final String MOD_ID = "crowbuddy";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Hello Crow Buddy world!");
		ModEntities.register();
		ModItems.register();
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}

