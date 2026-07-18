package com.crowbuddy.client;

import com.crowbuddy.CrowBuddy;
import com.crowbuddy.client.networking.ModClientNetworking;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrowBuddyClient implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger(CrowBuddy.MOD_ID + "-client");

	@Override
	public void onInitializeClient() {
		LOGGER.info("Crow Buddy client initializing.");
		ModClientEntities.register();
		ModClientNetworking.registerReceivers();

		// Phase 4: BlockEntity renderer for CrowNestBlockEntity
		// registerNestRenderer();

		// Phase 4: Baby crow model (GeckoLib baby variant)
		// registerBabyModel();

		// Phase 4: Particle type registration (if custom particles needed)
		// registerParticles();
	}
}
