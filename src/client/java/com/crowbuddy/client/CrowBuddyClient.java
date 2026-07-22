package com.crowbuddy.client;

import com.crowbuddy.client.networking.ModClientNetworking;
import com.crowbuddy.item.ModItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.world.item.CreativeModeTabs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrowBuddyClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("crowbuddy-client");

    @Override
    public void onInitializeClient() {
        LOGGER.info("Crow Buddy client initializing.");
        ModClientEntities.register();
        ModClientNetworking.registerReceivers();

        // Tiny Takeover: register items in creative tabs
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS)
            .register(output -> output.prepend(ModItems.CROW_SPAWN_EGG));

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)
            .register(output -> {
                output.prepend(ModItems.BLACK_FEATHER);
                output.prepend(ModItems.BLACK_OIL_SUNFLOWER_SEEDS);
            });

    }
}
