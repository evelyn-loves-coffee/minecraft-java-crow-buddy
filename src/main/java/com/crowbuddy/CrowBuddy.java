package com.crowbuddy;

import com.crowbuddy.event.CrowEventHub;
import com.crowbuddy.item.ModItems;
import com.crowbuddy.networking.ModNetworking;
import com.crowbuddy.registry.ModBlocks;
import com.crowbuddy.registry.ModEntities;
import com.crowbuddy.sound.ModSounds;
import com.crowbuddy.swarm.SwarmManager;
import com.crowbuddy.worldgen.CrowSpawning;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
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
		ModBlocks.register();
		ModItems.register();
		ModSounds.register();
		ModNetworking.registerPayloads();
		CrowEventHub.registerEvents();
		CrowSpawning.initialize();
		ServerLevelEvents.UNLOAD.register(SwarmManager::remove);
		ServerLevelEvents.UNLOAD.register((server, level) -> com.crowbuddy.goal.ScavengeRegistry.remove(level));
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
